/*
Approval controlller. Handles requests for approvals, including listing approvals for a household and deciding on an approval request.
*/
package com.householdops.app.approval;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.householdops.app.approval.ApprovalDtos.ApprovalResponse;
import com.householdops.app.approval.ApprovalDtos.DecideApprovalRequest;
import com.householdops.app.security.AuthenticatedPrincipal;
import com.householdops.app.security.SecurityAssertions;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ApprovalController {

    //Role check 
    private final ApprovalService approvalService;

    //Checks if the user has access to the household and returns a list of approvals 
    @GetMapping("/api/households/{householdId}/approvals")
    public Page<ApprovalResponse> findByHousehold(
            @PathVariable UUID householdId,
            @RequestParam(required = false) ApprovalStatus status,
            Pageable pageable,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        SecurityAssertions.requireHousehold(principal, householdId);
        return approvalService.findByHousehold(householdId, status, pageable).map(ApprovalResponse::from);
    }


    // Role check here narrows it to Owners; ApprovalService.decide() then checks
    // the caller is this household's principal, since a different
    // household's Owner shouldn't be able to decide someone else's approval.
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/api/approvals/{id}/decide")
    public ApprovalResponse decide(
            @PathVariable UUID id,
            @RequestBody DecideApprovalRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApprovalResponse.from(approvalService.decide(id, principal.getStaffId(), request));
    }
}
