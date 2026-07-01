package com.householdops.app.approval;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ApprovalDtos {

    public record ApprovalResponse(
            UUID id,
            UUID householdId,
            UUID requestedById,
            UUID principalId,
            ApprovalSubjectType subjectType,
            UUID subjectId,
            BigDecimal amount,
            String justification,
            ApprovalStatus status,
            Instant decidedAt,
            String decisionNote) {

        public static ApprovalResponse from(ApprovalRequest request) {
            return new ApprovalResponse(
                    request.getId(),
                    request.getHousehold().getId(),
                    request.getRequestedBy().getId(),
                    request.getPrincipal().getId(),
                    request.getSubjectType(),
                    request.getSubjectId(),
                    request.getAmount(),
                    request.getJustification(),
                    request.getStatus(),
                    request.getDecidedAt(),
                    request.getDecisionNote());
        }
    }

    public record DecideApprovalRequest(boolean approve, String note) {
    }

    private ApprovalDtos() {
    }
}
