package com.householdops.app.bootstrap;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.householdops.app.household.Household;
import com.householdops.app.household.HouseholdAccessGrant;
import com.householdops.app.household.HouseholdAccessGrantRepository;
import com.householdops.app.household.HouseholdDtos.CreateHouseholdRequest;
import com.householdops.app.household.HouseholdDtos.UpdateHouseholdRequest;
import com.householdops.app.household.HouseholdRepository;
import com.householdops.app.household.HouseholdService;
import com.householdops.app.inventory.AdjustmentReason;
import com.householdops.app.inventory.InventoryAdjustment;
import com.householdops.app.inventory.InventoryAdjustmentRepository;
import com.householdops.app.inventory.InventoryDtos.CreateInventoryItemRequest;
import com.householdops.app.inventory.InventoryDtos.InventoryItemResponse;
import com.householdops.app.inventory.InventoryItem;
import com.householdops.app.inventory.InventoryService;
import com.householdops.app.inventory.Vendor;
import com.householdops.app.inventory.VendorDtos.CreateVendorRequest;
import com.householdops.app.inventory.VendorService;
import com.householdops.app.shoppinglist.ShoppingListDtos.CreateShoppingListItemRequest;
import com.householdops.app.shoppinglist.ShoppingListService;
import com.householdops.app.staff.StaffMember;
import com.householdops.app.staff.StaffMemberDtos.CreateStaffMemberRequest;
import com.householdops.app.staff.StaffMemberService;
import com.householdops.app.staff.StaffRole;
import com.householdops.app.task.TaskDtos.CreateTaskRequest;
import com.householdops.app.task.TaskService;

