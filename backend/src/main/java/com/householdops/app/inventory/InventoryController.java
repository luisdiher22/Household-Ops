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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.householdops.app.inventory.InventoryDtos.CreateInventoryItemRequest;
import com.householdops.app.inventory.InventoryDtos.ImportResult;
import com.householdops.app.inventory.InventoryDtos.InventoryAdjustmentResponse;
import com.householdops.app.inventory.InventoryDtos.InventoryItemResponse;
import com.householdops.app.inventory.InventoryDtos.InventoryStatusResponse;
import com.householdops.app.inventory.InventoryDtos.UpdateInventoryItemRequest;
import com.householdops.app.inventory.InventoryDtos.ValuationResponse;
import com.householdops.app.security.AuthenticatedPrincipal;
import com.householdops.app.security.SecurityAssertions;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Unlike tasks (assignment is Owner/Manager-only, see TaskController), any
 * household member can create/update inventory items -- Staff restocking the
 * pantry and updating counts is the normal day-to-day flow this whole
 * project exists for, not something to gate behind a role check. CSV import
 * is the one exception 
 */
@RestController
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryStatusService inventoryStatusService;

    //Get all inventory items for a specific household, ensuring that the authenticated principal has access to that household.
    @GetMapping("/api/households/{householdId}/inventory")
    public List<InventoryItemResponse> findByHousehold(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        SecurityAssertions.requireHousehold(principal, householdId);
        return inventoryService.findByHousehold(householdId);
    }

    // Retrieves the inventory status for a specific household, ensuring that the authenticated principal has access to that household.
    @GetMapping("/api/households/{householdId}/inventory/status")
    public InventoryStatusResponse status(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        SecurityAssertions.requireHousehold(principal, householdId);
        return inventoryStatusService.computeStatus(householdId);
    }

    // Retrieves the valuation of inventory for a specific household, ensuring that the authenticated principal has access to that household.
    @GetMapping("/api/households/{householdId}/inventory/valuation")
    public ValuationResponse valuation(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        SecurityAssertions.requireHousehold(principal, householdId);
        return inventoryService.valuation(householdId);
    }
    // Retrieves the history of inventory adjustments for a specific inventory item, ensuring that the authenticated principal has access to the household associated with that item.
    @GetMapping("/api/inventory/{id}/history")
    public List<InventoryAdjustmentResponse> history(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return inventoryService.history(id, principal.getHouseholdId());
    }

    // Creates a new inventory item for a specific household, ensuring that the authenticated principal has access to that household.
    @PostMapping("/api/households/{householdId}/inventory")
    public InventoryItemResponse create(
            @PathVariable UUID householdId,
            @Valid @RequestBody CreateInventoryItemRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        SecurityAssertions.requireHousehold(principal, householdId);
        return inventoryService.create(householdId, principal.getStaffId(), request);
    }
    // Updates an existing inventory item, ensuring that the authenticated principal has access to the household associated with that item.
    @PatchMapping("/api/inventory/{id}")
    public InventoryItemResponse update(
            @PathVariable UUID id,
            @RequestBody UpdateInventoryItemRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return inventoryService.update(id, principal.getHouseholdId(), principal.getStaffId(), request);
    }

    /** Bulk onboarding is an Owner/Manager action -- same reasoning as vendor set-up */
    // Imports inventory items from a CSV file for a specific household.
    @PreAuthorize("hasAnyRole('OWNER', 'HOUSE_MANAGER')")
    @PostMapping("/api/households/{householdId}/inventory/import")
    public ImportResult importCsv(
            @PathVariable UUID householdId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        SecurityAssertions.requireHousehold(principal, householdId);
        return inventoryService.importCsv(householdId, principal.getStaffId(), file);
    }
}
