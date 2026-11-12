package com.gupta.linkly.controller;

import com.gupta.linkly.dto.AuthResponse;
import com.gupta.linkly.dto.LoginRequest;
import com.gupta.linkly.dto.RegisterRequest;
import com.gupta.linkly.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Stateless JWT auth. The token is returned in the body and sent back by the client in the
 * Authorization header. Logging out on one device = the client discards its token;
 * "log out everywhere" is POST /api/users/me/revoke-sessions.
 */
@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
