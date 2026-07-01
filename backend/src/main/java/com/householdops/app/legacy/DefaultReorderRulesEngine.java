package com.householdops.app.legacy;

import com.householdops.app.inventory.InventoryItem;
import com.householdops.app.inventory.ReorderRulesEngine;

/**
 * Deliberately a plain class with no Spring annotations -- it's instantiated
 * entirely by legacy/reorder-rules-context.xml (constructor-arg injection),
 * then imported into the Boot context via
 * config.LegacyRulesXmlConfig's @ImportResource. It still participates fully
 * in the container once loaded (autowirable, proxyable, etc.) -- only its
 * own definition lives outside annotation-based config.
 */
public class DefaultReorderRulesEngine implements ReorderRulesEngine {

    private final double defaultReorderMultiplier;
    private final int safetyStockBuffer;

    public DefaultReorderRulesEngine(double defaultReorderMultiplier, int safetyStockBuffer) {
        this.defaultReorderMultiplier = defaultReorderMultiplier;
        this.safetyStockBuffer = safetyStockBuffer;
    }

    @Override
    public boolean needsReorder(InventoryItem item) {
        return item.getCurrentQuantity() <= item.getReorderThreshold();
    }

    @Override
    public int reorderQuantityFor(InventoryItem item) {
        int scaled = (int) Math.ceil(item.getReorderQuantity() * defaultReorderMultiplier);
        return scaled + safetyStockBuffer;
    }
}
