package com.gupta.linkly.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Original URL is required")
    @org.hibernate.validator.constraints.URL(message = "Must be a valid URL (e.g. https://example.com)")
    private String originalUrl;
    
    private Boolean active = true;
}
