package com.gupta.linkly.controller;

import com.gupta.linkly.dto.MessageRequest;
import com.gupta.linkly.dto.PublicProfileResponse;
import com.gupta.linkly.service.MessageService;
import com.gupta.linkly.service.UrlShortenerService;
import com.gupta.linkly.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final UserService userService;
    private final MessageService messageService;
    private final UrlShortenerService urlShortenerService;

    @GetMapping("/u/{username}")
    public ResponseEntity<PublicProfileResponse> getPublicProfile(@PathVariable String username) {
        return ResponseEntity.ok(userService.getPublicProfile(username));
    }

    @PostMapping("/u/{username}/messages")
    public ResponseEntity<Void> leaveMessage(
            @PathVariable String username,
            @Valid @RequestBody MessageRequest request) {
        messageService.saveMessage(username, request);
        return ResponseEntity.ok().build();
    }
}
