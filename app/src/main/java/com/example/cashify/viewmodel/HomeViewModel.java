package com.example.cashify.viewmodel;

import android.app.Application;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cashify.database.AppDatabase;
import com.example.cashify.database.CategorySum;
import com.example.cashify.database.TransactionDao;
import com.example.cashify.database.TransactionWithCategory;
import com.example.cashify.model.LegendItem;
import com.example.cashify.repository.TransactionRepository;
import com.example.cashify.utils.CurrencyFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeViewModel extends AndroidViewModel {
    private final TransactionRepository repository;
    private final TransactionDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<DashboardState> dashboardData = new MutableLiveData<>();
    private final MutableLiveData<LinkedHashMap<String, Calendar>> availableMonths = new MutableLiveData<>();

    // Bộ màu chuẩn cho Rule of 5 (5 màu nổi + 1 màu xám)
    private final int[] CHART_COLORS = {
            Color.parseColor("#00BCD4"), // Cyan (Màu 1)
            Color.parseColor("#FF9800"), // Orange (Màu 2)
            Color.parseColor("#AB47BC"), // Purple (Màu 3)
            Color.parseColor("#FF4081"), // Pink (Màu 4)
            Color.parseColor("#4CAF50")  // Green (Màu 5)
    };
    private final int COLOR_OTHERS = Color.parseColor("#BDBDBD"); // Grey (Mục Khác)
    public static class DashboardState {
        public long actualBalance;
        public long totalIncome;
        public long totalExpense;
        public List<CategorySum> top5Categories;
        public long othersTotal;

        public List<LegendItem> legendItems; // Chứa data cho RecyclerView
        public List<LegendItem> subOthersList;   // Dành cho Popup
        public List<Integer> pieColors;      // Chứa màu để ném cho PieChart
    }

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = new TransactionRepository(application);
        dao = AppDatabase.getInstance(application).transactionDao();
    }

    // Lấy list kèm Category
    public LiveData<List<TransactionWithCategory>> getRecentTransactionsWithCategory() {
        return repository.getRecentTransactionsWithCategory();
    }

    public LiveData<DashboardState> getDashboardData() {
        return dashboardData;
    }

    public LiveData<LinkedHashMap<String, Calendar>> getAvailableMonths() {
        return availableMonths;
    }

    public void loadDashboardData(long startOfMonth, long endOfMonth) {
        executor.execute(() -> {
            DashboardState state = new DashboardState();
            state.actualBalance = dao.getMonthlyBalance(startOfMonth, endOfMonth);
            state.totalIncome = dao.getTotalIncome(startOfMonth, endOfMonth);
            state.totalExpense = dao.getTotalExpense(startOfMonth, endOfMonth);
            state.top5Categories = dao.getTop5ExpenseCategories(startOfMonth, endOfMonth);
            state.othersTotal = dao.getOtherExpenseTotal(startOfMonth, endOfMonth);

            //Xử lý màu sắc và Legend
            List<LegendItem> legends = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();

            for (int i = 0; i < state.top5Categories.size(); i++) {
                CategorySum cat = state.top5Categories.get(i);
                if (cat.total > 0) {
                    int color = CHART_COLORS[i % CHART_COLORS.length];
                    colors.add(color);

                    // Format tiền tệ sẵn từ ViewModel
                    String formattedMoney = CurrencyFormatter.formatCompactVND(cat.total);
                    legends.add(new LegendItem(color, cat.categoryName, formattedMoney));
                }
            }

            // Xử lý mục "Khác" (Nếu có)
            if (state.othersTotal > 0) {
                colors.add(COLOR_OTHERS);
                legends.add(new LegendItem(COLOR_OTHERS, "Others", CurrencyFormatter.formatCompactVND(state.othersTotal)));

                //Lấy chi tiết đám lẻ tẻ nhét vào subOthersList
                List<CategorySum> othersBreakdown = dao.getOthersBreakdown(startOfMonth, endOfMonth);
                List<LegendItem> subLegends = new ArrayList<>();
                for (CategorySum cat : othersBreakdown) {
                    subLegends.add(new LegendItem(COLOR_OTHERS, cat.categoryName, CurrencyFormatter.formatCompactVND(cat.total)));
                }
                state.subOthersList = subLegends;
            } else {
                state.subOthersList = new ArrayList<>(); // Rỗng nếu không có mục Khác
            }

            state.legendItems = legends;
            state.pieColors = colors;

            // Báo cho UI biết tao tính xong rồi nè, vẽ đi!
            dashboardData.postValue(state);
        });
    }

    public void loadAvailableMonths(String formatString) {
        executor.execute(() -> {
            List<Long> timestamps = dao.getAllTimestamps();
            LinkedHashMap<String, Calendar> monthMap = new LinkedHashMap<>();

            if (timestamps != null) {
                for (Long ts : timestamps) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(ts);
                    int m = cal.get(Calendar.MONTH) + 1;
                    int y = cal.get(Calendar.YEAR);

                    // Format thủ công để không dính tới Context của Activity
                    String label = String.format(formatString, m, y);

                    if (!monthMap.containsKey(label)) {
                        cal.set(Calendar.DAY_OF_MONTH, 1);
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        monthMap.put(label, cal);
                    }
                }
            }
            availableMonths.postValue(monthMap);
        });
    }

}