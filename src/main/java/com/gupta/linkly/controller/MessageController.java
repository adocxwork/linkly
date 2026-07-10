package com.gupta.linkly.controller;

import com.gupta.linkly.dto.MessageResponse;
import com.gupta.linkly.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<List<MessageResponse>> getMyMessages(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(messageService.getUserMessages(userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@AuthenticationPrincipal UserDetails userDetails, @PathVariable UUID id) {
        messageService.deleteMessage(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
