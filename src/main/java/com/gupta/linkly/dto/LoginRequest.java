package com.gupta.linkly.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /** Username or email. */
    @NotBlank(message = "Username or email is required")
    @Size(max = 254)
    private String identifier;

    @NotBlank(message = "Password is required")
    @Size(max = 72)
    private String password;
}
