package com.example.cashify.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.cashify.data.local.AppDatabase;
import com.example.cashify.data.local.CategorySum;
import com.example.cashify.data.model.Transaction;
import com.example.cashify.data.local.TransactionDao;
import com.example.cashify.data.local.TransactionWithCategory;
import com.example.cashify.data.remote.FirebaseManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionRepository {
    private final TransactionDao transactionDao;
    private final FirebaseManager firebaseManager;
    private final ExecutorService executor;

    public TransactionRepository(Context context){
        transactionDao=AppDatabase.getInstance(context).transactionDao();
        firebaseManager = FirebaseManager.getInstance();
        executor=Executors.newSingleThreadExecutor();
    }
    public void insert(Transaction transaction) {
        executor.execute(() -> {
            long id = transactionDao.insert(transaction);
            transaction.id = (int) id;
            syncTransactionToCloud(transaction);
        });
    }

    public void update(Transaction transaction) {
        executor.execute(() -> {
            transactionDao.update(transaction);
            syncTransactionToCloud(transaction);
        });
    }

    public void delete(Transaction transaction) {
        executor.execute(() -> {
            transactionDao.delete(transaction);
            // Logic xóa trên Firebase có thể gọi hàm xóa document riêng trong FirebaseManager
        });
    }
    public void getAll(String workspaceId, Callback<List<Transaction>> callback){
        executor.execute(() -> callback.onResult(transactionDao.getAll(workspaceId)));
    }

    public void getByDateRange(String workspaceId, long start, long end, Callback<List<Transaction>> callback){
        executor.execute(() -> callback.onResult(transactionDao.getByDateRange(workspaceId, start, end)));
    }

    public void getTotalIncome(String workspaceId, long start, long end, Callback<Long> callback){
        executor.execute(() -> callback.onResult(transactionDao.getTotalIncome(workspaceId, start, end)));
    }

    public void getTotalExpense(String workspaceId, long start, long end, Callback<Long> callback){
        executor.execute(() -> callback.onResult(transactionDao.getTotalExpense(workspaceId, start, end)));
    }

    public void getActualBalance(String workspaceId, Callback<Long> callback){
        executor.execute(() -> callback.onResult(transactionDao.getActualBalance(workspaceId)));
    }

    public void getMonthlyBalance(String workspaceId, long startDate, long endDate, Callback<Long> callback) {
        executor.execute(() -> callback.onResult(transactionDao.getMonthlyBalance(workspaceId, startDate, endDate)));
    }

    public LiveData<List<TransactionWithCategory>> getRecentTransactionsWithCategory(String workspaceId) {
        return transactionDao.getRecentTransactionsWithCategory(workspaceId);
    }

    public void getTop5ExpenseCategories(String workspaceId, long start, long end, Callback<List<CategorySum>> callback){
        executor.execute(() -> callback.onResult(transactionDao.getTop5ExpenseCategories(workspaceId, start, end)));
    }

    public void getOtherExpenseTotal(String workspaceId, long start, long end, Callback<Long> callback){
        executor.execute(() -> callback.onResult(transactionDao.getOtherExpenseTotal(workspaceId, start, end)));
    }

    public void getTotalExpenseByCategory(String workspaceId, int catergoryId, long start, long end, Callback<Long> callback){
        executor.execute(() -> callback.onResult(transactionDao.getTotalExpenseByCategory(workspaceId, catergoryId, start, end)));
    }

    public void countTransactionByDay(String workspaceId, long startOfDay, long endOfDay, Callback<Integer> callback){
        executor.execute(() -> callback.onResult(transactionDao.countTransactionsByDay(workspaceId, startOfDay, endOfDay)));
    }

    private void syncTransactionToCloud(Transaction t) {
        Map<String, Object> data = new HashMap<>();
        data.put("amount", t.amount);
        data.put("categoryId", t.categoryId);
        data.put("note", t.note);
        data.put("timestamp", t.timestamp);
        data.put("paymentMethod", t.paymentMethod);
        data.put("type", t.type);
        data.put("workspaceId", t.workspaceId); // Nên đẩy cả ID này lên mây để dễ quản lý

        String currentWorkspaceId = (t.workspaceId != null) ? t.workspaceId : "PERSONAL";

        firebaseManager.syncLocalToCloud(currentWorkspaceId, "transactions", String.valueOf(t.id), data, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d("SYNC_OK", "Transaction " + t.id + " Uploaded to cloud!");
            }
            @Override
            public void onError(String message) {
                Log.e("SYNC_FAIL", "Error uploading transaction: " + message);
            }
        });
    }

    public void getById(int id, Callback<Transaction> callback) {
        executor.execute(() -> {
            Transaction t = transactionDao.getById(id);
            // Trả kết quả về cho ViewModel
            if (callback != null) {
                callback.onResult(t);
            }
        });
    }

    //interface giúp trả kqua về UI thread
    public interface Callback<T>{
        void onResult(T result);
    }


}
