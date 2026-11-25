package com.gupta.linkly.controller;

import com.gupta.linkly.dto.MessageResponse;
import com.gupta.linkly.service.MessageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Tag(name = "Inbox")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public List<MessageResponse> getMyMessages(@AuthenticationPrincipal(expression = "user.id") UUID accountId,
                                              @RequestParam(required = false) LocalDateTime before,
                                              @RequestParam(required = false) UUID beforeId) {
        return messageService.getUserMessages(accountId, before, beforeId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@AuthenticationPrincipal(expression = "user.id") UUID accountId, @PathVariable UUID id) {
        messageService.deleteMessage(accountId, id);
        return ResponseEntity.noContent().build();
    }
}
