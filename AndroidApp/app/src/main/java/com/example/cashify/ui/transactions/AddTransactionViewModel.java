package com.example.cashify.ui.transactions;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.cashify.data.model.Category;
import com.example.cashify.data.model.Transaction;
import com.example.cashify.data.remote.ApiDto;
import com.example.cashify.data.repository.AddTransactionRepository;
import com.example.cashify.data.repository.TransactionRepository;
import com.example.cashify.utils.ApiClient;
import com.example.cashify.utils.ApiService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    // =========================================================================
    // CÁC LIVEDATA MỚI ĐỂ GIAO TIẾP VỚI ACTIVITY (Thêm vào phần khai báo)
    // =========================================================================
    public final MutableLiveData<String> achievementEvent = new MutableLiveData<>();
    public final MutableLiveData<Boolean> closeScreenEvent = new MutableLiveData<>();
    public final MutableLiveData<String> errorEvent = new MutableLiveData<>();

    private ListenerRegistration categorySnapshotListener;

    // =========================================================================
    // 1. LOGIC TẢI DANH MỤC (QUỸ CHUNG VÀ CÁ NHÂN)
    // =========================================================================
    public void fetchCategories(String workspaceId, int type) {
        if (workspaceId != null && !workspaceId.trim().isEmpty() && !workspaceId.equals("PERSONAL")) {
            if (categorySnapshotListener != null) categorySnapshotListener.remove();

            categorySnapshotListener = FirebaseFirestore.getInstance()
                    .collection("workspaces").document(workspaceId)
                    .collection("categories").whereEqualTo("type", type)
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null || snapshots == null) return;
                        List<Category> list = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Category c = doc.toObject(Category.class);
                            c.firestoreId = doc.getId();
                            if (c.isDeleted != 1) list.add(c);
                        }
                        categories.postValue(list);

                        // Fallback: Chọn lại danh mục nếu cái cũ bị ẩn/xóa
                        Category currentlySelected = selectedCategory.getValue();
                        if (currentlySelected != null && currentlySelected.firestoreId != null) {
                            boolean isStillValid = list.stream().anyMatch(cat -> currentlySelected.firestoreId.equals(cat.firestoreId));
                            if (!isStillValid) selectedCategory.postValue(list.isEmpty() ? null : list.get(0));
                        }
                    });
        } else {
            loadCategories(type); // Gọi hàm tải Room DB cũ
        }
    }

    // =========================================================================
    // 2. LOGIC TẢI GIAO DỊCH QUỸ CHUNG (EDIT)
    // =========================================================================
    public void loadWorkspaceTransactionForEdit(String workspaceId, String transactionId) {
        FirebaseFirestore.getInstance()
                .collection("workspaces").document(workspaceId)
                .collection("transactions").document(transactionId)
                .get()
                .addOnSuccessListener(doc -> {
                    Transaction t = doc.toObject(Transaction.class);
                    if (t != null) {
                        existingTransaction.postValue(t);
                        isExpense.postValue(t.type == 0);
                        fetchCategories(workspaceId, t.type);
                    }
                });
    }

    // =========================================================================
    // 3. LOGIC LƯU GIAO DỊCH QUỸ CHUNG (GỌI API)
    // =========================================================================
    public void saveWorkspaceTransaction(String workspaceId, double amount, String note, boolean isEditMode, String editId) {
        Category selectedCat = selectedCategory.getValue();

        ApiDto.TransactionRequest req = new ApiDto.TransactionRequest();
        req.Id = isEditMode ? editId : null;
        req.Amount = (long) amount;
        req.Note = note;
        req.Timestamp = calendar.getValue().getTimeInMillis();
        req.PaymentMethod = selectedPayment.getValue();
        req.Type = Boolean.TRUE.equals(isExpense.getValue()) ? 0 : 1;
        req.WorkspaceId = workspaceId;
        req.CategoryId = 0;

        if (selectedCat != null) {
            req.FirestoreCategoryId = selectedCat.firestoreId;
        } else if (isEditMode && existingTransaction.getValue() != null) {
            req.FirestoreCategoryId = existingTransaction.getValue().firestoreCategoryId;
        }

        FirebaseAuth.getInstance().getCurrentUser().getIdToken(true).addOnSuccessListener(tokenResult -> {
            String token = "Bearer " + tokenResult.getToken();
            ApiClient.getClient().create(ApiService.class).addWorkspaceTransaction(token, req).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.isSuccessful()) {
                        if (!isEditMode && response.body() instanceof Map) {
                            Map<String, Object> map = (Map<String, Object>) response.body();
                            if (Boolean.TRUE.equals(map.get("isNewCarry"))) achievementEvent.postValue("🦸‍♂️ New Carry (MVP)!");
                            else if (Boolean.TRUE.equals(map.get("isNewSpender"))) achievementEvent.postValue("🛍️ New Biggest Spender!");
                        }
                        closeScreenEvent.postValue(true);
                    } else {
                        errorEvent.postValue("Server rejected: " + response.code());
                    }
                }
                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    errorEvent.postValue("Network Error: " + t.getMessage());
                }
            });
        }).addOnFailureListener(e -> errorEvent.postValue("Auth Error: " + e.getMessage()));
    }

    // =========================================================================
    // 4. LOGIC XÓA GIAO DỊCH QUỸ CHUNG (GỌI API)
    // =========================================================================
    public void deleteWorkspaceTransaction(String workspaceId, String editId) {
        FirebaseAuth.getInstance().getCurrentUser().getIdToken(true).addOnSuccessListener(tokenResult -> {
            String token = "Bearer " + tokenResult.getToken();
            ApiDto.WorkspaceActionRequest req = new ApiDto.WorkspaceActionRequest();
            req.WorkspaceId = workspaceId;
            req.TransactionId = editId;

            ApiClient.getClient().create(ApiService.class).deleteWorkspaceTransaction(token, req).enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.isSuccessful()) {
                        achievementEvent.postValue("Transaction deleted!");
                        closeScreenEvent.postValue(true);
                    } else errorEvent.postValue("Server rejected: " + response.code());
                }
                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    errorEvent.postValue("Network Error: " + t.getMessage());
                }
            });
        });
    }

    // =========================================================================
    // 5. LOGIC GAMIFICATION CÁ NHÂN (XỬ LÝ DỮ LIỆU ĐỊA PHƯƠNG)
    // =========================================================================
    public void triggerPersonalGamification(double amount) {
        Calendar cal = calendar.getValue();
        int hour = (cal != null) ? cal.get(Calendar.HOUR_OF_DAY) : Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        boolean isExp = Boolean.TRUE.equals(isExpense.getValue());

        boolean isNightOwl = (hour >= 0 && hour < 4);
        boolean isBigSpender = (isExp && amount >= 10000000);

        android.content.SharedPreferences prefs = getApplication().getSharedPreferences("GamificationPrefs", Context.MODE_PRIVATE);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayStr = sdf.format(new Date());
        String lastDateStr = prefs.getString("last_transaction_date", "");
        int currentStreak = prefs.getInt("current_streak", 0);
        boolean isStreakJustUpdated = false;

        if (!todayStr.equals(lastDateStr)) {
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DAY_OF_YEAR, -1);
            if (lastDateStr.equals(sdf.format(yesterday.getTime()))) currentStreak++;
            else currentStreak = 1;

            isStreakJustUpdated = true;
            prefs.edit().putString("last_transaction_date", todayStr).putInt("current_streak", currentStreak).apply();
        }

        final int streak = currentStreak;
        final boolean streakUpdated = isStreakJustUpdated;
        boolean achievedBigWhale = prefs.getBoolean("achieved_big_whale", false);
        boolean achievedNightOwl = prefs.getBoolean("achieved_night_owl", false);

        getPersonalTransactionCount(count -> {
            if (streakUpdated && (streak == 3 || streak == 7 || streak == 15 || streak == 30 || streak == 100)) {
                achievementEvent.postValue("🔥 " + streak + "-Day Streak!");
            } else if (count == 10 || count == 50 || count == 100 || count == 500) {
                achievementEvent.postValue("🐝 Hardworking Bee (" + count + ")");
            } else if (isBigSpender && !achievedBigWhale) {
                prefs.edit().putBoolean("achieved_big_whale", true).apply();
                achievementEvent.postValue("🐋 Big Whale");
            } else if (isNightOwl && !achievedNightOwl) {
                prefs.edit().putBoolean("achieved_night_owl", true).apply();
                achievementEvent.postValue("🦉 Night Owl");
            }
            closeScreenEvent.postValue(true);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Prevent memory leaks when ViewModel dies
        if (categorySnapshotListener != null) categorySnapshotListener.remove();
        databaseExecutor.shutdown();
    }
}