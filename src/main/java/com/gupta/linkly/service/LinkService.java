package com.gupta.linkly.service;

import com.gupta.linkly.analytics.ClickEventPublisher;
import com.gupta.linkly.dto.AnalyticsResponse;
import com.gupta.linkly.dto.DashboardResponse;
import com.gupta.linkly.dto.LinkRequest;
import com.gupta.linkly.dto.LinkResponse;
import com.gupta.linkly.entity.Link;
import com.gupta.linkly.entity.User;
import com.gupta.linkly.exception.BadRequestException;
import com.gupta.linkly.exception.DuplicateResourceException;
import com.gupta.linkly.exception.ResourceNotFoundException;
import com.gupta.linkly.redis.CacheKeys;
import com.gupta.linkly.redis.RedisJsonCache;
import com.gupta.linkly.repository.ClickAnalyticsRepository;
import com.gupta.linkly.repository.LinkRepository;
import com.gupta.linkly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LinkService {

    private static final int ANALYTICS_DAYS = 30;

    private final LinkRepository linkRepository;
    private final UserRepository userRepository;
    private final ClickAnalyticsRepository analyticsRepository;
    private final UrlShortenerService urlShortenerService;
    private final ClickEventPublisher clickEventPublisher;
    private final RedisJsonCache cache;

    @Value("${app.links.max-per-user:200}")
    private int maxLinksPerUser = 200;

    /** Cached redirect data. A record, so it serialises cleanly to JSON in Redis. */
    public record RedirectTarget(UUID linkId, String originalUrl) {
    }

    // ------------------------------------------------------------------ owner CRUD

    @Transactional
    public LinkResponse addLink(String username, LinkRequest request) {
        User user = getUser(username);
        if (linkRepository.countByUserId(user.getId()) >= maxLinksPerUser) {
            throw new BadRequestException("You can have at most " + maxLinksPerUser + " links");
        }
        String alias = AuthService.blankToNull(request.getCustomAlias());
        String shortUrl;
        if (alias != null) {
            if (linkRepository.existsByShortUrl(alias)) {
                throw new DuplicateResourceException("This alias is already taken");
            }
            shortUrl = alias;
        } else {
            shortUrl = urlShortenerService.generateShortUrl();
        }
        Link link = Link.builder()
                .title(request.getTitle().trim())
                .originalUrl(request.getOriginalUrl().trim())
                .shortUrl(shortUrl)
                .active(request.getActive() == null || request.getActive())
                .clickCount(0L)
                // New links appear at the top of the list.
                .sortOrder(linkRepository.findMinSortOrder(user.getId()) - 1)
                .user(user)
                .build();
        linkRepository.saveAndFlush(link); // flush so a unique-constraint race surfaces here as 409
        evictProfile(username);
        return LinkResponse.from(link);
    }

    public List<LinkResponse> getAllLinks(String username) {
        User user = getUser(username);
        return linkRepository.findByUserIdOrderBySortOrderAscCreatedAtDesc(user.getId()).stream()
                .map(LinkResponse::from)
                .toList();
    }

    @Transactional
    public LinkResponse updateLink(String username, UUID linkId, LinkRequest request) {
        Link link = getOwnedLink(linkId, username);
        link.setTitle(request.getTitle().trim());
        link.setOriginalUrl(request.getOriginalUrl().trim());
        if (request.getActive() != null) {
            link.setActive(request.getActive());
        }
        cache.evictAfterCommit(List.of(CacheKeys.redirect(link.getShortUrl()), CacheKeys.profile(username)));
        return LinkResponse.from(link);
    }

    @Transactional
    public void deleteLink(String username, UUID linkId) {
        Link link = getOwnedLink(linkId, username);
        linkRepository.delete(link); // click rows are removed by ON DELETE CASCADE
        cache.evictAfterCommit(List.of(CacheKeys.redirect(link.getShortUrl()), CacheKeys.profile(username)));
    }

    /** Ids not owned by the user are ignored; links not mentioned keep their relative order after these. */
    @Transactional
    public void reorderLinks(String username, List<UUID> orderedIds) {
        User user = getUser(username);
        Map<UUID, Link> owned = linkRepository.findByUserIdOrderBySortOrderAscCreatedAtDesc(user.getId()).stream()
                .collect(Collectors.toMap(Link::getId, Function.identity()));
        int position = 0;
        for (UUID id : new LinkedHashSet<>(orderedIds)) {
            Link link = owned.remove(id);
            if (link != null) {
                link.setSortOrder(position++);
            }
        }
        for (Link remaining : owned.values()) {
            remaining.setSortOrder(position++);
        }
        evictProfile(username); // dirty checking writes the changes on commit
    }

    public DashboardResponse getDashboard(String username) {
        User user = getUser(username);
        return DashboardResponse.builder()
                .totalLinks(linkRepository.countByUserId(user.getId()))
                .totalClicks(linkRepository.sumClickCountByUserId(user.getId()))
                .build();
    }

    // ------------------------------------------------------------------ redirect hot path

    /**
     * Resolves a short code and records the click asynchronously.
     * Redis hit: zero database queries. Redis miss or Redis down: one indexed query.
     * The click is queued, never written on this request thread.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String resolveRedirect(String shortUrl, String clientIp, String userAgent) {
        String key = CacheKeys.redirect(shortUrl);
        RedirectTarget target = cache.get(key, RedirectTarget.class).orElseGet(() -> {
            LinkRepository.RedirectTarget row = linkRepository.findRedirectTarget(shortUrl)
                    .orElseThrow(() -> new ResourceNotFoundException("Link not found"));
            if (!Boolean.TRUE.equals(row.getActive()) || Boolean.TRUE.equals(row.getOwnerSuspended())) {
                throw new ResourceNotFoundException("Link is inactive");
            }
            RedirectTarget fresh = new RedirectTarget(row.getId(), row.getOriginalUrl());
            cache.put(key, fresh, CacheKeys.REDIRECT_TTL);
            return fresh;
        });
        clickEventPublisher.publish(target.linkId(), clientIp, userAgent);
        return target.originalUrl();
    }

    // ------------------------------------------------------------------ analytics

    public AnalyticsResponse getLinkAnalytics(String username, UUID linkId) {
        Link link = getOwnedLink(linkId, username);
        LocalDate today = LocalDate.now();
        LocalDate firstDay = today.minusDays(ANALYTICS_DAYS - 1L);

        Map<LocalDate, Long> perDay = new HashMap<>();
        for (Object[] row : analyticsRepository.countPerDaySince(linkId, firstDay.atStartOfDay())) {
            perDay.put(toLocalDate(row[0]), ((Number) row[1]).longValue());
        }
        List<AnalyticsResponse.DailyClicks> series = new ArrayList<>(ANALYTICS_DAYS);
        for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
            series.add(new AnalyticsResponse.DailyClicks(day, perDay.getOrDefault(day, 0L)));
        }

        return AnalyticsResponse.builder()
                .totalClicks(link.getClickCount())
                .uniqueVisitors(analyticsRepository.countUniqueVisitors(linkId))
                .clicksByCountry(toMap(analyticsRepository.countByCountry(linkId)))
                .clicksByDevice(toMap(analyticsRepository.countByDevice(linkId)))
                .clicksByBrowser(toMap(analyticsRepository.countByBrowser(linkId)))
                .clicksOverTime(series)
                .build();
    }

    private static Map<String, Long> toMap(List<ClickAnalyticsRepository.LabelCount> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        rows.forEach(r -> map.put(r.getLabel(), r.getCount()));
        return map;
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate date) return date;
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        if (value instanceof LocalDateTime dateTime) return dateTime.toLocalDate();
        return LocalDate.parse(String.valueOf(value));
    }

    // ------------------------------------------------------------------ helpers

    private void evictProfile(String username) {
        cache.evictAfterCommit(List.of(CacheKeys.profile(username)));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /** "Not yours" and "doesn't exist" both return 404, so link ids cannot be probed. */
    private Link getOwnedLink(UUID linkId, String username) {
        return linkRepository.findByIdAndUserUsername(linkId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found"));
    }
}
