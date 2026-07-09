package com.gupta.linkly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LinkResponse {
    private UUID id;
    private String title;
    private String originalUrl;
    private String shortUrl;
    private Boolean active;
    private Integer clickCount;
}
