package com.householdops.app.security;

import java.util.UUID;

import com.householdops.app.staff.StaffRole;

import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            UUID staffId,
            UUID householdId,
            String fullName,
            StaffRole role) {
    }

    private AuthDtos() {
    }
}
