package com.householdops.app.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Only relevant when the frontend is bundled in (mvn package -Pbundle-frontend).
 * React Router handles navigation client-side, but a hard refresh or a
 * direct URL on a sub-route like /tasks would otherwise 404 against Spring's
 * static resource handler, which only serves exact file matches.
 */
@Controller
public class SpaFallbackController {

    @GetMapping({"/", "/tasks", "/shopping-list", "/approvals", "/assistant", "/login"})
    public String index() {
        return "forward:/index.html";
    }
}
