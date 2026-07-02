package com.householdops.app.household;

import com.householdops.app.common.Auditable;
import com.householdops.app.staff.StaffMember;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Grants an Owner read access to a household beyond their own primary one --
 * I did this separately as it was a feature added after the initial design, and I didn't want to modify the existing Household entity too much.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "household_access_grant")
public class HouseholdAccessGrant extends Auditable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private StaffMember owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;
}
