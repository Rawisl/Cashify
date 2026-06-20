package com.example.cashify.ui.workspace;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.cashify.data.model.LogItem;
import com.example.cashify.data.repository.WorkspaceLogRepository;

import java.util.List;

/**
 * Acts as the Mediator between the View (Fragment) and the Data (Repository).
 *
 * Responsibilities:
 * - Holds a reference to the Repository.
 * - Exposes LiveData for the Fragment to observe.
 * - Manages the Firestore listener lifecycle (start/stop).
 * - Remains unaware of Android Views (No Context/Fragment imports).
 */
public class WorkspaceLogViewModel extends ViewModel {

    private final WorkspaceLogRepository repository;

    private final String workspaceId;

    private WorkspaceLogViewModel(String workspaceId) {
        this.workspaceId = workspaceId;
        this.repository = new WorkspaceLogRepository(workspaceId);

        // Start listening to Firestore real-time updates upon initialization
        repository.startListening();
    }

    // ── Exposed LiveData ──────────────────────────────────────────────────────
    public LiveData<List<LogItem>> getLogs()  { return repository.getLogs(); }
    public LiveData<String>        getError() { return repository.getError(); }

    // ── Lifecycle Cleanup ─────────────────────────────────────────────────────
    @Override
    protected void onCleared() {
        super.onCleared();
        // Stop the Firestore listener to prevent memory leaks when ViewModel is destroyed
        repository.stopListening();
    }

    // ── Factory implementation for parameterized ViewModel ────────────────────
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