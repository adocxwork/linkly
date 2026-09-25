package com.gupta.linkly.service;

import com.gupta.linkly.dto.ClickEvent;
import com.gupta.linkly.entity.Link;
import com.gupta.linkly.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsStreamConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private final AnalyticsService analyticsService;
    private final LinkRepository linkRepository;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            String json = message.getValue().get("payload");
            if (json != null) {
                ClickEvent event = objectMapper.readValue(json, ClickEvent.class);
                Optional<Link> linkOpt = linkRepository.findById(event.getLinkId());
                if (linkOpt.isPresent()) {
                    // This now happens completely detached from the user's request thread!
                    analyticsService.recordClick(linkOpt.get().getId(), event.getIp(), event.getUserAgent());
                    // Processed successfully
                    stringRedisTemplate.opsForStream().acknowledge("link-clicks-stream", "analytics-group", message.getId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to process event from stream", e);
        }
    }
}
