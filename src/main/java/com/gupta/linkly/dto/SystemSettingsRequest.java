package com.gupta.linkly.dto;

/** Admin switches; null means "leave unchanged". */
public record SystemSettingsRequest(Boolean keepAliveEnabled, Boolean redisEnabled) {
}
