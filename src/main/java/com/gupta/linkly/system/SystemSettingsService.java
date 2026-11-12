package com.gupta.linkly.system;

import com.gupta.linkly.entity.SystemSettings;
import com.gupta.linkly.redis.RedisGateway;
import com.gupta.linkly.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-controlled runtime switches (self-ping, Redis on/off).
 *
 * <p>The values live in the system_settings table so they survive restarts, and are cached in
 * memory so hot paths (every redirect checks whether Redis is enabled) never hit the database.
 * A periodic refresh keeps several instances in sync if the app is ever scaled out.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSettingsService {

    private final SystemSettingsRepository repository;
    private final RedisGateway redisGateway;

    private volatile Snapshot current = new Snapshot(false, true);

    public record Snapshot(boolean keepAliveEnabled, boolean redisEnabled) {
    }

    public Snapshot get() {
        return current;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    public void refresh() {
        try {
            SystemSettings settings = repository.findById(SystemSettings.DEFAULT_ID).orElseGet(SystemSettings::defaults);
            apply(settings);
        } catch (RuntimeException e) {
            log.warn("Could not load system settings, keeping current values: {}", e.getMessage());
        }
    }

    @Transactional
    public Snapshot update(Boolean keepAliveEnabled, Boolean redisEnabled) {
        SystemSettings settings = repository.findById(SystemSettings.DEFAULT_ID).orElseGet(SystemSettings::defaults);
        if (keepAliveEnabled != null) {
            settings.setKeepAliveEnabled(keepAliveEnabled);
        }
        if (redisEnabled != null) {
            settings.setRedisEnabled(redisEnabled);
        }
        repository.save(settings);
        apply(settings);
        log.info("System settings updated: keepAlive={}, redis={}", settings.isKeepAliveEnabled(), settings.isRedisEnabled());
        return current;
    }

    private void apply(SystemSettings settings) {
        current = new Snapshot(settings.isKeepAliveEnabled(), settings.isRedisEnabled());
        redisGateway.setEnabled(settings.isRedisEnabled());
    }
}
