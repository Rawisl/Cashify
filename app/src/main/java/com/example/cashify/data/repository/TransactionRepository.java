package com.example.cashify.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.cashify.data.local.AppDatabase;
import com.example.cashify.data.local.CategorySum;
import com.example.cashify.data.model.Transaction;
import com.example.cashify.data.local.TransactionDao;
import com.example.cashify.data.local.TransactionWithCategory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionRepository {
    private final TransactionDao transactionDao;
    private final ExecutorService executor;

    public TransactionRepository(Context context){
        transactionDao=AppDatabase.getInstance(context).transactionDao();
        executor=Executors.newSingleThreadExecutor();
    }
    public void insert(Transaction transaction){
        executor.execute(() -> transactionDao.insert(transaction));
    }
    public void update(Transaction transaction){
        executor.execute(() -> transactionDao.update(transaction));
    }
    public void delete(Transaction transaction){
        executor.execute(() -> transactionDao.delete(transaction));
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
