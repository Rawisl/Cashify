package com.example.cashify.data.repository;

import android.util.Log;

import com.example.cashify.data.model.Transaction;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RemoteTransactionRepoImpl implements ITransactionRepo {
    private final FirebaseFirestore db;
    private final String COLLECTION_NAME = "transactions";

    public RemoteTransactionRepoImpl() {
        this.db = FirebaseFirestore.getInstance();

        // GIẢI QUYẾT TODO 5: OFFLINE CACHE
        // Android SDK của Firebase mặc định ĐÃ BẬT SẴN tính năng Offline Persistence rồi!
    }

    @Override
    public void getHistory(String workspaceId, OnDataLoadedListener listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("workspaceId", workspaceId) // Lọc đúng Quỹ
                .orderBy("timestamp", Query.Direction.DESCENDING) // Xếp mới nhất lên đầu
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Transaction> transactions = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Transaction t = doc.toObject(Transaction.class);
                        t.id = doc.getId();
                        transactions.add(t);
                    }
                    listener.onSuccess(transactions);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseRepo", "Failed to fetch transaction history: ", e);
                    listener.onError(e);
                });
    }

    @Override
    public void addTransaction(Transaction transaction, OnActionCompleteListener listener) {
        // Lấy UID của user đang đăng nhập (Bắt buộc phải có để lưu vào Personal)
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            listener.onError(new Exception("Bạn chưa đăng nhập!"));
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        DocumentReference docRef;

        // PHÂN LUỒNG 1: LƯU VÀO VÍ CÁ NHÂN
        if (transaction.workspaceId == null || transaction.workspaceId.equals("PERSONAL")) {
            if (transaction.id != null && !transaction.id.isEmpty()) {
                // Sửa giao dịch cũ (Đường dẫn: users/{uid}/transactions/{id})
                docRef = db.collection("users").document(uid)
                        .collection("transactions").document(transaction.id);
            } else {
                // Thêm giao dịch mới
                docRef = db.collection("users").document(uid).collection("transactions").document();
                transaction.id = docRef.getId();
            }
        }
        // PHÂN LUỒNG 2: LƯU VÀO QUỸ NHÓM
        else {
            if (transaction.id != null && !transaction.id.isEmpty()) {
                // Sửa giao dịch cũ (Đường dẫn: workspaces/{workspaceId}/transactions/{id})
                docRef = db.collection("workspaces").document(transaction.workspaceId)
                        .collection("transactions").document(transaction.id);
            } else {
                // Thêm giao dịch mới
                docRef = db.collection("workspaces").document(transaction.workspaceId)
                        .collection("transactions").document();
                transaction.id = docRef.getId();
            }
        }

        // Tống lên Firebase theo đúng đường dẫn đã phân luồng
        docRef.set(transaction)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> {
                    android.util.Log.e("FirebaseRepo", "Lỗi lưu giao dịch: ", e);
                    listener.onError(e);
                });
    }

    //XỬ LÝ SỔ NỢ / CHIA TIỀN (Dọn đường trước)
    public void updateDebtStatus(String transactionId, boolean isPaid, OnActionCompleteListener listener) {
        db.collection(COLLECTION_NAME).document(transactionId)
                .update("isPaid", isPaid)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e("FirebaseRepo", "Failed to update debt status: ", e);
                    listener.onError(e);
                });
    }
}