package com.example.cashify.ui.transactions;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cashify.data.local.AppDatabase;
import com.example.cashify.data.model.Category;
import com.example.cashify.data.local.CategoryDao;
import com.example.cashify.data.model.Transaction;
import com.example.cashify.data.local.TransactionDao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionViewModel extends AndroidViewModel {

    private final TransactionDao transactionDao;
    private final CategoryDao categoryDao;

    private String currentWorkspaceId = "PERSONAL";

    // Chỉ giữ lại Search Query, bỏ currentFilter cũ vì đã dùng LiveData Multi-Filter
    private String currentSearchQuery = "";

    private final MutableLiveData<List<HistoryItem>> _historyItems = new MutableLiveData<>();

    // --- CÁC TRẠNG THÁI MULTI-FILTER ---
    public final MutableLiveData<long[]> selectedDateRange = new MutableLiveData<>(null);
    public final MutableLiveData<Integer> selectedType = new MutableLiveData<>(null);
    public final MutableLiveData<String> selectedMethod = new MutableLiveData<>(null);
    public final MutableLiveData<Integer> selectedCategoryId = new MutableLiveData<>(null);

    private final MutableLiveData<List<FilterChip>> filterChips = new MutableLiveData<>();

    public LiveData<List<FilterChip>> getFilterChips() { return filterChips; }
    public LiveData<List<HistoryItem>> getGroupedTransactions() { return _historyItems; }

    public TransactionViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        transactionDao = db.transactionDao();
        categoryDao = db.categoryDao();

        initDefaultChips(); // Khởi tạo chip mặc định ngay khi mở app
    }

    public void initDefaultChips() {
        List<FilterChip> list = new ArrayList<>();
        list.add(new FilterChip("🗓️ Time", FilterChip.FilterType.DATE));
        list.add(new FilterChip("📈 Type", FilterChip.FilterType.TYPE));
        list.add(new FilterChip("🪙 Payment", FilterChip.FilterType.METHOD));
        list.add(new FilterChip("🏷️ Category", FilterChip.FilterType.CATEGORY));
        filterChips.setValue(list);
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
     * Hàm fetch dữ liệu: Lấy tất cả, sau đó lọc kết hợp Search + 4 loại Filter
     */
    public void fetchHistoryData(String workspaceId, String query) {
        this.currentWorkspaceId = workspaceId != null ? workspaceId : "PERSONAL";
        this.currentSearchQuery = query != null ? query : "";

        new Thread(() -> {
            // Lấy tất cả dữ liệu từ DB (Chưa cần đổi DAO, lọc in-memory vẫn rất nhanh)
            List<Transaction> transactions = transactionDao.getAll(currentWorkspaceId);

            List<HistoryItem> uiModels = new ArrayList<>();
            if (transactions == null || transactions.isEmpty()) {
                _historyItems.postValue(uiModels);
                return;
            }

            // Đọc các trạng thái filter hiện tại
            long[] dates = selectedDateRange.getValue();
            Integer type = selectedType.getValue();
            String method = selectedMethod.getValue();
            Integer categoryId = selectedCategoryId.getValue();

            List<Transaction> filteredList = new ArrayList<>();
            for (Transaction t : transactions) {
                // 1. Lọc theo Search (Bỏ qua hoa/thường)
                if (!currentSearchQuery.isEmpty()) {
                    if (t.note == null || !t.note.toLowerCase().contains(currentSearchQuery.toLowerCase())) {
                        continue;
                    }
                }

                // 2. Lọc theo Loại (0: Chi, 1: Thu)
                if (type != null && t.type != type) continue;

                // 3. Lọc theo Khoảng thời gian
                if (dates != null && dates.length == 2) {
                    if (t.timestamp < dates[0] || t.timestamp > dates[1]) continue;
                }

                // 4. Lọc theo Ví thanh toán
                if (method != null && (t.paymentMethod == null || !t.paymentMethod.equals(method))) continue;

                // 5. Lọc theo Danh mục
                if (categoryId != null && t.categoryId != categoryId) continue;

                // Vượt qua toàn bộ filter -> hợp lệ
                filteredList.add(t);
            }

            // Gom nhóm theo ngày để tạo Header
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH);
            String lastDate = "";

            for (Transaction trans : filteredList) {
                String currentDate = sdf.format(new Date(trans.timestamp));

                if (!currentDate.equals(lastDate)) {
                    uiModels.add(new HistoryItem(currentDate));
                    lastDate = currentDate;
                }

                Category cat = categoryDao.getCategoryById(trans.categoryId);
                String catName = (cat != null) ? cat.name : "Unknown";
                String catColor = (cat != null) ? cat.colorCode : "#000000";
                String catIcon = (cat != null) ? cat.iconName : "ic_other";

                uiModels.add(new HistoryItem(trans, catName, catIcon, catColor));
            }

            _historyItems.postValue(uiModels);
        }).start();
    }

    // --- OVERLOADS ---
    public void fetchHistoryData(String workspaceId) { fetchHistoryData(workspaceId, currentSearchQuery); }

    /**
     * Xóa nhanh (dùng cho Swipe to Delete)
     */
    public void deleteOnly(Transaction transaction) {
        new Thread(() -> {
            transactionDao.delete(transaction);
            fetchHistoryData(currentWorkspaceId); // Refresh lại danh sách (giữ nguyên filter đang chọn)
        }).start();
    }

    /**
     * Chèn lại (dùng cho nút UNDO trên Snackbar)
     */
    public void insertOnly(Transaction transaction) {
        new Thread(() -> {
            transactionDao.insert(transaction);
            fetchHistoryData(currentWorkspaceId);
        }).start();
    }

    public void updateAndRefresh(Transaction transaction) {
        new Thread(() -> {
            transactionDao.update(transaction);
            fetchHistoryData(currentWorkspaceId);
        }).start();
    }

    // --- INNER CLASS CHO RECYCLERVIEW MULTI-TYPE ---
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