package com.example.cashify.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cashify.database.AppDatabase;
import com.example.cashify.database.Category;
import com.example.cashify.database.CategoryDao;
import com.example.cashify.database.Transaction;
import com.example.cashify.database.TransactionDao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionViewModel extends AndroidViewModel {

    private final TransactionDao transactionDao;
    private final CategoryDao categoryDao;
    private String categoryColor;

    // 1. Biến này dùng để ghi nhớ tab hiện tại (ALL, INCOME, hay EXPENSE)
    private String currentFilter = "ALL";

    private final MutableLiveData<List<HistoryItem>> _historyItems = new MutableLiveData<>();
    public LiveData<List<HistoryItem>> getGroupedTransactions() { return _historyItems; }

    public TransactionViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        transactionDao = db.transactionDao();
        categoryDao = db.categoryDao();
    }

    private String currentSearchQuery = ""; // Thêm biến này để lưu từ khóa tìm kiếm

    // --- HÀM FETCH CHÍNH (Cập nhật để hỗ trợ Search) ---
    public void fetchHistoryData(String filterType, String query) {
        this.currentFilter = filterType;
        this.currentSearchQuery = query; // Lưu lại từ khóa

        new Thread(() -> {
            List<Transaction> transactions;

            // 1. Lấy dữ liệu theo Filter và Search Query
            // Lưu ý: Nếu bạn chưa viết hàm search trong DAO, ta sẽ lọc bằng code Java ở dưới
            switch (filterType) {
                case "INCOME":
                    transactions = transactionDao.getTransactionsByType(1);
                    break;
                case "EXPENSE":
                    transactions = transactionDao.getTransactionsByType(0);
                    break;
                default:
                    transactions = transactionDao.getAll();
                    break;
            }

            List<HistoryItem> uiModels = new ArrayList<>();

            if (transactions == null || transactions.isEmpty()) {
                _historyItems.postValue(uiModels);
                return;
            }

            // 2. LỌC THEO TỪ KHÓA (Search Logic)
            List<Transaction> filteredList = new ArrayList<>();
            for (Transaction t : transactions) {
                // Nếu từ khóa trống HOẶC ghi chú chứa từ khóa (không phân biệt hoa thường)
                if (query.isEmpty() || (t.note != null && t.note.toLowerCase().contains(query.toLowerCase()))) {
                    filteredList.add(t);
                }
            }

            // 3. Chuyển đổi sang UI Model (HistoryItem)
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            String lastDate = "";

            for (Transaction trans : filteredList) {
                String currentDate = sdf.format(new Date(trans.timestamp));

                if (!currentDate.equals(lastDate)) {
                    uiModels.add(new HistoryItem(currentDate));
                    lastDate = currentDate;
                }

                Category cat = categoryDao.getCategoryById(trans.categoryId);
                String catName = (cat != null) ? cat.name : "Unknown";
                String catColor= (cat != null) ? cat.colorCode: "#000000";
                String catIcon = (cat != null) ? cat.iconName : "ic_other";

                uiModels.add(new HistoryItem(trans, catName, catIcon, catColor));
            }

            _historyItems.postValue(uiModels);
        }).start();
    }

    // --- HÀM OVERLOAD (Để không bị lỗi code cũ) ---
    public void fetchHistoryData(String filterType) {
        fetchHistoryData(filterType, ""); // Mặc định search trống
    }

    public void fetchHistoryData() {
        fetchHistoryData(currentFilter, currentSearchQuery);
    }

    public void updateAndRefresh(Transaction transaction) {
        new Thread(() -> {
            transactionDao.update(transaction);
            // Giờ đây gọi fetchHistoryData() sẽ không bị báo lỗi nữa
            fetchHistoryData();
        }).start();
    }

    public void deleteAndRefresh(Transaction transaction) {
        new Thread(() -> {
            transactionDao.delete(transaction);
            fetchHistoryData();
        }).start();
    }

    // --- INNER CLASS ---
    public static class HistoryItem {
        public static final int TYPE_DATE_HEADER = 0;
        public static final int TYPE_TRANSACTION = 1;

        private int type;
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
        public String getCategoryColor(){return categoryColor;}
    }
}