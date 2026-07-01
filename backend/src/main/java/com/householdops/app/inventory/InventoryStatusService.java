package com.householdops.app.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.householdops.app.inventory.InventoryDtos.InventoryItemResponse;
import com.householdops.app.inventory.InventoryDtos.InventoryStatusResponse;

import lombok.RequiredArgsConstructor;

/**
 * Computes the low-stock/expiring-soon/running-out-soon summary for a
 * household. This is the read-heavy, moderately expensive aggregation --
 * hit by the dashboard and by the assistant's inventory tool on nearly
 * every household-scoped query -- so it's backed by a Redis cache-aside,
 * evicted explicitly on inventory writes (see InventoryService's
 * @CacheEvict calls) rather than left to the 5-minute default TTL alone.
 */
@Service
@RequiredArgsConstructor
public class InventoryStatusService {

    private static final int RUNNING_OUT_SOON_DAYS = 7;

    private final InventoryRepository inventoryRepository;
    private final VendorRepository vendorRepository;
    private final InventoryAdjustmentRepository adjustmentRepository;

    @Cacheable(value = "inventoryStatus", key = "#householdId")
    @Transactional(readOnly = true)
    public InventoryStatusResponse computeStatus(UUID householdId) {
        List<InventoryItem> all = inventoryRepository.findByHouseholdId(householdId);
        Map<UUID, String> vendorNamesById = vendorRepository.findByHouseholdId(householdId).stream()
                .collect(Collectors.toMap(Vendor::getId, Vendor::getName));
        Map<UUID, List<InventoryAdjustment>> consumptionByItemId = adjustmentRepository
                .findByHouseholdIdAndReason(householdId, AdjustmentReason.CONSUMPTION).stream()
                .collect(Collectors.groupingBy(a -> a.getInventoryItem().getId()));
        Instant now = Instant.now();

        List<InventoryItemResponse> responses = all.stream()
                .map(item -> {
                    String vendorName = item.getPreferredVendor() != null ? vendorNamesById.get(item.getPreferredVendor().getId()) : null;
                    Integer predicted = ConsumptionPredictor.predictDaysUntilEmpty(
                            consumptionByItemId.getOrDefault(item.getId(), List.of()), item.getCurrentQuantity(), now);
                    return InventoryItemResponse.from(item, vendorName, predicted);
                })
                .toList();

        List<InventoryItemResponse> lowStock = responses.stream().filter(InventoryItemResponse::lowStock).toList();
        List<InventoryItemResponse> expiringSoon = responses.stream().filter(InventoryItemResponse::expiringSoon).toList();
        List<InventoryItemResponse> runningOutSoon = responses.stream()
                .filter(r -> r.predictedDaysUntilEmpty() != null && r.predictedDaysUntilEmpty() <= RUNNING_OUT_SOON_DAYS)
                .toList();

        return new InventoryStatusResponse(householdId, responses.size(), lowStock.size(), lowStock, expiringSoon, runningOutSoon);
    }
}
