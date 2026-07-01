package com.householdops.app.household;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public class HouseholdDtos {

    public record HouseholdResponse(
            UUID id,
            String name,
            String address,
            String timezone,
            UUID principalUserId,
            BigDecimal approvalThreshold,
            Instant createdAt,
            Instant updatedAt) {

        public static HouseholdResponse from(Household household) {
            return new HouseholdResponse(
                    household.getId(),
                    household.getName(),
                    household.getAddress(),
                    household.getTimezone(),
                    household.getPrincipalUser() != null ? household.getPrincipalUser().getId() : null,
                    household.getApprovalThreshold(),
                    household.getCreatedAt(),
                    household.getUpdatedAt());
        }
    }

    public record CreateHouseholdRequest(
            @NotBlank String name,
            @NotBlank String address,
            String timezone,
            BigDecimal approvalThreshold) {
    }

    public record UpdateHouseholdRequest(
            String name,
            String address,
            String timezone,
            BigDecimal approvalThreshold,
            UUID principalUserId) {
    }

    private HouseholdDtos() {
    }
}
