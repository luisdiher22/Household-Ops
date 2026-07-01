package com.householdops.app.inventory;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.householdops.app.inventory.InventoryDtos.CreateInventoryItemRequest;
import com.householdops.app.inventory.InventoryDtos.InventoryItemResponse;
import com.householdops.app.inventory.InventoryDtos.InventoryStatusResponse;
import com.householdops.app.inventory.InventoryDtos.UpdateInventoryItemRequest;
import com.householdops.app.security.AuthenticatedPrincipal;
import com.householdops.app.security.SecurityAssertions;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryStatusService inventoryStatusService;

    @GetMapping("/api/households/{householdId}/inventory")
    public List<InventoryItemResponse> findByHousehold(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        SecurityAssertions.requireHousehold(principal, householdId);
        return inventoryService.findByHousehold(householdId).stream().map(InventoryItemResponse::from).toList();
    }

    @GetMapping("/api/households/{householdId}/inventory/status")
    public InventoryStatusResponse status(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        SecurityAssertions.requireHousehold(principal, householdId);
        return inventoryStatusService.computeStatus(householdId);
    }

    @PostMapping("/api/households/{householdId}/inventory")
    public InventoryItemResponse create(
            @PathVariable UUID householdId,
            @Valid @RequestBody CreateInventoryItemRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        SecurityAssertions.requireHousehold(principal, householdId);
        return InventoryItemResponse.from(inventoryService.create(householdId, request));
    }

    @PatchMapping("/api/inventory/{id}")
    public InventoryItemResponse update(
            @PathVariable UUID id,
            @RequestBody UpdateInventoryItemRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return InventoryItemResponse.from(inventoryService.update(id, principal.getHouseholdId(), request));
    }
}
