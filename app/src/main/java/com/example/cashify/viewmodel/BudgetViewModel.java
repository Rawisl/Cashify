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

public class BudgetViewModel extends AndroidViewModel {

    private final BudgetRepository repository;

    public static class BudgetUIState {
        public List<BudgetWithSpent> displayList;
        public Budget masterBudget;
        public long masterSpent;
        public long totalCatLimits;
    }

    private final MutableLiveData<BudgetUIState> uiState = new MutableLiveData<>();
    public LiveData<BudgetUIState> getUiState() { return uiState; }

    public interface BudgetActionCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public BudgetViewModel(@NonNull Application application) {
        super(application);
        repository = new BudgetRepository(application);
    }

    // --- HÀM TẠO NHÃN DÁN ĐỂ TÁCH BIỆT DỮ LIỆU ---
    private String getActualPeriod(String periodType, boolean isLinkedMode) {
        return isLinkedMode ? periodType + "_LINKED" : periodType;
    }

    // --- LOGIC: TÍNH TOÁN THỜI GIAN (ĐÃ DỜI TỪ FRAGMENT SANG) ---
    private long[] calculateTimeRange(String periodType) {
        Calendar cal = Calendar.getInstance();
        long startTime, endTime;
        if (periodType.equals("WEEK")) {
            cal.setFirstDayOfWeek(Calendar.MONDAY);
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
            startTime = cal.getTimeInMillis();
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
            endTime = cal.getTimeInMillis();
        } else {
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
            startTime = cal.getTimeInMillis();
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
            endTime = cal.getTimeInMillis();
        }
        return new long[]{startTime, endTime};
    }

    // --- LOGIC: LOAD VÀ GOM DỮ LIỆU ---
    public void loadBudgetsData(boolean isLinkedMode, String periodType) {
        long now = System.currentTimeMillis();
        long[] timeRange = calculateTimeRange(periodType);
        long startTime = timeRange[0];
        long endTime = timeRange[1];

        String actualPeriod = getActualPeriod(periodType, isLinkedMode);

        if (isLinkedMode && periodType.equals("MONTH")) {
            String weekPeriod = getActualPeriod("WEEK", true);
            repository.getLinkedMonthlyCategoryBudgets(startTime, endTime, weekPeriod, plannedData ->
                    fetchRemainingData(plannedData, startTime, endTime, now, actualPeriod));
        } else {
            repository.getActiveBudgetsWithSpent(now, actualPeriod, plannedData ->
                    fetchRemainingData(plannedData, startTime, endTime, now, actualPeriod));
        }
    }

    private void fetchRemainingData(List<BudgetWithSpent> plannedData, long startTime, long endTime, long now, String periodType) {
        repository.getUnplannedExpenses(startTime, endTime, now, periodType, unplannedData -> {
            repository.getMasterBudget(now, periodType, master -> {
                repository.getMasterSpentAmount(startTime, endTime, masterSpent -> {
                    repository.getTotalCategoryLimits(periodType, startTime, endTime, totalCatLimits -> {

                        BudgetUIState state = new BudgetUIState();
                        List<BudgetWithSpent> displayList = new ArrayList<>();
                        if (plannedData != null) {
                            for (BudgetWithSpent b : plannedData) if (b.categoryId != -1) displayList.add(b);
                        }
                        if (unplannedData != null) displayList.addAll(unplannedData);

                        state.displayList = displayList;
                        state.masterBudget = master;
                        state.masterSpent = masterSpent;
                        state.totalCatLimits = totalCatLimits;

                        uiState.postValue(state);
                    });
                });
            });
        });
    }

