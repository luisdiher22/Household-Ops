package com.householdops.app.inventory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.householdops.app.common.exception.ResourceNotFoundException;
import com.householdops.app.household.Household;
import com.householdops.app.household.HouseholdRepository;
import com.householdops.app.inventory.InventoryDtos.CreateInventoryItemRequest;
import com.householdops.app.inventory.InventoryDtos.UpdateInventoryItemRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final HouseholdRepository householdRepository;

    @Transactional(readOnly = true)
    public List<InventoryItem> findByHousehold(UUID householdId) {
        return inventoryRepository.findByHouseholdId(householdId);
    }

    @Transactional(readOnly = true)
    public InventoryItem getById(UUID id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + id));
    }

    @Transactional
    public InventoryItem create(UUID householdId, CreateInventoryItemRequest request) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Household not found: " + householdId));

        InventoryItem item = new InventoryItem();
        item.setHousehold(household);
        item.setName(request.name());
        item.setCategory(request.category());
        item.setCurrentQuantity(request.currentQuantity());
        item.setUnit(request.unit());
        item.setReorderThreshold(request.reorderThreshold());
        item.setReorderQuantity(request.reorderQuantity());
        item.setLastRestockedAt(Instant.now());

        return inventoryRepository.save(item);
    }

    @Transactional
    public InventoryItem update(UUID id, UpdateInventoryItemRequest request) {
        InventoryItem item = getById(id);

        if (request.currentQuantity() != null) {
            if (request.currentQuantity() > item.getCurrentQuantity()) {
                item.setLastRestockedAt(Instant.now());
            }
            item.setCurrentQuantity(request.currentQuantity());
        }
        if (request.reorderThreshold() != null) {
            item.setReorderThreshold(request.reorderThreshold());
        }
        if (request.reorderQuantity() != null) {
            item.setReorderQuantity(request.reorderQuantity());
        }

        return item;
    }
}
