package com.householdops.app.inventory;

/**
 * Decides whether an inventory item needs restocking and by how much.
 * The production implementation (DefaultReorderRulesEngine, in the
 * `legacy` package) is deliberately wired via classic Spring XML
 * (see legacy/reorder-rules-context.xml) rather than @Component --
 * modeling how a stable, rarely-touched business-rules component might
 * persist in a long-lived enterprise codebase rather than being migrated
 * to annotations for its own sake.
 */
public interface ReorderRulesEngine {

    boolean needsReorder(InventoryItem item);

    int reorderQuantityFor(InventoryItem item);
}
