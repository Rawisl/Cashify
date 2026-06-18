package com.example.cashify.ui.transactions;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.cashify.data.model.Category;
import com.example.cashify.data.model.Transaction;
import com.example.cashify.data.repository.AddTransactionRepository;
import com.example.cashify.data.repository.TransactionRepository;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddTransactionViewModel extends AndroidViewModel {

    private final AddTransactionRepository addRepo; // Manages Category data
    private final TransactionRepository transRepo;  // Manages Transaction data

    // Use a single thread executor to safely offload DB operations without Memory Leaks
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    private String currentWorkspaceId = "PERSONAL";

    // =========================================================================
    // UI STATES
    // =========================================================================
    public final MutableLiveData<Boolean> isExpense = new MutableLiveData<>(true);
    public final MutableLiveData<Boolean> isEditMode = new MutableLiveData<>(false);
    public final MutableLiveData<Calendar> calendar = new MutableLiveData<>(Calendar.getInstance());
    public final MutableLiveData<String> selectedPayment = new MutableLiveData<>("Cash");

    // =========================================================================
    // DATA STATES
    // =========================================================================
    public final MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    public final MutableLiveData<Category> selectedCategory = new MutableLiveData<>();
    public final MutableLiveData<Transaction> existingTransaction = new MutableLiveData<>();
    public final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>(false);

    public AddTransactionViewModel(@NonNull Application application) {
        super(application);
        this.addRepo = new AddTransactionRepository(application);
        this.transRepo = new TransactionRepository(application);
    }

    // Assign workspace context from the host Activity
    public void setCurrentWorkspaceId(String workspaceId) {
        if (workspaceId != null && !workspaceId.isEmpty()) {
            this.currentWorkspaceId = workspaceId;
        }
    }

    // =========================================================================
    // EDIT MODE LOGIC
    // =========================================================================
    public void loadTransactionForEdit(String transactionId) {
        isEditMode.setValue(true);
        transRepo.getById(transactionId, transaction -> {
            if (transaction != null) {
                existingTransaction.postValue(transaction);

                isExpense.postValue(transaction.type == 0);

                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(transaction.timestamp);
                calendar.postValue(cal);

                // Fetch relevant categories once the transaction type is determined
                loadCategories(transaction.type);
            }
        });
    }

    // =========================================================================
    // CATEGORY MANAGEMENT
    // =========================================================================
    public void setType(boolean expense) {
        isExpense.setValue(expense);
    }

    public void loadCategories(int type) {
        addRepo.getCategoriesByType(type, categories::postValue);
    }

    // =========================================================================
    // PERSISTENCE (SAVE / UPDATE / DELETE)
    // =========================================================================
    public void saveOrUpdate(String amountStr, String note) {
        if (amountStr == null || amountStr.isEmpty()) return;

        Category selected = selectedCategory.getValue();
        Transaction existing = existingTransaction.getValue();
        boolean editMode = Boolean.TRUE.equals(isEditMode.getValue());

        int finalCategoryId;
        if (selected != null) {
            finalCategoryId = selected.id;
        } else if (editMode && existing != null) {
            finalCategoryId = existing.categoryId;
        } else {
            return; // Abort if no category context is available
        }

        Transaction t = (editMode && existing != null) ? existing : new Transaction();

        // Sanitize string to extract numeric amount
        t.amount = Long.parseLong(amountStr.replaceAll("[^\\d]", ""));
        t.note = note;
        t.categoryId = finalCategoryId;
        t.timestamp = calendar.getValue() != null ? calendar.getValue().getTimeInMillis() : System.currentTimeMillis();
        t.type = Boolean.TRUE.equals(isExpense.getValue()) ? 0 : 1;
        t.paymentMethod = selectedPayment.getValue();

        // Enforce fallback to prevent detached transactions
        t.workspaceId = (currentWorkspaceId != null) ? currentWorkspaceId : "PERSONAL";

        // Offload DB execution to avoid blocking the main thread
        databaseExecutor.execute(() -> {
            if (editMode) transRepo.update(t);
            else transRepo.insert(t);

            saveSuccess.postValue(true);
        });
    }

    public void deleteCurrentTransaction() {
        Transaction current = existingTransaction.getValue();
        if (current != null) {
            databaseExecutor.execute(() -> {
                transRepo.delete(current);
                saveSuccess.postValue(true);
            });
        }
    }

    // =========================================================================
    // HELPER SETTERS
    // =========================================================================
    public void setPayment(String method) {
        selectedPayment.setValue(method);
    }

    public void setDate(int y, int m, int d) {
        Calendar cal = calendar.getValue();
        if (cal != null) {
            cal.set(y, m, d);
            calendar.setValue(cal);
        }
    }

    /**
     * Helper to fetch personal transaction count, used for evaluating achievements.
     */
    public void getPersonalTransactionCount(TransactionRepository.Callback<Integer> callback) {
        transRepo.getAll("PERSONAL", result -> {
            int count = (result != null) ? result.size() : 0;
            if (callback != null) callback.onResult(count);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Prevent memory leaks when ViewModel dies
        databaseExecutor.shutdown();
    }
}