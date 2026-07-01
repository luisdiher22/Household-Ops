package com.householdops.app.approval;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.householdops.app.approval.ApprovalDtos.DecideApprovalRequest;
import com.householdops.app.common.exception.BusinessRuleViolationException;
import com.householdops.app.common.exception.ResourceNotFoundException;
import com.householdops.app.household.Household;
import com.householdops.app.staff.StaffMember;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final ApprovalRepository approvalRepository;

    /**
     * Central threshold-trigger rule: if amount exceeds the household's
     * approvalThreshold, a PENDING ApprovalRequest is created against the
     * household's principal and the task/shopping-list write proceeds
     * un-gated (creation always succeeds) — but the subject can't be marked
     * complete/purchased while a PENDING approval exists for it, enforced
     * by callers via hasPendingApproval.
     */
    @Transactional
    public Optional<ApprovalRequest> requestIfOverThreshold(
            Household household,
            StaffMember requestedBy,
            ApprovalSubjectType subjectType,
            UUID subjectId,
            BigDecimal amount,
            String justification) {

        if (amount == null || amount.compareTo(household.getApprovalThreshold()) <= 0) {
            return Optional.empty();
        }

        StaffMember principal = household.getPrincipalUser();
        if (principal == null) {
            throw new BusinessRuleViolationException(
                    "Household " + household.getId() + " has no principal assigned to approve this request");
        }

        ApprovalRequest request = new ApprovalRequest();
        request.setHousehold(household);
        request.setRequestedBy(requestedBy);
        request.setPrincipal(principal);
        request.setSubjectType(subjectType);
        request.setSubjectId(subjectId);
        request.setAmount(amount);
        request.setJustification(justification);

        return Optional.of(approvalRepository.save(request));
    }

    @Transactional(readOnly = true)
    public boolean hasPendingApproval(ApprovalSubjectType subjectType, UUID subjectId) {
        return approvalRepository.findBySubjectTypeAndSubjectIdAndStatus(subjectType, subjectId, ApprovalStatus.PENDING)
                .isPresent();
    }

    @Transactional(readOnly = true)
    public Page<ApprovalRequest> findByHousehold(UUID householdId, ApprovalStatus status, Pageable pageable) {
        return status != null
                ? approvalRepository.findByHouseholdIdAndStatus(householdId, status, pageable)
                : approvalRepository.findByHouseholdId(householdId, pageable);
    }

    @Transactional
    public ApprovalRequest decide(UUID id, DecideApprovalRequest decision) {
        ApprovalRequest request = approvalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + id));

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessRuleViolationException("Approval request " + id + " has already been decided");
        }

        request.setStatus(decision.approve() ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
        request.setDecidedAt(Instant.now());
        request.setDecisionNote(decision.note());

        return request;
    }
}
