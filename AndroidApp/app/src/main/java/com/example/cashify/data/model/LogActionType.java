package com.example.cashify.data.model;

/**
 * Constants định nghĩa các loại hành động lưu Audit Log.
 * Tập trung tại đây để đồng bộ toàn project và tránh lỗi Typo (lỗi gõ sai chữ) khi gọi hàm pushLog().
 */
public final class LogActionType {

    // --- Transactions ---
    public static final String ADD_TRANS    = "ADD_TRANS";
    public static final String EDIT_TRANS   = "EDIT_TRANS";
    public static final String DELETE_TRANS = "DELETE_TRANS";

    // --- Workspace Members ---
    public static final String CREATE_WORKSPACE = "CREATE_WORKSPACE";
    public static final String ADD_MEMBER       = "ADD_MEMBER";
    public static final String LEAVE_WORKSPACE  = "LEAVE_WORKSPACE";
    public static final String KICK_MEMBER      = "KICK_MEMBER";

    // --- Workspace Config ---
    public static final String RENAME_WORKSPACE = "RENAME_WORKSPACE";
    public static final String CHANGE_ICON      = "CHANGE_ICON";

    private LogActionType() {} // Private constructor để ngăn việc khởi tạo object từ class này
}