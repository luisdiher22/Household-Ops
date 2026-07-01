package com.householdops.app.task;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.householdops.app.approval.ApprovalService;
import com.householdops.app.approval.ApprovalSubjectType;
import com.householdops.app.task.TaskDtos.CreateTaskRequest;
import com.householdops.app.task.TaskDtos.TaskResponse;
import com.householdops.app.task.TaskDtos.UpdateTaskRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final ApprovalService approvalService;

    @GetMapping("/api/households/{householdId}/tasks")
    public Page<TaskResponse> findByHousehold(
            @PathVariable UUID householdId,
            @RequestParam(required = false) TaskStatus status,
            Pageable pageable) {
        return taskService.findByHousehold(householdId, status, pageable).map(this::toResponse);
    }

    @GetMapping("/api/households/{householdId}/tasks/upcoming")
    public List<TaskResponse> findUpcoming(
            @PathVariable UUID householdId,
            @RequestParam LocalDate dueBefore) {
        return taskService.findUpcoming(householdId, dueBefore).stream().map(this::toResponse).toList();
    }

    @GetMapping("/api/tasks/{id}")
    public TaskResponse getById(@PathVariable UUID id) {
        return toResponse(taskService.getById(id));
    }

    @PostMapping("/api/households/{householdId}/tasks")
    public TaskResponse create(@PathVariable UUID householdId, @Valid @RequestBody CreateTaskRequest request) {
        return toResponse(taskService.create(householdId, request));
    }

    @PatchMapping("/api/tasks/{id}")
    public TaskResponse update(@PathVariable UUID id, @RequestBody UpdateTaskRequest request) {
        return toResponse(taskService.update(id, request));
    }

    private TaskResponse toResponse(HouseholdTask task) {
        boolean approvalPending = approvalService.hasPendingApproval(ApprovalSubjectType.TASK, task.getId());
        return TaskResponse.from(task, approvalPending);
    }
}
