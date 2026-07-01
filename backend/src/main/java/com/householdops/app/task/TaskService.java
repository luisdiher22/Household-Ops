package com.householdops.app.task;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.householdops.app.approval.ApprovalService;
import com.householdops.app.approval.ApprovalSubjectType;
import com.householdops.app.common.exception.BusinessRuleViolationException;
import com.householdops.app.common.exception.ResourceNotFoundException;
import com.householdops.app.household.Household;
import com.householdops.app.household.HouseholdRepository;
import com.householdops.app.staff.StaffMember;
import com.householdops.app.staff.StaffMemberRepository;
import com.householdops.app.task.TaskDtos.CreateTaskRequest;
import com.householdops.app.task.TaskDtos.UpdateTaskRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final List<TaskStatus> OPEN_STATUSES = List.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS, TaskStatus.BLOCKED);

    private final TaskRepository taskRepository;
    private final HouseholdRepository householdRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final ApprovalService approvalService;

    @Transactional(readOnly = true)
    public Page<HouseholdTask> findByHousehold(UUID householdId, TaskStatus status, Pageable pageable) {
        return status != null
                ? taskRepository.findByHouseholdIdAndStatus(householdId, status, pageable)
                : taskRepository.findByHouseholdId(householdId, pageable);
    }

    @Transactional(readOnly = true)
    public List<HouseholdTask> findUpcoming(UUID householdId, LocalDate dueBefore) {
        return taskRepository.findByHouseholdIdAndStatusInAndDueDateLessThanEqual(householdId, OPEN_STATUSES, dueBefore);
    }

    @Transactional(readOnly = true)
    public HouseholdTask getById(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found: " + id));
    }

    @Transactional
    public HouseholdTask create(UUID householdId, CreateTaskRequest request) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Household not found: " + householdId));
        StaffMember createdBy = staffMemberRepository.findById(request.createdById())
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found: " + request.createdById()));

        HouseholdTask task = new HouseholdTask();
        task.setHousehold(household);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setCreatedBy(createdBy);
        task.setDueDate(request.dueDate());
        task.setEstimatedCost(request.estimatedCost());

        if (request.assignedToId() != null) {
            task.setAssignedTo(staffMemberRepository.findById(request.assignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff member not found: " + request.assignedToId())));
        }

        task = taskRepository.save(task);

        approvalService.requestIfOverThreshold(
                household, createdBy, ApprovalSubjectType.TASK, task.getId(), task.getEstimatedCost(),
                "Task \"" + task.getTitle() + "\" estimated at " + task.getEstimatedCost());

        return task;
    }

    @Transactional
    public HouseholdTask update(UUID id, UpdateTaskRequest request) {
        HouseholdTask task = getById(id);

        if (request.title() != null) {
            task.setTitle(request.title());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }
        if (request.estimatedCost() != null) {
            task.setEstimatedCost(request.estimatedCost());
        }
        if (request.assignedToId() != null) {
            task.setAssignedTo(staffMemberRepository.findById(request.assignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff member not found: " + request.assignedToId())));
        }
        if (request.status() != null) {
            applyStatusChange(task, request.status());
        }

        return task;
    }

    private void applyStatusChange(HouseholdTask task, TaskStatus newStatus) {
        if (newStatus == TaskStatus.DONE && approvalService.hasPendingApproval(ApprovalSubjectType.TASK, task.getId())) {
            throw new BusinessRuleViolationException(
                    "Task " + task.getId() + " cannot be marked DONE while an approval is still pending");
        }
        task.setStatus(newStatus);
    }
}
