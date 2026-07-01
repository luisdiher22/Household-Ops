package com.householdops.app.task;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<HouseholdTask, UUID> {

    Page<HouseholdTask> findByHouseholdId(UUID householdId, Pageable pageable);

    Page<HouseholdTask> findByHouseholdIdAndStatus(UUID householdId, TaskStatus status, Pageable pageable);

    List<HouseholdTask> findByHouseholdIdAndStatusInAndDueDateLessThanEqual(
            UUID householdId, Collection<TaskStatus> statuses, LocalDate dueDate);
}
