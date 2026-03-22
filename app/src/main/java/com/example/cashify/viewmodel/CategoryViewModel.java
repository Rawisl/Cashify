package com.example.cashify.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import com.example.cashify.database.Category;
import com.example.cashify.repository.CategoryRepository;
import java.util.List;

public class CategoryViewModel extends AndroidViewModel {

    private final CategoryRepository repository;

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        repository = new CategoryRepository(application);
    }

    public void insert(Category category) {
        repository.insert(category);
    }

    public void update(Category category) {
        repository.update(category);
    }

    public void getCategoriesByType(int type, CategoryRepository.Callback<List<Category>> callback) {
        repository.getCategoriesByType(type, callback);
    }

    public void getCategoryById(int id, CategoryRepository.Callback<Category> callback) {
        repository.getCategoryById(id, callback);
    }

    public void getAllActive(CategoryRepository.Callback<List<Category>> callback) {
        repository.getAllActive(callback);
    }

    // Gọi cái này khi user bấm xóa danh mục, tự xử lý soft/hard delete
    public void deleteCategory(int categoryId) {
        repository.deleteCategory(categoryId);
    }
}