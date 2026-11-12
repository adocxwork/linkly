package com.gupta.linkly.entity;

import jakarta.persistence.*;
import lombok.*;

/** Runtime switches the admin can flip from the dashboard. Single row with id "DEFAULT". */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "system_settings")
public class SystemSettings {

    public static final String DEFAULT_ID = "DEFAULT";

    @Id
    private String id;

    /** Periodically call our own public health URL so Render's free tier does not sleep. */
    @Column(name = "keep_alive_enabled", nullable = false)
    private boolean keepAliveEnabled;

    /** When false the app never talks to Redis: caches are skipped, clicks use the local queue. */
    @Builder.Default
    @Column(name = "redis_enabled", nullable = false)
    private boolean redisEnabled = true;

    public static SystemSettings defaults() {
        return SystemSettings.builder().id(DEFAULT_ID).keepAliveEnabled(false).redisEnabled(true).build();
    }
}