/**
 * Seeds one demo household with staff across all four roles, so the API and
 * (later) the frontend are demoable immediately after startup without any
 * manual setup. 
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_PASSWORD = "password123";

    private final HouseholdRepository householdRepository;
    private final HouseholdService householdService;
    private final HouseholdAccessGrantRepository accessGrantRepository;
    private final StaffMemberService staffMemberService;
    private final InventoryService inventoryService;
    private final VendorService vendorService;
    private final InventoryAdjustmentRepository adjustmentRepository;
    private final TaskService taskService;
    private final ShoppingListService shoppingListService;

    public DemoDataSeeder(
            HouseholdRepository householdRepository,
            HouseholdService householdService,
            HouseholdAccessGrantRepository accessGrantRepository,
            StaffMemberService staffMemberService,
            InventoryService inventoryService,
            VendorService vendorService,
            InventoryAdjustmentRepository adjustmentRepository,
            TaskService taskService,
            ShoppingListService shoppingListService) {
        this.householdRepository = householdRepository;
        this.householdService = householdService;
        this.accessGrantRepository = accessGrantRepository;
        this.staffMemberService = staffMemberService;
        this.inventoryService = inventoryService;
        this.vendorService = vendorService;
        this.adjustmentRepository = adjustmentRepository;
        this.taskService = taskService;
        this.shoppingListService = shoppingListService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (householdRepository.count() > 0) {
            log.info("Demo data already present, skipping seed");
            return;
        }

        Household aspen = householdService.create(
                new CreateHouseholdRequest("Aspen House", "123 Powder Ridge Rd, Aspen, CO", "America/Denver", BigDecimal.valueOf(250)));
        UUID householdId = aspen.getId();

        StaffMember owner = staffMemberService.create(householdId,
                new CreateStaffMemberRequest("Evelyn Cross", "owner@householdops.dev", DEMO_PASSWORD, StaffRole.OWNER));
        StaffMember manager = staffMemberService.create(householdId,
                new CreateStaffMemberRequest("Marcus Bell", "manager@householdops.dev", DEMO_PASSWORD, StaffRole.HOUSE_MANAGER));
        StaffMember staff = staffMemberService.create(householdId,
                new CreateStaffMemberRequest("Priya Nair", "staff@householdops.dev", DEMO_PASSWORD, StaffRole.STAFF));
        staffMemberService.create(householdId,
                new CreateStaffMemberRequest("Dana Ruiz (Vendor)", "vendor@householdops.dev", DEMO_PASSWORD, StaffRole.VENDOR));

        householdService.update(householdId, new UpdateHouseholdRequest(null, null, null, null, owner.getId()));

        Vendor freshMarket = vendorService.create(householdId,
                new CreateVendorRequest("Fresh Market Co.", "orders@freshmarketco.example", "555-0101", "Weekly grocery delivery"));
        Vendor cleanPro = vendorService.create(householdId,
                new CreateVendorRequest("CleanPro Supplies", "sales@cleanpro.example", "555-0102", null));

        InventoryItemResponse oliveOil = inventoryService.create(householdId, owner.getId(), new CreateInventoryItemRequest(
                "Olive Oil", "PANTRY", 2, "bottles", 3, 4, freshMarket.getId(), BigDecimal.valueOf(18.50), null));
        inventoryService.create(householdId, owner.getId(), new CreateInventoryItemRequest(
                "Paper Towels", "CLEANING", 10, "rolls", 4, 12, cleanPro.getId(), BigDecimal.valueOf(2.25), null));
        inventoryService.create(householdId, owner.getId(), new CreateInventoryItemRequest(
                "Pool Chlorine", "MAINTENANCE_SUPPLY", 1, "buckets", 2, 3, null, BigDecimal.valueOf(45.00), null));
        inventoryService.create(householdId, owner.getId(), new CreateInventoryItemRequest(
                "Champagne (Dom Perignon)", "WINE_CELLAR", 6, "bottles", 2, 6, freshMarket.getId(), BigDecimal.valueOf(220.00),
                LocalDate.now().plusDays(10)));

        // Backdated consumption history so the "days until empty" prediction has
        // something to compute from immediately, without waiting on real usage.
        seedConsumptionHistory(aspen, oliveOil.id(), 6, 4, 10);
        seedConsumptionHistory(aspen, oliveOil.id(), 4, 2, 5);

        taskService.create(householdId, manager.getId(), new CreateTaskRequest(
                "Restock pantry", "Weekly grocery run", staff.getId(),
                LocalDate.now().plusDays(3), BigDecimal.valueOf(150)));

        // Estimated cost (800) exceeds the household's approval threshold (250),
        // so this seeds a real PENDING ApprovalRequest via TaskService's own logic.
        taskService.create(householdId, manager.getId(), new CreateTaskRequest(
                "Repair pool pump", "Pump is making noise, needs a technician", null,
                LocalDate.now().plusDays(5), BigDecimal.valueOf(800)));

        shoppingListService.create(householdId, staff.getId(), new CreateShoppingListItemRequest(
                "Dry cleaning pickup", 1, BigDecimal.valueOf(40), null));

        log.info("Seeded demo household '{}' ({}) with 4 staff, 2 vendors, 4 inventory items, 2 tasks, 1 shopping item", aspen.getName(), householdId);
        log.info("Demo login credentials (password for all: '{}'): owner@householdops.dev, manager@householdops.dev, staff@householdops.dev, vendor@householdops.dev", DEMO_PASSWORD);

        seedSecondProperty(owner);
    }

    /**
     * A second household with its own local staff, granted to the Aspen
     * owner via HouseholdAccessGrant -- gives the portfolio overview
     * (PortfolioController) something real to show: one Owner, two
     * properties, each with its own on-the-ground manager but the same
     * approval authority.
     */
    private void seedSecondProperty(StaffMember ownerAcrossProperties) {
        Household miami = householdService.create(
                new CreateHouseholdRequest("Miami Beach Villa", "88 Ocean Drive, Miami Beach, FL", "America/New_York", BigDecimal.valueOf(250)));
        UUID miamiId = miami.getId();

        StaffMember miamiManager = staffMemberService.create(miamiId,
                new CreateStaffMemberRequest("Sofia Reyes", "manager-miami@householdops.dev", DEMO_PASSWORD, StaffRole.HOUSE_MANAGER));

        // Same principal as Aspen House -- one Owner across both properties,
        // consistent with what HouseholdAccessGrant models below.
        householdService.update(miamiId, new UpdateHouseholdRequest(null, null, null, null, ownerAcrossProperties.getId()));

        Vendor oceanSupply = vendorService.create(miamiId,
                new CreateVendorRequest("Ocean Supply Co.", "orders@oceansupply.example", "555-0201", "Pool and dock maintenance"));

        inventoryService.create(miamiId, miamiManager.getId(), new CreateInventoryItemRequest(
                "Pool Salt", "MAINTENANCE_SUPPLY", 1, "bags", 2, 4, oceanSupply.getId(), BigDecimal.valueOf(12.00), null));
        inventoryService.create(miamiId, miamiManager.getId(), new CreateInventoryItemRequest(
                "Beach Towels", "LINENS", 20, "count", 8, 12, null, BigDecimal.valueOf(9.50), null));

        HouseholdAccessGrant grant = new HouseholdAccessGrant();
        grant.setOwner(ownerAcrossProperties);
        grant.setHousehold(miami);
        accessGrantRepository.save(grant);

        log.info("Seeded second property '{}' ({}) with 1 local manager, 1 vendor, 2 inventory items; portfolio access granted to {}",
                miami.getName(), miamiId, ownerAcrossProperties.getEmail());
    }

    private void seedConsumptionHistory(Household household, UUID inventoryItemId, int previousQuantity, int newQuantity, int daysAgo) {
        InventoryItem item = inventoryService.getById(inventoryItemId, household.getId());

        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setInventoryItem(item);
        adjustment.setHousehold(household);
        adjustment.setPreviousQuantity(previousQuantity);
        adjustment.setNewQuantity(newQuantity);
        adjustment.setDelta(newQuantity - previousQuantity);
        adjustment.setReason(AdjustmentReason.CONSUMPTION);
        adjustment.setOccurredAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS));

        adjustmentRepository.save(adjustment);
    }
}
