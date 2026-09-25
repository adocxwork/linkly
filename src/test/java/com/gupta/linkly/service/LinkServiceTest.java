package com.gupta.linkly.service;

import com.gupta.linkly.dto.LinkRequest;
import com.gupta.linkly.dto.LinkResponse;
import com.gupta.linkly.entity.Link;
import com.gupta.linkly.entity.User;
import com.gupta.linkly.repository.LinkRepository;
import com.gupta.linkly.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UrlShortenerService urlShortenerService;

    @InjectMocks
    private LinkService linkService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("adityagupta")
                .build();
    }

    @Test
    void testAddLink() {
        LinkRequest request = new LinkRequest("My Link", "https://example.com", true, null);

        when(userRepository.findByUsername("adityagupta")).thenReturn(Optional.of(testUser));
        when(urlShortenerService.generateShortUrl()).thenReturn("abcdef");
        
        Link savedLink = Link.builder()
                .id(UUID.randomUUID())
                .title("My Link")
                .originalUrl("https://example.com")
                .shortUrl("abcdef")
                .clickCount(0L)
                .user(testUser)
                .build();
                
        when(linkRepository.save(any(Link.class))).thenReturn(savedLink);

        LinkResponse response = linkService.addLink("adityagupta", request);

        assertNotNull(response);
        assertEquals("My Link", response.getTitle());
        assertEquals("abcdef", response.getShortUrl());
        verify(linkRepository, times(1)).save(any(Link.class));
    }
}
