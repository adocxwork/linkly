package com.gupta.linkly.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** One row per recorded click. Written in batches by {@code ClickBatchWriter}. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "click_analytics")
public class ClickAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id", nullable = false)
    private Link link;

    /** Salted hash of the visitor IP: lets us count unique visitors without storing the IP. */
    @Column(name = "ip_hash")
    private String ipHash;

    private String country;
    private String city;
    private String browser;
    private String deviceType; // Desktop, Mobile, Tablet

    /** When the click happened (not when it was processed, which can be later). */
    @Column(nullable = false, updatable = false)
    private LocalDateTime clickedAt;
}
