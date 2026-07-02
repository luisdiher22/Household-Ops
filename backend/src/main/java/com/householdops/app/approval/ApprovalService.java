package com.householdops.app.approval;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
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


    // The repository for managing approval requests
    private final ApprovalRepository approvalRepository;

   // Creates a new approval request if the amount exceeds the household's approval threshold. 
   // Returns an Optional containing the created ApprovalRequest, or an empty Optional if no approval is needed.
    @Transactional
    public Optional<ApprovalRequest> requestIfOverThreshold(
            Household household,
            StaffMember requestedBy,
            ApprovalSubjectType subjectType,
            UUID subjectId,
            BigDecimal amount,
            String justification) {

        // If the amount is null or less than or equal to the household's approval threshold, no approval is needed.            
        if (amount == null || amount.compareTo(household.getApprovalThreshold()) <= 0) {
            return Optional.empty();
        }

        
        StaffMember principal = household.getPrincipalUser();
        if (principal == null) {
            throw new BusinessRuleViolationException(
                    "Household " + household.getId() + " has no principal assigned to approve this request");
        }
        //If the amount exceeds the threshold and the household has a principal, create a new approval request and save it to the repository.
        ApprovalRequest request = new ApprovalRequest();
        request.setHousehold(household);
        request.setRequestedBy(requestedBy);
        request.setPrincipal(principal);
        request.setSubjectType(subjectType);
        request.setSubjectId(subjectId);
        request.setAmount(amount);
        request.setJustification(justification);

        // Save the approval request and return it wrapped in an Optional
        return Optional.of(approvalRepository.save(request));
    }

    // Checks if there is a pending approval request for the given subject type and subject ID.
    @Transactional(readOnly = true)
    public boolean hasPendingApproval(ApprovalSubjectType subjectType, UUID subjectId) {
        return approvalRepository.findBySubjectTypeAndSubjectIdAndStatus(subjectType, subjectId, ApprovalStatus.PENDING)
                .isPresent();
    }

    // Finds approval requests for a given household, optionally filtered by status, with pagination support.
    @Transactional(readOnly = true)
    public Page<ApprovalRequest> findByHousehold(UUID householdId, ApprovalStatus status, Pageable pageable) {
        return status != null
                ? approvalRepository.findByHouseholdIdAndStatus(householdId, status, pageable)
                : approvalRepository.findByHouseholdId(householdId, pageable);
    }

    // Decides an approval request by either approving or rejecting it, based on the provided decision.
    // Throws an exception if the caller is not the assigned principal or if the request has already been decided.
    @Transactional
    public ApprovalRequest decide(UUID id, UUID callerStaffId, DecideApprovalRequest decision) {
        ApprovalRequest request = approvalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Approval request not found: " + id));

        // Ensure that the caller is the assigned principal for this approval request
        if (!request.getPrincipal().getId().equals(callerStaffId)) {
            throw new AccessDeniedException("Only the assigned principal can decide approval request " + id);
        }
        // Ensure that the approval request has not already been decided
        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new BusinessRuleViolationException("Approval request " + id + " has already been decided");
        }
        // Update the approval request's status, decided timestamp, and decision note based on the provided decision
        request.setStatus(decision.approve() ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
        request.setDecidedAt(Instant.now());
        request.setDecisionNote(decision.note());

        // Save the updated approval request and return it
        return request;
    }
}
