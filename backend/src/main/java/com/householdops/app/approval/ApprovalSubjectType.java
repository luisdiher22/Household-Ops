package com.householdops.app.approval;

/**
 * What kind of record an ApprovalRequest is gating. A soft reference
 * (subjectType + subjectId on ApprovalRequest) is used instead of two
 * nullable foreign keys or JPA inheritance, since the set of approvable
 * subject types is small and unlikely to grow much beyond this.
 */
public enum ApprovalSubjectType {
    TASK,
    SHOPPING_ITEM
}
