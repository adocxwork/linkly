package com.gupta.linkly.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Link {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2048)
    private String originalUrl;

    /** The short code used in /r/{shortUrl}. Unique across all users. */
    @Column(nullable = false, unique = true)
    private String shortUrl;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    /** Denormalised counter. Only the analytics writer updates this column. */
    @Builder.Default
    @Column(nullable = false, updatable = false)
    private Long clickCount = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Integer sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
