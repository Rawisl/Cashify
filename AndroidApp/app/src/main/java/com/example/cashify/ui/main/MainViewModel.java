package com.example.cashify.ui.main;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cashify.data.local.AppDatabase;
import com.example.cashify.data.model.Category;
import com.example.cashify.data.model.Budget;
import com.example.cashify.data.model.Transaction;
import com.example.cashify.data.model.User;
import com.example.cashify.data.model.Workspace;
import com.example.cashify.data.model.WorkspaceInvitation;
import com.example.cashify.data.remote.FirebaseManager;
import com.example.cashify.data.repository.IWorkspaceRepo;
import com.example.cashify.data.repository.RemoteWorkspaceRepoImpl;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class MainViewModel extends AndroidViewModel {

    private final IWorkspaceRepo workspaceRepo;
    private final FirebaseManager firebaseManager;

    // --- State & Data LiveData ---
    private final MutableLiveData<List<Workspace>> _workspaces = new MutableLiveData<>();
    public LiveData<List<Workspace>> workspaces = _workspaces;

    private final MutableLiveData<Workspace> _currentWorkspace = new MutableLiveData<>();
    public LiveData<Workspace> currentWorkspace = _currentWorkspace;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    public final MutableLiveData<Boolean> syncCompleted = new MutableLiveData<>(false);

    private final MutableLiveData<User> _userProfile = new MutableLiveData<>();
    public LiveData<User> getUserProfile() { return _userProfile; }

    private final MutableLiveData<Integer> _invitationCount = new MutableLiveData<>();
    public LiveData<Integer> getInvitationCount() { return _invitationCount; }

    private final MutableLiveData<Integer> _unreadNotificationCount = new MutableLiveData<>(0);
    public LiveData<Integer> getUnreadNotificationCount() { return _unreadNotificationCount; }

    // --- Listeners for cleanup ---
    private ListenerRegistration userProfileListener;
    private ListenerRegistration transListener;
    private ListenerRegistration catListener;
    private ListenerRegistration budgetListener;

    public MainViewModel(@NonNull Application application) {
        super(application);
        this.firebaseManager = FirebaseManager.getInstance();
        this.workspaceRepo = new RemoteWorkspaceRepoImpl();

        // Tự động load số Notification ngay khi khởi tạo
        loadUnreadNotifications();
        //load user profile func
        loadUserProfile();
        loadInvitationsCount();

        String currentUid = firebaseManager.getCurrentUserId();
        if (currentUid != null) {
            loadWorkspaces(currentUid);
        }
    }

    // ============================================================
    // BỔ SUNG HÀM LOAD NOTIFICATION ĐỂ GẮN VÀO TOOLBAR
    // ============================================================
    public void loadUnreadNotifications() {
        firebaseManager.listenToUnreadNotifications(new FirebaseManager.DataCallback<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                _unreadNotificationCount.postValue(count);
            }

            @Override
            public void onError(String message) {
                Log.e("MainViewModel", "Failed to load notifications: " + message);
            }
        });
    }

    public void loadUserProfile() {
        String uid = FirebaseManager.getInstance().getCurrentUserId();
        if (uid == null) return;

        if (userProfileListener != null) userProfileListener.remove();

        userProfileListener = FirebaseFirestore.getInstance().collection("users").document(uid)
                .addSnapshotListener((doc, e) -> {
                    if (e == null && doc != null && doc.exists()) {
                        _userProfile.setValue(doc.toObject(User.class));
                    }
                });
    }

    public void loadInvitationsCount() {
        FirebaseManager.getInstance().listenToWorkspaceInvitations(new FirebaseManager.DataCallback<List<WorkspaceInvitation>>() {
            @Override
            public void onSuccess(List<WorkspaceInvitation> data) {
                _invitationCount.setValue((data != null) ? data.size() : 0);
            }
            @Override
            public void onError(String message) {
                _invitationCount.setValue(0);
            }
        });
    }

    public LiveData<List<Workspace>> getWorkspaces() {
        return workspaces;
    }

    public void loadWorkspaces(String userId) {
        _isLoading.setValue(true);
        workspaceRepo.getWorkspaces(userId, new IWorkspaceRepo.OnWorkspacesLoadedListener() {
            @Override
            public void onSuccess(List<Workspace> list) {
                _isLoading.postValue(false);
                _workspaces.postValue(list);
            }

            @Override
            public void onError(Exception e) {
                _isLoading.postValue(false);
                Log.e("MainViewModel", "Unable to load workspace list: " + e.getMessage());
            }
        });
    }

    public void selectWorkspace(Workspace workspace) {
        if (workspace != null) {
            _currentWorkspace.setValue(workspace);
        }
    }

    // ============================================================
    // DATA SYNC & OFFLINE CACHING
    // ============================================================

    public void syncAllDataFromServer() {
        _isLoading.postValue(true);

        firebaseManager.getAllCategoriesFromCloud(new FirebaseManager.DataCallback<List<DocumentSnapshot>>() {
            @Override
            public void onSuccess(List<DocumentSnapshot> catDocs) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        AppDatabase db = AppDatabase.getInstance(getApplication());
                        List<Category> localCats = db.categoryDao().getAll();

                        for (DocumentSnapshot doc : catDocs) {
                            Category c = new Category();
                            c.firestoreId = doc.getId();
                            c.name = doc.getString("name");
                            c.iconName = doc.getString("iconName");
                            c.colorCode = doc.getString("colorCode");

                            Long type = doc.getLong("type");
                            c.type = (type != null) ? type.intValue() : 0;
                            c.workspaceId = "PERSONAL";

                            Long isDef = doc.getLong("isDefault");
                            c.isDefault = (isDef != null) ? isDef.intValue() : 0;

                            Long isDel = doc.getLong("isDeleted");
                            c.isDeleted = (isDel != null) ? isDel.intValue() : 0;

                            boolean existsLocally = false;
                            for (Category local : localCats) {
                                boolean matchId = (local.firestoreId != null && local.firestoreId.equals(c.firestoreId));
                                boolean matchName = (local.name != null && local.name.equalsIgnoreCase(c.name) && local.type == c.type);

                                if (matchId || matchName) {
                                    c.id = local.id;
                                    existsLocally = true;
                                    break;
                                }
                            }

                            if (existsLocally) {
                                db.categoryDao().update(c);
                            } else {
                                db.categoryDao().insert(c);
                            }
                        }
                        Log.d("SYNC", "Downloaded " + catDocs.size() + " categories");

                        fetchBudgets();
                        fetchTransactions();

                    } catch (Exception e) {
                        Log.e("SYNC", "Category sync error: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(String message) {
                _isLoading.postValue(false);
            }
        });
    }

    private void fetchBudgets() {
        firebaseManager.getAllBudgetsFromCloud(new FirebaseManager.DataCallback<List<DocumentSnapshot>>() {
            @Override
            public void onSuccess(List<DocumentSnapshot> documents) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        AppDatabase db = AppDatabase.getInstance(getApplication());
                        for (DocumentSnapshot doc : documents) {
                            Budget b = new Budget();
                            b.id = Integer.parseInt(doc.getId());
                            b.limitAmount = doc.getLong("limitAmount") != null ? doc.getLong("limitAmount") : 0L;
                            b.categoryId = doc.getLong("categoryId") != null ? doc.getLong("categoryId").intValue() : -1;
                            b.startDate = doc.getLong("startDate") != null ? doc.getLong("startDate") : 0L;
                            b.endDate = doc.getLong("endDate") != null ? doc.getLong("endDate") : 0L;
                            b.periodType = doc.getString("periodType");
                            db.budgetDao().insert(b);
                        }
                        Log.d("SYNC", "Downloaded " + documents.size() + " budgets.");
                    } catch (Exception e) {
                        Log.e("SYNC", "Budget sync error: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(String m) {}
        });
    }

    private void fetchTransactions() {
        firebaseManager.getAllTransactionsFromCloud("PERSONAL", new FirebaseManager.DataCallback<List<DocumentSnapshot>>() {
            @Override
            public void onSuccess(List<DocumentSnapshot> documents) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        AppDatabase db = AppDatabase.getInstance(getApplication());
                        List<Category> localCats = db.categoryDao().getAll();

                        for (DocumentSnapshot doc : documents) {
                            try {
                                Transaction t = new Transaction();
                                t.id = doc.getId();

                                Object amountObj = doc.get("amount");
                                t.amount = (amountObj instanceof Number) ? ((Number) amountObj).longValue() : 0L;

                                String firestoreCatId = doc.getString("firestoreCategoryId");
                                boolean categoryMapped = false;

                                if (firestoreCatId != null && !firestoreCatId.isEmpty()) {
                                    for (Category c : localCats) {
                                        if (firestoreCatId.equals(c.firestoreId)) {
                                            t.categoryId = c.id;
                                            categoryMapped = true;
                                            break;
                                        }
                                    }
                                }

                                if (!categoryMapped) {
                                    Object catIdObj = doc.get("categoryId");
                                    int oldCatId = (catIdObj instanceof Number) ? ((Number) catIdObj).intValue() : 1;

                                    for (Category c : localCats) {
                                        if (c.id == oldCatId || (c.firestoreId != null && c.firestoreId.equals(String.valueOf(oldCatId)))) {
                                            t.categoryId = c.id;
                                            categoryMapped = true;
                                            break;
                                        }
                                    }

                                    if (!categoryMapped) {
                                        t.categoryId = oldCatId;

                                        boolean idExists = false;
                                        for (Category c : localCats) { if (c.id == t.categoryId) { idExists = true; break; } }

                                        if (!idExists) {
                                            Category tempCat = new Category();
                                            tempCat.id = t.categoryId;
                                            tempCat.firestoreId = (firestoreCatId != null && !firestoreCatId.isEmpty()) ? firestoreCatId : String.valueOf(oldCatId);
                                            tempCat.name = "Syncing...";
                                            tempCat.iconName = "ic_other";
                                            tempCat.colorCode = "#A9A9A9";
                                            tempCat.type = doc.getLong("type") != null ? doc.getLong("type").intValue() : 0;
                                            tempCat.workspaceId = "PERSONAL";

                                            try { db.categoryDao().insert(tempCat); } catch(Exception ignored){}
                                            localCats.add(tempCat);
                                        }
                                    }
                                }

                                t.note = doc.getString("note");

                                Object tsObj = doc.get("timestamp");
                                if (tsObj instanceof com.google.firebase.Timestamp) {
                                    t.timestamp = ((com.google.firebase.Timestamp) tsObj).toDate().getTime();
                                } else if (tsObj instanceof Number) {
                                    t.timestamp = ((Number) tsObj).longValue();
                                } else {
                                    t.timestamp = System.currentTimeMillis();
                                }

                                t.paymentMethod = doc.getString("paymentMethod");
                                if (t.paymentMethod == null) t.paymentMethod = "cash";

                                Object typeObj = doc.get("type");
                                t.type = (typeObj instanceof Number) ? ((Number) typeObj).intValue() : 0;
                                t.workspaceId = "PERSONAL";

                                db.transactionDao().insert(t);
                            } catch (Exception parseEx) {
                                Log.e("SYNC", "Skipped malformed transaction: " + doc.getId(), parseEx);
                            }
                        }
                        Log.d("SYNC", "Downloaded " + documents.size() + " transactions.");
                        _isLoading.postValue(false);
                        syncCompleted.postValue(true);
                    } catch (Exception e) {
                        Log.e("SYNC", "Transaction sync error: " + e.getMessage());
                        _isLoading.postValue(false);
                    }
                });
            }

            @Override
            public void onError(String m) {
                _isLoading.postValue(false);
            }
        });
    }

    public void uploadTransactionToFirebase(Transaction t) {
        Map<String, Object> data = new HashMap<>();
        data.put("amount", t.amount);
        data.put("note", t.note);
        data.put("categoryId", t.categoryId);
        data.put("timestamp", t.timestamp);
        data.put("paymentMethod", t.paymentMethod);
        data.put("type", t.type);

        data.put("workspaceId", t.workspaceId != null ? t.workspaceId : "PERSONAL");
        if (t.firestoreCategoryId != null && !t.firestoreCategoryId.isEmpty()) {
            data.put("firestoreCategoryId", t.firestoreCategoryId);
        }

        String docId = t.id;

        firebaseManager.syncLocalToCloud(t.workspaceId, "transactions", docId, data, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d("FIREBASE_SYNC", "Upload transaction " + docId + " to cloud successfully!");
            }

            @Override
            public void onError(String message) {
                Log.e("FIREBASE_SYNC", "Uploading transaction to cloud failed: " + message);
            }
        });
    }

    // ============================================================
    // REAL-TIME MULTI-DEVICE SYNC
    // ============================================================
    public void startRealTimeSync() {
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.e("REALTIME_SYNC", "Sync failed: Not logged in");
            return;
        }
        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore dbCloud = FirebaseFirestore.getInstance();

        if (transListener != null) transListener.remove();
        if (catListener != null) catListener.remove();
        if (budgetListener != null) budgetListener.remove();

        // 1. LISTEN TO TRANSACTIONS
        transListener = dbCloud.collection("users").document(uid).collection("transactions")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null) {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                AppDatabase db = AppDatabase.getInstance(getApplication());

                                if (snapshots.isEmpty()) {
                                    db.transactionDao().deleteAllTransactions("PERSONAL");
                                    syncCompleted.postValue(true);
                                    return;
                                }

                                List<Category> localCats = db.categoryDao().getAll();

                                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                    try {
                                        Transaction t = new Transaction();
                                        t.id = doc.getId();

                                        Object amountObj = doc.get("amount");
                                        t.amount = (amountObj instanceof Number) ? ((Number) amountObj).longValue() : 0L;

                                        String firestoreCatId = doc.getString("firestoreCategoryId");
                                        boolean categoryMapped = false;

                                        if (firestoreCatId != null && !firestoreCatId.isEmpty()) {
                                            for (Category c : localCats) {
                                                if (firestoreCatId.equals(c.firestoreId)) {
                                                    t.categoryId = c.id;
                                                    categoryMapped = true;
                                                    break;
                                                }
                                            }
                                        }

                                        if (!categoryMapped) {
                                            Object catIdObj = doc.get("categoryId");
                                            int oldCatId = (catIdObj instanceof Number) ? ((Number) catIdObj).intValue() : 1;

                                            for (Category c : localCats) {
                                                if (c.id == oldCatId || (c.firestoreId != null && c.firestoreId.equals(String.valueOf(oldCatId)))) {
                                                    t.categoryId = c.id;
                                                    categoryMapped = true;
                                                    break;
                                                }
                                            }

                                            if (!categoryMapped) {
                                                t.categoryId = oldCatId;

                                                boolean idExists = false;
                                                for (Category c : localCats) { if (c.id == t.categoryId) { idExists = true; break; } }

                                                if (!idExists) {
                                                    Category tempCat = new Category();
                                                    tempCat.id = t.categoryId;
                                                    tempCat.firestoreId = (firestoreCatId != null && !firestoreCatId.isEmpty()) ? firestoreCatId : String.valueOf(oldCatId);
                                                    tempCat.name = "Syncing...";
                                                    tempCat.iconName = "ic_other";
                                                    tempCat.colorCode = "#A9A9A9";
                                                    tempCat.type = doc.getLong("type") != null ? doc.getLong("type").intValue() : 0;
                                                    tempCat.workspaceId = "PERSONAL";

                                                    try { db.categoryDao().insert(tempCat); } catch(Exception ignored){}
                                                    localCats.add(tempCat);
                                                }
                                            }
                                        }

                                        t.note = doc.getString("note");

                                        Object tsObj = doc.get("timestamp");
                                        if (tsObj instanceof com.google.firebase.Timestamp) {
                                            t.timestamp = ((com.google.firebase.Timestamp) tsObj).toDate().getTime();
                                        } else if (tsObj instanceof Number) {
                                            t.timestamp = ((Number) tsObj).longValue();
                                        } else {
                                            t.timestamp = System.currentTimeMillis();
                                        }

                                        t.paymentMethod = doc.getString("paymentMethod");
                                        if (t.paymentMethod == null) t.paymentMethod = "cash";

                                        Object typeObj = doc.get("type");
                                        t.type = (typeObj instanceof Number) ? ((Number) typeObj).intValue() : 0;

                                        t.workspaceId = "PERSONAL";

                                        db.transactionDao().insert(t);
                                    } catch (Exception parseEx) {
                                        Log.e("REALTIME_SYNC", "Transaction parsing error: " + doc.getId(), parseEx);
                                    }
                                }
                                syncCompleted.postValue(true);
                            } catch (Exception ex) {
                                Log.e("REALTIME_SYNC", "Transaction sync error: " + ex.getMessage());
                            }
                        });
                    }
                });

        // 2. LISTEN TO CATEGORIES
        catListener = dbCloud.collection("users").document(uid).collection("categories")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null) {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                AppDatabase db = AppDatabase.getInstance(getApplication());
                                List<Category> localCats = db.categoryDao().getAll();

                                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                    Category c = new Category();
                                    c.firestoreId = doc.getId();
                                    c.name = doc.getString("name");
                                    c.iconName = doc.getString("iconName");
                                    c.colorCode = doc.getString("colorCode");

                                    Number type = (Number) doc.get("type");
                                    c.type = (type != null) ? type.intValue() : 0;
                                    c.workspaceId = "PERSONAL";

                                    Long isDef = doc.getLong("isDefault");
                                    c.isDefault = (isDef != null) ? isDef.intValue() : 0;

                                    Long isDel = doc.getLong("isDeleted");
                                    c.isDeleted = (isDel != null) ? isDel.intValue() : 0;

                                    boolean existsLocally = false;
                                    for (Category local : localCats) {
                                        boolean matchId = (local.firestoreId != null && local.firestoreId.equals(c.firestoreId));
                                        boolean matchName = (local.name != null && local.name.equalsIgnoreCase(c.name) && local.type == c.type);

                                        if (matchId || matchName) {
                                            c.id = local.id;
                                            existsLocally = true;
                                            break;
                                        }
                                    }

                                    if (existsLocally) {
                                        db.categoryDao().update(c);
                                    } else {
                                        db.categoryDao().insert(c);
                                    }
                                }
                                syncCompleted.postValue(true);
                            } catch (Exception ex) {
                                Log.e("REALTIME_SYNC", "Category sync error: " + ex.getMessage());
                            }
                        });
                    }
                });

        // 3. LISTEN TO BUDGETS
        budgetListener = dbCloud.collection("users").document(uid).collection("budgets")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null) {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                AppDatabase db = AppDatabase.getInstance(getApplication());
                                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                    Budget b = new Budget();
                                    b.id = Integer.parseInt(doc.getId());

                                    Number limitAmount = (Number) doc.get("limitAmount");
                                    b.limitAmount = (limitAmount != null) ? limitAmount.longValue() : 0L;

                                    Number catId = (Number) doc.get("categoryId");
                                    b.categoryId = (catId != null) ? catId.intValue() : -1;

                                    Number startDate = (Number) doc.get("startDate");
                                    b.startDate = (startDate != null) ? startDate.longValue() : 0L;

                                    Number endDate = (Number) doc.get("endDate");
                                    b.endDate = (endDate != null) ? endDate.longValue() : 0L;

                                    b.periodType = doc.getString("periodType");

                                    db.budgetDao().insert(b);
                                }
                                syncCompleted.postValue(true);
                            } catch (Exception ex) {
                                Log.e("REALTIME_SYNC", "Budget sync error: " + ex.getMessage());
                            }
                        });
                    }
                });
    }

    // ============================================================
    // DỌN DẸP LẮNG NGHE ĐỂ KHÔNG TRÀN BỘ NHỚ
    // ============================================================
    @Override
    protected void onCleared() {
        super.onCleared();
        if (userProfileListener != null) userProfileListener.remove();
        if (transListener != null) transListener.remove();
        if (catListener != null) catListener.remove();
        if (budgetListener != null) budgetListener.remove();
    }

    public void checkAndSeedLocalData(String currentUserId, Runnable onDataReady) {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplication());
                android.content.SharedPreferences prefs = getApplication().getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
                String lastUserId = prefs.getString("last_uid", "");

                if (!lastUserId.equals(currentUserId)) {
                    db.clearAllTables();
                    prefs.edit().putString("last_uid", currentUserId).apply();
                }

                com.example.cashify.data.local.DatabaseSeeder.seedIfEmpty(getApplication());

                int count = db.transactionDao().countTransactions("PERSONAL");

                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (count == 0) {
                        syncAllDataFromServer();
                    } else {
                        if (onDataReady != null) onDataReady.run();
                    }
                    startRealTimeSync();
                });

            } catch (Exception e) {
                Log.e("AUTH_FLOW", "Database error: " + e.getMessage());
            }
        });
    }

    public void registerFcmToken() {
        FirebaseManager.getInstance().getFcmToken(new FirebaseManager.DataCallback<String>() {
            @Override public void onSuccess(String token) {}
            @Override public void onError(String message) {}
        });
    }
}