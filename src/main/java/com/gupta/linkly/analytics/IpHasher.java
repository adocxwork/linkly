package com.gupta.linkly.analytics;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * HMAC-SHA256 of the visitor IP with a server-side secret. Same IP gives the same hash (so we can
 * count unique visitors), but the IP cannot be recovered from the database. A plain unsalted hash
 * would be reversible because the IPv4 space is small enough to brute-force.
 */
@Component
public class IpHasher {

    private final byte[] key;

    public IpHasher(@Value("${app.analytics.ip-hash-salt:}") String salt) {
        this.key = (salt == null || salt.isBlank() ? "linkly-dev-salt" : salt).getBytes(StandardCharsets.UTF_8);
    }

    public String hash(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] digest = mac.doFinal(ip.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }
}
