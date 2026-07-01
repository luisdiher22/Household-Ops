package com.householdops.app.inventory;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.householdops.app.household.Household;
import com.householdops.app.inventory.InventoryDtos.InventoryStatusResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryStatusServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Test
    void flagsOnlyItemsAtOrBelowTheirOwnThreshold() {
        InventoryStatusService service = new InventoryStatusService(inventoryRepository);
        UUID householdId = UUID.randomUUID();
        Household household = new Household();
        household.setId(householdId);

        InventoryItem lowStock = item(household, "Olive Oil", 2, 3);
        InventoryItem wellStocked = item(household, "Paper Towels", 10, 4);
        InventoryItem exactlyAtThreshold = item(household, "Pool Chlorine", 2, 2);

        when(inventoryRepository.findByHouseholdId(householdId)).thenReturn(List.of(lowStock, wellStocked, exactlyAtThreshold));

        InventoryStatusResponse status = service.computeStatus(householdId);

        assertThat(status.totalItems()).isEqualTo(3);
        assertThat(status.lowStockCount()).isEqualTo(2);
        assertThat(status.lowStockItems()).extracting("name").containsExactlyInAnyOrder("Olive Oil", "Pool Chlorine");
    }

    private InventoryItem item(Household household, String name, int currentQuantity, int reorderThreshold) {
        InventoryItem item = new InventoryItem();
        item.setId(UUID.randomUUID());
        item.setHousehold(household);
        item.setName(name);
        item.setCategory("PANTRY");
        item.setUnit("units");
        item.setCurrentQuantity(currentQuantity);
        item.setReorderThreshold(reorderThreshold);
        return item;
    }
}
