package com.gupta.linkly.service;

import com.gupta.linkly.entity.Link;
import com.gupta.linkly.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final LinkRepository linkRepository;

    @Async
    @Transactional
    public void recordClick(Link link) {
        link.setClickCount(link.getClickCount() + 1);
        linkRepository.save(link);
    }
}
