package com.gupta.linkly.system;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;

/**
 * Optional self-ping that stops Render's free tier from putting the service to sleep after
 * 15 minutes without traffic. It calls the public /actuator/health URL, which goes out through
 * Render's load balancer and therefore counts as inbound traffic, and also runs a database check
 * which keeps a free Supabase project active.
 *
 * <p>Limitation: a sleeping service cannot wake itself. An external pinger (e.g. cron-job.org)
 * is more robust; this switch exists so it can be managed from the admin dashboard.
 */
@Slf4j
@Service
public class KeepAliveService {

    private final SystemSettingsService settings;
    private final RestClient restClient;
    private final String baseUrl;

    @Getter private volatile Instant lastPingAt;
    @Getter private volatile Boolean lastPingOk;
    @Getter private volatile String lastPingMessage;

    public KeepAliveService(SystemSettingsService settings, @Value("${app.keep-alive.url:}") String baseUrl) {
        this.settings = settings;
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(15_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public String getTargetUrl() {
        return baseUrl.isEmpty() ? null : baseUrl + "/actuator/health";
    }

    @Scheduled(initialDelay = 60_000, fixedDelayString = "${app.keep-alive.interval-ms:600000}")
    public void ping() {
        if (!settings.get().keepAliveEnabled()) {
            return;
        }
        String target = getTargetUrl();
        lastPingAt = Instant.now();
        if (target == null) {
            lastPingOk = false;
            lastPingMessage = "No URL configured (set SELF_PING_URL; Render provides RENDER_EXTERNAL_URL)";
            return;
        }
        try {
            var response = restClient.get().uri(target).retrieve().toBodilessEntity();
            lastPingOk = response.getStatusCode().is2xxSuccessful();
            lastPingMessage = "HTTP " + response.getStatusCode().value();
            log.debug("Self-ping {} -> {}", target, lastPingMessage);
        } catch (RuntimeException e) {
            lastPingOk = false;
            lastPingMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.warn("Self-ping to {} failed: {}", target, e.getMessage());
        }
    }
}
