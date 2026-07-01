package com.householdops.app.assistant;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.householdops.app.approval.ApprovalDtos.ApprovalResponse;
import com.householdops.app.approval.ApprovalService;
import com.householdops.app.approval.ApprovalStatus;
import com.householdops.app.approval.ApprovalSubjectType;
import com.householdops.app.assistant.AssistantDtos.ToolCallRecord;
import com.householdops.app.inventory.InventoryDtos.InventoryItemResponse;
import com.householdops.app.inventory.InventoryDtos.InventoryStatusResponse;
import com.householdops.app.inventory.InventoryDtos.ValuationResponse;
import com.householdops.app.inventory.InventoryService;
import com.householdops.app.inventory.InventoryStatusService;
import com.householdops.app.shoppinglist.ShoppingListDtos.ShoppingListItemResponse;
import com.householdops.app.shoppinglist.ShoppingListItemStatus;
import com.householdops.app.shoppinglist.ShoppingListService;
import com.householdops.app.task.HouseholdTask;
import com.householdops.app.task.TaskDtos.TaskResponse;
import com.householdops.app.task.TaskService;

import lombok.RequiredArgsConstructor;

/**
 * Six read-only tools grounding the assistant in the caller's own live data
 * -- function-calling over fixed structured queries, not RAG, since these
 * questions ("what's low on stock", "what's due soon") need reliable
 * relational lookups, not semantic document search. None of these tools
 * perform writes; that's a deliberate safety boundary.
 *
 * The household to query always comes from ToolContext (populated by
 * AssistantOrchestrationService from the authenticated caller), never from
 * an LLM-supplied argument -- the model can't be prompted into pulling
 * another household's data. Each method also appends its own invocation to
 * the shared audit-log list carried in the same context, since a tool call
 * is the only place that reliably knows both that it ran and what it was
 * actually asked for.
 */
@Component
@RequiredArgsConstructor
public class AssistantTools {

    private final InventoryStatusService inventoryStatusService;
    private final InventoryService inventoryService;
    private final TaskService taskService;
    private final ApprovalService approvalService;
    private final ShoppingListService shoppingListService;

    @Tool(description = "Get current inventory levels for the caller's household: low-stock items, items expiring soon, and items predicted to run out within a week based on recent consumption")
    public InventoryStatusResponse getInventoryStatus(ToolContext toolContext) {
        UUID householdId = householdId(toolContext);
        record(toolContext, "getInventoryStatus", "household=" + householdId);
        return inventoryStatusService.computeStatus(householdId);
    }

    @Tool(description = "Get every inventory item for the caller's household, including ones that are well-stocked -- use this for questions about a specific item's vendor, cost, or quantity (e.g. 'who supplies the olive oil') rather than getInventoryStatus, which only returns items needing attention")
    public List<InventoryItemResponse> getAllInventoryItems(ToolContext toolContext) {
        UUID householdId = householdId(toolContext);
        record(toolContext, "getAllInventoryItems", "household=" + householdId);
        return inventoryService.findByHousehold(householdId);
    }

    @Tool(description = "Get the total dollar value of the caller's household's inventory, broken down by category (e.g. 'what's my wine cellar worth', 'how much is tied up in cleaning supplies'). Only items with a recorded unit cost are included")
    public ValuationResponse getInventoryValuation(ToolContext toolContext) {
        UUID householdId = householdId(toolContext);
        record(toolContext, "getInventoryValuation", "household=" + householdId);
        return inventoryService.valuation(householdId);
    }

    @Tool(description = "Get open/in-progress tasks for the caller's household due on or before a given date")
    public List<TaskResponse> getUpcomingTasks(
            @ToolParam(description = "ISO-8601 date (yyyy-MM-dd), e.g. the end of this week") String dueBefore,
            ToolContext toolContext) {
        UUID householdId = householdId(toolContext);
        record(toolContext, "getUpcomingTasks", "household=" + householdId + ", dueBefore=" + dueBefore);
        List<HouseholdTask> tasks = taskService.findUpcoming(householdId, LocalDate.parse(dueBefore));
        return tasks.stream()
                .map(task -> TaskResponse.from(task, approvalService.hasPendingApproval(ApprovalSubjectType.TASK, task.getId())))
                .toList();
    }

    @Tool(description = "Get pending approval requests (spend over the household's threshold awaiting the principal's decision) for the caller's household")
    public List<ApprovalResponse> getPendingApprovals(ToolContext toolContext) {
        UUID householdId = householdId(toolContext);
        record(toolContext, "getPendingApprovals", "household=" + householdId);
        return approvalService.findByHousehold(householdId, ApprovalStatus.PENDING, Pageable.unpaged())
                .map(ApprovalResponse::from)
                .getContent();
    }

    @Tool(description = "Get shopping list items for the caller's household, optionally filtered by status (PENDING, PURCHASED, CANCELLED)")
    public List<ShoppingListItemResponse> getShoppingList(
            @ToolParam(description = "Optional status filter: PENDING, PURCHASED, or CANCELLED. Omit to get all items.", required = false) String status,
            ToolContext toolContext) {
        UUID householdId = householdId(toolContext);
        record(toolContext, "getShoppingList", "household=" + householdId + ", status=" + status);
        ShoppingListItemStatus statusFilter = status == null ? null : ShoppingListItemStatus.valueOf(status);
        return shoppingListService.findByHousehold(householdId, statusFilter, Pageable.unpaged())
                .map(item -> ShoppingListItemResponse.from(item, approvalService.hasPendingApproval(ApprovalSubjectType.SHOPPING_ITEM, item.getId())))
                .getContent();
    }

    private UUID householdId(ToolContext toolContext) {
        return UUID.fromString((String) toolContext.getContext().get("householdId"));
    }

    @SuppressWarnings("unchecked")
    private void record(ToolContext toolContext, String tool, String inputSummary) {
        ((List<ToolCallRecord>) toolContext.getContext().get("auditLog")).add(new ToolCallRecord(tool, inputSummary));
    }
}
