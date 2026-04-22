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
    public void getAll(Callback<List<Transaction>> callback){
        executor.execute(() ->callback.onResult(transactionDao.getAll()));
    }
    public void getByDateRange(long start, long end, Callback<List<Transaction>> callback){
        executor.execute(() -> callback.onResult(transactionDao.getByDateRange(start, end)));
    }
    public void getTotalIncome(long start, long end, Callback<Long> callback){
        executor.execute(() -> callback.onResult(transactionDao.getTotalIncome(start, end)));
    }
    public void getTotalExpense(long start, long end, Callback<Long> callback){
        executor.execute(() -> callback.onResult(transactionDao.getTotalExpense(start, end)));
    }
    public void getActualBalance(Callback<Long> callback){
        executor.execute(() -> callback.onResult(transactionDao.getActualBalance()));
    }
    public void getMonthlyBalance(long startDate, long endDate, Callback<Long> callback) {
        executor.execute(() -> callback.onResult(transactionDao.getMonthlyBalance(startDate, endDate)));
    }

    public LiveData<List<TransactionWithCategory>> getRecentTransactionsWithCategory() {
        return transactionDao.getRecentTransactionsWithCategory();
    }
    public void getTop5ExpenseCategories(long start, long end, Callback<List<CategorySum>> callback){
        executor.execute(()-> callback.onResult(transactionDao.getTop5ExpenseCategories(start, end)));
    }
    public void getOtherExpenseTotal(long start, long end, Callback<Long> callback){
        executor.execute(()->callback.onResult(transactionDao.getOtherExpenseTotal(start, end)));
    }
    public void getTotalExpenseByCategory(int catergoryId, long start, long end, Callback<Long> callback){
        executor.execute(()->callback.onResult(transactionDao.getTotalExpenseByCategory(catergoryId, start, end)));
    }
    public void countTransactionByDay(long startOfDay, long endOfDay, Callback<Integer> callback){
        executor.execute(()-> callback.onResult(transactionDao.countTransactionsByDay(startOfDay, endOfDay)));
    }

    private void syncTransactionToCloud(Transaction t) {
        Map<String, Object> data = new HashMap<>();
        data.put("amount", t.amount);
        data.put("categoryId", t.categoryId);
        data.put("note", t.note);
        data.put("timestamp", t.timestamp);
        data.put("paymentMethod", t.paymentMethod);
        data.put("type", t.type);

        firebaseManager.syncLocalToCloud("transactions", String.valueOf(t.id), data, new FirebaseManager.DataCallback<Void>() {
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
