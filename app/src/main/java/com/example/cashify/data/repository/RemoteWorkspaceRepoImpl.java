package com.example.cashify.data.repository;

import com.example.cashify.data.model.Workspace;
import com.example.cashify.data.model.User;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

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
        // QUERY: Lấy tất cả Workspace mà mảng "members" có chứa ID
        db.collection("workspaces")
                .whereArrayContains("members", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Workspace> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Workspace w = doc.toObject(Workspace.class);
                        w.setId(doc.getId()); // Đảm bảo ID luôn khớp với ID trên Firestore
                        list.add(w);
                    }
                    listener.onSuccess(list);
                })
                // Trả thẳng exception e về cho listener
                .addOnFailureListener(e -> listener.onError(e));
    }

    @Override
    public void createWorkspace(Workspace workspace, OnActionCompleteListener listener) {
        // ============================================================
        // TODO 2: TẠO DOCUMENT MỚI
        // - Dùng db.collection("workspaces").add(workspace).
        // - Lưu ý: Sau khi tạo thành công, phải update lại field "id" bằng documentId vừa tạo.
        // ============================================================
        // TẠO DOCUMENT: Để Firebase tự sinh ID
        db.collection("workspaces")
                .add(workspace)
                .addOnSuccessListener(documentReference -> {
                    // CẬP NHẬT ID: Lấy cái ID tự sinh đó nhét ngược lại vào field "id" bên trong document
                    String newId = documentReference.getId();
                    documentReference.update("id", newId)
                            .addOnSuccessListener(aVoid -> listener.onSuccess())
                            .addOnFailureListener(e -> listener.onError(new Exception("Created but failed to update ID: " + e.getMessage())));
                })
                .addOnFailureListener(e -> listener.onError(e));
    }

    @Override
    public void addMember(String workspaceId, String memberEmail, OnActionCompleteListener listener) {
        // ============================================================
        // TODO 3: MỜI THÀNH VIÊN (LOGIC PHỨC TẠP)
        // - B1: Query collection "users" tìm document có email == memberEmail.
        // - B2: Lấy UID của user đó.
        // - B3: Dùng FieldValue.arrayUnion(newUid) để thêm UID vào mảng "members" của WorkspaceId.
        // ============================================================
        // Tìm người dùng dựa trên Email
        db.collection("users")
                .whereEqualTo("email", memberEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Bọc String vào trong new Exception()
                        listener.onError(new Exception("User not found with this email!"));
                        return;
                    }

                    // Lấy UID của người vừa tìm thấy
                    DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                    String memberUid = userDoc.getString("uid");

                    if (memberUid == null) {
                        listener.onError(new Exception("Found user but UID is missing!"));
                        return;
                    }

                    // Thêm UID này vào mảng "members" của Workspace
                    DocumentReference wsRef = db.collection("workspaces").document(workspaceId);
                    wsRef.update("members", FieldValue.arrayUnion(memberUid))
                            .addOnSuccessListener(aVoid -> listener.onSuccess())
                            .addOnFailureListener(e -> listener.onError(new Exception("Failed to add member: " + e.getMessage())));
                })
                .addOnFailureListener(e -> listener.onError(new Exception("Error searching user: " + e.getMessage())));
    }
}
