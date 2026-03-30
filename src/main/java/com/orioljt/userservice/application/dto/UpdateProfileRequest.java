package com.orioljt.userservice.application.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 500) String bio,
        @Size(max = 500) String avatarUrl
) {}
