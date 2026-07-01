package com.householdops.app.shoppinglist;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.householdops.app.approval.ApprovalService;
import com.householdops.app.approval.ApprovalSubjectType;
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

    @GetMapping("/api/households/{householdId}/shopping-list")
    public Page<ShoppingListItemResponse> findByHousehold(
            @PathVariable UUID householdId,
            @RequestParam(required = false) ShoppingListItemStatus status,
            Pageable pageable) {
        return shoppingListService.findByHousehold(householdId, status, pageable).map(this::toResponse);
    }

    @PostMapping("/api/households/{householdId}/shopping-list")
    public ShoppingListItemResponse create(@PathVariable UUID householdId, @Valid @RequestBody CreateShoppingListItemRequest request) {
        return toResponse(shoppingListService.create(householdId, request));
    }

    @PatchMapping("/api/shopping-list/{id}")
    public ShoppingListItemResponse updateStatus(@PathVariable UUID id, @RequestBody UpdateShoppingListItemStatusRequest request) {
        return toResponse(shoppingListService.updateStatus(id, request.status()));
    }

    private ShoppingListItemResponse toResponse(ShoppingListItem item) {
        boolean approvalPending = approvalService.hasPendingApproval(ApprovalSubjectType.SHOPPING_ITEM, item.getId());
        return ShoppingListItemResponse.from(item, approvalPending);
    }
}
