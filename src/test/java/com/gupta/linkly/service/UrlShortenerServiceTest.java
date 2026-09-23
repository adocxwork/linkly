package com.gupta.linkly.service;

import com.gupta.linkly.repository.LinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @InjectMocks
    private UrlShortenerService urlShortenerService;

    @Test
    void testGenerateShortUrl() {
        when(linkRepository.existsByShortUrl(anyString())).thenReturn(false);

        String shortUrl = urlShortenerService.generateShortUrl();

        assertNotNull(shortUrl);
        assertEquals(6, shortUrl.length());
    }
    
    @Test
    void testGenerateShortUrlWithCollision() {
        // First generated URL exists, second does not
        when(linkRepository.existsByShortUrl(anyString())).thenReturn(true).thenReturn(false);

        String shortUrl = urlShortenerService.generateShortUrl();

        assertNotNull(shortUrl);
        assertEquals(6, shortUrl.length());
    }
}
