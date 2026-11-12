package com.gupta.linkly.controller;

import com.gupta.linkly.dto.MessageRequest;
import com.gupta.linkly.dto.PublicProfileResponse;
import com.gupta.linkly.service.MessageService;
import com.gupta.linkly.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Unauthenticated endpoints used by public creator pages (rate limited per IP). */
@Tag(name = "Public")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final UserService userService;
    private final MessageService messageService;

    @GetMapping("/u/{username}")
    public PublicProfileResponse getPublicProfile(@PathVariable String username) {
        return userService.getPublicProfile(username);
    }

    @PostMapping("/u/{username}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public void leaveMessage(@PathVariable String username, @Valid @RequestBody MessageRequest request) {
        messageService.saveMessage(username, request);
    }
}
