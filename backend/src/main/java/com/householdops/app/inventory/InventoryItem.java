package com.householdops.app.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.householdops.app.common.Auditable;
import com.householdops.app.household.Household;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "inventory_item")
public class InventoryItem extends Auditable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(nullable = false)
    private String name;

    /** Free-form for now (e.g. PANTRY, CLEANING, TOILETRIES, MAINTENANCE_SUPPLY) — not an enum since households will want custom categories. */
    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private int currentQuantity;

    @Column(nullable = false)
    private String unit;

    /** When currentQuantity drops to/below this, the reorder engine flags it as low stock. */
    @Column(nullable = false)
    private int reorderThreshold;

    /** How much to add to the shopping list when the reorder engine triggers. */
    @Column(nullable = false)
    private int reorderQuantity;

    private Instant lastRestockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_vendor_id")
    private Vendor preferredVendor;

    @Column(precision = 12, scale = 2)
    private BigDecimal unitCost;

    private LocalDate expirationDate;
}
