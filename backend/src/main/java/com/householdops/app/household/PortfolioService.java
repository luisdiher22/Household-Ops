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
 * Deliberately its own authorization path rather than an extension of
 * SecurityAssertions.requireHousehold: that check (and everything built on
 * it -- every controller's household-scoping, JwtAuthenticationFilter's
 * re-derivation of the principal, the assistant's ToolContext) assumes one
 * household per staff member, and retrofitting it to understand grants
 * would touch every one of those call sites this late in the build.
 * Reading HouseholdAccessGrant directly here instead keeps that model
 * untouched. It's also intentionally read-only: an Owner sees a summary of
 * every property they have access to, but still has to be logged in as that
 * property's own staff to act on it day-to-day -- full cross-household
 * drill-down would need the wider change above and isn't attempted here.
 */
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final HouseholdAccessGrantRepository grantRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final InventoryService inventoryService;
    private final InventoryStatusService inventoryStatusService;
    private final ApprovalService approvalService;

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(AuthenticatedPrincipal principal) {
        if (principal.getRole() != StaffRole.OWNER) {
            throw new AccessDeniedException("Only an Owner has a portfolio");
        }

        // Deliberately not principal.getHouseholdId() -- that reflects whatever
        // property is currently active (see AuthenticatedPrincipal's
        // activeHouseholdId), which could already be a granted household, not
        // the caller's actual home. Re-fetching the staff member's own
        // household FK is the only reliable source of "home" here.
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

        List<PropertySummary> summaries = households.stream()
                .map(household -> summarize(household, household.getId().equals(primary.getId())))
                .toList();

        return new PortfolioResponse(summaries);
    }

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
