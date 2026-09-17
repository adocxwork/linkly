package com.gupta.linkly.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickEvent implements Serializable {
    private UUID linkId;
    private String ip;
    private String userAgent;
}
