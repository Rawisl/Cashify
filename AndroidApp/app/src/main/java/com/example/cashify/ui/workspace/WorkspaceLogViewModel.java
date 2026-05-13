package com.example.cashify.ui.workspace;


import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.cashify.data.model.LogItem;
import com.example.cashify.data.repository.WorkspaceLogRepository;

import java.util.List;

/**
 * WorkspaceLogViewModel.java
 * Trung gian giữa Fragment (View) và Repository (Data).
 *
 * Trách nhiệm:
 *  - Giữ reference đến Repository
 *  - Expose LiveData để Fragment observe
 *  - Bắt đầu / dừng listener theo vòng đời ViewModel
 *  - Không biết gì về Android View (không import Context/Fragment)
 *
 * Dùng Factory vì ViewModel cần tham số (workspaceId).
 */
public class WorkspaceLogViewModel extends ViewModel {

    private final WorkspaceLogRepository repository;

    private WorkspaceLogViewModel(String workspaceId) {
        repository = new WorkspaceLogRepository(workspaceId);
        // Bắt đầu lắng nghe ngay khi ViewModel được tạo
        repository.startListening();
    }

    // ── LiveData expose ra Fragment ───────────────────────────────────────────
    public LiveData<List<LogItem>> getLogs()  { return repository.getLogs(); }
    public LiveData<String>        getError() { return repository.getError(); }

    // ── Khi ViewModel bị hủy (Fragment bị destroy hoàn toàn) ─────────────────
    @Override
    protected void onCleared() {
        super.onCleared();
        repository.stopListening(); // dọn dẹp Firestore listener, tránh memory leak
    }

    // ── Factory để truyền workspaceId vào constructor ─────────────────────────
    public static class Factory implements ViewModelProvider.Factory {
        private final String workspaceId;

        public Factory(String workspaceId) {
            this.workspaceId = workspaceId;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(WorkspaceLogViewModel.class)) {
                return (T) new WorkspaceLogViewModel(workspaceId);
            }
            throw new IllegalArgumentException("Unknown ViewModel: " + modelClass.getName());
        }
    }
}
