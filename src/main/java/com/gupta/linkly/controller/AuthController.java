package com.gupta.linkly.controller;

import com.gupta.linkly.dto.AuthResponse;
import com.gupta.linkly.dto.LoginRequest;
import com.gupta.linkly.dto.RegisterRequest;
import com.gupta.linkly.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest requestBody, jakarta.servlet.http.HttpServletRequest request) {
        AuthResponse response = authService.register(requestBody);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, createCookie(response.getToken(), request).toString())
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest requestBody, jakarta.servlet.http.HttpServletRequest request) {
        AuthResponse response = authService.login(requestBody);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, createCookie(response.getToken(), request).toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(jakarta.servlet.http.HttpServletRequest request) {
        org.springframework.http.ResponseCookie cookie = org.springframework.http.ResponseCookie.from("linkly_token", "")
                .httpOnly(true)
                .secure(isSecure(request))
                .path("/")
                .maxAge(0) // Expire immediately
                .sameSite(isSecure(request) ? "None" : "Lax")
                .build();
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    private boolean isSecure(jakarta.servlet.http.HttpServletRequest request) {
        String xForwardedProto = request.getHeader("X-Forwarded-Proto");
        if (xForwardedProto != null) {
            return xForwardedProto.contains("https");
        }
        return request.isSecure() || !request.getServerName().equals("localhost");
    }

    private org.springframework.http.ResponseCookie createCookie(String token, jakarta.servlet.http.HttpServletRequest request) {
        boolean secure = isSecure(request);
        return org.springframework.http.ResponseCookie.from("linkly_token", token)
                .httpOnly(true)
                .secure(secure) // True in production, false locally
                .path("/")
                .maxAge(24 * 60 * 60) // 1 day
                .sameSite(secure ? "None" : "Lax") // None for prod cross-origin, Lax for local same-site
                .build();
    }
}
