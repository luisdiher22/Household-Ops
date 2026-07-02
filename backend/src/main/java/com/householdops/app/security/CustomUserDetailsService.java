package com.householdops.app.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.householdops.app.staff.StaffMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final StaffMemberRepository staffMemberRepository;

    // Loads a user by their email address, returning an AuthenticatedPrincipal that wraps the corresponding StaffMember. 
    // Throws UsernameNotFoundException if no staff member with the given email exists.
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return staffMemberRepository.findByEmailIgnoreCase(email)
                .map(AuthenticatedPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("No staff member with email: " + email));
    }
}
