package com.example.cashify.ui.transactions;

import android.app.Application;
import android.util.Log;

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
import com.example.cashify.utils.TransactionQueryBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionViewModel extends AndroidViewModel {

    private final TransactionDao transactionDao;
    private final CategoryDao categoryDao;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    private String currentWorkspaceId = "PERSONAL";
    private String currentSearchQuery = "";

    private static final int PAGE_SIZE = 10;
    private int currentOffset = 0;
    private boolean isLastPage = false;
    private boolean isLoadingData = false;

    private final MutableLiveData<List<HistoryItem>> _historyItems = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<HistoryItem>> getGroupedTransactions() { return _historyItems; }

    private final MutableLiveData<List<FilterChip>> filterChips = new MutableLiveData<>();
    public LiveData<List<FilterChip>> getFilterChips() { return filterChips; }

    private final MutableLiveData<List<Category>> _filterCategories = new MutableLiveData<>();
    public LiveData<List<Category>> getFilterCategories() { return _filterCategories; }

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
        currentSearchQuery = "";
        initDefaultChips();
        fetchHistoryData(currentWorkspaceId, "", true);
    }

    public void fetchHistoryData(String workspaceId, String query, boolean isRefresh) {
        this.currentWorkspaceId = workspaceId != null ? workspaceId : "PERSONAL";
        this.currentSearchQuery = query != null ? query : "";

        if (isRefresh) {
            currentOffset = 0;
            isLastPage = false;
            isLoadingData = false;
        }

        if (isLastPage || isLoadingData) return;
        isLoadingData = true;

        databaseExecutor.execute(() -> {
            try {
                List<Category> allCategories = categoryDao.getAll();
                Map<String, Category> firestoreCatMap = new HashMap<>();
                Map<Integer, Category> localCatMap = new HashMap<>();

                for (Category c : allCategories) {
                    if (c.firestoreId != null) firestoreCatMap.put(c.firestoreId, c);
                    localCatMap.put(c.id, c);
                }

                androidx.sqlite.db.SupportSQLiteQuery sqlQuery = TransactionQueryBuilder.buildFilteredQuery(
                        currentWorkspaceId, currentSearchQuery, selectedDateRange.getValue(),
                        selectedType.getValue(), selectedMethod.getValue(), selectedCategoryId.getValue(),
                        PAGE_SIZE, currentOffset
                );

                List<Transaction> pagedTransactions = transactionDao.getFilteredTransactions(sqlQuery);

                if (pagedTransactions.isEmpty()) {
                    isLastPage = true;
                    isLoadingData = false;
                    if (isRefresh) _historyItems.postValue(new ArrayList<>());
                    return;
                }

                List<HistoryItem> uiModels = isRefresh ? new ArrayList<>() : new ArrayList<>(_historyItems.getValue() != null ? _historyItems.getValue() : new ArrayList<>());
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH);

                //Lấy đúng cái Date của Item THỨ N nằm ở ĐÁY mảng hiện tại
                String lastDate = "";
                if (!isRefresh && !uiModels.isEmpty()) {
                    for (int i = uiModels.size() - 1; i >= 0; i--) {
                        if (uiModels.get(i).getType() == HistoryItem.TYPE_TRANSACTION) {
                            lastDate = sdf.format(new Date(uiModels.get(i).getTransaction().timestamp));
                            break;
                        }
                    }
                }

                for (Transaction trans : pagedTransactions) {
                    String currentDate = sdf.format(new Date(trans.timestamp));
                    if (!currentDate.equals(lastDate)) {
                        uiModels.add(new HistoryItem(currentDate));
                        lastDate = currentDate;
                    }

                    Category cat = null;
                    if (trans.firestoreCategoryId != null && !trans.firestoreCategoryId.isEmpty()) {
                        cat = firestoreCatMap.get(trans.firestoreCategoryId);
                    }
                    if (cat == null) cat = localCatMap.get(trans.categoryId);

                    String catName = (cat != null) ? cat.name : "Unknown";
                    String catColor = (cat != null) ? cat.colorCode : "#000000";
                    String catIcon = (cat != null) ? cat.iconName : "ic_other";

                    uiModels.add(new HistoryItem(trans, catName, catIcon, catColor));
                }

                currentOffset += pagedTransactions.size();
                isLastPage = pagedTransactions.size() < PAGE_SIZE;
                isLoadingData = false;

                _historyItems.postValue(uiModels);

            } catch (Exception e) {
                isLoadingData = false;
                Log.e("PAGING_ERROR", "Error parsing transactions: " + e.getMessage());
            }
        });
    }

    public void loadMore() {
        fetchHistoryData(currentWorkspaceId, currentSearchQuery, false);
    }

    public void deleteOnly(Transaction transaction) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String collectionPath = (transaction.workspaceId == null || transaction.workspaceId.equals("PERSONAL"))
                ? "users/" + user.getUid() + "/transactions"
                : "workspaces/" + transaction.workspaceId + "/transactions";

        databaseExecutor.execute(() -> {
            transactionDao.delete(transaction);
            fetchHistoryData(currentWorkspaceId, currentSearchQuery, true);
        });

        FirebaseFirestore.getInstance().collection(collectionPath).document(transaction.id).delete();
    }

    public void loadCategoriesForFilter() {
        databaseExecutor.execute(() -> _filterCategories.postValue(categoryDao.getAll()));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        databaseExecutor.shutdown();
    }

    public static class HistoryItem {
        public static final int TYPE_DATE_HEADER = 0;
        public static final int TYPE_TRANSACTION = 1;
        private final int type;
        private String date;
        private Transaction transaction;
        private String categoryName;
        private String categoryIcon;
        private String categoryColor;
        public HistoryItem(String date) { this.type = TYPE_DATE_HEADER; this.date = date; }
        public HistoryItem(Transaction transaction, String categoryName, String categoryIcon, String categoryColor) {
            this.type = TYPE_TRANSACTION; this.transaction = transaction; this.categoryName = categoryName; this.categoryColor = categoryColor; this.categoryIcon = categoryIcon;
        }
        public int getType() { return type; }
        public String getDate() { return date; }
        public Transaction getTransaction() { return transaction; }
        public String getCategoryName() { return categoryName; }
        public String getCategoryIcon() { return categoryIcon; }
        public String getCategoryColor(){ return categoryColor; }
    }
}