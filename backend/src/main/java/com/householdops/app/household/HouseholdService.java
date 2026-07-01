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

    @Transactional(readOnly = true)
    public Household getById(UUID id) {
        return householdRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Household not found: " + id));
    }

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
