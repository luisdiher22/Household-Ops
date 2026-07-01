package com.householdops.app.inventory;

import java.time.Instant;

import com.householdops.app.common.Auditable;
import com.householdops.app.household.Household;
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

/**
 * One row per quantity change on an InventoryItem -- both an audit trail
 * (who changed what, when) and the raw data the consumption-rate
 * prediction is computed from.
 *
 * occurredAt is separate from Auditable's inherited createdAt: createdAt is
 * "when this row was written," occurredAt is "when the change actually
 * happened" (matters if someone logs an adjustment after the fact, e.g. an
 * end-of-day reconciliation, and lets seed data represent real history).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inventory_adjustment")
public class InventoryAdjustment extends Auditable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem inventoryItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(nullable = false)
    private int previousQuantity;

    @Column(nullable = false)
    private int newQuantity;

    @Column(nullable = false)
    private int delta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AdjustmentReason reason;

    /** Null for system-generated adjustments (e.g. seed data with no specific actor). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjusted_by_id")
    private StaffMember adjustedBy;

    @Column(nullable = false)
    private Instant occurredAt;
}
