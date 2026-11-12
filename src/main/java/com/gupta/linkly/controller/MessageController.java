package com.gupta.linkly.controller;

import com.gupta.linkly.dto.MessageResponse;
import com.gupta.linkly.service.MessageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Inbox")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public List<MessageResponse> getMyMessages(Authentication auth) {
        return messageService.getUserMessages(auth.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(Authentication auth, @PathVariable UUID id) {
        messageService.deleteMessage(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
