package com.example.cashify.data.repository;

import com.example.cashify.data.model.Transaction;

import java.util.List;

/**
 * Interface trừu tượng hóa tầng Data cho giao dịch.
 * Đảm bảo nguyên lý Dependency Inversion: ViewModel chỉ gọi Interface này.
 * Ví dụ triển khai:
 * - Local: ITransactionRepo repo = new LocalTransactionRepoImpl(TransactionDao());
 * - Remote: ITransactionRepo repo = new RemoteTransactionRepoImpl();
 */
public interface ITransactionRepo {

    void getHistory(String workspaceId, OnDataLoadedListener listener);

    void addTransaction(Transaction transaction, OnActionCompleteListener listener);

    // Dùng Callback Listener để xử lý bất đồng bộ (tránh treo UI khi chờ Database/Network)
    interface OnDataLoadedListener {
        void onSuccess(List<Transaction> transactions);
        void onError(Exception e);
    }

    interface OnActionCompleteListener {
        void onSuccess();
        void onError(Exception e);
    }
}