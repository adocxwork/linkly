package com.gupta.linkly.service;

import com.gupta.linkly.repository.LinkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @InjectMocks
    private UrlShortenerService service;

    @Test
    void generatesBase62CodeOfFixedLength() {
        when(linkRepository.existsByShortUrl(anyString())).thenReturn(false);
        assertThat(service.generateShortUrl()).hasSize(UrlShortenerService.URL_LENGTH).matches("[A-Za-z0-9]+");
    }

    @Test
    void retriesOnCollision() {
        when(linkRepository.existsByShortUrl(anyString())).thenReturn(true, false);
        service.generateShortUrl();
        verify(linkRepository, times(2)).existsByShortUrl(anyString());
    }

    @Test
    void givesUpAfterTooManyCollisionsInsteadOfLoopingForever() {
        when(linkRepository.existsByShortUrl(anyString())).thenReturn(true);
        assertThatThrownBy(service::generateShortUrl).isInstanceOf(IllegalStateException.class);
    }
}
