package com.householdops.app.inventory;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public class VendorDtos {

    public record VendorResponse(
            UUID id,
            UUID householdId,
            String name,
            String contactEmail,
            String contactPhone,
            String notes) {

        public static VendorResponse from(Vendor vendor) {
            return new VendorResponse(
                    vendor.getId(),
                    vendor.getHousehold().getId(),
                    vendor.getName(),
                    vendor.getContactEmail(),
                    vendor.getContactPhone(),
                    vendor.getNotes());
        }
    }

    public record CreateVendorRequest(
            @NotBlank String name,
            String contactEmail,
            String contactPhone,
            String notes) {
    }

    public record UpdateVendorRequest(
            String name,
            String contactEmail,
            String contactPhone,
            String notes) {
    }

    private VendorDtos() {
    }
}
