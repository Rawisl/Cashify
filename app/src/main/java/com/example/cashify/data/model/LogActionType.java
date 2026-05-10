package com.example.cashify.data.model;

/**
 * LogActionType.java
 * Constants cho tất cả loại hành động có thể ghi log.
 * Toàn team dùng chung file này để tránh typo khi gọi pushLog().
 */
public final class LogActionType {

    // ── Giao dịch ─────────────────────────────────────────────────────────────
    public static final String ADD_TRANS    = "ADD_TRANS";
    public static final String EDIT_TRANS   = "EDIT_TRANS";
    public static final String DELETE_TRANS = "DELETE_TRANS";

    // ── Thành viên ────────────────────────────────────────────────────────────
    public static final String CREATE_WORKSPACE = "CREATE_WORKSPACE";
    public static final String ADD_MEMBER       = "ADD_MEMBER";
    public static final String LEAVE_WORKSPACE  = "LEAVE_WORKSPACE";
    public static final String KICK_MEMBER      = "KICK_MEMBER";

    // ── Cấu hình ──────────────────────────────────────────────────────────────
    public static final String RENAME_WORKSPACE = "RENAME_WORKSPACE";
    public static final String CHANGE_ICON      = "CHANGE_ICON";

    private LogActionType() {}
}