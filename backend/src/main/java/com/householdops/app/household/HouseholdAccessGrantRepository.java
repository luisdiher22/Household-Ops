package com.householdops.app.household;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HouseholdAccessGrantRepository extends JpaRepository<HouseholdAccessGrant, UUID> {

    List<HouseholdAccessGrant> findByOwnerId(UUID ownerId);
}
