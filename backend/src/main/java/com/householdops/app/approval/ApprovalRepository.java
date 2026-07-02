package com.householdops.app.approval;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRepository extends JpaRepository<ApprovalRequest, UUID> {

    // Find all approval requests for a given household, optionally filtered by status, with pagination support.
    Page<ApprovalRequest> findByHouseholdIdAndStatus(UUID householdId, ApprovalStatus status, Pageable pageable);

    // Find all approval requests for a given household, with pagination support.
    Page<ApprovalRequest> findByHouseholdId(UUID householdId, Pageable pageable);

    // Find a specific approval request by its subject type, subject ID, and status.
    Optional<ApprovalRequest> findBySubjectTypeAndSubjectIdAndStatus(
            ApprovalSubjectType subjectType, UUID subjectId, ApprovalStatus status);
}
