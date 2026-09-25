package com.gupta.linkly.service;

import com.gupta.linkly.entity.ClickAnalytics;
import com.gupta.linkly.entity.Link;
import com.gupta.linkly.repository.ClickAnalyticsRepository;
import com.gupta.linkly.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class AnalyticsService {

    private final LinkRepository linkRepository;
    private final ClickAnalyticsRepository analyticsRepository;
    private final RestTemplate restTemplate;
    private final java.util.Map<String, String[]> geoCache = new java.util.concurrent.ConcurrentHashMap<>();

    public AnalyticsService(LinkRepository linkRepository, ClickAnalyticsRepository analyticsRepository) {
        this.linkRepository = linkRepository;
        this.analyticsRepository = analyticsRepository;
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Transactional
    public void recordClick(java.util.UUID linkId, String ip, String userAgent) {
        try {
            // Increment simple counter
            // Ignore localhost/internal IPs
            if (ip == null || ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) {
                return;
            }

            // Increment simple counter
            linkRepository.incrementClickCount(linkId);

            // Parse device and browser
            String deviceType = "Desktop";
            if (userAgent != null) {
                String ua = userAgent.toLowerCase();
                if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
                    deviceType = "Mobile";
                } else if (ua.contains("ipad") || ua.contains("tablet")) {
                    deviceType = "Tablet";
                }
            }

            String browser = "Unknown";
            if (userAgent != null) {
                String ua = userAgent.toLowerCase();
                if (ua.contains("edg")) browser = "Edge";
                else if (ua.contains("chrome")) browser = "Chrome";
                else if (ua.contains("safari")) browser = "Safari";
                else if (ua.contains("firefox")) browser = "Firefox";
            }

            // Fetch Geo Data
            String country = "Unknown";
            String city = "Unknown";
            try {
                if (geoCache.containsKey(ip)) {
                    String[] cached = geoCache.get(ip);
                    country = cached[0];
                    city = cached[1];
                } else {
                    String url = "https://get.geojs.io/v1/ip/geo/" + ip + ".json";
                    Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                    if (response != null && response.get("country") != null) {
                        country = (String) response.get("country");
                        city = (String) response.get("city");
                        if (geoCache.size() < 10000) geoCache.put(ip, new String[]{country, city});
                    }
                }
            } catch (Exception e) {
                // Log without exposing raw IP aggressively
                log.error("Failed to fetch geo-data for an IP");
            }

            // Save advanced analytics
            ClickAnalytics analytics = ClickAnalytics.builder()
                    .link(linkRepository.getReferenceById(linkId))
                    .ipAddress(ip)
                    .country(country)
                    .city(city)
                    .deviceType(deviceType)
                    .browser(browser)
                    .build();

            analyticsRepository.save(analytics);

        } catch (Exception e) {
            log.error("Failed to record analytics for link {}: {}", linkId, e.getMessage());
        }
    }
}
