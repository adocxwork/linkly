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
    public void redirect(@PathVariable String shortUrl, HttpServletResponse response) throws IOException {
        try {
            String originalUrl = linkService.getOriginalUrlAndIncrementClick(shortUrl);
            response.sendRedirect(originalUrl);
        } catch (com.gupta.linkly.exception.ResourceNotFoundException ex) {
            response.sendRedirect("http://localhost:5173/link-error");
        }
    }
}
