package com.gupta.linkly.service;

import com.gupta.linkly.dto.ClickEvent;
import com.gupta.linkly.entity.Link;
import com.gupta.linkly.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsStreamConsumer implements StreamListener<String, ObjectRecord<String, ClickEvent>> {

    private final AnalyticsService analyticsService;
    private final LinkRepository linkRepository;

    @Override
    public void onMessage(ObjectRecord<String, ClickEvent> message) {
        try {
            ClickEvent event = message.getValue();
            if (event != null) {
                Optional<Link> linkOpt = linkRepository.findById(event.getLinkId());
                if (linkOpt.isPresent()) {
                    // This now happens completely detached from the user's request thread!
                    analyticsService.recordClick(linkOpt.get(), event.getIp(), event.getUserAgent());
                    log.info("Processed click event from stream for link: {}", event.getLinkId());
                }
            }
        } catch (Exception e) {
            log.error("Failed to process event from stream", e);
        }
    }
}
