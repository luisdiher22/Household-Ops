package com.householdops.app.inventory;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.householdops.app.inventory.VendorDtos.CreateVendorRequest;
import com.householdops.app.inventory.VendorDtos.UpdateVendorRequest;
import com.householdops.app.inventory.VendorDtos.VendorResponse;
import com.householdops.app.security.AuthenticatedPrincipal;
import com.householdops.app.security.SecurityAssertions;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Vendor set-up (unlike day-to-day inventory quantity updates) is an Owner/Manager action, same reasoning as StaffMemberController.create(). */
@RestController
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @GetMapping("/api/households/{householdId}/vendors")
    public List<VendorResponse> findByHousehold(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        SecurityAssertions.requireHousehold(principal, householdId);
        return vendorService.findByHousehold(householdId).stream().map(VendorResponse::from).toList();
    }

    @PreAuthorize("hasAnyRole('OWNER', 'HOUSE_MANAGER')")
    @PostMapping("/api/households/{householdId}/vendors")
    public VendorResponse create(
            @PathVariable UUID householdId,
            @Valid @RequestBody CreateVendorRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        SecurityAssertions.requireHousehold(principal, householdId);
        return VendorResponse.from(vendorService.create(householdId, request));
    }

    @PreAuthorize("hasAnyRole('OWNER', 'HOUSE_MANAGER')")
    @PatchMapping("/api/vendors/{id}")
    public VendorResponse update(
            @PathVariable UUID id,
            @RequestBody UpdateVendorRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return VendorResponse.from(vendorService.update(id, principal.getHouseholdId(), request));
    }
}
