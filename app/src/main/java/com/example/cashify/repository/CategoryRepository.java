package com.example.cashify.repository;

import android.content.Context;
import com.example.cashify.database.AppDatabase;
import com.example.cashify.database.Category;
import com.example.cashify.database.CategoryDao;
import com.example.cashify.database.DatabaseSeeder;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoryRepository {

    private final CategoryDao categoryDao;
    private final ExecutorService executor;

    public CategoryRepository(Context context)
    {
        categoryDao = AppDatabase.getInstance(context).categoryDao();
        executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> DatabaseSeeder.seedIfEmpty(context));
    }

    public void insert(Category category,Runnable onComplete)
    {
        executor.execute(() -> {
            categoryDao.insert(category);
            // Lưu xong rồi mới gọi callback
            if (onComplete != null)
            {
                onComplete.run();
            }
        });
    }

    public void update(Category category, Runnable onComplete)
    {
        executor.execute(() -> {
            categoryDao.update(category);
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    public void deleteCategory(int categoryId, Runnable onComplete)
    {
        executor.execute(() -> {
            int count = categoryDao.countTransactionsByCategory(categoryId);
            if (count > 0) {
                categoryDao.softDelete(categoryId);
            } else {
                categoryDao.hardDelete(categoryId);
            }

            if (onComplete != null) {
                onComplete.run();
            }
        });
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

    public interface Callback<T> {
        void onResult(T result);
    }
}