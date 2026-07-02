package com.householdops.app.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public class InventoryDtos {

    private static final int EXPIRING_SOON_WINDOW_DAYS = 14;

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
            Instant lastRestockedAt,
            UUID vendorId,
            String vendorName,
            BigDecimal unitCost,
            BigDecimal totalValue,
            LocalDate expirationDate,
            boolean expiringSoon,
            Integer predictedDaysUntilEmpty) {

        /** vendorName and predictedDaysUntilEmpty come from batch-fetched context (see InventoryService), not the entity alone */
        public static InventoryItemResponse from(InventoryItem item, String vendorName, Integer predictedDaysUntilEmpty) {
            BigDecimal totalValue = item.getUnitCost() != null
                    ? item.getUnitCost().multiply(BigDecimal.valueOf(item.getCurrentQuantity()))
                    : null;
            boolean expiringSoon = item.getExpirationDate() != null
                    && !item.getExpirationDate().isAfter(LocalDate.now().plusDays(EXPIRING_SOON_WINDOW_DAYS));

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
                    item.getLastRestockedAt(),
                    item.getPreferredVendor() != null ? item.getPreferredVendor().getId() : null,
                    vendorName,
                    item.getUnitCost(),
                    totalValue,
                    item.getExpirationDate(),
                    expiringSoon,
                    predictedDaysUntilEmpty);
        }
    }

    public record CreateInventoryItemRequest(
            @NotBlank String name,
            @NotBlank String category,
            @PositiveOrZero int currentQuantity,
            @NotBlank String unit,
            @PositiveOrZero int reorderThreshold,
            @PositiveOrZero int reorderQuantity,
            UUID vendorId,
            BigDecimal unitCost,
            LocalDate expirationDate) {
    }

    // reason lets a caller mark a quantity change as a MANUAL_CORRECTION (e.g.
    // after a physical recount) so it's excluded from the consumption-rate
    // prediction; if omitted, the sign of the change infers RESTOCK/CONSUMPTION.
    public record UpdateInventoryItemRequest(
            Integer currentQuantity,
            Integer reorderThreshold,
            Integer reorderQuantity,
            UUID vendorId,
            BigDecimal unitCost,
            LocalDate expirationDate,
            AdjustmentReason reason) {
    }

    public record InventoryStatusResponse(
            UUID householdId,
            int totalItems,
            int lowStockCount,
            List<InventoryItemResponse> lowStockItems,
            List<InventoryItemResponse> expiringSoonItems,
            List<InventoryItemResponse> runningOutSoonItems) {
    }

    public record CategoryValuation(String category, BigDecimal totalValue, int itemCount) {
    }

    public record ValuationResponse(UUID householdId, BigDecimal totalValue, List<CategoryValuation> byCategory) {
    }

    public record InventoryAdjustmentResponse(
            UUID id,
            UUID inventoryItemId,
            int previousQuantity,
            int newQuantity,
            int delta,
            AdjustmentReason reason,
            UUID adjustedById,
            String adjustedByName,
            Instant occurredAt) {

        public static InventoryAdjustmentResponse from(InventoryAdjustment adjustment) {
            return new InventoryAdjustmentResponse(
                    adjustment.getId(),
                    adjustment.getInventoryItem().getId(),
                    adjustment.getPreviousQuantity(),
                    adjustment.getNewQuantity(),
                    adjustment.getDelta(),
                    adjustment.getReason(),
                    adjustment.getAdjustedBy() != null ? adjustment.getAdjustedBy().getId() : null,
                    adjustment.getAdjustedBy() != null ? adjustment.getAdjustedBy().getFullName() : null,
                    adjustment.getOccurredAt());
        }
    }

    public record ImportResult(int imported, List<String> errors) {
    }

    private InventoryDtos() {
    }
}
