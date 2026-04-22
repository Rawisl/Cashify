package com.example.cashify.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.cashify.data.local.AppDatabase;
import com.example.cashify.data.model.Budget;
import com.example.cashify.data.local.BudgetDao;
import com.example.cashify.data.local.BudgetWithSpent;
import com.example.cashify.data.remote.FirebaseManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BudgetRepository {

    private final BudgetDao budgetDao;
    private final FirebaseManager firebaseManager;
    private final ExecutorService executor;

    public BudgetRepository(Context context) {
        budgetDao = AppDatabase.getInstance(context).budgetDao();
        firebaseManager = FirebaseManager.getInstance();
        executor = Executors.newSingleThreadExecutor();
    }

    public void insert(Budget budget) {
        executor.execute(() -> {
            // Lưu Local và lấy ID thực tế từ Room
            long id = budgetDao.insert(budget);
            budget.id = (int) id;

            // Đẩy bản sao lên Firebase
            syncBudgetToCloud(budget);
        });
    }

    public void update(Budget budget) {
        executor.execute(() -> {
            budgetDao.update(budget);
            syncBudgetToCloud(budget);
        });
    }

    public void delete(Budget budget) {
        executor.execute(() -> {
            budgetDao.delete(budget);
            // Gửi yêu cầu xóa document trên cloud (nếu FirebaseManager hỗ trợ) Hoặc đơn giản là sync một Map rỗng/đánh dấu xóa
            Log.d("FIREBASE_SYNC", "Đã xóa ngân sách tại máy.");
        });
    }

    private void syncBudgetToCloud(Budget budget) {
        Map<String, Object> data = new HashMap<>();
        data.put("limitAmount", budget.limitAmount);
        data.put("categoryId", budget.categoryId);
        data.put("startDate", budget.startDate);
        data.put("endDate", budget.endDate);
        data.put("periodType", budget.periodType);

        firebaseManager.syncLocalToCloud("budgets", String.valueOf(budget.id), data, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d("FIREBASE_SYNC", "Đồng bộ Budget " + budget.id + " thành công!");
            }

            @Override
            public void onError(String message) {
                Log.e("FIREBASE_SYNC", "Lỗi đồng bộ Budget: " + message);
            }
        });
    }

    public void getActiveBudgets(long now, Callback<List<Budget>> callback) {
        executor.execute(() -> callback.onResult(budgetDao.getActiveBudgets(now)));
    }

    public void getBudgetByCategory(int categoryId, long startTime, long endTime, String periodType, Callback<Budget> callback) {
        executor.execute(() -> callback.onResult(budgetDao.getBudgetByCategory(categoryId, startTime, endTime, periodType)));
    }

    public void getMasterBudget(long startTime, long endTime, String periodType, Callback<Budget> callback) {
        executor.execute(() -> callback.onResult(budgetDao.getMasterBudget(startTime, endTime, periodType)));
    }

    public void getActiveBudgetsWithSpent(long startTime, long endTime, String periodType, Callback<List<BudgetWithSpent>> callback) {
        executor.execute(() -> callback.onResult(budgetDao.getActiveBudgetsWithSpent(startTime, endTime, periodType)));
    }

    public void getLinkedMonthlyCategoryBudgets(long monthStart, long monthEnd, String weekPeriod, Callback<List<BudgetWithSpent>> callback) {
        executor.execute(() -> callback.onResult(budgetDao.getLinkedMonthlyCategoryBudgets(monthStart, monthEnd, weekPeriod)));
    }

    public void getSumLinkedWeeklyCategoryLimits(long monthStart, long monthEnd, String weekPeriod, Callback<Long> callback) {
        executor.execute(() -> callback.onResult(budgetDao.getSumLinkedWeeklyCategoryLimits(monthStart, monthEnd, weekPeriod)));
    }

    public void getSumOtherWeeklyMasterLimits(long monthStart, long monthEnd, long currentWeekStart, String weekPeriod, Callback<Long> callback) {
        executor.execute(() -> callback.onResult(budgetDao.getSumOtherWeeklyMasterLimits(monthStart, monthEnd, currentWeekStart, weekPeriod)));
    }

    public void getMasterSpentAmount(long startDate, long endDate, Callback<Long> callback) {
        executor.execute(() -> callback.onResult(budgetDao.getMasterSpentAmount(startDate, endDate)));
    }

    public void getTotalCategoryLimits(String periodType, long startTime, long endTime, Callback<Long> callback) {
        executor.execute(() -> callback.onResult(budgetDao.getTotalCategoryLimits(periodType, startTime, endTime)));
    }

    public void getTotalCategoryLimitExcluding(int excludedId, String periodType, long startTime, long endTime, Callback<Long> callback) {
        executor.execute(() -> callback.onResult(budgetDao.getTotalCategoryLimitExcluding(excludedId, periodType, startTime, endTime)));
    }

    public void getUnplannedExpenses(long startDate, long endDate, String periodType, Callback<List<BudgetWithSpent>> callback) {
        executor.execute(() -> callback.onResult(budgetDao.getUnplannedExpenses(startDate, endDate, periodType)));
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}