    // --- LOGIC: KIỂM TRA CHẶN TIỀN TRƯỚC KHI LƯU ---
    public void validateAndSaveBudget(int categoryId, double limitAmount, String periodType, boolean isLinkedMode, BudgetActionCallback callback) {
        long[] timeRange = calculateTimeRange(periodType);
        long startTime = timeRange[0];
        long endTime = timeRange[1];
        String actualPeriod = getActualPeriod(periodType, isLinkedMode);

        if (isLinkedMode) {
            String weekPeriod = getActualPeriod("WEEK", true);
            if (periodType.equals("MONTH") && categoryId == -1) {
                repository.getSumOtherWeeklyMasterLimits(startTime, endTime, 0, weekPeriod, sumWeeklyMasters -> {
                    if (limitAmount < sumWeeklyMasters) callback.onError("Master Tháng không được nhỏ hơn tổng các Master Tuần (" + CurrencyFormatter.formatCompactVND(sumWeeklyMasters) + ")!");
                    else executeActualSave(categoryId, limitAmount, actualPeriod, startTime, endTime, callback);
                });
                return;
            }
            else if (periodType.equals("WEEK") && categoryId == -1) {
                Calendar monthCal = Calendar.getInstance();
                monthCal.setTimeInMillis(startTime); monthCal.set(Calendar.DAY_OF_MONTH, 1);
                monthCal.set(Calendar.HOUR_OF_DAY, 0); monthCal.set(Calendar.MINUTE, 0); monthCal.set(Calendar.SECOND, 0);
                long monthStart = monthCal.getTimeInMillis();
                monthCal.set(Calendar.DAY_OF_MONTH, monthCal.getActualMaximum(Calendar.DAY_OF_MONTH));
                monthCal.set(Calendar.HOUR_OF_DAY, 23); monthCal.set(Calendar.MINUTE, 59); monthCal.set(Calendar.SECOND, 59);
                long monthEnd = monthCal.getTimeInMillis();

                String monthPeriod = getActualPeriod("MONTH", true);

                repository.getMasterBudget(monthStart, monthPeriod, masterMonth -> {
                    long monthLimit = (masterMonth != null) ? masterMonth.limitAmount : 0;
                    if (monthLimit == 0 && limitAmount > 0) {
                        callback.onError("Vui lòng cài đặt Master Tháng trước khi phân bổ cho Tuần!");
                        return;
                    }
                    if (monthLimit > 0) {
                        repository.getSumOtherWeeklyMasterLimits(monthStart, monthEnd, startTime, weekPeriod, sumOthers -> {
                            if (sumOthers + limitAmount > monthLimit) callback.onError("Tổng Master Tuần vượt quá Master Tháng (" + CurrencyFormatter.formatCompactVND(monthLimit) + ")!");
                            else executeActualSave(categoryId, limitAmount, actualPeriod, startTime, endTime, callback);
                        });
                        return;
                    }
                    executeActualSave(categoryId, limitAmount, actualPeriod, startTime, endTime, callback);
                });
                return;
            }
        }

        // LOGIC ĐỘC LẬP HOÀN TOÀN
        if (categoryId == -1) {
            repository.getTotalCategoryLimits(actualPeriod, startTime, endTime, totalCatLimits -> {
                if (limitAmount < totalCatLimits && !(isLinkedMode && periodType.equals("MONTH"))) {
                    callback.onError("Master Budget không được nhỏ hơn tổng danh mục con!");
                } else {
                    executeActualSave(categoryId, limitAmount, actualPeriod, startTime, endTime, callback);
                }
            });
        } else {
            repository.getMasterBudget(startTime, actualPeriod, master -> {
                if (master == null || master.limitAmount == 0) {
                    callback.onError("Vui lòng cài đặt Master Budget trước khi phân bổ cho danh mục!");
                    return;
                }
                repository.getTotalCategoryLimitExcluding(categoryId, actualPeriod, startTime, endTime, totalOthers -> {
                    if (totalOthers + limitAmount > master.limitAmount) {
                        String overAmount = CurrencyFormatter.formatCompactVND((totalOthers + limitAmount) - master.limitAmount);
                        callback.onError("Tổng ngân sách danh mục vượt quá Master Budget (" + overAmount + ")!");
                    } else {
                        executeActualSave(categoryId, limitAmount, actualPeriod, startTime, endTime, callback);
                    }
                });
            });
        }
    }

    private void executeActualSave(int categoryId, double limitAmount, String periodType, long startTime, long endTime, BudgetActionCallback callback) {
        Budget budget = new Budget();
        budget.categoryId = categoryId;
        budget.limitAmount = (long) limitAmount;
        budget.periodType = periodType;
        budget.startDate = startTime;
        budget.endDate = endTime;

        repository.getBudgetByCategory(categoryId, System.currentTimeMillis(), periodType, existing -> {
            if (existing != null) budget.id = existing.id;
            if (existing != null) repository.update(budget);
            else repository.insert(budget);
            callback.onSuccess("Đã lưu thành công!");
        });
    }

    // --- LOGIC: XÓA ---
    public void deleteBudget(int categoryId, String periodType, boolean isLinkedMode, BudgetActionCallback callback) {
        String actualPeriod = getActualPeriod(periodType, isLinkedMode);
        repository.getBudgetByCategory(categoryId, System.currentTimeMillis(), actualPeriod, budget -> {
            if (budget != null) {
                repository.delete(budget);
                callback.onSuccess("Đã xóa hạn mức ngân sách!");
            }
        });
    }
}