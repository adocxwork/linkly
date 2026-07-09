package com.gupta.linkly.service;

import com.gupta.linkly.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private final LinkRepository linkRepository;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int URL_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    public String generateShortUrl() {
        String shortUrl;
        do {
            shortUrl = generateRandomString();
        } while (linkRepository.existsByShortUrl(shortUrl));
        return shortUrl;
    }

    private String generateRandomString() {
        StringBuilder sb = new StringBuilder(URL_LENGTH);
        for (int i = 0; i < URL_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
