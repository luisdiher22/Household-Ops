package com.householdops.app.household;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.householdops.app.household.HouseholdDtos.CreateHouseholdRequest;
import com.householdops.app.household.HouseholdDtos.HouseholdResponse;
import com.householdops.app.household.HouseholdDtos.UpdateHouseholdRequest;
import com.householdops.app.security.AuthenticatedPrincipal;
import com.householdops.app.security.SecurityAssertions;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/households")
@RequiredArgsConstructor
public class HouseholdController {

    private final HouseholdService householdService;

    /** Every staff member belongs to exactly one household, so "list" is just that one. */
    @GetMapping
    public List<HouseholdResponse> findAll(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return List.of(HouseholdResponse.from(householdService.getById(principal.getHouseholdId())));
    }

    @GetMapping("/{id}")
    public HouseholdResponse getById(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        SecurityAssertions.requireHousehold(principal, id);
        return HouseholdResponse.from(householdService.getById(id));
    }

    /** No household to scope against yet -- an OWNER provisioning an additional household (e.g. a second property). */
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping
    public ResponseEntity<HouseholdResponse> create(@Valid @RequestBody CreateHouseholdRequest request) {
        Household household = householdService.create(request);
        return ResponseEntity.ok(HouseholdResponse.from(household));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PatchMapping("/{id}")
    public HouseholdResponse update(
            @PathVariable UUID id,
            @RequestBody UpdateHouseholdRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        SecurityAssertions.requireHousehold(principal, id);
        return HouseholdResponse.from(householdService.update(id, request));
    }
}
