package com.householdops.app.security;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.householdops.app.common.exception.ResourceNotFoundException;
import com.householdops.app.household.HouseholdAccessGrantRepository;
import com.householdops.app.security.AuthDtos.AuthResponse;
import com.householdops.app.security.AuthDtos.LoginRequest;
import com.householdops.app.security.AuthDtos.RefreshRequest;
import com.householdops.app.security.AuthDtos.SwitchHouseholdRequest;
import com.householdops.app.staff.StaffMember;
import com.householdops.app.staff.StaffMemberRepository;
import com.householdops.app.staff.StaffRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final StaffMemberRepository staffMemberRepository;
    private final HouseholdAccessGrantRepository accessGrantRepository;

    // Handles user login by authenticating the provided email and password, returning an authentication response with access and refresh tokens.
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        AuthenticatedPrincipal principal = (AuthenticatedPrincipal) auth.getPrincipal();
        return toAuthResponse(principal);
    }

    /**
     * The refresh token itself deliberately carries no household (see
     * JwtService.generateRefreshToken), so the caller passes back whichever
     * household was active in their expiring access token. If it's no longer
     * valid (grant revoked, role changed), this silently falls back to the
     * staff member's own household rather than failing the refresh 
     */

    // Unlike switchHousehold(), this is a silent fallback rather than a user action, so an invalid household is not an error.
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        Claims claims = jwtService.parseClaims(request.refreshToken());
        if (!jwtService.isRefreshToken(claims)) {
            throw new JwtException("Not a refresh token");
        }

        UUID staffId = jwtService.extractStaffId(claims);
        StaffMember staffMember = staffMemberRepository.findById(staffId)
                .filter(StaffMember::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found or inactive: " + staffId));

        UUID activeHouseholdId = request.activeHouseholdId() != null && isHouseholdAllowed(staffMember, request.activeHouseholdId())
                ? request.activeHouseholdId()
                : staffMember.getHousehold().getId();

        return toAuthResponse(new AuthenticatedPrincipal(staffMember, activeHouseholdId));
    }

    /** Unlike refresh(), this is a direct user action -- an invalid target household is a real error, not something to silently paper over. */
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/switch-household")
    public AuthResponse switchHousehold(
            @Valid @RequestBody SwitchHouseholdRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        StaffMember staffMember = staffMemberRepository.findById(principal.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found: " + principal.getStaffId()));

        if (!isHouseholdAllowed(staffMember, request.householdId())) {
            throw new AccessDeniedException("Not granted access to household " + request.householdId());
        }

        return toAuthResponse(new AuthenticatedPrincipal(staffMember, request.householdId()));
    }

    private boolean isHouseholdAllowed(StaffMember staffMember, UUID requestedHouseholdId) {
        return requestedHouseholdId.equals(staffMember.getHousehold().getId())
                || (staffMember.getRole() == StaffRole.OWNER
                        && accessGrantRepository.existsByOwnerIdAndHouseholdId(staffMember.getId(), requestedHouseholdId));
    }

    private AuthResponse toAuthResponse(AuthenticatedPrincipal principal) {
        return new AuthResponse(
                jwtService.generateAccessToken(principal),
                jwtService.generateRefreshToken(principal),
                principal.getStaffId(),
                principal.getHouseholdId(),
                principal.getFullName(),
                principal.getRole());
    }
}
