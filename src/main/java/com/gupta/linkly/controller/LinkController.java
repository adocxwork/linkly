package com.gupta.linkly.controller;

import com.gupta.linkly.dto.*;
import com.gupta.linkly.service.LinkService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Links")
@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LinkResponse addLink(@AuthenticationPrincipal(expression = "user.id") UUID accountId, @Valid @RequestBody LinkRequest request) {
        return linkService.addLink(accountId, request);
    }

    @GetMapping
    public List<LinkResponse> getAllLinks(@AuthenticationPrincipal(expression = "user.id") UUID accountId) {
        return linkService.getAllLinks(accountId);
    }

    @PutMapping("/{id}")
    public LinkResponse updateLink(@AuthenticationPrincipal(expression = "user.id") UUID accountId, @PathVariable UUID id, @Valid @RequestBody LinkRequest request) {
        return linkService.updateLink(accountId, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLink(@AuthenticationPrincipal(expression = "user.id") UUID accountId, @PathVariable UUID id) {
        linkService.deleteLink(accountId, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderLinks(@AuthenticationPrincipal(expression = "user.id") UUID accountId, @Valid @RequestBody ReorderRequest request) {
        linkService.reorderLinks(accountId, request.getLinkIds());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard(@AuthenticationPrincipal(expression = "user.id") UUID accountId) {
        return linkService.getDashboard(accountId);
    }

    @GetMapping("/{id}/analytics")
    public AnalyticsResponse getLinkAnalytics(@AuthenticationPrincipal(expression = "user.id") UUID accountId, @PathVariable UUID id) {
        return linkService.getLinkAnalytics(accountId, id);
    }
}
