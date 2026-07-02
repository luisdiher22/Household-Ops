package com.householdops.app.inventory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.householdops.app.common.exception.ResourceNotFoundException;
import com.householdops.app.household.Household;
import com.householdops.app.household.HouseholdRepository;
import com.householdops.app.inventory.InventoryDtos.CreateInventoryItemRequest;
import com.householdops.app.inventory.InventoryDtos.ImportResult;
import com.householdops.app.inventory.InventoryDtos.InventoryItemResponse;
import com.householdops.app.inventory.InventoryDtos.UpdateInventoryItemRequest;
import com.householdops.app.security.SecurityAssertions;
import com.householdops.app.staff.StaffMember;
import com.householdops.app.staff.StaffMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final HouseholdRepository householdRepository;
    private final VendorRepository vendorRepository;
    private final InventoryAdjustmentRepository adjustmentRepository;
    private final StaffMemberRepository staffMemberRepository;

    // Retrieves all inventory items for a specific household, ensuring that the authenticated principal has access to that household.
    @Transactional(readOnly = true)
    public List<InventoryItemResponse> findByHousehold(UUID householdId) {
        List<InventoryItem> items = inventoryRepository.findByHouseholdId(householdId);
        Map<UUID, String> vendorNamesById = vendorRepository.findByHouseholdId(householdId).stream()
                .collect(Collectors.toMap(Vendor::getId, Vendor::getName));
        Map<UUID, List<InventoryAdjustment>> consumptionByItemId = adjustmentRepository
                .findByHouseholdIdAndReason(householdId, AdjustmentReason.CONSUMPTION).stream()
                .collect(Collectors.groupingBy(a -> a.getInventoryItem().getId()));
        Instant now = Instant.now();

        return items.stream()
                .map(item -> toResponse(item, vendorNamesById, consumptionByItemId, now))
                .toList();
    }

    // Retrieves a specific inventory item by its ID, ensuring that the authenticated principal has access to the household associated with that item.
    @Transactional(readOnly = true)
    public InventoryItem getById(UUID id, UUID callerHouseholdId) {
        InventoryItem item = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + id));
        SecurityAssertions.requireHousehold(callerHouseholdId, item.getHousehold().getId());
        return item;
    }
    // Creates a new inventory item for a specific household, ensuring that the authenticated principal has access to that household.
    @CacheEvict(value = "inventoryStatus", key = "#householdId")
    @Transactional
    public InventoryItemResponse create(UUID householdId, UUID actingStaffId, CreateInventoryItemRequest request) {
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
        item.setUnitCost(request.unitCost());
        item.setExpirationDate(request.expirationDate());
        item.setLastRestockedAt(Instant.now());

        if (request.vendorId() != null) {
            item.setPreferredVendor(requireVendor(request.vendorId(), householdId));
        }

        item = inventoryRepository.save(item);

        if (request.currentQuantity() > 0) {
            logAdjustment(item, household, 0, request.currentQuantity(), AdjustmentReason.INITIAL, actingStaffId);
        }

        return toResponse(item, vendorNameOrNull(item), List.of(), Instant.now());
    }

    // Updates an existing inventory item, ensuring that the authenticated principal has access to the household associated with that item.
    @CacheEvict(value = "inventoryStatus", key = "#callerHouseholdId")
    @Transactional
    public InventoryItemResponse update(UUID id, UUID callerHouseholdId, UUID actingStaffId, UpdateInventoryItemRequest request) {
        InventoryItem item = getById(id, callerHouseholdId);
        Household household = item.getHousehold();

        if (request.currentQuantity() != null && request.currentQuantity() != item.getCurrentQuantity()) {
            int previous = item.getCurrentQuantity();
            int updated = request.currentQuantity();
            AdjustmentReason reason = request.reason() != null
                    ? request.reason()
                    : (updated > previous ? AdjustmentReason.RESTOCK : AdjustmentReason.CONSUMPTION);

            if (updated > previous) {
                item.setLastRestockedAt(Instant.now());
            }
            item.setCurrentQuantity(updated);
            logAdjustment(item, household, previous, updated, reason, actingStaffId);
        }
        if (request.reorderThreshold() != null) {
            item.setReorderThreshold(request.reorderThreshold());
        }
        if (request.reorderQuantity() != null) {
            item.setReorderQuantity(request.reorderQuantity());
        }
        if (request.unitCost() != null) {
            item.setUnitCost(request.unitCost());
        }
        if (request.expirationDate() != null) {
            item.setExpirationDate(request.expirationDate());
        }
        if (request.vendorId() != null) {
            item.setPreferredVendor(requireVendor(request.vendorId(), callerHouseholdId));
        }

        return toResponse(item, vendorNameOrNull(item), consumptionHistoryFor(item.getId()), Instant.now());
    }

    // Retrieves the valuation of inventory for a specific household, ensuring that the authenticated principal has access to that household.
    @Transactional(readOnly = true)
    public InventoryDtos.ValuationResponse valuation(UUID householdId) {
        List<InventoryItem> items = inventoryRepository.findByHouseholdId(householdId);

        Map<String, List<InventoryItem>> byCategory = items.stream()
                .filter(item -> item.getUnitCost() != null)
                .collect(Collectors.groupingBy(InventoryItem::getCategory));

        List<InventoryDtos.CategoryValuation> categoryValuations = byCategory.entrySet().stream()
                .map(entry -> new InventoryDtos.CategoryValuation(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(item -> item.getUnitCost().multiply(BigDecimal.valueOf(item.getCurrentQuantity())))
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        entry.getValue().size()))
                .toList();

        BigDecimal total = categoryValuations.stream()
                .map(InventoryDtos.CategoryValuation::totalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new InventoryDtos.ValuationResponse(householdId, total, categoryValuations);
    }

    // Retrieves the history of inventory adjustments for a specific inventory item, ensuring that the authenticated principal has access to the household associated with that item.
    @Transactional(readOnly = true)
    public List<InventoryDtos.InventoryAdjustmentResponse> history(UUID itemId, UUID callerHouseholdId) {
        InventoryItem item = getById(itemId, callerHouseholdId);
        return adjustmentRepository
                .findByInventoryItemIdOrderByOccurredAtDesc(item.getId(), org.springframework.data.domain.PageRequest.of(0, 100))
                .stream()
                .map(InventoryDtos.InventoryAdjustmentResponse::from)
                .toList();
    }

    /**
     * Csv import format: name,category,unit,currentQuantity,reorderThreshold,reorderQuantity,unitCost
     * (unitCost is optional)
     */
    @CacheEvict(value = "inventoryStatus", key = "#householdId")
    @Transactional
    public ImportResult importCsv(UUID householdId, UUID actingStaffId, MultipartFile file) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Household not found: " + householdId));

        List<String> errors = new ArrayList<>();
        int imported = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) {
                return new ImportResult(0, List.of("File is empty"));
            }

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    importRow(household, line, actingStaffId);
                    imported++;
                } catch (Exception e) {
                    errors.add("Row " + rowNumber + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            errors.add("Could not read file: " + e.getMessage());
        }

        return new ImportResult(imported, errors);
    }

    // Helper methods
    private void importRow(Household household, String line, UUID actingStaffId) {
        // name,category,unit,currentQuantity,reorderThreshold,reorderQuantity,unitCost
        String[] columns = line.split(",", -1);
        if (columns.length < 6) {
            throw new IllegalArgumentException("Expected at least 6 columns, got " + columns.length);
        }

        InventoryItem item = new InventoryItem();
        item.setHousehold(household);
        item.setName(columns[0].trim());
        item.setCategory(columns[1].trim());
        item.setUnit(columns[2].trim());
        int quantity = Integer.parseInt(columns[3].trim());
        item.setCurrentQuantity(quantity);
        item.setReorderThreshold(Integer.parseInt(columns[4].trim()));
        item.setReorderQuantity(Integer.parseInt(columns[5].trim()));
        if (columns.length > 6 && !columns[6].isBlank()) {
            item.setUnitCost(new BigDecimal(columns[6].trim()));
        }
        item.setLastRestockedAt(Instant.now());

        item = inventoryRepository.save(item);
        if (quantity > 0) {
            logAdjustment(item, household, 0, quantity, AdjustmentReason.INITIAL, actingStaffId);
        }
    }

    private void logAdjustment(InventoryItem item, Household household, int previous, int updated, AdjustmentReason reason, UUID actingStaffId) {
        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setInventoryItem(item);
        adjustment.setHousehold(household);
        adjustment.setPreviousQuantity(previous);
        adjustment.setNewQuantity(updated);
        adjustment.setDelta(updated - previous);
        adjustment.setReason(reason);
        adjustment.setOccurredAt(Instant.now());
        if (actingStaffId != null) {
            StaffMember staffMember = staffMemberRepository.findById(actingStaffId).orElse(null);
            adjustment.setAdjustedBy(staffMember);
        }
        adjustmentRepository.save(adjustment);
    }

    private Vendor requireVendor(UUID vendorId, UUID householdId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + vendorId));
        SecurityAssertions.requireHousehold(householdId, vendor.getHousehold().getId());
        return vendor;
    }

    private String vendorNameOrNull(InventoryItem item) {
        return item.getPreferredVendor() != null ? item.getPreferredVendor().getName() : null;
    }

    private List<InventoryAdjustment> consumptionHistoryFor(UUID itemId) {
        return adjustmentRepository.findByInventoryItemIdOrderByOccurredAtDesc(itemId, org.springframework.data.domain.PageRequest.of(0, 1000))
                .stream()
                .filter(a -> a.getReason() == AdjustmentReason.CONSUMPTION)
                .toList();
    }

    private InventoryItemResponse toResponse(
            InventoryItem item,
            Map<UUID, String> vendorNamesById,
            Map<UUID, List<InventoryAdjustment>> consumptionByItemId,
            Instant now) {
        String vendorName = item.getPreferredVendor() != null ? vendorNamesById.get(item.getPreferredVendor().getId()) : null;
        Integer predicted = ConsumptionPredictor.predictDaysUntilEmpty(
                consumptionByItemId.getOrDefault(item.getId(), List.of()), item.getCurrentQuantity(), now);
        return InventoryItemResponse.from(item, vendorName, predicted);
    }

    private InventoryItemResponse toResponse(InventoryItem item, String vendorName, List<InventoryAdjustment> consumptionHistory, Instant now) {
        Integer predicted = ConsumptionPredictor.predictDaysUntilEmpty(consumptionHistory, item.getCurrentQuantity(), now);
        return InventoryItemResponse.from(item, vendorName, predicted);
    }
}
