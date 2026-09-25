package com.gupta.linkly.service;

import com.gupta.linkly.dto.DashboardResponse;
import com.gupta.linkly.dto.LinkRequest;
import com.gupta.linkly.dto.LinkResponse;
import com.gupta.linkly.entity.Link;
import com.gupta.linkly.entity.User;
import com.gupta.linkly.exception.ResourceNotFoundException;
import com.gupta.linkly.repository.LinkRepository;
import com.gupta.linkly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;
    private final UserRepository userRepository;
    private final UrlShortenerService urlShortenerService;
    private final AnalyticsService analyticsService;
    private final com.gupta.linkly.repository.ClickAnalyticsRepository analyticsRepository;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @org.springframework.cache.annotation.CacheEvict(value = "publicProfiles", key = "#username")
    public LinkResponse addLink(String username, LinkRequest request) {
        User user = getUserByUsername(username);
        
        String shortUrl;
        if (request.getCustomAlias() != null && !request.getCustomAlias().trim().isEmpty()) {
            if (linkRepository.findByShortUrl(request.getCustomAlias()).isPresent()) {
                throw new com.gupta.linkly.exception.DuplicateResourceException("This alias is already taken");
            }
            shortUrl = request.getCustomAlias().trim();
        } else {
            shortUrl = urlShortenerService.generateShortUrl();
        }

        Link link = Link.builder()
                .title(request.getTitle())
                .originalUrl(request.getOriginalUrl())
                .shortUrl(shortUrl)
                .active(request.getActive() != null ? request.getActive() : true)
                .clickCount(0L)
                .user(user)
                .build();

        linkRepository.save(link);
        return mapToLinkResponse(link);
    }

    public List<LinkResponse> getAllLinks(String username) {
        User user = getUserByUsername(username);
        return linkRepository.findByUserOrderBySortOrderAscCreatedAtDesc(user).stream()
                .map(this::mapToLinkResponse)
                .collect(Collectors.toList());
    }

    @org.springframework.cache.annotation.CacheEvict(value = "publicProfiles", key = "#username")
    public LinkResponse updateLink(String username, UUID linkId, LinkRequest request) {
        Link link = getLinkByIdAndUser(linkId, username);

        link.setTitle(request.getTitle());
        link.setOriginalUrl(request.getOriginalUrl());
        try { stringRedisTemplate.delete("redirect:" + link.getShortUrl()); } catch (Exception e) {}
        if (request.getActive() != null) {
            link.setActive(request.getActive());
        }

        linkRepository.save(link);
        return mapToLinkResponse(link);
    }

    @org.springframework.cache.annotation.CacheEvict(value = "publicProfiles", key = "#username")
    public void deleteLink(String username, UUID linkId) {
        Link link = getLinkByIdAndUser(linkId, username);
        linkRepository.delete(link);
        try { stringRedisTemplate.delete("redirect:" + link.getShortUrl()); } catch (Exception e) {}
    }

    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = "publicProfiles", key = "#username")
    public void reorderLinks(String username, List<UUID> orderedIds) {
        User user = getUserByUsername(username);
        List<Link> userLinks = linkRepository.findByUserOrderBySortOrderAscCreatedAtDesc(user);
        
        // Ensure all provided IDs belong to the user
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            Link link = userLinks.stream().filter(l -> l.getId().equals(id)).findFirst().orElse(null);
            if (link != null) {
                link.setSortOrder(i);
                linkRepository.save(link);
            }
        }
    }

    public DashboardResponse getDashboard(String username) {
        User user = getUserByUsername(username);
        List<Link> links = linkRepository.findByUserOrderBySortOrderAscCreatedAtDesc(user);
        
        long totalLinks = links.size();
        long totalClicks = links.stream().mapToLong(Link::getClickCount).sum();

        return DashboardResponse.builder()
                .totalLinks(totalLinks)
                .totalClicks(totalClicks)
                .build();
    }

    @Transactional
        public String getOriginalUrlAndIncrementClick(String shortUrl, String ip, String userAgent) {
        String cacheKey = "redirect:" + shortUrl;
        String cachedData = null;
        try {
            cachedData = stringRedisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            // Ignore Redis failures
        }
        
        java.util.UUID linkId;
        String originalUrl;

        if (cachedData != null) {
            String[] parts = cachedData.split("\\|");
            linkId = java.util.UUID.fromString(parts[0]);
            originalUrl = parts[1];
        } else {
            Link link = linkRepository.findByShortUrl(shortUrl)
                    .orElseThrow(() -> new ResourceNotFoundException("Link not found"));

            if (Boolean.FALSE.equals(link.getActive()) || Boolean.TRUE.equals(link.getUser().getIsSuspended())) {
                throw new ResourceNotFoundException("Link is inactive");
            }
            linkId = link.getId();
            originalUrl = link.getOriginalUrl();
            try {
                stringRedisTemplate.opsForValue().set(cacheKey, linkId + "|" + originalUrl, java.time.Duration.ofHours(24));
            } catch (Exception e) {
                // Ignore Redis failures
            }
        }

        try {
            com.gupta.linkly.dto.ClickEvent event = new com.gupta.linkly.dto.ClickEvent(linkId, ip, userAgent);
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(event);
            stringRedisTemplate.opsForStream().add("link-clicks-stream", java.util.Collections.singletonMap("payload", json));
        } catch (Exception e) {
            analyticsService.recordClick(linkId, ip, userAgent);
        }

        return originalUrl;
    }


    public com.gupta.linkly.dto.AnalyticsResponse getLinkAnalytics(String username, UUID linkId) {
        Link link = getLinkByIdAndUser(linkId, username);
        List<com.gupta.linkly.entity.ClickAnalytics> clicks = analyticsRepository.findByLink(link);

        java.util.Map<String, Long> clicksByCountry = clicks.stream()
                .collect(Collectors.groupingBy(c -> c.getCountry() != null ? c.getCountry() : "Unknown", Collectors.counting()));
        
        java.util.Map<String, Long> clicksByDevice = clicks.stream()
                .collect(Collectors.groupingBy(c -> c.getDeviceType() != null ? c.getDeviceType() : "Unknown", Collectors.counting()));
        
        java.util.Map<String, Long> clicksByBrowser = clicks.stream()
                .collect(Collectors.groupingBy(c -> c.getBrowser() != null ? c.getBrowser() : "Unknown", Collectors.counting()));

        return com.gupta.linkly.dto.AnalyticsResponse.builder()
                .clicksByCountry(clicksByCountry)
                .clicksByDevice(clicksByDevice)
                .clicksByBrowser(clicksByBrowser)
                .build();
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Link getLinkByIdAndUser(UUID linkId, String username) {
        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found"));

        if (!link.getUser().getUsername().equals(username)) {
            throw new ResourceNotFoundException("Not authorized to access this link");
        }
        return link;
    }

    private LinkResponse mapToLinkResponse(Link link) {
        return LinkResponse.builder()
                .id(link.getId())
                .title(link.getTitle())
                .originalUrl(link.getOriginalUrl())
                .shortUrl(link.getShortUrl())
                .active(link.getActive())
                .clickCount(link.getClickCount())
                .build();
    }
}
