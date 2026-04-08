package com.example.cashify.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cashify.database.AppDatabase;
import com.example.cashify.database.CategorySum;
import com.example.cashify.database.TransactionDao;
import com.example.cashify.database.TransactionWithCategory;
import com.example.cashify.repository.TransactionRepository;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeViewModel extends AndroidViewModel {
    private final TransactionRepository repository;
    private final TransactionDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static class DashboardState {
        public long actualBalance;
        public long totalIncome;
        public long totalExpense;
        public List<CategorySum> top5Categories;
        public long othersTotal;
    }

    private final MutableLiveData<DashboardState> dashboardData = new MutableLiveData<>();
    private final MutableLiveData<LinkedHashMap<String, Calendar>> availableMonths = new MutableLiveData<>();

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