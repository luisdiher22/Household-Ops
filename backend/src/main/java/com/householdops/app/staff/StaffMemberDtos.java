package com.householdops.app.staff;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class StaffMemberDtos {

    public record StaffMemberResponse(
            UUID id,
            String fullName,
            String email,
            StaffRole role,
            UUID householdId,
            boolean active) {

        public static StaffMemberResponse from(StaffMember staffMember) {
            return new StaffMemberResponse(
                    staffMember.getId(),
                    staffMember.getFullName(),
                    staffMember.getEmail(),
                    staffMember.getRole(),
                    staffMember.getHousehold().getId(),
                    staffMember.isActive());
        }
    }

    public record CreateStaffMemberRequest(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password,
            @NotNull StaffRole role) {
    }

    public record UpdateStaffMemberRequest(
            String fullName,
            StaffRole role,
            Boolean active) {
    }

    private StaffMemberDtos() {
    }
}
