package com.householdops.app.household;

import java.math.BigDecimal;
import java.util.TimeZone;

import com.householdops.app.common.Auditable;
import com.householdops.app.staff.StaffMember;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Represents a household entity in the application, containing information such as name, address, timezone, principal user, and approval threshold.
// Extends the Auditable class to inherit common auditing fields like createdAt and updatedAt.
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "household")
public class Household extends Auditable {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String timezone = TimeZone.getDefault().getID();

    /** The principal/owner whose approval is required once approvalThreshold is exceeded. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "principal_user_id")
    private StaffMember principalUser;

    // The threshold above which spend requests require the principal's approval. 
    @Column(name = "approval_threshold", nullable = false, precision = 12, scale = 2)
    private BigDecimal approvalThreshold = BigDecimal.valueOf(250);
}
