package com.example.cashify.data.repository;

import com.example.cashify.data.local.TransactionDao;
import com.example.cashify.data.model.Transaction;

public class LocalTransactionRepoImpl implements ITransactionRepo {
    private final TransactionDao transactionDao;

    // TODO 1: Inject TransactionDao từ AppDatabase vào đây thông qua Constructor
    public LocalTransactionRepoImpl(TransactionDao dao) {
        this.transactionDao = dao;
    }

    @Override
    public void getHistory(String workspaceId, OnDataLoadedListener listener) {
        // ============================================================
        // TODO 2: LOGIC LẤY DATA OFFLINE
        // - Sử dụng transactionDao.getAll() để lấy data từ Room
        // - Nếu thành công: gọi listener.onSuccess(list)
        // - Nếu lỗi: gọi listener.onError(e)
        // Lưu ý: Room bắt buộc chạy trên Thread riêng (Worker Thread), phải dùng ExecutorService hoặc AsyncTask (cũ) để bọc lại.
        // ============================================================
    }

    @Override
    public void addTransaction(Transaction transaction, OnActionCompleteListener listener)
    {
        // ============================================================
        // TODO 3: THÊM GIAO DỊCH MỚI
        // - Gọi transactionDao.insert(transaction)
        // - Đảm bảo workspaceId được gán đúng (UID cá nhân hoặc ID nhóm)
        // ============================================================
    }

    // TODO 4: Bổ sung các hàm Delete, Update tương ứng từ DAO vào Interface trước rồi mới Override ở đây
}
