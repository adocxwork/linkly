package com.gupta.linkly.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageRequest {

    @Size(max = 50, message = "Name must be at most 50 characters")
    private String senderName;

    @NotBlank(message = "Message content is required")
    @Size(max = 1000, message = "Message must be at most 1000 characters")
    private String content;
}
