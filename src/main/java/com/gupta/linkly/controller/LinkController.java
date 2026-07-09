package com.gupta.linkly.controller;

import com.gupta.linkly.dto.DashboardResponse;
import com.gupta.linkly.dto.LinkRequest;
import com.gupta.linkly.dto.LinkResponse;
import com.gupta.linkly.service.LinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @PostMapping
    public ResponseEntity<LinkResponse> addLink(
            Authentication authentication,
            @Valid @RequestBody LinkRequest request
    ) {
        return new ResponseEntity<>(linkService.addLink(authentication.getName(), request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<LinkResponse>> getAllLinks(Authentication authentication) {
        return ResponseEntity.ok(linkService.getAllLinks(authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LinkResponse> updateLink(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody LinkRequest request
    ) {
        return ResponseEntity.ok(linkService.updateLink(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLink(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        linkService.deleteLink(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(Authentication authentication) {
        return ResponseEntity.ok(linkService.getDashboard(authentication.getName()));
    }
}
