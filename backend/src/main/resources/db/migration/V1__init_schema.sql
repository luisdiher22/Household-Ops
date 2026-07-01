-- Household and StaffMember reference each other (a household has a
-- principal, a staff member belongs to a household), so household is
-- created first without the principal FK, staff_member is created with
-- its FK to household, then the household -> staff_member FK is added.

CREATE TABLE household (
    id                  UUID PRIMARY KEY,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL,
    name                VARCHAR(255) NOT NULL,
    address             VARCHAR(255) NOT NULL,
    timezone            VARCHAR(64) NOT NULL,
    principal_user_id   UUID,
    approval_threshold  NUMERIC(12, 2) NOT NULL
);

CREATE TABLE staff_member (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(32) NOT NULL,
    household_id    UUID NOT NULL REFERENCES household (id),
    active          BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE household
    ADD CONSTRAINT fk_household_principal_user
    FOREIGN KEY (principal_user_id) REFERENCES staff_member (id);

CREATE INDEX idx_staff_member_household_id ON staff_member (household_id);

CREATE TABLE inventory_item (
    id                  UUID PRIMARY KEY,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL,
    household_id        UUID NOT NULL REFERENCES household (id),
    name                VARCHAR(255) NOT NULL,
    category            VARCHAR(64) NOT NULL,
    current_quantity    INTEGER NOT NULL,
    unit                VARCHAR(32) NOT NULL,
    reorder_threshold   INTEGER NOT NULL,
    reorder_quantity    INTEGER NOT NULL,
    last_restocked_at   TIMESTAMP
);

CREATE INDEX idx_inventory_item_household_id ON inventory_item (household_id);

CREATE TABLE shopping_list_item (
    id                  UUID PRIMARY KEY,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL,
    household_id        UUID NOT NULL REFERENCES household (id),
    inventory_item_id   UUID REFERENCES inventory_item (id),
    description         VARCHAR(255) NOT NULL,
    quantity            INTEGER NOT NULL,
    estimated_cost      NUMERIC(12, 2),
    status              VARCHAR(32) NOT NULL,
    auto_generated      BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_shopping_list_item_household_id ON shopping_list_item (household_id);

CREATE TABLE household_task (
    id                          UUID PRIMARY KEY,
    created_at                  TIMESTAMP NOT NULL,
    updated_at                  TIMESTAMP NOT NULL,
    household_id                UUID NOT NULL REFERENCES household (id),
    title                       VARCHAR(255) NOT NULL,
    description                 TEXT,
    assigned_to_id               UUID REFERENCES staff_member (id),
    created_by_id                UUID NOT NULL REFERENCES staff_member (id),
    status                      VARCHAR(32) NOT NULL,
    due_date                    DATE,
    estimated_cost               NUMERIC(12, 2),
    linked_inventory_item_id     UUID REFERENCES inventory_item (id)
);

CREATE INDEX idx_household_task_household_id ON household_task (household_id);
CREATE INDEX idx_household_task_assigned_to_id ON household_task (assigned_to_id);

CREATE TABLE approval_request (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    household_id    UUID NOT NULL REFERENCES household (id),
    requested_by_id  UUID NOT NULL REFERENCES staff_member (id),
    principal_id    UUID NOT NULL REFERENCES staff_member (id),
    subject_type    VARCHAR(32) NOT NULL,
    subject_id      UUID NOT NULL,
    amount          NUMERIC(12, 2) NOT NULL,
    justification   TEXT,
    status          VARCHAR(32) NOT NULL,
    decided_at      TIMESTAMP,
    decision_note   TEXT
);

CREATE INDEX idx_approval_request_household_id ON approval_request (household_id);
CREATE INDEX idx_approval_request_subject ON approval_request (subject_type, subject_id);
