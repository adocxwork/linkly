package com.gupta.linkly.controller;

import com.gupta.linkly.dto.AuthResponse;
import com.gupta.linkly.dto.ChangeSettingsRequest;
import com.gupta.linkly.dto.UpdateProfileRequest;
import com.gupta.linkly.dto.UserProfileResponse;
import com.gupta.linkly.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Account")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public UserProfileResponse getProfile(@AuthenticationPrincipal(expression = "user.id") UUID accountId) {
        return userService.getProfile(accountId);
    }

    @PutMapping("/profile")
    public UserProfileResponse updateProfile(@AuthenticationPrincipal(expression = "user.id") UUID accountId, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(accountId, request);
    }

    @Operation(summary = "Update settings and rotate the token when the password changes")
    @PutMapping("/settings")
    public AuthResponse updateSettings(@AuthenticationPrincipal(expression = "user.id") UUID accountId, @Valid @RequestBody ChangeSettingsRequest request) {
        return userService.updateSettings(accountId, request);
    }

    @Operation(summary = "Log out of all devices")
    @PostMapping("/me/revoke-sessions")
    public ResponseEntity<Void> revokeSessions(@AuthenticationPrincipal(expression = "user.id") UUID accountId) {
        userService.revokeAllSessions(accountId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal(expression = "user.id") UUID accountId) {
        userService.deleteAccount(accountId);
        return ResponseEntity.noContent().build();
    }
}
