package com.householdops.app.security;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;

/** Object-level (household-scoping) checks: every staff member belongs to exactly one household. */
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
}
