package com.example.cashify.data.repository;

import com.example.cashify.data.model.User;
import com.example.cashify.data.model.Workspace;
import com.example.cashify.ui.transactions.TransactionViewModel;

import java.util.List;

public interface IWorkspaceRepo {
    //Interface quy định các hàm như getWorkspaces(userId), createWorkspace(workspace).

    // ============================================================
    // PHẦN 1: QUẢN LÝ QUỸ & SIDEBAR (Cũ của ghệ)
    // ============================================================
    // Lấy tất cả Workspace mà User này là thành viên (để hiện lên Side Bar)
    void getWorkspaces(String userId, OnWorkspacesLoadedListener listener);

    // Tạo một Quỹ mới (Cá nhân hoặc Nhóm)
    void createWorkspace(Workspace workspace, OnActionCompleteListener listener);

    // Mời thành viên mới vào Quỹ (Dùng email để tìm UID)
    void addMember(String workspaceId, String memberEmail, OnActionCompleteListener listener);

    // ============================================================
    // PHẦN 2: DÀNH CHO MÀN HÌNH WORKSPACE DETAIL (Mới bổ sung)
    // ============================================================
    // Lấy chi tiết 1 Quỹ (để lấy Tên, Số dư, Tổng thu, Tổng chi)
    void getWorkspaceById(String workspaceId, OnWorkspaceDetailLoadedListener listener);

    // Lấy chi tiết danh sách User (Tên, Avatar) trong Quỹ này
    void getWorkspaceMembers(String workspaceId, OnMembersLoadedListener listener);

    // Lấy lịch sử giao dịch của Quỹ này (Đã được nhóm theo ngày để ném thẳng vào Adapter)
    void getWorkspaceTransactions(String workspaceId, OnTransactionsLoadedListener listener);


    // ============================================================
    // INTERFACE TRẢ KẾT QUẢ (CALLBACKS)
    // ============================================================
    interface OnWorkspacesLoadedListener {
        void onSuccess(List<Workspace> workspaces);
        void onError(Exception e);
    }

    interface OnActionCompleteListener {
        void onSuccess();
        void onError(Exception e);
    }

    // --- Các Callback mới bổ sung ---
    interface OnWorkspaceDetailLoadedListener {
        void onSuccess(Workspace workspace);
        void onError(Exception e);
    }

    interface OnMembersLoadedListener {
        void onSuccess(List<User> members);
        void onError(Exception e);
    }

    interface OnTransactionsLoadedListener {
        void onSuccess(List<TransactionViewModel.HistoryItem> transactions);
        void onError(Exception e);
    }
}