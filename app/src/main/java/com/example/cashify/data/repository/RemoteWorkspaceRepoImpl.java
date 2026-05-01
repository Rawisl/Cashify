package com.example.cashify.data.repository;

import android.util.Log;

import com.example.cashify.data.model.Workspace;
import com.example.cashify.data.model.User;
import com.example.cashify.ui.transactions.TransactionViewModel;
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
        // Đổi .get() thành Snapshot để realtime như giao dịch!
        db.collection("workspaces")
                .whereArrayContains("members", userId)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        listener.onError(e);
                        return;
                    }
                    if (queryDocumentSnapshots != null) {
                        List<Workspace> list = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Workspace w = doc.toObject(Workspace.class);
                            w.setId(doc.getId());
                            list.add(w);
                        }
                        listener.onSuccess(list);
                    }
                });
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
    @Override
    public void getWorkspaceById(String workspaceId, OnWorkspaceDetailLoadedListener listener) {
        // Lấy chi tiết Quỹ (Số dư, Thu, Chi)
        db.collection("workspaces").document(workspaceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Workspace workspace = documentSnapshot.toObject(Workspace.class);
                        if (workspace != null) {
                            workspace.setId(documentSnapshot.getId()); // Đảm bảo gán ID
                            listener.onSuccess(workspace);
                        } else {
                            listener.onError(new Exception("Workspace data casting error"));
                        }
                    } else {
                        listener.onError(new Exception("No data found for this fund"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    @Override
    public void getWorkspaceMembers(String workspaceId, OnMembersLoadedListener listener) {
        // Bước 1: Lấy danh sách UID (List<String>) từ Quỹ này trước
        db.collection("workspaces").document(workspaceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) return;
                    Workspace ws = documentSnapshot.toObject(Workspace.class);
                    if (ws == null || ws.getMembers() == null || ws.getMembers().isEmpty()) {
                        listener.onSuccess(new ArrayList<>()); // Quỹ chưa có ai (hoặc lỗi)
                        return;
                    }

                    // Bước 2: Dùng mảng UID đó chọc thẳng vào bảng Users để lấy Tên + Avatar
                    List<String> uids = ws.getMembers();
                    db.collection("users")
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), uids)
                            .get()
                            .addOnSuccessListener(userSnaps -> {
                                List<User> memberList = new ArrayList<>();
                                for (QueryDocumentSnapshot userDoc : userSnaps) {
                                    memberList.add(userDoc.toObject(User.class));
                                }
                                listener.onSuccess(memberList); // Trả List<User> về cho ViewModel
                            })
                            .addOnFailureListener(listener::onError);
                })
                .addOnFailureListener(listener::onError);
    }

    @Override
    public void getWorkspaceTransactions(String workspaceId, OnTransactionsLoadedListener listener) {
        // Đổi .get() thành .addSnapshotListener() để đồng bộ Real-time như Ví cá nhân!
        db.collection("workspaces").document(workspaceId).collection("transactions")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    // Nếu có lỗi thì báo lỗi
                    if (e != null) {
                        listener.onError(e);
                        return;
                    }

                    // Nếu có data cập nhật (bao gồm cả lúc mới tải và lúc có thêm/sửa/xóa)
                    if (queryDocumentSnapshots != null) {
                        List<com.example.cashify.data.model.Transaction> list = new ArrayList<>();
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            com.example.cashify.data.model.Transaction t = doc.toObject(com.example.cashify.data.model.Transaction.class);
                            t.id = doc.getId();
                            list.add(t);
                        }

                        // Chế biến qua hàm mapToHistoryItems rồi ném cho Giao diện vẽ
                        listener.onSuccess(mapToHistoryItems(list));
                    }
                });
    }

    // Hàm phụ trợ để nhóm giao dịch theo ngày (cho giao diện đẹp)
    private List<com.example.cashify.ui.transactions.TransactionViewModel.HistoryItem> mapToHistoryItems(List<com.example.cashify.data.model.Transaction> transactions) {
        List<com.example.cashify.ui.transactions.TransactionViewModel.HistoryItem> historyItems = new java.util.ArrayList<>();
        if (transactions == null || transactions.isEmpty()) return historyItems;

        // Định dạng ngày giống y hệt bên TransactionViewModel đang xài
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.ENGLISH);
        String lastDate = "";

        for (com.example.cashify.data.model.Transaction t : transactions) {
            String currentDate = sdf.format(new java.util.Date(t.timestamp));

            // 1. Tạo Header Ngày (Nếu là ngày mới)
            if (!currentDate.equals(lastDate)) {
                historyItems.add(new com.example.cashify.ui.transactions.TransactionViewModel.HistoryItem(currentDate));
                lastDate = currentDate;
            }

            // 2. Tạo Item Giao Dịch
            // (Lưu ý: Vì trong file Repo này không gọi được Room Database để tra tên Category,
            // nên tạm thời set cứng tên/icon mặc định. Sau này có thời gian rảnh thì map id sau)
            historyItems.add(new com.example.cashify.ui.transactions.TransactionViewModel.HistoryItem(
                    t,
                    "Mục chi tiêu " + t.categoryId, // Tên tạm
                    "ic_other",                     // Icon mặc định
                    "#808080"                       // Màu xám mặc định
            ));
        }
        return historyItems;
    }
}
