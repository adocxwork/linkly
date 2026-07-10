package com.gupta.linkly.dto;

import com.gupta.linkly.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
