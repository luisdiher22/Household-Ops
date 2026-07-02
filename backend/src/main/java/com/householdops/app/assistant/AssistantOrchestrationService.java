package com.householdops.app.assistant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.householdops.app.assistant.AssistantDtos.QueryResponse;
import com.householdops.app.assistant.AssistantDtos.ToolCallRecord;
import com.householdops.app.security.AuthenticatedPrincipal;

/**
 *  Spring AI's ChatClient already runs the
 * tool-calling round-trip loop internally (send prompt -> model requests a
 * tool -> execute it -> send the result back -> repeat until a final
 * answer), so there's no hand-rolled loop to manage here. This just wires
 * the household-scoped, audit-logged ToolContext into that call and shapes
 * the response.
 */
@Service
public class AssistantOrchestrationService {

    private static final String SYSTEM_PROMPT = """
            You are a household operations assistant for a household management team
            (an owner/principal and their house manager, staff, and vendors).

            Answer questions about inventory, tasks, approvals, and the shopping list
            using the tools provided -- always call a tool to get real data rather than
            guessing or estimating figures. All tools are automatically scoped to the
            caller's own household, so you never need to ask which household to use.

            Give direct, concise answers a busy house manager can act on immediately.
            If a question needs both task and inventory information, call both tools
            before answering.
            """;

    // The ChatClient for interacting with the AI model
    // The AssistantTools for providing household-specific tool functionality
    private final ChatClient chatClient;
    private final AssistantTools assistantTools;

    // Constructor for the AssistantOrchestrationService, initializing the ChatClient and AssistantTools
    public AssistantOrchestrationService(ChatClient.Builder chatClientBuilder, AssistantTools assistantTools) {
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
        this.assistantTools = assistantTools;
    }

    // Handles a user query by sending it to the AI model along with the household context and audit log.
    public QueryResponse query(AuthenticatedPrincipal principal, String question) {
        List<ToolCallRecord> auditLog = new ArrayList<>();

        String answer = chatClient.prompt()
                .user(question)
                .tools(assistantTools)
                .toolContext(Map.of(
                        "householdId", principal.getHouseholdId().toString(),
                        "auditLog", auditLog))
                .call()
                .content();

        return new QueryResponse(answer, auditLog);
    }
}
