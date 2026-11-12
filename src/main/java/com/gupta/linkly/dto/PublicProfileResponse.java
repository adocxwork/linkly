package com.gupta.linkly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * What anonymous visitors see on /p/{username}. Contains no private data: no email, no click
 * counts, and the UPI ID only when the creator enabled UPI payments.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicProfileResponse {
    private String name;
    private String username;
    private String bio;
    private List<PublicLink> links;
    private String upiId;
    private Boolean enableUpiPayment;
    private Boolean enablePublicMessaging;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicLink {
        private UUID id;
        private String title;
        private String shortUrl;
    }
}
