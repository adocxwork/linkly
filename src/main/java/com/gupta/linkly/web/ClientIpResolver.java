package com.gupta.linkly.web;

import com.gupta.linkly.analytics.IpAddresses;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Works out the real client IP behind reverse proxies.
 *
 * <p>Each proxy APPENDS the address it received the request from to X-Forwarded-For, so the
 * header looks like {@code <whatever the client sent>, ..., <added by our proxy>}. Everything to
 * the left of what our own proxies added is client-controlled and can be forged. With N trusted
 * proxies, the real client IP is therefore the N-th entry counting from the RIGHT.
 *
 * <p>The old code used the LEFT-most entry, so anyone could send
 * {@code X-Forwarded-For: 1.2.3.4} to bypass rate limits and fake analytics.
 */
@Component
public class ClientIpResolver {

    private final int trustedProxyCount;

    public ClientIpResolver(@Value("${app.trusted-proxy-count:1}") int trustedProxyCount) {
        this.trustedProxyCount = Math.max(0, trustedProxyCount);
    }

    public String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (trustedProxyCount == 0 || forwarded == null || forwarded.isBlank()) {
            return request.getRemoteAddr();
        }
        List<String> hops = Arrays.stream(forwarded.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        if (hops.isEmpty()) {
            return request.getRemoteAddr();
        }
        String candidate = hops.get(Math.max(0, hops.size() - trustedProxyCount));
        return IpAddresses.isValid(candidate) ? candidate : request.getRemoteAddr();
    }

    public int getTrustedProxyCount() {
        return trustedProxyCount;
    }
}
