package com.example.cashify.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.cashify.data.local.AppDatabase;
import com.example.cashify.data.model.Category;
import com.example.cashify.data.local.CategoryDao;
//import com.example.cashify.data.local.DatabaseSeeder;
import com.example.cashify.data.remote.FirebaseManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoryRepository {

    private final CategoryDao categoryDao;
    private final FirebaseManager firebaseManager;
    private final ExecutorService executor;

    public CategoryRepository(Context context)
    {
        categoryDao = AppDatabase.getInstance(context).categoryDao();
        executor = Executors.newSingleThreadExecutor();
        firebaseManager = FirebaseManager.getInstance();
//        executor.execute(() -> DatabaseSeeder.seedIfEmpty(context));
    }

    public void insert(Category category, Runnable onComplete) {
        executor.execute(() -> {
            // Lưu Local lấy ID
            long id = categoryDao.insert(category);
            category.id = (int) id;

            // Chỉ đồng bộ nếu là danh mục User tự tạo
            if (category.isDefault == 0) {
                syncCategoryToCloud(category);
            }

            if (onComplete != null) onComplete.run();
        });
    }

    public void update(Category category, Runnable onComplete) {
        executor.execute(() -> {
            // Cập nhật Local
            categoryDao.update(category);

            // Cập nhật Cloud (Nếu không phải mặc định)
            if (category.isDefault == 0) {
                syncCategoryToCloud(category);
            }

            if (onComplete != null) onComplete.run();
        });
    }

    private void syncCategoryToCloud(Category category) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", category.name);
        data.put("iconName", category.iconName);
        data.put("colorCode", category.colorCode);
        data.put("type", category.type);
        data.put("isDefault", 0);
        data.put("isDeleted", category.isDeleted);

        String currentWorkspaceId = (category.workspaceId != null) ? category.workspaceId : "PERSONAL";
        data.put("workspaceId", currentWorkspaceId);

        firebaseManager.syncLocalToCloud(currentWorkspaceId, "categories", String.valueOf(category.id), data, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d("FIREBASE_SYNC", "Synchronous success: " + category.name);
            }

            @Override
            public void onError(String message) {
                Log.e("FIREBASE_SYNC", "Synchronous failed: " + message);
            }
        });
    }

    public void deleteCategory(int categoryId, Runnable onComplete) {
        executor.execute(() -> {
            // Trước khi xóa, kiểm tra xem có phải danh mục mặc định không
            Category category = categoryDao.getCategoryById(categoryId);
            if (category != null) {
                int count = categoryDao.countTransactionsByCategory(categoryId);
                if (count > 0) {
                    categoryDao.softDelete(categoryId);
                    // Đồng bộ trạng thái đã xóa (isDeleted = 1) lên Firebase
                    if (category.isDefault == 0) {
                        category.isDeleted = 1;
                        syncCategoryToCloud(category);
                    }
                } else {
                    categoryDao.hardDelete(categoryId);
                    // Nếu muốn xóa hẳn document trên Firebase, cần viết thêm hàm deleteDocument trong FirebaseManager
                }
            }

            if (onComplete != null) onComplete.run();
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