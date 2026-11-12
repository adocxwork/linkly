package com.gupta.linkly.service;

import com.gupta.linkly.analytics.ClickEventPublisher;
import com.gupta.linkly.dto.LinkRequest;
import com.gupta.linkly.dto.LinkResponse;
import com.gupta.linkly.entity.Link;
import com.gupta.linkly.entity.User;
import com.gupta.linkly.exception.DuplicateResourceException;
import com.gupta.linkly.exception.ResourceNotFoundException;
import com.gupta.linkly.redis.CacheKeys;
import com.gupta.linkly.redis.RedisJsonCache;
import com.gupta.linkly.repository.ClickAnalyticsRepository;
import com.gupta.linkly.repository.LinkRepository;
import com.gupta.linkly.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock private LinkRepository linkRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClickAnalyticsRepository analyticsRepository;
    @Mock private UrlShortenerService urlShortenerService;
    @Mock private ClickEventPublisher clickEventPublisher;
    @Mock private RedisJsonCache cache;

    @InjectMocks
    private LinkService linkService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).username("alice").build();
    }

    private static LinkRequest request(String alias) {
        return new LinkRequest("GitHub", "https://github.com", true, alias);
    }

    @Test
    void addLinkGeneratesCodeAndPutsNewLinkFirst() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(urlShortenerService.generateShortUrl()).thenReturn("abc1234");
        when(linkRepository.findMinSortOrder(user.getId())).thenReturn(0);

        LinkResponse response = linkService.addLink("alice", request(null));

        ArgumentCaptor<Link> saved = ArgumentCaptor.forClass(Link.class);
        verify(linkRepository).saveAndFlush(saved.capture());
        assertThat(response.getShortUrl()).isEqualTo("abc1234");
        assertThat(saved.getValue().getSortOrder()).isEqualTo(-1);
        verify(cache).evictAfterCommit(List.of(CacheKeys.profile("alice")));
    }

    @Test
    void addLinkRejectsTakenAlias() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(linkRepository.existsByShortUrl("taken")).thenReturn(true);
        assertThatThrownBy(() -> linkService.addLink("alice", request("taken")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(linkRepository, never()).saveAndFlush(any());
    }

    @Test
    void redirectCacheHitSkipsDatabase() {
        UUID linkId = UUID.randomUUID();
        when(cache.get(CacheKeys.redirect("abc"), LinkService.RedirectTarget.class))
                .thenReturn(Optional.of(new LinkService.RedirectTarget(linkId, "https://example.com")));

        assertThat(linkService.resolveRedirect("abc", "8.8.8.8", "Mozilla")).isEqualTo("https://example.com");
        verifyNoInteractions(linkRepository);
        verify(clickEventPublisher).publish(linkId, "8.8.8.8", "Mozilla");
    }

    @Test
    void redirectCacheMissReadsDatabaseAndCaches() {
        UUID linkId = UUID.randomUUID();
        when(cache.get(anyString(), eq(LinkService.RedirectTarget.class))).thenReturn(Optional.empty());
        when(linkRepository.findRedirectTarget("abc")).thenReturn(Optional.of(target(linkId, true, false)));

        assertThat(linkService.resolveRedirect("abc", "8.8.8.8", "Mozilla")).isEqualTo("https://example.com");
        verify(cache).put(eq(CacheKeys.redirect("abc")), any(LinkService.RedirectTarget.class), eq(CacheKeys.REDIRECT_TTL));
    }

    @Test
    void inactiveLinkOrSuspendedOwnerIsNotFoundAndNotCounted() {
        when(cache.get(anyString(), eq(LinkService.RedirectTarget.class))).thenReturn(Optional.empty());
        when(linkRepository.findRedirectTarget("off")).thenReturn(Optional.of(target(UUID.randomUUID(), false, false)));
        when(linkRepository.findRedirectTarget("banned")).thenReturn(Optional.of(target(UUID.randomUUID(), true, true)));

        assertThatThrownBy(() -> linkService.resolveRedirect("off", "1.1.1.1", "x")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> linkService.resolveRedirect("banned", "1.1.1.1", "x")).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(clickEventPublisher);
        verify(cache, never()).put(anyString(), any(), any());
    }

    @Test
    void cannotUpdateSomeoneElsesLink() {
        UUID id = UUID.randomUUID();
        when(linkRepository.findByIdAndUserUsername(id, "mallory")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> linkService.updateLink("mallory", id, request(null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static LinkRepository.RedirectTarget target(UUID id, boolean active, boolean suspended) {
        return new LinkRepository.RedirectTarget() {
            public UUID getId() { return id; }
            public String getOriginalUrl() { return "https://example.com"; }
            public Boolean getActive() { return active; }
            public Boolean getOwnerSuspended() { return suspended; }
        };
    }
}
