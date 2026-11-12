package com.gupta.linkly.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A registered creator (or the admin).
 *
 * <p>Links, messages and click analytics reference users through foreign keys with
 * {@code ON DELETE CASCADE}, so deleting a user is a single DELETE statement. We deliberately do
 * not map those collections here: JPA cascades would load every child row into memory first.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash, never the raw password. */
    @Column(nullable = false)
    private String password;

    private String bio;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private Role role = Role.ROLE_USER;

    @Builder.Default
    @Column(name = "is_suspended", nullable = false)
    private Boolean isSuspended = false;

    /**
     * Embedded in every JWT. Incrementing it (password change, suspension, "log out everywhere")
     * invalidates all tokens issued before, which plain stateless JWTs cannot otherwise do.
     */
    @Builder.Default
    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion = 0;

    private String upiId;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enableUpiPayment = false;

    @Builder.Default
    @Column(name = "enable_public_messaging", nullable = false)
    private Boolean enablePublicMessaging = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public boolean isAdmin() {
        return role == Role.ROLE_ADMIN;
    }

    public boolean suspended() {
        return Boolean.TRUE.equals(isSuspended);
    }

    public void bumpTokenVersion() {
        tokenVersion = (tokenVersion == null ? 0 : tokenVersion) + 1;
    }
}
