package com.example.assettracker.dto;

import com.example.assettracker.entity.AssetStatus;

import java.time.LocalDateTime;

public record AssetResponse(
        Long id,
        String name,
        String serialNumber,
        AssetStatus status,
        CategoryResponse category,
        UserResponse assignedTo,
        LocalDateTime createdAt
) {
}
