package com.example.cashify.data.repository;

import com.example.cashify.data.model.Workspace;
import com.google.firebase.firestore.FirebaseFirestore;

public class RemoteWorkspaceRepoImpl implements IWorkspaceRepo {
    //TODO cho bạn Firebase: Viết câu query Firestore để lấy tất cả Workspace mà members có chứa currentUserID.

    private final FirebaseFirestore db;

    public RemoteWorkspaceRepoImpl() {
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public void getWorkspaces(String userId, OnWorkspacesLoadedListener listener) {
        // ============================================================
        // TODO 1: QUERY DANH SÁCH QUỸ
        // - Truy cập collection "workspaces".
        // - Dùng .whereArrayContains("members", userId) để lấy các quỹ user tham gia.
        // - Convert result sang List<Workspace> và gọi listener.onSuccess().
        // ============================================================
    }

    @Override
    public void createWorkspace(Workspace workspace, OnActionCompleteListener listener) {
        // ============================================================
        // TODO 2: TẠO DOCUMENT MỚI
        // - Dùng db.collection("workspaces").add(workspace).
        // - Lưu ý: Sau khi tạo thành công, phải update lại field "id" bằng documentId vừa tạo.
        // ============================================================
    }

    @Override
    public void addMember(String workspaceId, String memberEmail, OnActionCompleteListener listener) {
        // ============================================================
        // TODO 3: MỜI THÀNH VIÊN (LOGIC PHỨC TẠP)
        // - B1: Query collection "users" tìm document có email == memberEmail.
        // - B2: Lấy UID của user đó.
        // - B3: Dùng FieldValue.arrayUnion(newUid) để thêm UID vào mảng "members" của WorkspaceId.
        // ============================================================
    }
}
