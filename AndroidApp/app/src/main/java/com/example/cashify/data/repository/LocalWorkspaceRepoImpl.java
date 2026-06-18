package com.example.cashify.data.repository;

import com.example.cashify.data.local.WorkspaceDao;
import com.example.cashify.data.model.User;
import com.example.cashify.data.model.Workspace;
import com.example.cashify.ui.transactions.TransactionViewModel;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Triển khai Repository cho Quỹ nhóm (Cache dữ liệu Offline cho Sidebar)
public class LocalWorkspaceRepoImpl implements IWorkspaceRepo {

    private final WorkspaceDao workspaceDao;
    private final ExecutorService executor;

    public LocalWorkspaceRepoImpl(WorkspaceDao dao) {
        this.workspaceDao = dao;
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void getWorkspaces(String userId, OnWorkspacesLoadedListener listener) {
        executor.execute(() -> {
            try {
                // Đổ data từ Local Cache lên UI ngay lập tức để user không bị khựng chờ mạng
                List<Workspace> list = workspaceDao.getAllWorkspaces();
                if (listener != null) listener.onSuccess(list);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }

    @Override
    public void createWorkspace(Workspace workspace, OnActionCompleteListener listener) {
        executor.execute(() -> {
            try {
                workspaceDao.insert(workspace);
                if (listener != null) listener.onSuccess();
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }

    @Override
    public void addMember(String workspaceId, String memberEmail, OnActionCompleteListener listener) {
        // Local DB không có thẩm quyền xử lý Logic gửi lời mời (Bắt buộc phải qua Remote/Firebase)
        if (listener != null) listener.onError(new UnsupportedOperationException("Local DB cannot invite members."));
    }

    @Override
    public void getWorkspaceById(String workspaceId, OnWorkspaceDetailLoadedListener listener) {
        executor.execute(() -> {
            try {
                Workspace workspace = workspaceDao.getWorkspaceById(workspaceId);
                if (listener != null) {
                    if (workspace != null) listener.onSuccess(workspace);
                    else listener.onError(new Exception("Workspace not found in cache"));
                }
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }

    @Override
    public void getWorkspaceMembers(String workspaceId, OnMembersLoadedListener listener) {
        // Chưa hỗ trợ lưu Cache danh sách User Profile (Để trống hợp lý)
        if (listener != null) listener.onError(new UnsupportedOperationException("Members not cached locally."));
    }

    @Override
    public void getWorkspaceTransactions(String workspaceId, OnTransactionsLoadedListener listener) {
        // Việc mapping sang HistoryItem hiện đang được xử lý ở Remote, Local tạm thời chặn
        if (listener != null) listener.onError(new UnsupportedOperationException("Fetch from RemoteTransactionRepo instead."));
    }
}