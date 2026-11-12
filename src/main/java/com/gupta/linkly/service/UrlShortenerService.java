package com.gupta.linkly.service;

import com.gupta.linkly.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Random 7-character Base62 codes: 62^7 ≈ 3.5 trillion combinations, so collisions are rare and
 * codes cannot be enumerated. The unique constraint on links.short_url is the final guarantee.
 */
@Service
@RequiredArgsConstructor
public class UrlShortenerService {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    static final int URL_LENGTH = 7;
    private static final int MAX_ATTEMPTS = 10;

    private final LinkRepository linkRepository;
    private final SecureRandom random = new SecureRandom();

    public String generateShortUrl() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = randomCode();
            if (!linkRepository.existsByShortUrl(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique short code");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(URL_LENGTH);
        for (int i = 0; i < URL_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
