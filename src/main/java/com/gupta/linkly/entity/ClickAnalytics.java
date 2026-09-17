package com.gupta.linkly.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
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

    private String ipAddress;
    private String country;
    private String city;
    private String browser;
    private String deviceType; // Desktop, Mobile, Tablet

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime clickedAt;
}
