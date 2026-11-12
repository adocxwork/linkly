package com.gupta.linkly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {
    private long totalClicks;
    private long uniqueVisitors;
    private Map<String, Long> clicksByCountry;
    private Map<String, Long> clicksByDevice;
    private Map<String, Long> clicksByBrowser;
    /** One entry per day for the last 30 days (zero-filled). */
    private List<DailyClicks> clicksOverTime;

    public record DailyClicks(LocalDate date, long clicks) {
    }
}
