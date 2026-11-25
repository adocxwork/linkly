package com.gupta.linkly.security;

import com.gupta.linkly.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Issues and verifies HMAC-signed JWTs.
 * Claims: sub = immutable user ID, ver = the user's token version (see {@link User#getTokenVersion()}).
 */
@Component
public class JwtUtil {

    public static final String VERSION_CLAIM = "ver";

    private final SecretKey signingKey;
    private final long expirationMs;
    private final JwtParser parser;

    public JwtUtil(@Value("${jwt.secret:}") String secret, @Value("${jwt.expiration:86400000}") long expirationMs) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            // Fail fast at startup with a clear message instead of on the first login.
            throw new IllegalStateException("JWT_SECRET must be set and at least 32 characters long");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        if (expirationMs <= 0) {
            throw new IllegalStateException("JWT expiration must be positive");
        }
        this.expirationMs = expirationMs;
        this.parser = Jwts.parser().verifyWith(signingKey).build();
    }

    public String generateToken(User user) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(VERSION_CLAIM, user.getTokenVersion())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    /** Verifies signature and expiry. Throws {@link JwtException} if the token is not valid. */
    public Claims parse(String token) {
        return parser.parseSignedClaims(token).getPayload();
    }

    public static int tokenVersion(Claims claims) {
        Integer version = claims.get(VERSION_CLAIM, Integer.class);
        return version == null ? 0 : version;
    }
}
