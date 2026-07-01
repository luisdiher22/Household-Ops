package com.householdops.app.inventory;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.householdops.app.inventory.InventoryDtos.InventoryItemResponse;
import com.householdops.app.inventory.InventoryDtos.InventoryStatusResponse;

import lombok.RequiredArgsConstructor;

/**
 * Computes the low-stock summary for a household. This is the read-heavy,
 * moderately expensive aggregation that Week 2 backs with a Redis
 * cache-aside (@Cacheable, evicted on inventory writes) — it's hit by the
 * dashboard and by the assistant's InventoryStatusTool on nearly every
 * household-scoped query.
 */
@Service
@RequiredArgsConstructor
public class InventoryStatusService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true)
    public InventoryStatusResponse computeStatus(UUID householdId) {
        List<InventoryItem> all = inventoryRepository.findByHouseholdId(householdId);
        List<InventoryItemResponse> lowStock = all.stream()
                .filter(item -> item.getCurrentQuantity() <= item.getReorderThreshold())
                .map(InventoryItemResponse::from)
                .toList();

        return new InventoryStatusResponse(householdId, all.size(), lowStock.size(), lowStock);
    }
}
