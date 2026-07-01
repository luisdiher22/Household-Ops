package com.householdops.app.inventory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public class InventoryDtos {

    public record InventoryItemResponse(
            UUID id,
            UUID householdId,
            String name,
            String category,
            int currentQuantity,
            String unit,
            int reorderThreshold,
            int reorderQuantity,
            boolean lowStock,
            Instant lastRestockedAt) {

        public static InventoryItemResponse from(InventoryItem item) {
            return new InventoryItemResponse(
                    item.getId(),
                    item.getHousehold().getId(),
                    item.getName(),
                    item.getCategory(),
                    item.getCurrentQuantity(),
                    item.getUnit(),
                    item.getReorderThreshold(),
                    item.getReorderQuantity(),
                    item.getCurrentQuantity() <= item.getReorderThreshold(),
                    item.getLastRestockedAt());
        }
    }

    public record CreateInventoryItemRequest(
            @NotBlank String name,
            @NotBlank String category,
            @PositiveOrZero int currentQuantity,
            @NotBlank String unit,
            @PositiveOrZero int reorderThreshold,
            @PositiveOrZero int reorderQuantity) {
    }

    public record UpdateInventoryItemRequest(
            Integer currentQuantity,
            Integer reorderThreshold,
            Integer reorderQuantity) {
    }

    public record InventoryStatusResponse(
            UUID householdId,
            int totalItems,
            int lowStockCount,
            List<InventoryItemResponse> lowStockItems) {
    }

    private InventoryDtos() {
    }
}
