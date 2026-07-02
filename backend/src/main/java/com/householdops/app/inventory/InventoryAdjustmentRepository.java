package com.householdops.app.inventory;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, UUID> {

    List<InventoryAdjustment> findByInventoryItemIdOrderByOccurredAtDesc(UUID inventoryItemId, Pageable pageable);

   
    List<InventoryAdjustment> findByHouseholdIdAndReason(UUID householdId, AdjustmentReason reason);
}
