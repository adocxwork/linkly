package com.gupta.linkly.service;

import com.gupta.linkly.entity.SystemSettings;
import com.gupta.linkly.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeepAliveService {

    private final SystemSettingsRepository settingsRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${VITE_BACKEND_URL:https://linkly-amwf.onrender.com}")
    private String backendUrl;

    public boolean isKeepAliveEnabled() {
        return settingsRepository.findById("DEFAULT")
                .map(SystemSettings::isKeepAliveEnabled)
                .orElse(false); // Default is false if not set
    }

    public void setKeepAlive(boolean enabled) {
        SystemSettings settings = settingsRepository.findById("DEFAULT")
                .orElse(SystemSettings.builder().id("DEFAULT").build());
        settings.setKeepAliveEnabled(enabled);
        settingsRepository.save(settings);
    }

    // Runs every 14 minutes (840,000 milliseconds)
    @Scheduled(fixedRate = 840000)
    public void selfPing() {
        if (isKeepAliveEnabled()) {
            try {
                String pingUrl = backendUrl + "/api/system/health";
                log.info("Sending self-ping to prevent Render sleep: {}", pingUrl);
                restTemplate.getForEntity(pingUrl, String.class);
            } catch (Exception e) {
                log.error("Failed to self-ping", e);
            }
        }
    }
}
