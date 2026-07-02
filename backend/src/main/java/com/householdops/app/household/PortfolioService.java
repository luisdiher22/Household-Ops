package com.householdops.app.household;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.householdops.app.approval.ApprovalService;
import com.householdops.app.approval.ApprovalStatus;
import com.householdops.app.common.exception.ResourceNotFoundException;
import com.householdops.app.household.PortfolioDtos.PortfolioResponse;
import com.householdops.app.household.PortfolioDtos.PropertySummary;
import com.householdops.app.inventory.InventoryService;
import com.householdops.app.inventory.InventoryStatusService;
import com.householdops.app.security.AuthenticatedPrincipal;
import com.householdops.app.staff.StaffMember;
import com.householdops.app.staff.StaffMemberRepository;
import com.householdops.app.staff.StaffRole;

import lombok.RequiredArgsConstructor;

/**
    * Service for managing and retrieving information about a user's portfolio of households.
    * This service is responsible for aggregating data from multiple households that an owner has access to,
    * including their primary household and any additional households granted through HouseholdAccessGrant.
    */
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final HouseholdAccessGrantRepository grantRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final InventoryService inventoryService;
    private final InventoryStatusService inventoryStatusService;
    private final ApprovalService approvalService;

    // Retrieves the portfolio of households for the authenticated principal, ensuring that only owners can access their portfolio.
    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(AuthenticatedPrincipal principal) {
        if (principal.getRole() != StaffRole.OWNER) {
            throw new AccessDeniedException("Only an Owner has a portfolio");
        }

       // Fetch the staff member and their primary household, then gather all households they have access to
        StaffMember staffMember = staffMemberRepository.findById(principal.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found: " + principal.getStaffId()));
        Household primary = staffMember.getHousehold();

        List<Household> households = new ArrayList<>();
        households.add(primary);
        for (HouseholdAccessGrant grant : grantRepository.findByOwnerId(principal.getStaffId())) {
            Household granted = grant.getHousehold();
            if (households.stream().noneMatch(h -> h.getId().equals(granted.getId()))) {
                households.add(granted);
            }
        }
        //
        List<PropertySummary> summaries = households.stream()
                .map(household -> summarize(household, household.getId().equals(primary.getId())))
                .toList();

        return new PortfolioResponse(summaries);
    }

    // Summarizes the key information of a household, including its valuation, inventory status, and pending approvals.
    private PropertySummary summarize(Household household, boolean isPrimary) {
        var valuation = inventoryService.valuation(household.getId());
        var status = inventoryStatusService.computeStatus(household.getId());
        long pendingApprovals = approvalService
                .findByHousehold(household.getId(), ApprovalStatus.PENDING, Pageable.unpaged())
                .getTotalElements();

        return new PropertySummary(
                household.getId(),
                household.getName(),
                household.getAddress(),
                isPrimary,
                valuation.totalValue(),
                status.lowStockItems().size(),
                pendingApprovals);
    }
}
