-- Lets an Owner see summary data for a household beyond the one their own
-- staff_member row belongs to (a family-office principal overseeing several
-- properties, each staffed independently). Deliberately additive: nothing
-- about the single-household-per-staff-member model changes, so every
-- existing authorization check keeps working unmodified -- see
-- PortfolioService for why.
CREATE TABLE household_access_grant (
    id              UUID PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    owner_id        UUID NOT NULL REFERENCES staff_member (id),
    household_id    UUID NOT NULL REFERENCES household (id),
    UNIQUE (owner_id, household_id)
);

CREATE INDEX idx_household_access_grant_owner_id ON household_access_grant (owner_id);
