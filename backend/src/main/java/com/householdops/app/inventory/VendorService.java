package com.householdops.app.inventory;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.householdops.app.common.exception.ResourceNotFoundException;
import com.householdops.app.household.Household;
import com.householdops.app.household.HouseholdRepository;
import com.householdops.app.inventory.VendorDtos.CreateVendorRequest;
import com.householdops.app.inventory.VendorDtos.UpdateVendorRequest;
import com.householdops.app.security.SecurityAssertions;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final HouseholdRepository householdRepository;

    @Transactional(readOnly = true)
    public List<Vendor> findByHousehold(UUID householdId) {
        return vendorRepository.findByHouseholdId(householdId);
    }

    @Transactional
    public Vendor create(UUID householdId, CreateVendorRequest request) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Household not found: " + householdId));

        Vendor vendor = new Vendor();
        vendor.setHousehold(household);
        vendor.setName(request.name());
        vendor.setContactEmail(request.contactEmail());
        vendor.setContactPhone(request.contactPhone());
        vendor.setNotes(request.notes());

        return vendorRepository.save(vendor);
    }

    @Transactional
    public Vendor update(UUID id, UUID callerHouseholdId, UpdateVendorRequest request) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found: " + id));
        SecurityAssertions.requireHousehold(callerHouseholdId, vendor.getHousehold().getId());

        if (request.name() != null) {
            vendor.setName(request.name());
        }
        if (request.contactEmail() != null) {
            vendor.setContactEmail(request.contactEmail());
        }
        if (request.contactPhone() != null) {
            vendor.setContactPhone(request.contactPhone());
        }
        if (request.notes() != null) {
            vendor.setNotes(request.notes());
        }

        return vendor;
    }
}
