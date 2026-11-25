package com.gupta.linkly.service;

import com.gupta.linkly.analytics.ClickEventPublisher;
import com.gupta.linkly.dto.LinkRequest;
import com.gupta.linkly.dto.LinkResponse;
import com.gupta.linkly.entity.Link;
import com.gupta.linkly.entity.User;
import com.gupta.linkly.exception.DuplicateResourceException;
import com.gupta.linkly.exception.ResourceNotFoundException;
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
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(urlShortenerService.generateShortUrl()).thenReturn("abc1234");
        when(linkRepository.findMinSortOrder(user.getId())).thenReturn(0);

        LinkResponse response = linkService.addLink(user.getId(), request(null));

        ArgumentCaptor<Link> saved = ArgumentCaptor.forClass(Link.class);
        verify(linkRepository).saveAndFlush(saved.capture());
        assertThat(response.getShortUrl()).isEqualTo("abc1234");
        assertThat(saved.getValue().getSortOrder()).isEqualTo(-1);
    }

    @Test
    void addLinkRejectsTakenAlias() {
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(linkRepository.existsByShortUrl("taken")).thenReturn(true);
        assertThatThrownBy(() -> linkService.addLink(user.getId(), request("taken")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(linkRepository, never()).saveAndFlush(any());
    }

    @Test
    void redirectReadUsesCurrentDatabaseStateAndRecordsOnlyGet() {
        UUID linkId = UUID.randomUUID();
        when(linkRepository.findRedirectTarget("abc")).thenReturn(Optional.of(target(linkId, true, false)));

        assertThat(linkService.resolveRedirect("abc", "8.8.8.8", "Mozilla", false)).isEqualTo("https://example.com");
        verifyNoInteractions(clickEventPublisher);
        assertThat(linkService.resolveRedirect("abc", "8.8.8.8", "Mozilla", true)).isEqualTo("https://example.com");
        verify(clickEventPublisher).publish(linkId, "8.8.8.8", "Mozilla");
    }

    @Test
    void inactiveLinkOrSuspendedOwnerIsNotFoundAndNotCounted() {
        when(linkRepository.findRedirectTarget("off")).thenReturn(Optional.of(target(UUID.randomUUID(), false, false)));
        when(linkRepository.findRedirectTarget("banned")).thenReturn(Optional.of(target(UUID.randomUUID(), true, true)));

        assertThatThrownBy(() -> linkService.resolveRedirect("off", "1.1.1.1", "x", true)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> linkService.resolveRedirect("banned", "1.1.1.1", "x", true)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(clickEventPublisher);
    }

    @Test
    void rejectsUrlsThatCannotBeRedirectedTo() {
        LinkRequest bad = new LinkRequest("x", "https://example.com/a|b", true, null);
        assertThatThrownBy(() -> linkService.addLink(user.getId(), bad))
                .isInstanceOf(com.gupta.linkly.exception.BadRequestException.class);
        verifyNoInteractions(linkRepository);
    }

    @Test
    void cannotUpdateSomeoneElsesLink() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(linkRepository.findByIdAndUserId(id, user.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> linkService.updateLink(user.getId(), id, request(null)))
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
