package com.householdops.app.task;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.householdops.app.approval.ApprovalService;
import com.householdops.app.approval.ApprovalSubjectType;
import com.householdops.app.common.exception.BusinessRuleViolationException;
import com.householdops.app.household.Household;
import com.householdops.app.household.HouseholdRepository;
import com.householdops.app.staff.StaffMemberRepository;
import com.householdops.app.task.TaskDtos.UpdateTaskRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private HouseholdRepository householdRepository;
    @Mock
    private StaffMemberRepository staffMemberRepository;
    @Mock
    private ApprovalService approvalService;

    private TaskService taskService;

    private Household household;
    private HouseholdTask task;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, householdRepository, staffMemberRepository, approvalService);

        household = new Household();
        household.setId(UUID.randomUUID());

        task = new HouseholdTask();
        task.setId(UUID.randomUUID());
        task.setHousehold(household);
        task.setStatus(TaskStatus.OPEN);

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
    }

    @Test
    void cannotMarkTaskDoneWhileApprovalIsPending() {
        when(approvalService.hasPendingApproval(ApprovalSubjectType.TASK, task.getId())).thenReturn(true);

        UpdateTaskRequest request = new UpdateTaskRequest(null, null, null, TaskStatus.DONE, null, null);

        assertThatThrownBy(() -> taskService.update(task.getId(), household.getId(), request))
                .isInstanceOf(BusinessRuleViolationException.class);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.OPEN);
    }

    @Test
    void marksTaskDoneWhenNoApprovalIsPending() {
        when(approvalService.hasPendingApproval(ApprovalSubjectType.TASK, task.getId())).thenReturn(false);

        UpdateTaskRequest request = new UpdateTaskRequest(null, null, null, TaskStatus.DONE, null, null);
        HouseholdTask updated = taskService.update(task.getId(), household.getId(), request);

        assertThat(updated.getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void otherStatusTransitionsAreNotGatedByApproval() {
        UpdateTaskRequest request = new UpdateTaskRequest(null, null, null, TaskStatus.IN_PROGRESS, null, null);
        HouseholdTask updated = taskService.update(task.getId(), household.getId(), request);

        assertThat(updated.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void gettingATaskFromAnotherHouseholdIsDenied() {
        UUID someoneElsesHouseholdId = UUID.randomUUID();

        assertThatThrownBy(() -> taskService.getById(task.getId(), someoneElsesHouseholdId))
                .isInstanceOf(AccessDeniedException.class);
    }
}
