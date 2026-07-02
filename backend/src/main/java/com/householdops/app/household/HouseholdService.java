package com.householdops.app.household;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.householdops.app.common.exception.ResourceNotFoundException;
import com.householdops.app.household.HouseholdDtos.CreateHouseholdRequest;
import com.householdops.app.household.HouseholdDtos.UpdateHouseholdRequest;
import com.householdops.app.staff.StaffMember;
import com.householdops.app.staff.StaffMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HouseholdService {

    private final HouseholdRepository householdRepository;
    private final StaffMemberRepository staffMemberRepository;

    // Retrieves a household by its ID, throwing a ResourceNotFoundException if the household does not exist.
    @Transactional(readOnly = true)
    public Household getById(UUID id) {
        return householdRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Household not found: " + id));
    }

    // Creates a new household based on the provided request data, setting default values for timezone and approval threshold if not specified.
    @Transactional
    public Household create(CreateHouseholdRequest request) {
        Household household = new Household();
        household.setName(request.name());
        household.setAddress(request.address());
        if (request.timezone() != null) {
            household.setTimezone(request.timezone());
        }
        if (request.approvalThreshold() != null) {
            household.setApprovalThreshold(request.approvalThreshold());
        }
        return householdRepository.save(household);
    }

    // Updates an existing household with the provided request data, allowing for partial updates of fields such as name, address,
    //  timezone, approval threshold, and principal user.
    @Transactional
    public Household update(UUID id, UpdateHouseholdRequest request) {
        Household household = getById(id);

        if (request.name() != null) {
            household.setName(request.name());
        }
        if (request.address() != null) {
            household.setAddress(request.address());
        }
        if (request.timezone() != null) {
            household.setTimezone(request.timezone());
        }
        if (request.approvalThreshold() != null) {
            household.setApprovalThreshold(request.approvalThreshold());
        }
        if (request.principalUserId() != null) {
            StaffMember principal = staffMemberRepository.findById(request.principalUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff member not found: " + request.principalUserId()));
            household.setPrincipalUser(principal);
        }

        return household;
    }
}
