package com.gupta.linkly.controller;

import com.gupta.linkly.dto.UserProfileResponse;
import com.gupta.linkly.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/users/{username}/suspend")
    public ResponseEntity<Void> toggleSuspendUser(@PathVariable String username) {
        userService.toggleSuspendUser(username);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{username}")
    public ResponseEntity<Void> deleteUser(@PathVariable String username) {
        userService.deleteUserByAdmin(username);
        return ResponseEntity.noContent().build();
    }
}
