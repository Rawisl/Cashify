package com.example.cashify.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.cashify.data.local.AppDatabase;
import com.example.cashify.data.model.Category;
import com.example.cashify.data.local.CategoryDao;
import com.example.cashify.data.remote.FirebaseManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Xử lý logic nghiệp vụ và đồng bộ dữ liệu cho Danh mục (Categories)
public class CategoryRepository {

    private final CategoryDao categoryDao;
    private final FirebaseManager firebaseManager;
    private final ExecutorService executor;

    public CategoryRepository(Context context) {
        categoryDao = AppDatabase.getInstance(context).categoryDao();
        executor = Executors.newSingleThreadExecutor();
        firebaseManager = FirebaseManager.getInstance();
    }

    public void insert(Category category, Runnable onComplete) {
        executor.execute(() -> {
            // Lưu xuống Local trước để lấy ID tự sinh từ SQLite
            long id = categoryDao.insert(category);
            category.id = (int) id;

            // Chỉ đồng bộ lên Cloud những danh mục do User tự tạo (isDefault == 0)
            if (category.isDefault == 0) {
                syncCategoryToCloud(category);
            }

            if (onComplete != null) onComplete.run();
        });
    }

    public void update(Category category, Runnable onComplete) {
        executor.execute(() -> {
            categoryDao.update(category);

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
                Log.d("FIREBASE_SYNC", "Category synced successfully: " + category.name);
            }

            @Override
            public void onError(String message) {
                Log.e("FIREBASE_SYNC", "Category sync failed: " + message);
            }
        });
    }

    public void deleteCategory(int categoryId, Runnable onComplete) {
        executor.execute(() -> {
            Category category = categoryDao.getCategoryById(categoryId);
            if (category != null) {
                int count = categoryDao.countTransactionsByCategory(categoryId);
                String currentWorkspaceId = (category.workspaceId != null) ? category.workspaceId : "PERSONAL";

                if (count > 0) {
                    // Logic Xóa mềm (Soft Delete): Ẩn danh mục nếu đã có giao dịch phát sinh để bảo toàn lịch sử
                    categoryDao.softDelete(categoryId);
                    if (category.isDefault == 0) {
                        category.isDeleted = 1;
                        syncCategoryToCloud(category);
                    }
                } else {
                    // Logic Xóa cứng (Hard Delete): Xóa vĩnh viễn nếu danh mục chưa từng được sử dụng
                    categoryDao.hardDelete(categoryId);
                    if (category.isDefault == 0) {
                        firebaseManager.deleteDocumentFromCloud(currentWorkspaceId, "categories", String.valueOf(category.id), new FirebaseManager.DataCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Log.d("FIREBASE_SYNC", "Hard delete success on Cloud!");
                            }
                            @Override
                            public void onError(String message) {
                                Log.e("FIREBASE_SYNC", "Hard delete failed on Cloud: " + message);
                            }
                        });
                    }
                }
            }

            if (onComplete != null) onComplete.run();
        });
    }

    public void restoreCategory(int categoryId, Runnable onComplete) {
        executor.execute(() -> {
            Category category = categoryDao.getCategoryById(categoryId);

            // Phục hồi trạng thái hoạt động cho danh mục đã bị xóa mềm
            if (category != null && category.isDeleted == 1) {
                category.isDeleted = 0;
                categoryDao.update(category);

                if (category.isDefault == 0) {
                    syncCategoryToCloud(category);
                }
            }

            if (onComplete != null) onComplete.run();
        });
    }

    // --- CÁC HÀM GET DỮ LIỆU ĐỌC TRỰC TIẾP TỪ LOCAL ---

    public void getCategoriesByType(int type, Callback<List<Category>> callback) {
        executor.execute(() -> callback.onResult(categoryDao.getCategoriesByType(type)));
    }

    public void getCategoryById(int id, Callback<Category> callback) {
        executor.execute(() -> callback.onResult(categoryDao.getCategoryById(id)));
    }

    public void getAllActive(Callback<List<Category>> callback) {
        executor.execute(() -> callback.onResult(categoryDao.getAllActive()));
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}