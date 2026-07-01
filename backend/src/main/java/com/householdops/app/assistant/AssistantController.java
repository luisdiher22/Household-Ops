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

    private final AssistantOrchestrationService assistantOrchestrationService;

    @PostMapping("/query")
    public QueryResponse query(@Valid @RequestBody QueryRequest request, @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return assistantOrchestrationService.query(principal, request.question());
    }
}
