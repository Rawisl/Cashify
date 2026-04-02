package com.example.cashify.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import com.example.cashify.database.Budget;
import com.example.cashify.database.BudgetWithSpent;
import com.example.cashify.repository.BudgetRepository;
import java.util.List;

public class BudgetViewModel extends AndroidViewModel {

    private final BudgetRepository repository;

    public BudgetViewModel(@NonNull Application application) {
        super(application);
        repository = new BudgetRepository(application);
    }

    public void insert(Budget budget) {
        repository.insert(budget);
    }

    public void update(Budget budget) {
        repository.update(budget);
    }

    public void delete(Budget budget) {
        repository.delete(budget);
    }

    public void getActiveBudgets(long now, BudgetRepository.Callback<List<Budget>> callback) {
        repository.getActiveBudgets(now, callback);
    }

    public void getBudgetByCategory(int categoryId, long now, String periodType, BudgetRepository.Callback<Budget> callback) {
        repository.getBudgetByCategory(categoryId, now, periodType, callback);
    }

    public void getMasterBudget(long now, String periodType, BudgetRepository.Callback<Budget> callback) {
        repository.getMasterBudget(now, periodType, callback);
    }

    public void getActiveBudgetsWithSpent(long now, String periodType, BudgetRepository.Callback<List<BudgetWithSpent>> callback) {
        repository.getActiveBudgetsWithSpent(now, periodType, callback);
    }
}