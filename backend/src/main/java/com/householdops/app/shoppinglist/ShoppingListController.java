package com.householdops.app.shoppinglist;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.householdops.app.approval.ApprovalService;
import com.householdops.app.approval.ApprovalSubjectType;
import com.householdops.app.household.HouseholdService;
import com.householdops.app.inventory.ReorderJob;
import com.householdops.app.security.AuthenticatedPrincipal;
import com.householdops.app.security.SecurityAssertions;
import com.householdops.app.shoppinglist.ShoppingListDtos.CreateShoppingListItemRequest;
import com.householdops.app.shoppinglist.ShoppingListDtos.ShoppingListItemResponse;
import com.householdops.app.shoppinglist.ShoppingListDtos.UpdateShoppingListItemStatusRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ShoppingListController {

    private final ShoppingListService shoppingListService;
    private final ApprovalService approvalService;
    private final HouseholdService householdService;
    private final ReorderJob reorderJob;

    @GetMapping("/api/households/{householdId}/shopping-list")
    public Page<ShoppingListItemResponse> findByHousehold(
            @PathVariable UUID householdId,
            @RequestParam(required = false) ShoppingListItemStatus status,
            Pageable pageable,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        SecurityAssertions.requireHousehold(principal, householdId);
        return shoppingListService.findByHousehold(householdId, status, pageable).map(this::toResponse);
    }

    @PostMapping("/api/households/{householdId}/shopping-list")
    public ShoppingListItemResponse create(
            @PathVariable UUID householdId,
            @Valid @RequestBody CreateShoppingListItemRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        SecurityAssertions.requireHousehold(principal, householdId);
        return toResponse(shoppingListService.create(householdId, principal.getStaffId(), request));
    }

    /** Manually runs the XML-wired reorder engine for this household -- same effect as waiting for ReorderJob's schedule, but on demand for demo purposes. */
    @PostMapping("/api/households/{householdId}/shopping-list/generate")
    public Map<String, Integer> generate(
            @PathVariable UUID householdId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        SecurityAssertions.requireHousehold(principal, householdId);
        int queued = reorderJob.runForHousehold(householdService.getById(householdId));
        return Map.of("itemsQueued", queued);
    }

    @PatchMapping("/api/shopping-list/{id}")
    public ShoppingListItemResponse updateStatus(
            @PathVariable UUID id,
            @RequestBody UpdateShoppingListItemStatusRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return toResponse(shoppingListService.updateStatus(id, principal.getHouseholdId(), request.status()));
    }

    private ShoppingListItemResponse toResponse(ShoppingListItem item) {
        boolean approvalPending = approvalService.hasPendingApproval(ApprovalSubjectType.SHOPPING_ITEM, item.getId());
        return ShoppingListItemResponse.from(item, approvalPending);
    }
}
