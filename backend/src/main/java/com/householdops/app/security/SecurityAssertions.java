package com.householdops.app.security;

import java.util.Set;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;

import com.householdops.app.staff.StaffRole;

/** Object-level (household-scoping) and role checks that don't fit a static @PreAuthorize expression. */
public final class SecurityAssertions {

    private SecurityAssertions() {
    }

    public static void requireHousehold(AuthenticatedPrincipal principal, UUID householdId) {
        requireHousehold(principal.getHouseholdId(), householdId);
    }

    /**
     * UUID-only overload so domain services can enforce household-scoping
     * without depending on the security package's principal type.
     */
    public static void requireHousehold(UUID callerHouseholdId, UUID resourceHouseholdId) {
        if (!callerHouseholdId.equals(resourceHouseholdId)) {
            throw new AccessDeniedException("Not authorized for household " + resourceHouseholdId);
        }
    }

    /**
     * For checks that depend on request content, not just the endpoint itself
     * (e.g. "only an Owner/Manager may set assignedToId"), where a static
     * @PreAuthorize expression can't reach into the request body.
     */
    public static void requireRole(AuthenticatedPrincipal principal, StaffRole... allowed) {
        if (Set.of(allowed).stream().noneMatch(role -> role == principal.getRole())) {
            throw new AccessDeniedException("Role " + principal.getRole() + " is not permitted to perform this action");
        }
    }
}
