package com.example.cashify.data.repository;

import com.example.cashify.data.local.WorkspaceDao;
import com.example.cashify.data.model.Workspace;

public class LocalWorkspaceRepoImpl implements IWorkspaceRepo {
    //Dùng để lưu tạm (Cache) danh sách các Quỹ vào Room để khi mất mạng Side Bar vẫn hiện tên Quỹ được.
    private final WorkspaceDao workspaceDao;

    public LocalWorkspaceRepoImpl(WorkspaceDao dao) {
        this.workspaceDao = dao;
    }

    @Override
    public void getWorkspaces(String userId, OnWorkspacesLoadedListener listener) {
        // TODO 4: Lấy data từ Room đổ lên UI nhanh để user không phải chờ xoay vòng vòng
    }

    @Override
    public void createWorkspace(Workspace workspace, OnActionCompleteListener listener) {
        // TODO 5: Lưu Workspace mới vào SQLite
    }

    @Override
    public void addMember(String workspaceId, String memberEmail, OnActionCompleteListener listener) {
        // (Local thường không xử lý mời thành viên, có thể để trống hoặc báo lỗi)
    }
}
