package com.gupta.linkly.dto;

import com.gupta.linkly.entity.Link;
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
public class LinkResponse {
    private UUID id;
    private String title;
    private String originalUrl;
    private String shortUrl;
    private Boolean active;
    private Long clickCount;
    private LocalDateTime createdAt;

    public static LinkResponse from(Link link) {
        return new LinkResponse(link.getId(), link.getTitle(), link.getOriginalUrl(), link.getShortUrl(),
                link.getActive(), link.getClickCount(), link.getCreatedAt());
    }
}
