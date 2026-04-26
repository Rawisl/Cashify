package com.example.cashify.data.repository;

import com.example.cashify.data.model.Transaction;
import com.google.firebase.firestore.FirebaseFirestore;

public class RemoteTransactionRepoImpl implements ITransactionRepo {
    private final FirebaseFirestore db;
    private final String COLLECTION_NAME = "transactions";

    public RemoteTransactionRepoImpl() {
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public void getHistory(String workspaceId, OnDataLoadedListener listener) {
        // ============================================================
        // TODO 1: TRUY VẤN DỮ LIỆU TỪ FIRESTORE
        // - Truy cập collection "transactions".
        // - Dùng .whereEqualTo("workspaceId", workspaceId) để lọc đúng dữ liệu.
        // - Dùng .orderBy("date", Query.Direction.DESCENDING) để sắp xếp giao dịch mới nhất lên đầu.
        // - Dùng .get() (Lấy 1 lần) hoặc .addSnapshotListener() (Lấy Real-time).
        // ============================================================

        // ============================================================
        // TODO 2: CHUYỂN ĐỔI DỮ LIỆU (MAPPING)
        // - Chạy vòng lặp for (QueryDocumentSnapshot doc : value)
        // - Dùng doc.toObject(Transaction.class) để biến Document thành Object Java.
        // - Sau khi gom đủ List, gọi listener.onSuccess(list).
        // ============================================================
    }

    @Override
    public void addTransaction(Transaction transaction, OnActionCompleteListener listener) {
        // ============================================================
        // TODO 3: ĐẨY DỮ LIỆU LÊN CLOUD
        // - Dùng db.collection(COLLECTION_NAME).add(transaction)
        // - .addOnSuccessListener -> listener.onSuccess()
        // - .addOnFailureListener -> listener.onError(e)
        // ============================================================
    }

    // ============================================================
    // TODO 4: XỬ LÝ SỔ NỢ / CHIA TIỀN (TƯƠNG LAI)
    // - Viết hàm cập nhật trạng thái "Đã trả / Chưa trả" cho giao dịch.
    // - Đảm bảo khi một người sửa, tất cả thành viên trong nhóm đều thấy thay đổi.
    // ============================================================

    // ============================================================
    // TODO 5: QUẢN LÝ OFFLINE CACHE (TÙY CHỌN)
    // - Bật tính năng Firestore Persistence để app vẫn xem được data khi mất mạng.
    // ============================================================
}