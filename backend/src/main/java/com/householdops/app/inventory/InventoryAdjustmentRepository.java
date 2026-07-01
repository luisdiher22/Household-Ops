package com.householdops.app.inventory;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, UUID> {

    List<InventoryAdjustment> findByInventoryItemIdOrderByOccurredAtDesc(UUID inventoryItemId, Pageable pageable);

    /** Fetched once per household (not per item) so computing predictions for a whole list doesn't N+1 query. */
    List<InventoryAdjustment> findByHouseholdIdAndReason(UUID householdId, AdjustmentReason reason);
}
