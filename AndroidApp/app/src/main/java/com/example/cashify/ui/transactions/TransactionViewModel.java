package com.example.cashify.ui.transactions;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cashify.R;
import com.example.cashify.data.local.AppDatabase;
import com.example.cashify.data.model.Category;
import com.example.cashify.data.local.CategoryDao;
import com.example.cashify.data.model.Transaction;
import com.example.cashify.data.local.TransactionDao;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionViewModel extends AndroidViewModel {

    private final TransactionDao transactionDao;
    private final CategoryDao categoryDao;

    // Thread pool to replace memory-leaking 'new Thread()' instantiations
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    private String currentWorkspaceId = "PERSONAL";
    private String currentSearchQuery = "";

    // =========================================================================
    // STATE LIVEDATA
    // =========================================================================
    private final MutableLiveData<List<HistoryItem>> _historyItems = new MutableLiveData<>();
    public LiveData<List<HistoryItem>> getGroupedTransactions() { return _historyItems; }

    private final MutableLiveData<List<FilterChip>> filterChips = new MutableLiveData<>();
    public LiveData<List<FilterChip>> getFilterChips() { return filterChips; }
    private final MutableLiveData<List<Category>> _filterCategories = new MutableLiveData<>();
    public LiveData<List<Category>> getFilterCategories() { return _filterCategories; }

    // Multi-Filter States
    public final MutableLiveData<long[]> selectedDateRange = new MutableLiveData<>(null);
    public final MutableLiveData<Integer> selectedType = new MutableLiveData<>(null);
    public final MutableLiveData<String> selectedMethod = new MutableLiveData<>(null);
    public final MutableLiveData<Integer> selectedCategoryId = new MutableLiveData<>(null);

    public TransactionViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        transactionDao = db.transactionDao();
        categoryDao = db.categoryDao();

        initDefaultChips();
    }

    public void initDefaultChips() {
        List<FilterChip> list = new ArrayList<>();
        list.add(new FilterChip(getApplication().getString(R.string.time_chip), FilterChip.FilterType.DATE));
        list.add(new FilterChip(getApplication().getString(R.string.type_chip), FilterChip.FilterType.TYPE));
        list.add(new FilterChip(getApplication().getString(R.string.payment_chip), FilterChip.FilterType.METHOD));
        list.add(new FilterChip(getApplication().getString(R.string.category_chip), FilterChip.FilterType.CATEGORY));
        filterChips.setValue(list);
        loadCategoriesForFilter();
    }

    public void resetFilters() {
        selectedDateRange.setValue(null);
        selectedType.setValue(null);
        selectedMethod.setValue(null);
        selectedCategoryId.setValue(null);
        initDefaultChips();
        fetchHistoryData(currentWorkspaceId);
    }

    /**
     * Fetches transactions from local DB and applies search & multi-filter logic in-memory.
     */
    public void fetchHistoryData(String workspaceId, String query) {
        this.currentWorkspaceId = workspaceId != null ? workspaceId : "PERSONAL";
        this.currentSearchQuery = query != null ? query : "";

        databaseExecutor.execute(() -> {
            List<Transaction> transactions = transactionDao.getAll(currentWorkspaceId);
            List<HistoryItem> uiModels = new ArrayList<>();

            if (transactions == null || transactions.isEmpty()) {
                _historyItems.postValue(uiModels);
                return;
            }

            // Read current filter states
            long[] dates = selectedDateRange.getValue();
            Integer type = selectedType.getValue();
            String method = selectedMethod.getValue();
            Integer categoryId = selectedCategoryId.getValue();

            List<Transaction> filteredList = new ArrayList<>();
            for (Transaction t : transactions) {
                // 1. Search Query Filter
                if (!currentSearchQuery.isEmpty()) {
                    if (t.note == null || !t.note.toLowerCase().contains(currentSearchQuery.toLowerCase())) {
                        continue;
                    }
                }

                // 2. Type Filter (0: Expense, 1: Income)
                if (type != null && t.type != type) continue;

                // 3. Date Range Filter
                if (dates != null && dates.length == 2) {
                    if (t.timestamp < dates[0] || t.timestamp > dates[1]) continue;
                }

                // 4. Payment Method Filter
                if (method != null && (t.paymentMethod == null || !t.paymentMethod.equals(method))) continue;

                // 5. Category Filter
                if (categoryId != null && t.categoryId != categoryId) continue;

                filteredList.add(t);
            }

            // Group filtered items by Date Header
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH);
            String lastDate = "";

            for (Transaction trans : filteredList) {
                String currentDate = sdf.format(new Date(trans.timestamp));

                if (!currentDate.equals(lastDate)) {
                    uiModels.add(new HistoryItem(currentDate));
                    lastDate = currentDate;
                }

                Category cat = null;
                // Cloud Sync ID Resolution
                if (trans.firestoreCategoryId != null && !trans.firestoreCategoryId.isEmpty()) {
                    List<Category> allCats = categoryDao.getAll();
                    for (Category c : allCats) {
                        if (trans.firestoreCategoryId.equals(c.firestoreId)) {
                            cat = c; break;
                        }
                    }
                }

                // Fallback to local ID
                if (cat == null) {
                    cat = categoryDao.getCategoryById(trans.categoryId);
                }

                String catName = (cat != null) ? cat.name : "Unknown";
                String catColor = (cat != null) ? cat.colorCode : "#000000";
                String catIcon = (cat != null) ? cat.iconName : "ic_other";

                uiModels.add(new HistoryItem(trans, catName, catIcon, catColor));
            }

            _historyItems.postValue(uiModels);
        });
    }

    public void fetchHistoryData(String workspaceId) {
        fetchHistoryData(workspaceId, currentSearchQuery);
    }

    // =========================================================================
    // CRUD OPERATIONS (Syncing Local & Cloud)
    // =========================================================================

    public void deleteOnly(Transaction transaction) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        databaseExecutor.execute(() -> {
            transactionDao.delete(transaction);
            fetchHistoryData(currentWorkspaceId);
        });

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("transactions").document(transaction.id)
                .delete();
    }

    public void insertOnly(Transaction transaction) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String collectionPath = (transaction.workspaceId == null || transaction.workspaceId.equals("PERSONAL"))
                ? "users/" + user.getUid() + "/transactions"
                : "workspaces/" + transaction.workspaceId + "/transactions";

        if (transaction.id == null || transaction.id.isEmpty()) {
            transaction.id = FirebaseFirestore.getInstance().collection(collectionPath).document().getId();
        }

        databaseExecutor.execute(() -> {
            transactionDao.insert(transaction);
            fetchHistoryData(currentWorkspaceId);
        });

        FirebaseFirestore.getInstance().collection(collectionPath).document(transaction.id).set(transaction);
    }

    public void updateAndRefresh(Transaction transaction) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String collectionPath = (transaction.workspaceId == null || transaction.workspaceId.equals("PERSONAL"))
                ? "users/" + user.getUid() + "/transactions"
                : "workspaces/" + transaction.workspaceId + "/transactions";

        databaseExecutor.execute(() -> {
            transactionDao.update(transaction);
            fetchHistoryData(currentWorkspaceId);
        });

        FirebaseFirestore.getInstance().collection(collectionPath).document(transaction.id).set(transaction);
    }
    public void loadCategoriesForFilter() {
        databaseExecutor.execute(() -> {
            _filterCategories.postValue(categoryDao.getAll());
        });
    }
    @Override
    protected void onCleared() {
        super.onCleared();
        databaseExecutor.shutdown();
    }

    // =========================================================================
    // INNER DATA MODEL FOR RECYCLER VIEW
    // =========================================================================

    public static class HistoryItem {
        public static final int TYPE_DATE_HEADER = 0;
        public static final int TYPE_TRANSACTION = 1;

        private final int type;
        private String date;
        private Transaction transaction;
        private String categoryName;
        private String categoryIcon;
        private String categoryColor;

        public HistoryItem(String date) {
            this.type = TYPE_DATE_HEADER;
            this.date = date;
        }

        public HistoryItem(Transaction transaction, String categoryName, String categoryIcon, String categoryColor) {
            this.type = TYPE_TRANSACTION;
            this.transaction = transaction;
            this.categoryName = categoryName;
            this.categoryColor = categoryColor;
            this.categoryIcon = categoryIcon;
        }

        public int getType() { return type; }
        public String getDate() { return date; }
        public Transaction getTransaction() { return transaction; }
        public String getCategoryName() { return categoryName; }
        public String getCategoryIcon() { return categoryIcon; }
        public String getCategoryColor(){ return categoryColor; }
    }
}