-- Adds vendor tracking, cost/valuation, expiration dates, and an
-- adjustment audit trail to the inventory module.

CREATE TABLE vendor (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    household_id    UUID NOT NULL REFERENCES household (id),
    name            VARCHAR(255) NOT NULL,
    contact_email   VARCHAR(255),
    contact_phone   VARCHAR(64),
    notes           TEXT
);

CREATE INDEX idx_vendor_household_id ON vendor (household_id);

ALTER TABLE inventory_item
    ADD COLUMN preferred_vendor_id UUID REFERENCES vendor (id),
    ADD COLUMN unit_cost           NUMERIC(12, 2),
    ADD COLUMN expiration_date     DATE;

-- Every quantity change (restock, consumption, a manual correction after a
-- physical recount, or the initial stock when an item is first tracked) is
-- logged here, both as an audit trail and as the raw data the consumption-
-- rate prediction is computed from.
CREATE TABLE inventory_adjustment (
    id                  UUID PRIMARY KEY,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL,
    inventory_item_id   UUID NOT NULL REFERENCES inventory_item (id),
    household_id        UUID NOT NULL REFERENCES household (id),
    previous_quantity    INTEGER NOT NULL,
    new_quantity        INTEGER NOT NULL,
    delta               INTEGER NOT NULL,
    reason              VARCHAR(32) NOT NULL,
    adjusted_by_id       UUID REFERENCES staff_member (id),
    occurred_at         TIMESTAMP NOT NULL
);

CREATE INDEX idx_inventory_adjustment_item_id ON inventory_adjustment (inventory_item_id);
CREATE INDEX idx_inventory_adjustment_household_id ON inventory_adjustment (household_id);
CREATE INDEX idx_inventory_adjustment_item_reason ON inventory_adjustment (inventory_item_id, reason);
