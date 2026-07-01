package com.householdops.app.assistant;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public class AssistantDtos {

    public record QueryRequest(@NotBlank String question) {
    }

    public record ToolCallRecord(String tool, String inputSummary) {
    }

    public record QueryResponse(String answer, List<ToolCallRecord> toolCalls) {
    }

    private AssistantDtos() {
    }
}
