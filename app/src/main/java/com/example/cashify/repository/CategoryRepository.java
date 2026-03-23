package com.example.cashify.repository;

import android.content.Context;
import com.example.cashify.database.AppDatabase;
import com.example.cashify.database.Category;
import com.example.cashify.database.CategoryDao;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoryRepository {

    private final CategoryDao categoryDao;
    private final ExecutorService executor;

    public CategoryRepository(Context context) {
        categoryDao = AppDatabase.getInstance(context).categoryDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public void insert(Category category) {
        executor.execute(() -> categoryDao.insert(category));
    }

    public void update(Category category) {
        executor.execute(() -> categoryDao.update(category));
    }

    public void getCategoriesByType(int type, Callback<List<Category>> callback) {
        executor.execute(() -> callback.onResult(categoryDao.getCategoriesByType(type)));
    }

    public void getCategoryById(int id, Callback<Category> callback) {
        executor.execute(() -> callback.onResult(categoryDao.getCategoryById(id)));
    }

    public void getAllActive(Callback<List<Category>> callback) {
        executor.execute(() -> callback.onResult(categoryDao.getAllActive()));
    }

    // Tự động chọn soft hay hard delete
    public void deleteCategory(int categoryId) {
        executor.execute(() -> {
            int count = categoryDao.countTransactionsByCategory(categoryId);
            if (count > 0) {
                categoryDao.softDelete(categoryId); // Có giao dịch → xóa mềm
            } else {
                categoryDao.hardDelete(categoryId); // Chưa có giao dịch → xóa hẳn
            }
        });
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}