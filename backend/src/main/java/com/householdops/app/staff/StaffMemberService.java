package com.householdops.app.staff;

import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.householdops.app.common.exception.ResourceNotFoundException;
import com.householdops.app.household.Household;
import com.householdops.app.household.HouseholdRepository;
import com.householdops.app.security.SecurityAssertions;
import com.householdops.app.staff.StaffMemberDtos.CreateStaffMemberRequest;
import com.householdops.app.staff.StaffMemberDtos.UpdateStaffMemberRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StaffMemberService {

    private final StaffMemberRepository staffMemberRepository;
    private final HouseholdRepository householdRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<StaffMember> findByHousehold(UUID householdId) {
        return staffMemberRepository.findByHouseholdId(householdId);
    }

    @Transactional(readOnly = true)
    public StaffMember getById(UUID id, UUID callerHouseholdId) {
        StaffMember staffMember = staffMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found: " + id));
        SecurityAssertions.requireHousehold(callerHouseholdId, staffMember.getHousehold().getId());
        return staffMember;
    }

    @Transactional
    public StaffMember create(UUID householdId, CreateStaffMemberRequest request) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Household not found: " + householdId));

        StaffMember staffMember = new StaffMember();
        staffMember.setFullName(request.fullName());
        staffMember.setEmail(request.email().toLowerCase());
        staffMember.setPasswordHash(passwordEncoder.encode(request.password()));
        staffMember.setRole(request.role());
        staffMember.setHousehold(household);

        return staffMemberRepository.save(staffMember);
    }

    @Transactional
    public StaffMember update(UUID id, UUID callerHouseholdId, UpdateStaffMemberRequest request) {
        StaffMember staffMember = getById(id, callerHouseholdId);

        if (request.fullName() != null) {
            staffMember.setFullName(request.fullName());
        }
        if (request.role() != null) {
            staffMember.setRole(request.role());
        }
        if (request.active() != null) {
            staffMember.setActive(request.active());
        }

        return staffMember;
    }
}
