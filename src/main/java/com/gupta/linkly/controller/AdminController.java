package com.gupta.linkly.controller;

import com.gupta.linkly.dto.SystemSettingsRequest;
import com.gupta.linkly.dto.UserProfileResponse;
import com.gupta.linkly.service.UserService;
import com.gupta.linkly.system.SystemHealthService;
import com.gupta.linkly.system.SystemSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin-only (enforced in SecurityConfig: /api/admin/** requires ROLE_ADMIN). */
@Tag(name = "Admin")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final SystemHealthService healthService;
    private final SystemSettingsService settingsService;

    @GetMapping("/users")
    public List<UserProfileResponse> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/users/{username}/suspend")
    public UserProfileResponse toggleSuspendUser(@PathVariable String username) {
        return userService.toggleSuspendUser(username);
    }

    @DeleteMapping("/users/{username}")
    public ResponseEntity<Void> deleteUser(@PathVariable String username) {
        userService.deleteUserByAdmin(username);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Live status of PostgreSQL, Redis, the click pipeline, Geo-IP, self-ping and JVM")
    @GetMapping("/system/health")
    public SystemHealthService.HealthReport systemHealth(HttpServletRequest request) {
        return healthService.check(request);
    }

    @GetMapping("/system/settings")
    public SystemSettingsService.Snapshot getSettings() {
        return settingsService.get();
    }

    @Operation(summary = "Turn the self-ping and/or Redis on or off at runtime (persisted)")
    @PutMapping("/system/settings")
    public SystemSettingsService.Snapshot updateSettings(@RequestBody SystemSettingsRequest request) {
        return settingsService.update(request.keepAliveEnabled(), request.redisEnabled());
    }
}
