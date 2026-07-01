package com.householdops.app.legacy;

import org.junit.jupiter.api.Test;

import com.householdops.app.inventory.InventoryItem;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A plain POJO with no Spring dependencies -- these constructor arguments
 * mirror the values wired in legacy/reorder-rules-context.xml.
 */
class DefaultReorderRulesEngineTest {

    private final DefaultReorderRulesEngine engine = new DefaultReorderRulesEngine(1.0, 2);

    @Test
    void needsReorderWhenAtOrBelowThreshold() {
        InventoryItem item = item(3, 3);
        assertThat(engine.needsReorder(item)).isTrue();
    }

    @Test
    void doesNotNeedReorderAboveThreshold() {
        InventoryItem item = item(4, 3);
        assertThat(engine.needsReorder(item)).isFalse();
    }

    @Test
    void reorderQuantityAppliesMultiplierAndSafetyBuffer() {
        DefaultReorderRulesEngine doubling = new DefaultReorderRulesEngine(2.0, 3);
        InventoryItem item = new InventoryItem();
        item.setReorderQuantity(4);

        assertThat(doubling.reorderQuantityFor(item)).isEqualTo(11); // 4 * 2.0 + 3
    }

    private InventoryItem item(int currentQuantity, int reorderThreshold) {
        InventoryItem item = new InventoryItem();
        item.setCurrentQuantity(currentQuantity);
        item.setReorderThreshold(reorderThreshold);
        return item;
    }
}
