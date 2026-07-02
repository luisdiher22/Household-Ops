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
    *  This class defines the tools available to the household operations assistant. Each method is annotated with @Tool.
 */
@Component
@RequiredArgsConstructor
public class AssistantTools {

    // Services for handling inventory, tasks, approvals, and shopping list operations
    private final InventoryStatusService inventoryStatusService;
    private final InventoryService inventoryService;
    private final TaskService taskService;
    private final ApprovalService approvalService;
    private final ShoppingListService shoppingListService;

    /*
        This method retrieves the current inventory status for the caller's household, including low-stock items, items expiring soon,
        and items predicted to run out within a week based on recent consumption.
        It takes a ToolContext as input, which contains the household ID and an audit log for recording tool calls. 
        The method returns an InventoryStatusResponse containing the relevant inventory information.
     */
    @Tool(description = "Get current inventory levels for the caller's household: low-stock items, items expiring soon, and items predicted to run out within a week based on recent consumption")
    public InventoryStatusResponse getInventoryStatus(ToolContext toolContext) {
        UUID householdId = householdId(toolContext);
        record(toolContext, "getInventoryStatus", "household=" + householdId);
        return inventoryStatusService.computeStatus(householdId);
    }

    /*
        This method retrieves all inventory items for the caller's household, including those that are well-stocked.
        It takes a ToolContext as input, which contains the household ID and an audit log for recording tool calls.
        The method returns a list of InventoryItemResponse objects containing the relevant inventory information.
     */
    @Tool(description = "Get every inventory item for the caller's household, including ones that are well-stocked -- use this for questions about a specific item's vendor, cost, or quantity (e.g. 'who supplies the olive oil') rather than getInventoryStatus, which only returns items needing attention")
    public List<InventoryItemResponse> getAllInventoryItems(ToolContext toolContext) {
        UUID householdId = householdId(toolContext);
        record(toolContext, "getAllInventoryItems", "household=" + householdId);
        return inventoryService.findByHousehold(householdId);
    }

    /*
        This method retrieves the total dollar value of the caller's household's inventory, broken down by category.
        It takes a ToolContext as input, which contains the household ID and an audit log for recording tool calls.
        The method returns a ValuationResponse containing the relevant valuation information.
     */
    @Tool(description = "Get the total dollar value of the caller's household's inventory, broken down by category (e.g. 'what's my wine cellar worth', 'how much is tied up in cleaning supplies'). Only items with a recorded unit cost are included")
    public ValuationResponse getInventoryValuation(ToolContext toolContext) {
        UUID householdId = householdId(toolContext);
        record(toolContext, "getInventoryValuation", "household=" + householdId);
        return inventoryService.valuation(householdId);
    }

    /*
        This method retrieves open/in-progress tasks for the caller's household due on or before a given date.
        It takes a ToolContext as input, which contains the household ID and an audit log for recording tool calls.
        The method returns a list of TaskResponse objects containing the relevant task information.
     */
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
    /*
        This method retrieves pending approval requests for the caller's household, which are spend requests over the household's threshold awaiting the principal's decision.
        It takes a ToolContext as input, which contains the household ID and an audit log for recording tool calls.
        The method returns a list of ApprovalResponse objects containing the relevant approval information.
     */
    @Tool(description = "Get pending approval requests (spend over the household's threshold awaiting the principal's decision) for the caller's household")
    public List<ApprovalResponse> getPendingApprovals(ToolContext toolContext) {
        UUID householdId = householdId(toolContext);
        record(toolContext, "getPendingApprovals", "household=" + householdId);
        return approvalService.findByHousehold(householdId, ApprovalStatus.PENDING, Pageable.unpaged())
                .map(ApprovalResponse::from)
                .getContent();
    }

    /*
        This method retrieves shopping list items for the caller's household, optionally filtered by status.
        It takes a ToolContext as input, which contains the household ID and an audit log for recording tool calls.
        The method returns a list of ShoppingListItemResponse objects containing the relevant shopping list information.
     */
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

    // Helper method to extract the household ID from the ToolContext
    private UUID householdId(ToolContext toolContext) {
        return UUID.fromString((String) toolContext.getContext().get("householdId"));
    }

    // Helper method to record tool calls in the audit log within the ToolContext
    @SuppressWarnings("unchecked")
    private void record(ToolContext toolContext, String tool, String inputSummary) {
        ((List<ToolCallRecord>) toolContext.getContext().get("auditLog")).add(new ToolCallRecord(tool, inputSummary));
    }
}
