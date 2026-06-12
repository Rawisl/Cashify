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
            transactionDao.insert(transaction);
            // Báo cho Firebase: Đây là lệnh Insert -> Tính toán cộng dồn
            firebaseManager.syncTransactionWithStats(true, false, transaction, new FirebaseManager.DataCallback<Void>() {
                @Override
                public void onSuccess(Void data) { Log.d("SYNC_OK", "Insert and update stats successful!"); }
                @Override
                public void onError(String message) { Log.e("SYNC_FAIL", "Insert error: " + message); }
            });
        });
    }

    public void update(Transaction transaction) {
        executor.execute(() -> {
            transactionDao.update(transaction);
            // Báo cho Firebase: Đây chỉ là Update -> Đừng đụng vào bộ đếm Stats để tránh x2 dữ liệu
            firebaseManager.syncTransactionWithStats(false, false, transaction, new FirebaseManager.DataCallback<Void>() {
                @Override
                public void onSuccess(Void data) { Log.d("SYNC_OK", "Update successful!"); }
                @Override
                public void onError(String message) { Log.e("SYNC_FAIL", "Update error: " + message); }
            });
        });
    }

    public void delete(Transaction transaction) {
        executor.execute(() -> {
            transactionDao.delete(transaction);
            // Báo cho Firebase: Đây là lệnh Delete -> Trừ bộ đếm Stats đi
            firebaseManager.syncTransactionWithStats(false, true, transaction, new FirebaseManager.DataCallback<Void>() {
                @Override
                public void onSuccess(Void data) { Log.d("SYNC_OK", "Delete and deduct stats successful!"); }
                @Override
                public void onError(String message) { Log.e("SYNC_FAIL", "Delete error: " + message); }
            });
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



    public void getById(String id, Callback<Transaction> callback) {
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
