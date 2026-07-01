package com.householdops.app.staff;

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

import com.householdops.app.security.AuthenticatedPrincipal;
import com.householdops.app.security.SecurityAssertions;
import com.householdops.app.staff.StaffMemberDtos.CreateStaffMemberRequest;
import com.householdops.app.staff.StaffMemberDtos.StaffMemberResponse;
import com.householdops.app.staff.StaffMemberDtos.UpdateStaffMemberRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class StaffMemberController {

    private final StaffMemberService staffMemberService;

    @GetMapping("/api/households/{householdId}/staff")
    public List<StaffMemberResponse> findByHousehold(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        SecurityAssertions.requireHousehold(principal, householdId);
        return staffMemberService.findByHousehold(householdId).stream().map(StaffMemberResponse::from).toList();
    }

    @PreAuthorize("hasAnyRole('OWNER', 'HOUSE_MANAGER')")
    @PostMapping("/api/households/{householdId}/staff")
    public StaffMemberResponse create(
            @PathVariable UUID householdId,
            @Valid @RequestBody CreateStaffMemberRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        SecurityAssertions.requireHousehold(principal, householdId);
        return StaffMemberResponse.from(staffMemberService.create(householdId, request));
    }

    /** Role/active changes are sensitive HR actions, so the whole endpoint is OWNER-only rather than field-conditional. */
    @PreAuthorize("hasRole('OWNER')")
    @PatchMapping("/api/staff/{id}")
    public StaffMemberResponse update(
            @PathVariable UUID id,
            @RequestBody UpdateStaffMemberRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return StaffMemberResponse.from(staffMemberService.update(id, principal.getHouseholdId(), request));
    }
}
