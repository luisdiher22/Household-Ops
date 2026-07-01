package com.householdops.app.task;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public class TaskDtos {

    public record TaskResponse(
            UUID id,
            UUID householdId,
            String title,
            String description,
            UUID assignedToId,
            UUID createdById,
            TaskStatus status,
            LocalDate dueDate,
            BigDecimal estimatedCost,
            UUID linkedInventoryItemId,
            boolean approvalPending) {

        public static TaskResponse from(HouseholdTask task, boolean approvalPending) {
            return new TaskResponse(
                    task.getId(),
                    task.getHousehold().getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getAssignedTo() != null ? task.getAssignedTo().getId() : null,
                    task.getCreatedBy().getId(),
                    task.getStatus(),
                    task.getDueDate(),
                    task.getEstimatedCost(),
                    task.getLinkedInventoryItem() != null ? task.getLinkedInventoryItem().getId() : null,
                    approvalPending);
        }
    }

    public record CreateTaskRequest(
            @NotBlank String title,
            String description,
            UUID assignedToId,
            LocalDate dueDate,
            BigDecimal estimatedCost) {
    }

    public record UpdateTaskRequest(
            String title,
            String description,
            UUID assignedToId,
            TaskStatus status,
            LocalDate dueDate,
            BigDecimal estimatedCost) {
    }

    private TaskDtos() {
    }
}
