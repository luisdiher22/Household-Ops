package com.householdops.app.approval;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.householdops.app.approval.ApprovalDtos.ApprovalResponse;
import com.householdops.app.approval.ApprovalDtos.DecideApprovalRequest;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @GetMapping("/api/households/{householdId}/approvals")
    public Page<ApprovalResponse> findByHousehold(
            @PathVariable UUID householdId,
            @RequestParam(required = false) ApprovalStatus status,
            Pageable pageable) {
        return approvalService.findByHousehold(householdId, status, pageable).map(ApprovalResponse::from);
    }

    @PostMapping("/api/approvals/{id}/decide")
    public ApprovalResponse decide(@PathVariable UUID id, @RequestBody DecideApprovalRequest request) {
        return ApprovalResponse.from(approvalService.decide(id, request));
    }
}
