package com.gupta.linkly.controller;

import com.gupta.linkly.exception.ResourceNotFoundException;
import com.gupta.linkly.service.LinkService;
import com.gupta.linkly.web.ClientIpResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * GET /r/{code} → 302 to the original URL.
 * 302 (temporary), not 301: browsers cache 301s forever, which would skip our server and lose
 * every later click from analytics, and would ignore later edits or deactivation of the link.
 */
@Tag(name = "Redirect")
@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final LinkService linkService;
    private final ClientIpResolver clientIpResolver;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @GetMapping("/r/{shortUrl}")
    public ResponseEntity<Void> redirect(@PathVariable String shortUrl, HttpServletRequest request) {
        String target;
        try {
            target = linkService.resolveRedirect(shortUrl, clientIpResolver.resolve(request), request.getHeader(HttpHeaders.USER_AGENT));
        } catch (ResourceNotFoundException e) {
            target = frontendUrl.replaceAll("/+$", "") + "/link-error";
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(target))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }
}
