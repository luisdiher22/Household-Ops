package com.householdops.app.inventory;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<InventoryItem, UUID> {

    List<InventoryItem> findByHouseholdId(UUID householdId);

    @Query("select i from InventoryItem i where i.household.id = :householdId and i.currentQuantity <= i.reorderThreshold")
    List<InventoryItem> findLowStockByHouseholdId(@Param("householdId") UUID householdId);
}
