package com.householdops.app.household;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.householdops.app.household.PortfolioDtos.PortfolioResponse;
import com.householdops.app.security.AuthenticatedPrincipal;

import lombok.RequiredArgsConstructor;

/** Cross-property summary for an Owner managing more than one household -- see PortfolioService for the authorization design. */
@RestController
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/api/portfolio")
    public PortfolioResponse getPortfolio(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return portfolioService.getPortfolio(principal);
    }
}
