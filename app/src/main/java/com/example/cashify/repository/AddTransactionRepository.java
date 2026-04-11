package com.example.cashify.repository;

import android.content.Context;
import com.example.cashify.database.AppDatabase;
import com.example.cashify.database.Category;
import com.example.cashify.database.CategoryDao;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddTransactionRepository {
    private final CategoryDao categoryDao;
    private final ExecutorService executor;

    public AddTransactionRepository(Context context) {
        // Khởi tạo Database và Dao
        AppDatabase db = AppDatabase.getInstance(context);
        this.categoryDao = db.categoryDao();
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Lấy danh sách danh mục dựa trên loại giao dịch (0 = Chi, 1 = Thu).
     * Được sử dụng để lọc danh mục trong màn hình Category Selection.
     */
    public void getCategoriesByType(int type, Callback<List<Category>> callback) {
        executor.execute(() -> {
            List<Category> categories = categoryDao.getCategoriesByType(type);
            if (callback != null) {
                callback.onResult(categories);
            }
        });
    }

    /**
     * Lấy toàn bộ danh mục (dùng khi không cần lọc).
     */
    public void getAllCategories(Callback<List<Category>> callback) {
        executor.execute(() -> {
            List<Category> categories = categoryDao.getAllActive();
            if (callback != null) {
                callback.onResult(categories);
            }
        });
    }

    /**
     * Interface để trả kết quả về cho ViewModel trên Main Thread (nếu dùng LiveData postValue).
     */
    public interface Callback<T> {
        void onResult(T result);
    }
}