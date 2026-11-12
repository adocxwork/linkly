package com.gupta.linkly.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must be at most 100 characters")
    private String title;

    @NotBlank(message = "Original URL is required")
    @Size(max = 2048, message = "URL must be at most 2048 characters")
    @Pattern(regexp = ValidationPatterns.HTTP_URL, message = "URL must start with http:// or https://")
    @URL(message = "Must be a valid URL (e.g. https://example.com)")
    private String originalUrl;

    private Boolean active;

    @Pattern(regexp = ValidationPatterns.ALIAS_OR_EMPTY, message = "Alias must be 3-30 letters, numbers, dashes or underscores")
    private String customAlias;
}
