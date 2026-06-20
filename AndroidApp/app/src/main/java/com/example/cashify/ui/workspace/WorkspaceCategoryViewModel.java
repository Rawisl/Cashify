package com.example.cashify.ui.workspace;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.model.Category;
import com.example.cashify.data.remote.FirebaseManager;

import java.util.List;

public class WorkspaceCategoryViewModel extends ViewModel {

    private final FirebaseManager firebaseManager = FirebaseManager.getInstance();

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsLoading() { return _isLoading; }

    public static class ActionResult {
        public boolean isSuccess;
        public String message;
        public ActionResult(boolean isSuccess, String message) {
            this.isSuccess = isSuccess;
            this.message = message;
        }
    }

    private final MutableLiveData<ActionResult> _actionResult = new MutableLiveData<>();
    public LiveData<ActionResult> getActionResult() { return _actionResult; }
    public void clearActionResult() { _actionResult.setValue(null); }

    public void saveCategory(String workspaceId, Category editCat, String name, String iconName, String colorCode, int type) {
        _isLoading.setValue(true);

        if (editCat != null && editCat.firestoreId != null) {
            // ==========================================
            // EDIT MODE
            // ==========================================
            firebaseManager.editCategory(workspaceId, editCat.firestoreId, name, iconName, colorCode, type, new FirebaseManager.DataCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    _isLoading.postValue(false);
                    _actionResult.postValue(new ActionResult(true, "Category updated successfully"));
                }

                @Override
                public void onError(String message) {
                    _isLoading.postValue(false);
                    _actionResult.postValue(new ActionResult(false, message));
                }
            });
        } else {
            // ==========================================
            // ADD NEW MODE
            // ==========================================
            firebaseManager.addCategory(workspaceId, name, iconName, colorCode, type, new FirebaseManager.DataCallback<Void>() {
                @Override
                public void onSuccess(Void data) {
                    _isLoading.postValue(false);
                    _actionResult.postValue(new ActionResult(true, "Category added successfully"));
                }

                @Override
                public void onError(String message) {
                    _isLoading.postValue(false);
                    _actionResult.postValue(new ActionResult(false, message));
                }
            });
        }
    }

    private final MutableLiveData<List<Category>> _categoriesLiveData = new MutableLiveData<>();
    public LiveData<List<Category>> getCategoriesLiveData() { return _categoriesLiveData; }

    private com.google.firebase.firestore.ListenerRegistration categoryListener;

    // Lắng nghe danh sách Real-time
    public void loadCategories(String workspaceId) {
        if (workspaceId == null) return;

        if (categoryListener != null) categoryListener.remove();

        categoryListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("workspaces").document(workspaceId).collection("categories")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        android.util.Log.e("FIRESTORE", "Listen failed.", e);
                        return;
                    }

                    List<Category> list = new java.util.ArrayList<>();
                    if (snapshots != null) {
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshots) {
                            Category cat = doc.toObject(Category.class);
                            cat.firestoreId = doc.getId();
                            list.add(cat);
                        }
                    }
                    _categoriesLiveData.postValue(list);
                });
    }

    // Xóa mềm danh mục
    public void deleteCategory(String workspaceId, String categoryId) {
        firebaseManager.deleteCategory(workspaceId, categoryId, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                _actionResult.postValue(new ActionResult(true, "Category hidden successfully"));
            }
            @Override
            public void onError(String message) {
                _actionResult.postValue(new ActionResult(false, message));
            }
        });
    }

    // Khôi phục danh mục
    public void restoreCategory(String workspaceId, String categoryId) {
        firebaseManager.restoreCategory(workspaceId, categoryId, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                _actionResult.postValue(new ActionResult(true, "Category restored successfully"));
            }
            @Override
            public void onError(String message) {
                _actionResult.postValue(new ActionResult(false, message));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (categoryListener != null) {
            categoryListener.remove();
        }
    }
}