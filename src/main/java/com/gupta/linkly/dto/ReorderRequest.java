package com.gupta.linkly.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderRequest {

    @NotNull(message = "linkIds is required")
    @Size(max = 500)
    private List<UUID> linkIds;
}
