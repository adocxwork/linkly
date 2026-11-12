package com.gupta.linkly.controller;

import com.gupta.linkly.dto.*;
import com.gupta.linkly.service.LinkService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public LinkResponse addLink(Authentication auth, @Valid @RequestBody LinkRequest request) {
        return linkService.addLink(auth.getName(), request);
    }

    @GetMapping
    public List<LinkResponse> getAllLinks(Authentication auth) {
        return linkService.getAllLinks(auth.getName());
    }

    @PutMapping("/{id}")
    public LinkResponse updateLink(Authentication auth, @PathVariable UUID id, @Valid @RequestBody LinkRequest request) {
        return linkService.updateLink(auth.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLink(Authentication auth, @PathVariable UUID id) {
        linkService.deleteLink(auth.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderLinks(Authentication auth, @Valid @RequestBody ReorderRequest request) {
        linkService.reorderLinks(auth.getName(), request.getLinkIds());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard(Authentication auth) {
        return linkService.getDashboard(auth.getName());
    }

    @GetMapping("/{id}/analytics")
    public AnalyticsResponse getLinkAnalytics(Authentication auth, @PathVariable UUID id) {
        return linkService.getLinkAnalytics(auth.getName(), id);
    }
}
