package com.example.cashify.ui.home;

import android.app.Application;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cashify.data.local.AppDatabase;
import com.example.cashify.data.local.CategorySum;
import com.example.cashify.data.local.TransactionDao;
import com.example.cashify.data.local.TransactionWithCategory;
import com.example.cashify.data.model.LegendItem;
import com.example.cashify.data.remote.FirebaseManager;
import com.example.cashify.data.repository.TransactionRepository;
import com.example.cashify.utils.CurrencyFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeViewModel extends AndroidViewModel {

    // Repositories & Concurrency
    private final TransactionRepository repository;
    private final TransactionDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // UI States
    private final MutableLiveData<DashboardState> dashboardData = new MutableLiveData<>();
    private final MutableLiveData<LinkedHashMap<String, Calendar>> availableMonths = new MutableLiveData<>();
    private final MutableLiveData<Integer> unreadNotificationCount = new MutableLiveData<>(0);

    // Standard Rule of 5 Color Palette (5 vibrant colors + 1 grey)
    private final int[] CHART_COLORS = {
            Color.parseColor("#6B759E"), // Dark Blue (Color 1)
            Color.parseColor("#000767"), // Navy (Color 2)
            Color.parseColor("#BBC5F2"), // Light Purple (Color 3)
            Color.parseColor("#666FCA"), // Purple/Pink (Color 4)
            Color.parseColor("#5B5F64")  // Slate (Color 5)
    };
    private final int COLOR_OTHERS = Color.parseColor("#BDBDBD"); // Grey (Others)

    public static class DashboardState {
        public long actualBalance;
        public long totalIncome;
        public long totalExpense;
        public List<CategorySum> top5Categories;
        public long othersTotal;

        public List<LegendItem> legendItems;     // Data for standard RecyclerView Legend
        public List<LegendItem> subOthersList;   // Detailed breakdown for BottomSheet
        public List<Integer> pieColors;          // Correlated colors for PieChart
    }

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = new TransactionRepository(application);
        dao = AppDatabase.getInstance(application).transactionDao();
    }

    // --- Getters for UI Observation ---
    public LiveData<Integer> getUnreadNotificationCount() { return unreadNotificationCount; }
    public LiveData<DashboardState> getDashboardData() { return dashboardData; }
    public LiveData<LinkedHashMap<String, Calendar>> getAvailableMonths() { return availableMonths; }

    public LiveData<List<TransactionWithCategory>> getRecentTransactionsWithCategory(String workspaceId) {
        return repository.getRecentTransactionsWithCategory(workspaceId);
    }

    // =========================================================================
    // REAL-TIME NOTIFICATIONS
    // =========================================================================

    /**
     * Listens for unread notification count from Firebase.
     * Note: Direct FirebaseManager usage kept here temporarily to avoid massive refactoring.
     */
    public void listenToUnreadNotifications() {
        FirebaseManager.getInstance().listenToUnreadNotifications(new FirebaseManager.DataCallback<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                unreadNotificationCount.postValue(count != null ? count : 0);
            }

            @Override
            public void onError(String message) {
                unreadNotificationCount.postValue(0); // Hide badge on error
            }
        });
    }

    // =========================================================================
    // DASHBOARD DATA PROCESSING
    // =========================================================================

    public void loadDashboardData(String workspaceId, long startOfMonth, long endOfMonth) {
        executor.execute(() -> {
            DashboardState state = new DashboardState();
            state.actualBalance = dao.getMonthlyBalance(workspaceId, startOfMonth, endOfMonth);
            state.totalIncome = dao.getTotalIncome(workspaceId, startOfMonth, endOfMonth);
            state.totalExpense = dao.getTotalExpense(workspaceId, startOfMonth, endOfMonth);
            state.top5Categories = dao.getTop5ExpenseCategories(workspaceId, startOfMonth, endOfMonth);
            state.othersTotal = dao.getOtherExpenseTotal(workspaceId, startOfMonth, endOfMonth);

            List<LegendItem> legends = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();

            // Process Top 5 Categories
            for (int i = 0; i < state.top5Categories.size(); i++) {
                CategorySum cat = state.top5Categories.get(i);
                if (cat.total > 0) {
                    int color = CHART_COLORS[i % CHART_COLORS.length];
                    colors.add(color);

                    String formattedMoney = CurrencyFormatter.formatCompactVND(cat.total);
                    legends.add(new LegendItem(color, cat.categoryName, formattedMoney));
                }
            }

            // Process "Others" category if it exists
            if (state.othersTotal > 0) {
                colors.add(COLOR_OTHERS);
                legends.add(new LegendItem(COLOR_OTHERS, "Others", CurrencyFormatter.formatCompactVND(state.othersTotal)));

                List<CategorySum> othersBreakdown = dao.getOthersBreakdown(workspaceId, startOfMonth, endOfMonth);
                List<LegendItem> subLegends = new ArrayList<>();
                for (CategorySum cat : othersBreakdown) {
                    subLegends.add(new LegendItem(COLOR_OTHERS, cat.categoryName, CurrencyFormatter.formatCompactVND(cat.total)));
                }
                state.subOthersList = subLegends;
            } else {
                state.subOthersList = new ArrayList<>();
            }

            state.legendItems = legends;
            state.pieColors = colors;

            // Post the fully constructed state back to the Main Thread for UI rendering
            dashboardData.postValue(state);
        });
    }

    public void loadAvailableMonths(String workspaceId, String formatString) {
        executor.execute(() -> {
            List<Long> timestamps = dao.getAllTimestamps(workspaceId);
            LinkedHashMap<String, Calendar> monthMap = new LinkedHashMap<>();

            if (timestamps != null) {
                for (Long ts : timestamps) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(ts);
                    int m = cal.get(Calendar.MONTH) + 1;
                    int y = cal.get(Calendar.YEAR);

                    // Formatted externally to keep Context out of ViewModel
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

    // =========================================================================
    // LIFECYCLE MANAGEMENT
    // =========================================================================

    @Override
    protected void onCleared() {
        super.onCleared();
        // CRITICAL: Shut down the executor to prevent zombie threads and memory leaks
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}