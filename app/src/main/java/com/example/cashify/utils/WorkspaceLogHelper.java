package com.example.cashify.utils;

import com.example.cashify.data.model.LogActionType;
import com.example.cashify.data.repository.WorkspaceLogRepository;

/**
 * WorkspaceLogHelper.java — "Súng bắn log" của toàn team.
 *
 * Đây là class duy nhất toàn team cần biết để ghi log.
 * Delegate thực sự xuống WorkspaceLogRepository.pushLog().
 *
 * ═══════════════════════════════════════════════════════════
 *  CÁCH DÙNG (ném vào nhóm chat cho team):
 * ═══════════════════════════════════════════════════════════
 *
 *  // Thêm giao dịch
 *  WorkspaceLogHelper.pushLog(
 *      workspaceId,
 *      currentUserId,
 *      LogActionType.ADD_TRANS,
 *      "đã thêm khoản chi 150.000đ cho Ăn uống"
 *  );
 *
 *  // Đổi tên quỹ
 *  WorkspaceLogHelper.pushLog(
 *      workspaceId,
 *      currentUserId,
 *      LogActionType.RENAME_WORKSPACE,
 *      "đã đổi tên quỹ thành \"Quỹ Du lịch Đà Lạt\""
 *  );
 *
 * ═══════════════════════════════════════════════════════════
 *  LƯU Ý VỀ message:
 *  - KHÔNG cần thêm tên người dùng vào message.
 *  - Adapter sẽ tự fetch và ghép tên ở đầu.
 *  - Message chỉ cần là phần hành động, vd: "đã xóa khoản thu 500.000đ"
 * ═══════════════════════════════════════════════════════════
 */
public final class WorkspaceLogHelper {

    /**
     * Ghi một log hành động vào Firestore.
     *
     * @param workspaceId  ID của workspace/quỹ
     * @param userId       UID Firebase của người thực hiện
     * @param actionType   Hằng số từ {@link LogActionType}
     * @param message      Nội dung hành động (không cần tên user)
     */
    public static void pushLog(
            String workspaceId,
            String userId,
            String actionType,
            String message
    ) {
        WorkspaceLogRepository.pushLog(workspaceId, userId, actionType, message);
    }

    private WorkspaceLogHelper() {}
}
