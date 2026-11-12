package com.gupta.linkly.dto;

import com.gupta.linkly.entity.Role;
import com.gupta.linkly.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private UUID id;
    private String name;
    private String username;
    private String email;
    private String bio;
    private Role role;
    private Boolean isSuspended;
    private Boolean enablePublicMessaging;
    private String upiId;
    private Boolean enableUpiPayment;
    private LocalDateTime createdAt;

    /** Single mapping used everywhere (previously three copies that disagreed on fields). */
    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .bio(user.getBio())
                .role(user.getRole())
                .isSuspended(user.suspended())
                .enablePublicMessaging(user.getEnablePublicMessaging())
                .upiId(user.getUpiId())
                .enableUpiPayment(user.getEnableUpiPayment())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
