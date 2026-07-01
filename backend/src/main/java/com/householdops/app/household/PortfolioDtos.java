package com.householdops.app.household;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class PortfolioDtos {

    public record PropertySummary(
            UUID householdId,
            String name,
            String address,
            boolean primary,
            BigDecimal inventoryValue,
            long lowStockCount,
            long pendingApprovalCount) {
    }

    public record PortfolioResponse(List<PropertySummary> properties) {
    }

    private PortfolioDtos() {
    }
}
