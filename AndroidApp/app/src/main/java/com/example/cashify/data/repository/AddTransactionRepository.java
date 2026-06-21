package com.example.cashify.data.repository;

import android.content.Context;
import com.example.cashify.data.local.AppDatabase;
import com.example.cashify.data.model.Category;
import com.example.cashify.data.local.CategoryDao;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Repository trung gian cung cấp dữ liệu danh mục cho màn hình Thêm Giao dịch
public class AddTransactionRepository {
    private final CategoryDao categoryDao;
    private final ExecutorService executor;

    public AddTransactionRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.categoryDao = db.categoryDao();
        // Cấp phát 1 luồng chạy ngầm riêng biệt để tránh kẹt Main Thread khi query DB
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void getCategoriesByType(int type, Callback<List<Category>> callback) {
        executor.execute(() -> {
            List<Category> categories = categoryDao.getCategoriesByType(type);
            if (callback != null) {
                callback.onResult(categories);
            }
        });
    }

    public void getAllCategories(Callback<List<Category>> callback) {
        executor.execute(() -> {
            List<Category> categories = categoryDao.getAllActive();
            if (callback != null) {
                callback.onResult(categories);
            }
        });
    }

    // Interface hứng kết quả trả về cho ViewModel
    public interface Callback<T> {
        void onResult(T result);
    }
}