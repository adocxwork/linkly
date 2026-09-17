package com.gupta.linkly.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class AnalyticsResponse {
    private Map<String, Long> clicksByCountry;
    private Map<String, Long> clicksByDevice;
    private Map<String, Long> clicksByBrowser;
}
