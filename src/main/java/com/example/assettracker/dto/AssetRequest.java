package com.example.assettracker.dto;

import com.example.assettracker.entity.AssetStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating an asset. "status" is optional and defaults to AVAILABLE.
 * Allowed on creation: AVAILABLE or MAINTENANCE (use the assign endpoint for ASSIGNED).
 */
public record AssetRequest(
        @NotBlank(message = "name must not be blank")
        @Size(max = 150, message = "name must be at most 150 characters")
        String name,

        @NotBlank(message = "serialNumber must not be blank")
        @Size(max = 100, message = "serialNumber must be at most 100 characters")
        String serialNumber,

        @NotNull(message = "categoryId is required")
        Long categoryId,

        AssetStatus status
) {
}
