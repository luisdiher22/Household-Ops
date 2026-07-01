package com.householdops.app.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.householdops.app.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final String CLAIM_TOKEN_TYPE = "typ";
    private static final String CLAIM_HOUSEHOLD_ID = "householdId";
    private static final String CLAIM_ROLE = "role";
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(AuthenticatedPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(principal.getStaffId().toString())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .claim(CLAIM_HOUSEHOLD_ID, principal.getHouseholdId().toString())
                .claim(CLAIM_ROLE, principal.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProperties.getAccessTokenTtlMinutes(), ChronoUnit.MINUTES)))
                .signWith(signingKey)
                .compact();
    }

    /** Deliberately carries only the subject, not role/householdId -- AuthController.refresh() re-reads those fresh from the DB, so a role change or deactivation takes effect on the next refresh instead of surviving until this long-lived token expires. */
    public String generateRefreshToken(AuthenticatedPrincipal principal) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(principal.getStaffId().toString())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(jwtProperties.getRefreshTokenTtlDays(), ChronoUnit.DAYS)))
                .signWith(signingKey)
                .compact();
    }

    /** Throws io.jsonwebtoken.JwtException (or a subclass) for any invalid/expired/tampered token. */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    public UUID extractStaffId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    /** Present on access tokens only -- see AuthenticatedPrincipal's activeHouseholdId constructor for why this can differ from the staff member's own household. */
    public UUID extractHouseholdId(Claims claims) {
        String value = claims.get(CLAIM_HOUSEHOLD_ID, String.class);
        return value != null ? UUID.fromString(value) : null;
    }
}
