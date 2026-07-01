package com.householdops.app.inventory;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    List<Vendor> findByHouseholdId(UUID householdId);
}
