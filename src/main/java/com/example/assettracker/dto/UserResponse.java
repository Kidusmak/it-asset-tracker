package com.example.assettracker.dto;

import com.example.assettracker.entity.Role;

public record UserResponse(Long id, String username, Role role) {
}
