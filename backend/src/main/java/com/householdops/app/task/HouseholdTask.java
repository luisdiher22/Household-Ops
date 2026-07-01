package com.householdops.app.task;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.householdops.app.common.Auditable;
import com.householdops.app.household.Household;
import com.householdops.app.inventory.InventoryItem;
import com.householdops.app.staff.StaffMember;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "household_task")
public class HouseholdTask extends Auditable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private StaffMember assignedTo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private StaffMember createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TaskStatus status = TaskStatus.OPEN;

    private LocalDate dueDate;

    @Column(precision = 12, scale = 2)
    private BigDecimal estimatedCost;

    /** Set when this task was auto-generated in response to a low-stock inventory item. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_inventory_item_id")
    private InventoryItem linkedInventoryItem;
}
