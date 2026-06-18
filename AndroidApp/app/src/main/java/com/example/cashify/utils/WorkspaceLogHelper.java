package com.example.cashify.utils;

import com.example.cashify.data.model.LogActionType;
import com.example.cashify.data.repository.WorkspaceLogRepository;

/**
 * Facade utility for tracking Workspace Audit Logs.
 * * Acts as the single entry point for the entire team to record workspace activities.
 * It delegates the actual database operations to WorkspaceLogRepository.pushLog().
 *
 * ═══════════════════════════════════════════════════════════
 * USAGE EXAMPLES
 * ═══════════════════════════════════════════════════════════
 *
 * // 1. Adding a transaction
 * WorkspaceLogHelper.pushLog(
 * workspaceId,
 * currentUserId,
 * LogActionType.ADD_TRANS,
 * "added an expense of 150.000đ for Food"
 * );
 *
 * // 2. Renaming a workspace
 * WorkspaceLogHelper.pushLog(
 * workspaceId,
 * currentUserId,
 * LogActionType.RENAME_WORKSPACE,
 * "renamed the fund to \"Da Lat Trip Fund\""
 * );
 *
 * ═══════════════════════════════════════════════════════════
 * MESSAGE FORMATTING RULES:
 * - DO NOT include the user's name in the message string.
 * - The UI Adapter will automatically fetch and prepend the user's display name.
 * - The message should ONLY contain the action details, e.g., "deleted an income of 500.000đ"
 * ═══════════════════════════════════════════════════════════
 */
public final class WorkspaceLogHelper {

    /**
     * Pushes a new action log to the Firestore database.
     *
     * @param workspaceId  The target workspace/fund ID
     * @param userId       The Firebase UID of the user performing the action
     * @param actionType   A predefined constant from {@link LogActionType}
     * @param message      The action description (excluding the user's name)
     */
    public static void pushLog(
            String workspaceId,
            String userId,
            String actionType,
            String message
    ) {
        WorkspaceLogRepository.pushLog(workspaceId, userId, actionType, message);
    }

    // Prevents accidental instantiation
    private WorkspaceLogHelper() {}
}