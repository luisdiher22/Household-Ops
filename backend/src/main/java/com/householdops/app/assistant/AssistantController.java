package com.householdops.app.assistant;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.householdops.app.assistant.AssistantDtos.QueryRequest;
import com.householdops.app.assistant.AssistantDtos.QueryResponse;
import com.householdops.app.security.AuthenticatedPrincipal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    // The service responsible for orchestrating assistant-related operations
    private final AssistantOrchestrationService assistantOrchestrationService;

    // Handles POST requests to the /query endpoint, allowing users to submit questions to the assistant.
    // The request body must contain a valid QueryRequest object, and the authenticated principal is injected
    // to identify the user making the request. The method returns a QueryResponse containing the assistant
    @PostMapping("/query")
    public QueryResponse query(@Valid @RequestBody QueryRequest request, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return assistantOrchestrationService.query(principal, request.question());
    }
}
