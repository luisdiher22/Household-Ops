package com.householdops.app.security;

import java.io.IOException;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.householdops.app.household.HouseholdAccessGrantRepository;
import com.householdops.app.staff.StaffMember;
import com.householdops.app.staff.StaffMemberRepository;
import com.householdops.app.staff.StaffRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Re-loads the StaffMember from the DB on every request (rather than
 * trusting the role/household claims baked into the token) so a
 * deactivated staff member's access is revoked immediately 
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final StaffMemberRepository staffMemberRepository;
    private final HouseholdAccessGrantRepository accessGrantRepository;

    // Filter that intercepts incoming HTTP requests to authenticate users based on JWT tokens.
    //  It extracts the token from the Authorization header, validates it, and sets the authenticated principal in the security context if valid.
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            authenticate(header.substring(BEARER_PREFIX.length()), request);
        }

        filterChain.doFilter(request, response);
    }

    // Authenticates the user based on the provided JWT token. It parses the token,
    //  retrieves the associated StaffMember from the database, and sets the authenticated principal in the security context if the token
    //  is valid and the staff member is active.
    private void authenticate(String token, HttpServletRequest request) {
        try {
            Claims claims = jwtService.parseClaims(token);
            if (!jwtService.isAccessToken(claims)) {
                return;
            }

            UUID staffId = jwtService.extractStaffId(claims);
            StaffMember staffMember = staffMemberRepository.findById(staffId).orElse(null);
            if (staffMember == null || !staffMember.isActive()) {
                return;
            }

            AuthenticatedPrincipal principal = new AuthenticatedPrincipal(staffMember, resolveActiveHousehold(claims, staffMember));
            var authToken = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (JwtException | IllegalArgumentException e) {
            // Invalid/expired/tampered token: leave the request unauthenticated
            // and let the security filter chain's authorization rules reject it.
        }
    }

    // Resolves the active household for the authenticated principal based on the claims in the JWT token and the staff member's role and access grants.
    //  If the claimed household is valid and allowed, it is used; otherwise, the staff member's home household is used as the active household.
    private UUID resolveActiveHousehold(Claims claims, StaffMember staffMember) {
        UUID claimedHouseholdId = jwtService.extractHouseholdId(claims);
        UUID homeHouseholdId = staffMember.getHousehold().getId();

        if (claimedHouseholdId == null || claimedHouseholdId.equals(homeHouseholdId)) {
            return homeHouseholdId;
        }
        if (staffMember.getRole() == StaffRole.OWNER
                && accessGrantRepository.existsByOwnerIdAndHouseholdId(staffMember.getId(), claimedHouseholdId)) {
            return claimedHouseholdId;
        }
        return homeHouseholdId;
    }
}
