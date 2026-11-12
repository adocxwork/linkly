package com.gupta.linkly.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Partial update: null fields are left unchanged. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeSettingsRequest {

    @Size(max = 50, message = "Name must be at most 50 characters")
    private String name;

    @Size(max = 255, message = "Bio must be at most 255 characters")
    private String bio;

    @Email(message = "Invalid email format")
    @Size(max = 254, message = "Email is too long")
    private String email;

    /** Required when changing email or password, so a stolen session cannot take over the account. */
    @Size(max = 72)
    private String currentPassword;

    @Pattern(regexp = "^$|^.{8,72}$", message = "New password must be between 8 and 72 characters")
    private String newPassword;

    @Pattern(regexp = ValidationPatterns.UPI_ID_OR_EMPTY, message = "UPI ID must look like name@bank")
    private String upiId;

    private Boolean enableUpiPayment;
    private Boolean enablePublicMessaging;
}
