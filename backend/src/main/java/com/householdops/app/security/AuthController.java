package com.householdops.app.security;

import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.householdops.app.common.exception.ResourceNotFoundException;
import com.householdops.app.security.AuthDtos.AuthResponse;
import com.householdops.app.security.AuthDtos.LoginRequest;
import com.householdops.app.security.AuthDtos.RefreshRequest;
import com.householdops.app.staff.StaffMember;
import com.householdops.app.staff.StaffMemberRepository;

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

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        AuthenticatedPrincipal principal = (AuthenticatedPrincipal) auth.getPrincipal();
        return toAuthResponse(principal);
    }

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

        return toAuthResponse(new AuthenticatedPrincipal(staffMember));
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
