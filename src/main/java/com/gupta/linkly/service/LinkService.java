package com.gupta.linkly.service;

import com.gupta.linkly.dto.DashboardResponse;
import com.gupta.linkly.dto.LinkRequest;
import com.gupta.linkly.dto.LinkResponse;
import com.gupta.linkly.entity.Link;
import com.gupta.linkly.entity.User;
import com.gupta.linkly.exception.ResourceNotFoundException;
import com.gupta.linkly.repository.LinkRepository;
import com.gupta.linkly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;
    private final UserRepository userRepository;
    private final UrlShortenerService urlShortenerService;

    public LinkResponse addLink(String username, LinkRequest request) {
        User user = getUserByUsername(username);

        Link link = Link.builder()
                .title(request.getTitle())
                .originalUrl(request.getOriginalUrl())
                .shortUrl(urlShortenerService.generateShortUrl())
                .active(request.getActive() != null ? request.getActive() : true)
                .clickCount(0)
                .user(user)
                .build();

        linkRepository.save(link);
        return mapToLinkResponse(link);
    }

    public List<LinkResponse> getAllLinks(String username) {
        User user = getUserByUsername(username);
        return linkRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::mapToLinkResponse)
                .collect(Collectors.toList());
    }

    public LinkResponse updateLink(String username, UUID linkId, LinkRequest request) {
        Link link = getLinkByIdAndUser(linkId, username);

        link.setTitle(request.getTitle());
        link.setOriginalUrl(request.getOriginalUrl());
        if (request.getActive() != null) {
            link.setActive(request.getActive());
        }

        linkRepository.save(link);
        return mapToLinkResponse(link);
    }

    public void deleteLink(String username, UUID linkId) {
        Link link = getLinkByIdAndUser(linkId, username);
        linkRepository.delete(link);
    }

    public DashboardResponse getDashboard(String username) {
        User user = getUserByUsername(username);
        List<Link> links = linkRepository.findByUserOrderByCreatedAtDesc(user);
        
        long totalLinks = links.size();
        long totalClicks = links.stream().mapToLong(Link::getClickCount).sum();

        return DashboardResponse.builder()
                .totalLinks(totalLinks)
                .totalClicks(totalClicks)
                .build();
    }

    @Transactional
    public String getOriginalUrlAndIncrementClick(String shortUrl) {
        Link link = linkRepository.findByShortUrl(shortUrl)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found"));

        if (!link.getActive()) {
            throw new ResourceNotFoundException("Link is inactive");
        }

        link.setClickCount(link.getClickCount() + 1);
        linkRepository.save(link);

        return link.getOriginalUrl();
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Link getLinkByIdAndUser(UUID linkId, String username) {
        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found"));

        if (!link.getUser().getUsername().equals(username)) {
            throw new ResourceNotFoundException("Not authorized to access this link");
        }
        return link;
    }

    private LinkResponse mapToLinkResponse(Link link) {
        return LinkResponse.builder()
                .id(link.getId())
                .title(link.getTitle())
                .originalUrl(link.getOriginalUrl())
                .shortUrl(link.getShortUrl())
                .active(link.getActive())
                .clickCount(link.getClickCount())
                .build();
    }
}
