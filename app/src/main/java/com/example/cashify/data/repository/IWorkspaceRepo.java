package com.example.cashify.data.repository;

import com.example.cashify.data.model.Workspace;

import java.util.List;

public interface IWorkspaceRepo {
    //Interface quy định các hàm như getWorkspaces(userId), createWorkspace(workspace).

    // Lấy tất cả Workspace mà User này là thành viên (để hiện lên Side Bar)
    void getWorkspaces(String userId, OnWorkspacesLoadedListener listener);

    // Tạo một Quỹ mới (Cá nhân hoặc Nhóm)
    void createWorkspace(Workspace workspace, OnActionCompleteListener listener);

    // Mời thành viên mới vào Quỹ (Dùng email để tìm UID)
    void addMember(String workspaceId, String memberEmail, OnActionCompleteListener listener);

    // Interface trả kết quả
    interface OnWorkspacesLoadedListener {
        void onSuccess(List<Workspace> workspaces);
        void onError(Exception e);
    }

    interface OnActionCompleteListener {
        void onSuccess();
        void onError(Exception e);
    }
}
