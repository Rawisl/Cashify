package com.example.cashify.ui.category;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cashify.data.model.Category;
import com.example.cashify.data.repository.CategoryRepository;
import java.util.List;

public class CategoryViewModel extends AndroidViewModel {

    private final CategoryRepository repository;

    // Internal MutableLiveData (Writable) -> External LiveData (Read-only)
    private final MutableLiveData<List<Category>> incomeCategories = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> expenseCategories = new MutableLiveData<>();

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        repository = new CategoryRepository(application);
        // Load initial data immediately upon initialization
        refreshData();
    }

    // --- Getters for UI observation ---
    public LiveData<List<Category>> getIncomeCategories() { return incomeCategories; }
    public LiveData<List<Category>> getExpenseCategories() { return expenseCategories; }

    /**
     * Refreshes the category lists from the database.
     * postValue automatically triggers UI updates across all observing Activities/Fragments.
     */
    public void refreshData() {
        repository.getCategoriesByType(0, expenseCategories::postValue);
        repository.getCategoriesByType(1, incomeCategories::postValue);
    }

    // =========================================================================
    // CRUD OPERATIONS
    // =========================================================================

    public void insert(Category category) {
        repository.insert(category, this::refreshData);
    }

    public void update(Category category) {
        repository.update(category, this::refreshData);
    }

    /**
     * Handles category deletion. 
     * The Repository layer autonomously manages whether this is a soft or hard delete.
     */
    public void deleteCategory(int categoryId) {
        repository.deleteCategory(categoryId, this::refreshData);
    }

    public void restoreCategory(int categoryId) {
        repository.restoreCategory(categoryId, this::refreshData);
    }

    // =========================================================================
    // ONE-OFF QUERIES (Using Callbacks instead of LiveData streams)
    // =========================================================================

    public void getCategoriesByType(int type, CategoryRepository.Callback<List<Category>> callback) {
        repository.getCategoriesByType(type, callback);
    }

    public void getCategoryById(int id, CategoryRepository.Callback<Category> callback) {
        repository.getCategoryById(id, callback);
    }

    public void getAllActive(CategoryRepository.Callback<List<Category>> callback) {
        repository.getAllActive(callback);
    }
}