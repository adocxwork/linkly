package com.gupta.linkly.controller;

import com.gupta.linkly.service.LinkService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/r")
@RequiredArgsConstructor
public class RedirectController {

    private final LinkService linkService;

    @GetMapping("/{shortUrl}")
    public void redirect(@PathVariable String shortUrl, jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            } else {
                ip = ip.split(",")[0].trim();
            }
            String userAgent = request.getHeader("User-Agent");
            
            String originalUrl = linkService.getOriginalUrlAndIncrementClick(shortUrl, ip, userAgent);
            response.sendRedirect(originalUrl);
        } catch (com.gupta.linkly.exception.ResourceNotFoundException ex) {
            String baseUrl = System.getenv("FRONTEND_URL");
            if (baseUrl == null || baseUrl.isEmpty()) {
                baseUrl = "https://linkly-plum.vercel.app";
            }
            response.sendRedirect(baseUrl + "/link-error"); // Will redirect to frontend
        }
    }
}
