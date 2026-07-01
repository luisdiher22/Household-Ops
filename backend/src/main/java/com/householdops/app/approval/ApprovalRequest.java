package com.householdops.app.approval;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.householdops.app.common.Auditable;
import com.householdops.app.household.Household;
import com.householdops.app.staff.StaffMember;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Models Eleven Messages' "Approval message" pattern: a principal must sign off
 * before a task/shopping-list spend over the household's threshold proceeds.
 *
 * subjectType + subjectId is a soft reference to the HouseholdTask or
 * ShoppingListItem this approval gates, rather than two nullable FKs or a
 * polymorphic JPA hierarchy — simpler for a fixed, small set of subject types,
 * at the cost of no DB-level FK integrity on the subject (documented trade-off).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "approval_request")
public class ApprovalRequest extends Auditable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private StaffMember requestedBy;

    /** Resolved from household.principalUser at creation time. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "principal_id", nullable = false)
    private StaffMember principal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApprovalSubjectType subjectType;

    @Column(nullable = false)
    private UUID subjectId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(columnDefinition = "text")
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    private Instant decidedAt;

    @Column(columnDefinition = "text")
    private String decisionNote;
}
