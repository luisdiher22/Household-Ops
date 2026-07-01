package com.householdops.app.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.householdops.app.staff.StaffMember;
import com.householdops.app.staff.StaffRole;

import lombok.Getter;

/**
 * Spring Security principal wrapping a StaffMember, carrying the extra
 * claims (staffId, householdId, role) services need for household-scoping
 * checks without re-querying the DB. Constructible directly from a
 * StaffMember so DemoDataSeeder can exercise the exact same service-layer
 * authorization path real HTTP requests do, instead of a parallel
 * unauthenticated bootstrap path.
 */
@Getter
public class AuthenticatedPrincipal implements UserDetails {

    private final UUID staffId;
    private final UUID householdId;
    private final String email;
    private final String fullName;
    private final String passwordHash;
    private final StaffRole role;
    private final boolean active;

    public AuthenticatedPrincipal(StaffMember staffMember) {
        this(staffMember, null);
    }

    /**
     * activeHouseholdId lets an Owner operate against a household other than
     * their own (see HouseholdAccessGrant) without changing what "household
     * scoped" means anywhere else -- every existing check just reads
     * getHouseholdId() and gets the right value. Callers are responsible for
     * validating the override is actually allowed (home household or a live
     * grant) before constructing this; the constructor itself trusts it.
     */
    public AuthenticatedPrincipal(StaffMember staffMember, UUID activeHouseholdId) {
        this.staffId = staffMember.getId();
        this.householdId = activeHouseholdId != null ? activeHouseholdId : staffMember.getHousehold().getId();
        this.email = staffMember.getEmail();
        this.fullName = staffMember.getFullName();
        this.passwordHash = staffMember.getPasswordHash();
        this.role = staffMember.getRole();
        this.active = staffMember.isActive();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
