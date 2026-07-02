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
 * A principal must sign off before a task/shopping-list spend over the household's threshold proceeds.

 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "approval_request")
public class ApprovalRequest extends Auditable {

    //The household this approval belongs to
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    // The staff member who requested this approval
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private StaffMember requestedBy;

    // The staff member who is  the one who must approve or reject it
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "principal_id", nullable = false)
    private StaffMember principal;

    // The type of the subject (task or shopping list item) that this approval request is for
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApprovalSubjectType subjectType;

    // The ID of the subject (task or shopping list item) that this approval request is for
    @Column(nullable = false)
    private UUID subjectId;

    // The amount of money that this approval request is for
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // The justification for this approval request
    @Column(columnDefinition = "text")
    private String justification;

    // The status of this approval request (pending, approved, or rejected)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    // The timestamp when this approval request was decided (approved or rejected)
    private Instant decidedAt;

    // The note provided by the principal when deciding this approval request
    @Column(columnDefinition = "text")
    private String decisionNote;
}
