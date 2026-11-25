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
import com.gupta.linkly.repository.ClickAnalyticsRepository;
import com.gupta.linkly.repository.LinkRepository;
import com.gupta.linkly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
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

    @Value("${app.links.max-per-user:200}")
    private int maxLinksPerUser = 200;

    // ------------------------------------------------------------------ owner CRUD

    @Transactional
    public LinkResponse addLink(UUID accountId, LinkRequest request) {
        requireRedirectableUrl(request.getOriginalUrl());
        User user = getUserForUpdate(accountId);
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
        return LinkResponse.from(link);
    }

    public List<LinkResponse> getAllLinks(UUID accountId) {
        return linkRepository.findByUserIdOrderBySortOrderAscCreatedAtDesc(accountId).stream()
                .map(LinkResponse::from)
                .toList();
    }

    @Transactional
    public LinkResponse updateLink(UUID accountId, UUID linkId, LinkRequest request) {
        requireRedirectableUrl(request.getOriginalUrl());
        getUserForUpdate(accountId);
        Link link = getOwnedLink(linkId, accountId);
        link.setTitle(request.getTitle().trim());
        link.setOriginalUrl(request.getOriginalUrl().trim());
        if (request.getActive() != null) {
            link.setActive(request.getActive());
        }
        return LinkResponse.from(link);
    }

    @Transactional
    public void deleteLink(UUID accountId, UUID linkId) {
        getUserForUpdate(accountId);
        Link link = getOwnedLink(linkId, accountId);
        linkRepository.delete(link); // click rows are removed by ON DELETE CASCADE
    }

    /** Ids not owned by the user are ignored; links not mentioned keep their relative order after these. */
    @Transactional
    public void reorderLinks(UUID accountId, List<UUID> orderedIds) {
        User user = getUserForUpdate(accountId);
        Map<UUID, Link> owned = linkRepository.findByUserIdOrderBySortOrderAscCreatedAtDesc(user.getId()).stream()
                .collect(Collectors.toMap(Link::getId, Function.identity(), (first, second) -> first, LinkedHashMap::new));
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
    }

    public DashboardResponse getDashboard(UUID accountId) {
        return DashboardResponse.builder()
                .totalLinks(linkRepository.countByUserId(accountId))
                .totalClicks(linkRepository.sumClickCountByUserId(accountId))
                .build();
    }

    // ------------------------------------------------------------------ redirect hot path

    /** Resolves the current destination and hands off analytics for GET requests. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String resolveRedirect(String shortUrl, String clientIp, String userAgent, boolean recordClick) {
        LinkRepository.RedirectTarget target = linkRepository.findRedirectTarget(shortUrl)
                .filter(row -> Boolean.TRUE.equals(row.getActive()) && !Boolean.TRUE.equals(row.getOwnerSuspended()))
                .orElseThrow(() -> new ResourceNotFoundException("Link not found"));
        URI destination = requireRedirectableUrl(target.getOriginalUrl());
        if (recordClick) {
            clickEventPublisher.publish(target.getId(), clientIp, userAgent);
        }
        return destination.toASCIIString();
    }

    // ------------------------------------------------------------------ analytics

    public AnalyticsResponse getLinkAnalytics(UUID accountId, UUID linkId) {
        Link link = getOwnedLink(linkId, accountId);
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

    /**
     * Bean validation accepts some URLs that cannot be sent in an HTTP Location header
     * (e.g. containing "|" or "^"). Reject them up front instead of failing at redirect time.
     */
    private static URI requireRedirectableUrl(String url) {
        try {
            URI uri = new URI(url.trim());
            if (uri.getHost() == null || uri.getUserInfo() != null
                    || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw new BadRequestException("URL must be an HTTP(S) address with a host and no embedded credentials");
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new BadRequestException("URL contains characters that are not allowed; please encode them");
        }
    }

    private User getUserForUpdate(UUID accountId) {
        return userRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /** "Not yours" and "doesn't exist" both return 404, so link ids cannot be probed. */
    private Link getOwnedLink(UUID linkId, UUID accountId) {
        return linkRepository.findByIdAndUserId(linkId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found"));
    }
}
