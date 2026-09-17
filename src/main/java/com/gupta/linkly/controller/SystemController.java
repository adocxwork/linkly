package com.gupta.linkly.controller;

import com.gupta.linkly.service.KeepAliveService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final KeepAliveService keepAliveService;

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/keep-alive")
    public ResponseEntity<Map<String, Boolean>> getKeepAliveStatus() {
        return ResponseEntity.ok(Map.of("enabled", keepAliveService.isKeepAliveEnabled()));
    }

    @PostMapping("/keep-alive")
    public ResponseEntity<Map<String, Boolean>> toggleKeepAlive(@RequestBody Map<String, Boolean> request) {
        boolean enabled = request.getOrDefault("enabled", false);
        keepAliveService.setKeepAlive(enabled);
        return ResponseEntity.ok(Map.of("enabled", keepAliveService.isKeepAliveEnabled()));
    }
}
