package com.householdops.app.approval;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.householdops.app.approval.ApprovalDtos.DecideApprovalRequest;
import com.householdops.app.common.exception.BusinessRuleViolationException;
import com.householdops.app.household.Household;
import com.householdops.app.staff.StaffMember;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers the approval-threshold trigger -- the central business rule of the
 * project: spend over a household's threshold requires the specific
 * assigned principal's sign-off, and a decision can only be made once.
 */
@ExtendWith(MockitoExtension.class)
class ApprovalServiceTest {

    @Mock
    private ApprovalRepository approvalRepository;

    private ApprovalService approvalService;

    private Household household;
    private StaffMember principal;
    private StaffMember requestedBy;

    @BeforeEach
    void setUp() {
        approvalService = new ApprovalService(approvalRepository);

        principal = new StaffMember();
        principal.setId(UUID.randomUUID());

        requestedBy = new StaffMember();
        requestedBy.setId(UUID.randomUUID());

        household = new Household();
        household.setId(UUID.randomUUID());
        household.setApprovalThreshold(BigDecimal.valueOf(250));
        household.setPrincipalUser(principal);
    }

    @Test
    void doesNotRequestApprovalWhenAmountIsNull() {
        Optional<ApprovalRequest> result = approvalService.requestIfOverThreshold(
                household, requestedBy, ApprovalSubjectType.TASK, UUID.randomUUID(), null, "no cost yet");

        assertThat(result).isEmpty();
        verifyNoInteractions(approvalRepository);
    }

    @Test
    void doesNotRequestApprovalWhenAmountIsAtOrBelowThreshold() {
        Optional<ApprovalRequest> result = approvalService.requestIfOverThreshold(
                household, requestedBy, ApprovalSubjectType.TASK, UUID.randomUUID(), BigDecimal.valueOf(250), "at threshold");

        assertThat(result).isEmpty();
        verifyNoInteractions(approvalRepository);
    }

    @Test
    void createsPendingApprovalWhenAmountExceedsThreshold() {
        UUID subjectId = UUID.randomUUID();
        when(approvalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<ApprovalRequest> result = approvalService.requestIfOverThreshold(
                household, requestedBy, ApprovalSubjectType.TASK, subjectId, BigDecimal.valueOf(800), "over threshold");

        assertThat(result).isPresent();
        ArgumentCaptor<ApprovalRequest> captor = ArgumentCaptor.forClass(ApprovalRequest.class);
        verify(approvalRepository).save(captor.capture());

        ApprovalRequest saved = captor.getValue();
        assertThat(saved.getHousehold()).isEqualTo(household);
        assertThat(saved.getRequestedBy()).isEqualTo(requestedBy);
        assertThat(saved.getPrincipal()).isEqualTo(principal);
        assertThat(saved.getSubjectType()).isEqualTo(ApprovalSubjectType.TASK);
        assertThat(saved.getSubjectId()).isEqualTo(subjectId);
        assertThat(saved.getAmount()).isEqualByComparingTo("800");
        assertThat(saved.getStatus()).isEqualTo(ApprovalStatus.PENDING);
    }

    @Test
    void throwsWhenHouseholdHasNoPrincipalButAmountExceedsThreshold() {
        household.setPrincipalUser(null);

        assertThatThrownBy(() -> approvalService.requestIfOverThreshold(
                household, requestedBy, ApprovalSubjectType.TASK, UUID.randomUUID(), BigDecimal.valueOf(800), "no principal"))
                .isInstanceOf(BusinessRuleViolationException.class);

        verifyNoInteractions(approvalRepository);
    }

    @Test
    void decideRejectsCallerWhoIsNotTheAssignedPrincipal() {
        ApprovalRequest request = pendingRequest();
        when(approvalRepository.findById(request.getId())).thenReturn(Optional.of(request));

        UUID someoneElse = UUID.randomUUID();
        assertThatThrownBy(() -> approvalService.decide(request.getId(), someoneElse, new DecideApprovalRequest(true, "note")))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.PENDING);
    }

    @Test
    void decideRejectsAnAlreadyDecidedRequest() {
        ApprovalRequest request = pendingRequest();
        request.setStatus(ApprovalStatus.APPROVED);
        when(approvalRepository.findById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> approvalService.decide(request.getId(), principal.getId(), new DecideApprovalRequest(true, "note")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void decideApprovesWhenCalledByTheAssignedPrincipal() {
        ApprovalRequest request = pendingRequest();
        when(approvalRepository.findById(request.getId())).thenReturn(Optional.of(request));

        ApprovalRequest decided = approvalService.decide(request.getId(), principal.getId(), new DecideApprovalRequest(true, "approved"));

        assertThat(decided.getStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(decided.getDecidedAt()).isNotNull();
        assertThat(decided.getDecisionNote()).isEqualTo("approved");
    }

    private ApprovalRequest pendingRequest() {
        ApprovalRequest request = new ApprovalRequest();
        request.setId(UUID.randomUUID());
        request.setHousehold(household);
        request.setRequestedBy(requestedBy);
        request.setPrincipal(principal);
        request.setSubjectType(ApprovalSubjectType.TASK);
        request.setSubjectId(UUID.randomUUID());
        request.setAmount(BigDecimal.valueOf(800));
        request.setStatus(ApprovalStatus.PENDING);
        return request;
    }
}
