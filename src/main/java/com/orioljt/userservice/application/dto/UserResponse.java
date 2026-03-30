package com.orioljt.userservice.application.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String username,
        String firstName,
        String lastName,
        String bio,
        String avatarUrl,
        Set<String> roles,
        Instant createdAt
) {}
