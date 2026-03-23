package com.example.cashify.repository;

import android.content.Context;
import com.example.cashify.database.AppDatabase;
import com.example.cashify.database.Budget;
import com.example.cashify.database.BudgetDao;
import com.example.cashify.database.BudgetWithSpent;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BudgetRepository {

    private final BudgetDao budgetDao;
    private final ExecutorService executor;

    public BudgetRepository(Context context) {
        budgetDao = AppDatabase.getInstance(context).budgetDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public void insert(Budget budget) {
        executor.execute(() -> budgetDao.insert(budget));
    }

    public void update(Budget budget) {
        executor.execute(() -> budgetDao.update(budget));
    }

    public void delete(Budget budget) {
        executor.execute(() -> budgetDao.delete(budget));
    }

    public void getActiveBudgets(long now, Callback<List<Budget>> callback) {
        executor.execute(() -> callback.onResult(budgetDao.getActiveBudgets(now)));
    }

    public void getBudgetByCategory(int categoryId, long now, Callback<Budget> callback) {
        executor.execute(() -> callback.onResult(budgetDao.getBudgetByCategory(categoryId, now)));
    }

    public void getMasterBudget(long now, Callback<Budget> callback) {
        executor.execute(() -> callback.onResult(budgetDao.getMasterBudget(now)));
    }

    public void getActiveBudgetsWithSpent(long now, Callback<List<BudgetWithSpent>> callback) {
        executor.execute(() -> callback.onResult(budgetDao.getActiveBudgetsWithSpent(now)));
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}