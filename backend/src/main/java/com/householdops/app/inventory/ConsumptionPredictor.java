package com.householdops.app.inventory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Estimates how many days until an item runs out, from its own CONSUMPTION
 * history: total consumed since the earliest recorded consumption, divided
 * by the days elapsed, gives a daily rate; currentQuantity / rate gives the
 * prediction. Deliberately a simple average over the whole history rather
 * than a weighted/exponential-decay model -- easy to explain, and a
 * reasonable amount of sophistication for what this needs to do.
 *
 * Takes `now` as a parameter instead of calling Instant.now() internally so
 * it's trivially testable with fixed inputs.
 */
public final class ConsumptionPredictor {

    private ConsumptionPredictor() {
    }

    public static Integer predictDaysUntilEmpty(List<InventoryAdjustment> consumptionHistory, int currentQuantity, Instant now) {
        if (consumptionHistory.isEmpty() || currentQuantity <= 0) {
            return null;
        }

        Instant earliest = consumptionHistory.stream()
                .map(InventoryAdjustment::getOccurredAt)
                .min(Instant::compareTo)
                .orElseThrow();

        int totalConsumed = consumptionHistory.stream()
                .mapToInt(adjustment -> -adjustment.getDelta())
                .sum();
        if (totalConsumed <= 0) {
            return null;
        }

        double daysElapsed = Math.max(1.0, Duration.between(earliest, now).toHours() / 24.0);
        double dailyRate = totalConsumed / daysElapsed;

        return (int) Math.ceil(currentQuantity / dailyRate);
    }
}
