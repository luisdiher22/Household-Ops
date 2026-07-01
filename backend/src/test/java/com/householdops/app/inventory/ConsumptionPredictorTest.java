package com.householdops.app.inventory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsumptionPredictorTest {

    private final Instant now = Instant.parse("2026-07-01T00:00:00Z");

    @Test
    void returnsNullWithNoConsumptionHistory() {
        Integer predicted = ConsumptionPredictor.predictDaysUntilEmpty(List.of(), 10, now);
        assertThat(predicted).isNull();
    }

    @Test
    void returnsNullWhenCurrentQuantityIsZero() {
        Integer predicted = ConsumptionPredictor.predictDaysUntilEmpty(
                List.of(consumption(-4, 10)), 0, now);
        assertThat(predicted).isNull();
    }

    @Test
    void computesDailyRateFromTotalConsumedOverElapsedDays() {
        // 4 units consumed over 10 days = 0.4/day; 2 units left / 0.4 = 5 days
        Integer predicted = ConsumptionPredictor.predictDaysUntilEmpty(
                List.of(consumption(-2, 10), consumption(-2, 5)), 2, now);
        assertThat(predicted).isEqualTo(5);
    }

    @Test
    void ignoresRestockDeltasIfTheyWereAccidentallyIncluded() {
        // Only meant to be called with CONSUMPTION-reason adjustments, but the
        // math itself should still behave sanely: a positive delta reduces the
        // "total consumed" sum rather than being treated as extra consumption.
        Integer withOnlyConsumption = ConsumptionPredictor.predictDaysUntilEmpty(
                List.of(consumption(-4, 10)), 2, now);
        Integer withRestockMixedIn = ConsumptionPredictor.predictDaysUntilEmpty(
                List.of(consumption(-4, 10), consumption(2, 10)), 2, now);

        assertThat(withOnlyConsumption).isEqualTo(5);
        assertThat(withRestockMixedIn).isNotEqualTo(withOnlyConsumption);
    }

    @Test
    void returnsNullWhenNetConsumptionIsZeroOrNegative() {
        Integer predicted = ConsumptionPredictor.predictDaysUntilEmpty(
                List.of(consumption(-2, 10), consumption(2, 5)), 2, now);
        assertThat(predicted).isNull();
    }

    @Test
    void floorsElapsedDaysAtOneToAvoidDivideByNearZero() {
        // A single adjustment logged moments ago shouldn't produce an absurd rate.
        Integer predicted = ConsumptionPredictor.predictDaysUntilEmpty(
                List.of(consumptionAt(-1, now.minus(1, ChronoUnit.HOURS))), 10, now);
        assertThat(predicted).isEqualTo(10); // 1 unit/day rate, 10 units left
    }

    private InventoryAdjustment consumption(int delta, int daysAgo) {
        return consumptionAt(delta, now.minus(daysAgo, ChronoUnit.DAYS));
    }

    private InventoryAdjustment consumptionAt(int delta, Instant occurredAt) {
        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setDelta(delta);
        adjustment.setOccurredAt(occurredAt);
        adjustment.setReason(AdjustmentReason.CONSUMPTION);
        return adjustment;
    }
}
