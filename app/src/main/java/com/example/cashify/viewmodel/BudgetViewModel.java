package com.example.cashify.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cashify.database.Budget;
import com.example.cashify.database.BudgetWithSpent;
import com.example.cashify.repository.BudgetRepository;
import com.example.cashify.utils.CurrencyFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BudgetViewModel extends AndroidViewModel {

    private final BudgetRepository repository;

    public static class BudgetUIState {
        public List<BudgetWithSpent> displayList;
        public Budget masterBudget;
        public long masterSpent;
        public long totalCatLimits;
        public String monthLabel;
        public String weekLabel;
        public boolean isReadOnly;
    }

    private final MutableLiveData<BudgetUIState> uiState = new MutableLiveData<>();
    public LiveData<BudgetUIState> getUiState() { return uiState; }

    public interface BudgetActionCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    private int activeYear;
    private int activeMonth;
    private int activeWeekIndex;

    public BudgetViewModel(@NonNull Application application) {
        super(application);
        repository = new BudgetRepository(application);

        Calendar today = Calendar.getInstance();
        activeYear = today.get(Calendar.YEAR);
        activeMonth = today.get(Calendar.MONTH);
        int day = today.get(Calendar.DAY_OF_MONTH);
        activeWeekIndex = ((day - 1) / 7) + 1;
        if (activeWeekIndex > 5) activeWeekIndex = 5;
    }

    // --- HÀM TẠO NHÃN DÁN ĐỂ TÁCH BIỆT DỮ LIỆU ---
    private String getActualPeriod(String periodType, boolean isLinkedMode) {
        return isLinkedMode ? periodType + "_LINKED" : periodType;
    }

    // --- LOGIC: TÍNH TOÁN THỜI GIAN  ---
    private long[] calculateTimeRange(String periodType) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        long startTime, endTime;

        if (periodType.equals("WEEK")) {
            cal.set(activeYear, activeMonth, 1, 0, 0, 0);
            int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

            int startDay = (activeWeekIndex - 1) * 7 + 1;
            int endDay = startDay + 6;
            if (activeWeekIndex == 5 || endDay > maxDays) endDay = maxDays;

            cal.set(Calendar.DAY_OF_MONTH, startDay);
            startTime = cal.getTimeInMillis();

            cal.set(Calendar.DAY_OF_MONTH, endDay);
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
            endTime = cal.getTimeInMillis();
        } else {
            cal.set(activeYear, activeMonth, 1, 0, 0, 0);
            startTime = cal.getTimeInMillis();
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
            endTime = cal.getTimeInMillis();
        }
        return new long[]{startTime, endTime};
    }

    private boolean checkIsReadOnly() {
        Calendar today = Calendar.getInstance();
        int currentYear = today.get(Calendar.YEAR);
        int currentMonth = today.get(Calendar.MONTH);
        return (activeYear < currentYear) || (activeYear == currentYear && activeMonth < currentMonth);
    }

    // --- HÀM CHO FRAGMENT CẬP NHẬT THỜI GIAN ---
    public void updateSelectedMonth(int year, int month) {
        this.activeYear = year;
        this.activeMonth = month;
        // Reset về tuần 1 mỗi khi đổi tháng
        this.activeWeekIndex = 1;
    }

    public void updateSelectedWeek(int weekIndex) {
        this.activeWeekIndex = weekIndex;
    }

    public List<String> getAvailableMonths(String formatString) {
        List<String> options = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -24);
        for (int i = 0; i < 48; i++) {
            options.add(String.format(Locale.getDefault(), formatString, cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR)));
            cal.add(Calendar.MONTH, 1);
        }
        return options;
    }

    public List<String> getAvailableWeeks(String formatString) {
        List<String> options = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set(activeYear, activeMonth, 1);
        int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int totalWeeks = (maxDays > 28) ? 5 : 4;

        for (int i = 1; i <= totalWeeks; i++) {
            int startDay = (i - 1) * 7 + 1;
            int endDay = (i == 5 || startDay + 6 > maxDays) ? maxDays : startDay + 6;
            options.add(String.format(Locale.getDefault(), formatString, i, startDay, activeMonth + 1, endDay, activeMonth + 1));
        }
        return options;
    }

    // --- LOGIC: LOAD VÀ GOM DỮ LIỆU ---
    public void loadBudgetsData(boolean isLinkedMode, String periodType, String monthFormat, String weekFormat) {
        long[] timeRange = calculateTimeRange(periodType);
        long startTime = timeRange[0];
        long endTime = timeRange[1];

        String actualPeriod = getActualPeriod(periodType, isLinkedMode);

        if (isLinkedMode && periodType.equals("MONTH")) {
            String weekPeriod = getActualPeriod("WEEK", true);
            repository.getLinkedMonthlyCategoryBudgets(startTime, endTime, weekPeriod, plannedData ->
                    fetchRemainingData(plannedData, startTime, endTime, actualPeriod, monthFormat, weekFormat));
        } else {
            repository.getActiveBudgetsWithSpent(startTime, endTime, actualPeriod, plannedData ->
                    fetchRemainingData(plannedData, startTime, endTime, actualPeriod, monthFormat, weekFormat));
        }
    }

    private void fetchRemainingData(List<BudgetWithSpent> plannedData, long startTime, long endTime, String actualPeriod, String monthFormat, String weekFormat) {
        repository.getUnplannedExpenses(startTime, endTime, actualPeriod, unplannedData -> {
            repository.getMasterBudget(startTime, endTime, actualPeriod, master -> {
                repository.getMasterSpentAmount(startTime, endTime, masterSpent -> {
                    repository.getTotalCategoryLimits(actualPeriod, startTime, endTime, totalCatLimits -> {
                        BudgetUIState state = new BudgetUIState();
                        List<BudgetWithSpent> displayList = new ArrayList<>();

                        List<Integer> plannedCategoryIds = new ArrayList<>();

                        if (plannedData != null) {
                            for (BudgetWithSpent b : plannedData) {
                                if (b.categoryId != -1) {
                                    displayList.add(b);
                                    plannedCategoryIds.add(b.categoryId); // Ghi nhớ ID này lại
                                }
                            }
                        }

                        if (unplannedData != null) {
                            for (BudgetWithSpent u : unplannedData) {
                                if (!plannedCategoryIds.contains(u.categoryId)) {
                                    displayList.add(u);
                                }
                            }
                        }

                        state.displayList = displayList;
                        state.masterBudget = master;
                        state.masterSpent = (masterSpent != null) ? masterSpent : 0;
                        state.totalCatLimits = (totalCatLimits != null) ? totalCatLimits : 0;

                        state.monthLabel = String.format(Locale.getDefault(), monthFormat, activeMonth + 1, activeYear);

                        int startDay = (activeWeekIndex - 1) * 7 + 1;
                        Calendar cal = Calendar.getInstance();
                        cal.set(activeYear, activeMonth, 1);
                        int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                        int endDay = (activeWeekIndex == 5 || startDay + 6 > maxDays) ? maxDays : startDay + 6;
                        state.weekLabel = String.format(Locale.getDefault(), weekFormat, activeWeekIndex, startDay, activeMonth + 1, endDay, activeMonth + 1);

                        state.isReadOnly = checkIsReadOnly();

                        uiState.postValue(state);
                    });
                });
            });
        });
    }

    // --- LOGIC: KIỂM TRA CHẶN TIỀN TRƯỚC KHI LƯU ---
    public void validateAndSaveBudget(int categoryId, double limitAmount, String periodType, boolean isLinkedMode, BudgetActionCallback callback) {
        if (checkIsReadOnly()) {
            callback.onError("Past months are read-only!");
            return;
        }

        long[] timeRange = calculateTimeRange(periodType);
        long startTime = timeRange[0];
        long endTime = timeRange[1];
        String actualPeriod = getActualPeriod(periodType, isLinkedMode);

        if (isLinkedMode) {
            String weekPeriod = getActualPeriod("WEEK", true);
            if (periodType.equals("MONTH") && categoryId == -1) {
                repository.getSumOtherWeeklyMasterLimits(startTime, endTime, 0, weekPeriod, sumWeeklyMasters -> {
                    if (limitAmount < sumWeeklyMasters) callback.onError("Monthly Master Budget cannot be less than total Weekly Master Budgets (" + CurrencyFormatter.formatCompactVND(sumWeeklyMasters) + ")!");
                    else executeActualSave(categoryId, limitAmount, actualPeriod, startTime, endTime, callback);
                });
                return;
            }
            else if (periodType.equals("WEEK") && categoryId == -1) {
                long[] monthRange = calculateTimeRange("MONTH");
                long monthStart = monthRange[0];
                long monthEnd = monthRange[1];
                String monthPeriod = getActualPeriod("MONTH", true);

                repository.getMasterBudget(monthStart, monthEnd, monthPeriod, masterMonth -> {
                    long monthLimit = (masterMonth != null) ? masterMonth.limitAmount : 0;
                    if (monthLimit == 0 && limitAmount > 0) {
                        callback.onError("Please set the Monthly Master Budget first!");
                        return;
                    }
                    if (monthLimit > 0) {
                        repository.getSumOtherWeeklyMasterLimits(monthStart, monthEnd, startTime, weekPeriod, sumOthers -> {
                            if (sumOthers + limitAmount > monthLimit) callback.onError("Total Weekly Master Budgets exceed the Monthly Master (" + CurrencyFormatter.formatCompactVND(monthLimit) + ")!");
                            else executeActualSave(categoryId, limitAmount, actualPeriod, startTime, endTime, callback);
                        });
                        return;
                    }
                    executeActualSave(categoryId, limitAmount, actualPeriod, startTime, endTime, callback);
                });
                return;
            }
        }

        // LOGIC ĐỘC LẬP
        if (categoryId == -1) {
            repository.getTotalCategoryLimits(actualPeriod, startTime, endTime, totalCatLimits -> {
                if (limitAmount < totalCatLimits && !(isLinkedMode && periodType.equals("MONTH"))) {
                    callback.onError("Master Budget cannot be less than the total category budgets!");
                } else {
                    executeActualSave(categoryId, limitAmount, actualPeriod, startTime, endTime, callback);
                }
            });
        } else {
            repository.getMasterBudget(startTime, endTime, actualPeriod, master -> {
                if (master == null || master.limitAmount == 0) {
                    callback.onError("Please set the Master Budget first!");
                    return;
                }
                repository.getTotalCategoryLimitExcluding(categoryId, actualPeriod, startTime, endTime, totalOthers -> {
                    if (totalOthers + limitAmount > master.limitAmount) {
                        String overAmount = CurrencyFormatter.formatCompactVND((totalOthers + limitAmount) - master.limitAmount);
                        callback.onError("Total category budgets exceed the Master Budget (" + overAmount + ")!");
                    } else {
                        executeActualSave(categoryId, limitAmount, actualPeriod, startTime, endTime, callback);
                    }
                });
            });
        }
    }

    private void executeActualSave(int categoryId, double limitAmount, String actualPeriod, long startTime, long endTime, BudgetActionCallback callback) {
        Budget budget = new Budget();
        budget.categoryId = categoryId;
        budget.limitAmount = (long) limitAmount;
        budget.periodType = actualPeriod;
        budget.startDate = startTime;
        budget.endDate = endTime;

        repository.getBudgetByCategory(categoryId, startTime, endTime, actualPeriod, existing -> {
            if (existing != null) budget.id = existing.id;
            if (existing != null) repository.update(budget);
            else repository.insert(budget);
            callback.onSuccess("Saved successfully!");
        });
    }

    // --- LOGIC: XÓA ---
    public void deleteBudget(int categoryId, String periodType, boolean isLinkedMode, BudgetActionCallback callback) {
        if (checkIsReadOnly()) {
            callback.onError("Past months are read-only!");
            return;
        }
        String actualPeriod = getActualPeriod(periodType, isLinkedMode);
        long[] timeRange = calculateTimeRange(periodType);

        repository.getBudgetByCategory(categoryId, timeRange[0], timeRange[1], actualPeriod, budget -> {
            if (budget != null) {
                repository.delete(budget);
                callback.onSuccess("Budget deleted successfully!");
            }
        });
    }
}