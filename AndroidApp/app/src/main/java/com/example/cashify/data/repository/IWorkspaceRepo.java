package com.example.cashify.data.repository;

import com.example.cashify.data.model.User;
import com.example.cashify.data.model.Workspace;
import com.example.cashify.ui.transactions.TransactionViewModel;

import java.util.List;

/**
 * Interface trừu tượng hóa tầng Data cho Không gian làm việc (Workspace/Quỹ nhóm).
 * Định nghĩa các thao tác bất đồng bộ (Async) thông qua Callbacks.
 */
public interface IWorkspaceRepo {

    // ============================================================
    // PHẦN 1: QUẢN LÝ QUỸ & SIDEBAR
    // ============================================================

    // Lấy tất cả Workspace mà User hiện tại là thành viên (Dùng để render Sidebar)
    void getWorkspaces(String userId, OnWorkspacesLoadedListener listener);

    // Khởi tạo một Quỹ mới (Cá nhân hoặc Nhóm)
    void createWorkspace(Workspace workspace, OnActionCompleteListener listener);

    // Mời thành viên mới vào Quỹ thông qua Email
    void addMember(String workspaceId, String memberEmail, OnActionCompleteListener listener);

    // ============================================================
    // PHẦN 2: THÔNG TIN CHI TIẾT QUỸ NHÓM (WORKSPACE DETAIL)
    // ============================================================

    // Lấy chi tiết thông tin 1 Quỹ (Tên, Số dư, Tổng thu, Tổng chi)
    void getWorkspaceById(String workspaceId, OnWorkspaceDetailLoadedListener listener);

    // Lấy danh sách hồ sơ thành viên (User Profile: Tên, Avatar) thuộc Quỹ
    void getWorkspaceMembers(String workspaceId, OnMembersLoadedListener listener);

    // Lấy lịch sử giao dịch của Quỹ (Đã được tiền xử lý/nhóm theo ngày để phục vụ UI)
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