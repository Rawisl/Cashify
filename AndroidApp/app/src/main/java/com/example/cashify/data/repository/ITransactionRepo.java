package com.example.cashify.data.repository;

import com.example.cashify.data.model.Transaction;

import java.util.List;

public interface ITransactionRepo {

//    // Khi nào muốn xài offline thì:
//    ITransactionRepo repo = new LocalTransactionRepoImpl(TransactionDao());
//    // Khi nào muốn xài online thì:
//    ITransactionRepo repo = new RemoteTransactionRepoImpl();

    // Đây là các "điều khoản" bắt buộc mọi Repository phải tuân theo

    void getHistory(String workspaceId, OnDataLoadedListener listener);

    void addTransaction(Transaction transaction, OnActionCompleteListener listener);

    // Vì Firebase chạy bất đồng bộ (phải chờ mạng), ta dùng Listener để báo kết quả về UI
    interface OnDataLoadedListener {
        void onSuccess(List<Transaction> transactions);
        void onError(Exception e);
    }

    interface OnActionCompleteListener {
        void onSuccess();
        void onError(Exception e);
    }
}
