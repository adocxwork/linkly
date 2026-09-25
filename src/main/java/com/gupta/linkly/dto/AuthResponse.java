package com.gupta.linkly.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String token;
    private UserProfileResponse user;
}
