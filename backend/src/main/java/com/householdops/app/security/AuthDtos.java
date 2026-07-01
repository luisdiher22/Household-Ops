package com.householdops.app.security;

import java.util.UUID;

import com.householdops.app.staff.StaffRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AuthDtos {

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    // activeHouseholdId is nullable and optional for backward compatibility --
    // omitting it (or a stale/revoked one) just falls back to the staff
    // member's own household rather than failing the refresh outright.
    public record RefreshRequest(@NotBlank String refreshToken, UUID activeHouseholdId) {
    }

    public record SwitchHouseholdRequest(@NotNull UUID householdId) {
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
