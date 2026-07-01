package com.householdops.app.bootstrap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.householdops.app.household.Household;
import com.householdops.app.household.HouseholdDtos.CreateHouseholdRequest;
import com.householdops.app.household.HouseholdDtos.UpdateHouseholdRequest;
import com.householdops.app.household.HouseholdRepository;
import com.householdops.app.household.HouseholdService;
import com.householdops.app.inventory.InventoryDtos.CreateInventoryItemRequest;
import com.householdops.app.inventory.InventoryService;
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
 * manual setup. Runs through the real services, not raw repository saves,
 * so it also exercises the approval-threshold trigger on the "Repair pool
 * pump" task (estimated above the household's default threshold).
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_PASSWORD = "password123";

    private final HouseholdRepository householdRepository;
    private final HouseholdService householdService;
    private final StaffMemberService staffMemberService;
    private final InventoryService inventoryService;
    private final TaskService taskService;
    private final ShoppingListService shoppingListService;

    public DemoDataSeeder(
            HouseholdRepository householdRepository,
            HouseholdService householdService,
            StaffMemberService staffMemberService,
            InventoryService inventoryService,
            TaskService taskService,
            ShoppingListService shoppingListService) {
        this.householdRepository = householdRepository;
        this.householdService = householdService;
        this.staffMemberService = staffMemberService;
        this.inventoryService = inventoryService;
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

        inventoryService.create(householdId, new CreateInventoryItemRequest("Olive Oil", "PANTRY", 2, "bottles", 3, 4));
        inventoryService.create(householdId, new CreateInventoryItemRequest("Paper Towels", "CLEANING", 10, "rolls", 4, 12));
        inventoryService.create(householdId, new CreateInventoryItemRequest("Pool Chlorine", "MAINTENANCE_SUPPLY", 1, "buckets", 2, 3));

        taskService.create(householdId, new CreateTaskRequest(
                "Restock pantry", "Weekly grocery run", staff.getId(), manager.getId(),
                LocalDate.now().plusDays(3), BigDecimal.valueOf(150)));

        // Estimated cost (800) exceeds the household's approval threshold (250),
        // so this seeds a real PENDING ApprovalRequest via TaskService's own logic.
        taskService.create(householdId, new CreateTaskRequest(
                "Repair pool pump", "Pump is making noise, needs a technician", null, manager.getId(),
                LocalDate.now().plusDays(5), BigDecimal.valueOf(800)));

        shoppingListService.create(householdId, new CreateShoppingListItemRequest(
                "Dry cleaning pickup", 1, BigDecimal.valueOf(40), null, staff.getId()));

        log.info("Seeded demo household '{}' ({}) with 4 staff, 3 inventory items, 2 tasks, 1 shopping item", aspen.getName(), householdId);
        log.info("Demo login credentials (password for all: '{}'): owner@householdops.dev, manager@householdops.dev, staff@householdops.dev, vendor@householdops.dev", DEMO_PASSWORD);
    }
}
