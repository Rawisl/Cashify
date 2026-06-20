package com.example.cashify.data.repository;

import com.example.cashify.data.local.TransactionDao;
import com.example.cashify.data.model.Transaction;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Triển khai Repository cho Giao dịch (Đọc/Ghi dữ liệu Offline từ SQLite)
public class LocalTransactionRepoImpl implements ITransactionRepo {

    private final TransactionDao transactionDao;
    private final ExecutorService executor;

    // Phun phụ thuộc (Dependency Injection) DAO từ AppDatabase vào đây
    public LocalTransactionRepoImpl(TransactionDao dao) {
        this.transactionDao = dao;
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void getHistory(String workspaceId, OnDataLoadedListener listener) {
        executor.execute(() -> {
            try {
                // Room bắt buộc chạy trên Worker Thread
                List<Transaction> transactions = transactionDao.getAll(workspaceId);
                if (listener != null) listener.onSuccess(transactions);
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }

    @Override
    public void addTransaction(Transaction transaction, OnActionCompleteListener listener) {
        executor.execute(() -> {
            try {
                transactionDao.insert(transaction);
                if (listener != null) listener.onSuccess();
            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            }
        });
    }

    // TODO: Khi nào Interface ITransactionRepo cập nhật thêm hàm Update/Delete thì Override tiếp ở đây
}