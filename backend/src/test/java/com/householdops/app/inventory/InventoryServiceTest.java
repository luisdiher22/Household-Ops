package com.householdops.app.inventory;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.householdops.app.household.Household;
import com.householdops.app.household.HouseholdRepository;
import com.householdops.app.inventory.InventoryDtos.UpdateInventoryItemRequest;
import com.householdops.app.staff.StaffMemberRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Covers the RESTOCK/CONSUMPTION inference on quantity updates -- the piece of InventoryService's logic with real branching worth testing directly. */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private HouseholdRepository householdRepository;
    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private InventoryAdjustmentRepository adjustmentRepository;
    @Mock
    private StaffMemberRepository staffMemberRepository;

    private InventoryService inventoryService;

    private Household household;
    private InventoryItem item;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(inventoryRepository, householdRepository, vendorRepository, adjustmentRepository, staffMemberRepository);

        household = new Household();
        household.setId(UUID.randomUUID());

        item = new InventoryItem();
        item.setId(UUID.randomUUID());
        item.setHousehold(household);
        item.setCurrentQuantity(5);
        item.setReorderThreshold(2);

        when(inventoryRepository.findById(item.getId())).thenReturn(Optional.of(item));
    }

    @Test
    void increasingQuantityIsLoggedAsRestockByDefault() {
        inventoryService.update(item.getId(), household.getId(), null,
                new UpdateInventoryItemRequest(8, null, null, null, null, null, null));

        ArgumentCaptor<InventoryAdjustment> captor = ArgumentCaptor.forClass(InventoryAdjustment.class);
        verify(adjustmentRepository).save(captor.capture());

        InventoryAdjustment adjustment = captor.getValue();
        assertThat(adjustment.getReason()).isEqualTo(AdjustmentReason.RESTOCK);
        assertThat(adjustment.getPreviousQuantity()).isEqualTo(5);
        assertThat(adjustment.getNewQuantity()).isEqualTo(8);
        assertThat(adjustment.getDelta()).isEqualTo(3);
        assertThat(item.getCurrentQuantity()).isEqualTo(8);
    }

    @Test
    void decreasingQuantityIsLoggedAsConsumptionByDefault() {
        inventoryService.update(item.getId(), household.getId(), null,
                new UpdateInventoryItemRequest(3, null, null, null, null, null, null));

        ArgumentCaptor<InventoryAdjustment> captor = ArgumentCaptor.forClass(InventoryAdjustment.class);
        verify(adjustmentRepository).save(captor.capture());

        assertThat(captor.getValue().getReason()).isEqualTo(AdjustmentReason.CONSUMPTION);
        assertThat(captor.getValue().getDelta()).isEqualTo(-2);
    }

    @Test
    void explicitReasonOverridesInference() {
        // A decrease that's actually a recount correction, not real consumption --
        // shouldn't be counted toward the depletion prediction.
        inventoryService.update(item.getId(), household.getId(), null,
                new UpdateInventoryItemRequest(3, null, null, null, null, null, AdjustmentReason.MANUAL_CORRECTION));

        ArgumentCaptor<InventoryAdjustment> captor = ArgumentCaptor.forClass(InventoryAdjustment.class);
        verify(adjustmentRepository).save(captor.capture());

        assertThat(captor.getValue().getReason()).isEqualTo(AdjustmentReason.MANUAL_CORRECTION);
    }

    @Test
    void noAdjustmentLoggedWhenQuantityIsUnchanged() {
        inventoryService.update(item.getId(), household.getId(), null,
                new UpdateInventoryItemRequest(5, 3, null, null, null, null, null));

        // update() still reads adjustment history to compute the response's
        // predicted-depletion field regardless -- only the *write* path is
        // conditional on the quantity actually changing.
        verify(adjustmentRepository, never()).save(any());
    }

    @Test
    void restockingUpdatesLastRestockedAt() {
        assertThat(item.getLastRestockedAt()).isNull();

        inventoryService.update(item.getId(), household.getId(), null,
                new UpdateInventoryItemRequest(9, null, null, null, null, null, null));

        assertThat(item.getLastRestockedAt()).isNotNull();
    }
}
