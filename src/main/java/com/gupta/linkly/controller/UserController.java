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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Account")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public UserProfileResponse getProfile(Authentication auth) {
        return userService.getProfile(auth.getName());
    }

    @PutMapping("/profile")
    public UserProfileResponse updateProfile(Authentication auth, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(auth.getName(), request);
    }

    @Operation(summary = "Update account settings; returns a fresh token (old ones are revoked on password change)")
    @PutMapping("/settings")
    public AuthResponse updateSettings(Authentication auth, @Valid @RequestBody ChangeSettingsRequest request) {
        return userService.updateSettings(auth.getName(), request);
    }

    @Operation(summary = "Log out of all devices")
    @PostMapping("/me/revoke-sessions")
    public ResponseEntity<Void> revokeSessions(Authentication auth) {
        userService.revokeAllSessions(auth.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(Authentication auth) {
        userService.deleteAccount(auth.getName());
        return ResponseEntity.noContent().build();
    }
}
