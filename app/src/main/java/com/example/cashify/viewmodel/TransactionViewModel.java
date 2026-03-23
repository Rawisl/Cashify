package com.example.cashify.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import com.example.cashify.database.CategorySum;
import com.example.cashify.database.Transaction;
import com.example.cashify.repository.TransactionRepository;
import java.util.List;

public class TransactionViewModel extends AndroidViewModel {

    private final TransactionRepository repository;

    public TransactionViewModel(@NonNull Application application) {
        super(application);
        repository = new TransactionRepository(application);
    }

    public void insert(Transaction transaction) {
        repository.insert(transaction);
    }

    public void update(Transaction transaction) {
        repository.update(transaction);
    }

    public void delete(Transaction transaction) {
        repository.delete(transaction);
    }

    public void getAll(TransactionRepository.Callback<List<Transaction>> callback) {
        repository.getAll(callback);
    }

    public void getByDateRange(long start, long end, TransactionRepository.Callback<List<Transaction>> callback) {
        repository.getByDateRange(start, end, callback);
    }

    public void getTotalIncome(long start, long end, TransactionRepository.Callback<Long> callback) {
        repository.getTotalIncome(start, end, callback);
    }

    public void getTotalExpense(long start, long end, TransactionRepository.Callback<Long> callback) {
        repository.getTotalExpense(start, end, callback);
    }

    public void getActualBalance(TransactionRepository.Callback<Long> callback) {
        repository.getActualBalance(callback);
    }

    public void getRecentTransactions(int limit, TransactionRepository.Callback<List<Transaction>> callback) {
        repository.getRecentTransactions(limit, callback);
    }

    public void getTop5ExpenseCategories(long start, long end, TransactionRepository.Callback<List<CategorySum>> callback) {
        repository.getTop5ExpenseCategories(start, end, callback);
    }

    public void getOtherExpenseTotal(long start, long end, TransactionRepository.Callback<Long> callback) {
        repository.getOtherExpenseTotal(start, end, callback);
    }

    public void getTotalExpenseByCategory(int categoryId, long start, long end, TransactionRepository.Callback<Long> callback) {
        repository.getTotalExpenseByCategory(categoryId, start, end, callback);
    }

    public void countTransactionByDay(long startOfDay, long endOfDay, TransactionRepository.Callback<Integer> callback) {
        repository.countTransactionByDay(startOfDay, endOfDay, callback);
    }
}