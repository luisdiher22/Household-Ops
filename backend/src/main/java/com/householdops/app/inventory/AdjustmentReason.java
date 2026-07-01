package com.householdops.app.inventory;

public enum AdjustmentReason {
    /** First stock recorded when an item starts being tracked. */
    INITIAL,
    /** Quantity increased -- restocked. */
    RESTOCK,
    /** Quantity decreased through normal use -- the only reason counted toward the consumption-rate prediction. */
    CONSUMPTION,
    /** Quantity changed to correct a miscount, e.g. after a physical recount -- explicitly excluded from the consumption rate. */
    MANUAL_CORRECTION
}
