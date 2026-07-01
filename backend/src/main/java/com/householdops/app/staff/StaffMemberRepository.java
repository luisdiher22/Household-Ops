package com.householdops.app.staff;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffMemberRepository extends JpaRepository<StaffMember, UUID> {

    Optional<StaffMember> findByEmailIgnoreCase(String email);

    List<StaffMember> findByHouseholdId(UUID householdId);
}
