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
 * <p>Trust the edge's single IP header only when the deployment explicitly enables it.
 * Otherwise select the configured hop from the right of X-Forwarded-For. Unproxied
 * deployments use the socket address.
 */
@Component
public class ClientIpResolver {

    private final int trustedProxyCount;
    private final boolean trustCloudflareClientIp;

    public ClientIpResolver(@Value("${app.trusted-proxy-count:0}") int trustedProxyCount,
                            @Value("${app.trust-cloudflare-client-ip:false}") boolean trustCloudflareClientIp) {
        this.trustedProxyCount = Math.max(0, trustedProxyCount);
        this.trustCloudflareClientIp = trustCloudflareClientIp;
    }

    public String resolve(HttpServletRequest request) {
        if (trustCloudflareClientIp) {
            String edgeClientIp = request.getHeader("CF-Connecting-IP");
            if (edgeClientIp != null && IpAddresses.isValid(edgeClientIp.trim())) {
                return edgeClientIp.trim();
            }
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (trustedProxyCount == 0 || forwarded == null || forwarded.isBlank()) {
            return request.getRemoteAddr();
        }
        List<String> hops = Arrays.stream(forwarded.split(",", -1)).map(String::trim).toList();
        if (hops.size() < trustedProxyCount) {
            return request.getRemoteAddr();
        }
        String candidate = hops.get(hops.size() - trustedProxyCount);
        return IpAddresses.isValid(candidate) ? candidate : request.getRemoteAddr();
    }
}
