package com.example.cashify.ui.budget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.database.Budget;
import com.example.cashify.database.BudgetWithSpent;
import com.example.cashify.utils.NumpadBottomSheet;
import com.example.cashify.utils.CurrencyFormatter;
import com.example.cashify.viewmodel.BudgetViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class BudgetFragment extends Fragment {

    private BudgetAdapter adapter;
    private BudgetViewModel budgetViewModel;

    // Các View của thẻ Master Budget
    private TextView tvMasterTitle, tvMasterLimit, tvMasterSpent, tvMasterRemaining, tvMasterAlert;
    private TextView tvMonth, tvWeek;
    private ProgressBar pbMaster;
    private MaterialButtonToggleGroup toggleGroupPeriod;
    private MaterialButton btnWeekly, btnMonthly;
    private CardView cardMonthSelector, cardWeekSelector;

    private MaterialSwitch switchLinkedMode;
    private boolean isLinkedMode = false;
    private boolean isReadOnly = false;
    private ImageButton btnAddBudget;

    private Budget masterBudgetCache;
    private String currentPeriodType = "MONTH";
    private long mLastClickTime = 0;

    private Toast mCurrentToast;

    public BudgetFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_budget, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        budgetViewModel = new ViewModelProvider(this).get(BudgetViewModel.class);

        // Ánh xạ UI
        tvMasterTitle = view.findViewById(R.id.tvMasterTitle);
        tvMasterLimit = view.findViewById(R.id.tvMasterLimit);
        tvMasterSpent = view.findViewById(R.id.tvMasterSpent);
        tvMasterRemaining = view.findViewById(R.id.tvMasterRemaining);
        pbMaster = view.findViewById(R.id.pbMaster);
        tvMasterAlert = view.findViewById(R.id.tvMasterAlert);
        toggleGroupPeriod = view.findViewById(R.id.toggleGroupPeriod);
        btnWeekly = view.findViewById(R.id.btnWeekly);
        btnMonthly = view.findViewById(R.id.btnMonthly);
        btnAddBudget = view.findViewById(R.id.btnAddBudget);
        switchLinkedMode = view.findViewById(R.id.switchLinkedMode);

        cardMonthSelector = view.findViewById(R.id.cardMonthSelector);
        cardWeekSelector = view.findViewById(R.id.cardWeekSelector);
        tvMonth = view.findViewById(R.id.tvMonth);
        tvWeek = view.findViewById(R.id.tvWeek);

        // Quản lý Linked Mode Switch
        SharedPreferences prefs = requireContext().getSharedPreferences("BudgetPrefs", Context.MODE_PRIVATE);
        isLinkedMode = prefs.getBoolean("isLinkedMode", false);
        if (switchLinkedMode != null) {
            switchLinkedMode.setChecked(isLinkedMode);
            switchLinkedMode.setOnCheckedChangeListener((button, isChecked) -> {
                isLinkedMode = isChecked;
                prefs.edit().putBoolean("isLinkedMode", isLinkedMode).apply();
                updateToggleColors();
                triggerLoadData();
            });
        }

        // --- LẮNG NGHE SỰ KIỆN CHUYỂN TAB WEEKLY / MONTHLY ---
        toggleGroupPeriod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                currentPeriodType = (checkedId == R.id.btnWeekly) ? "WEEK" : "MONTH";
                tvMasterTitle.setText(currentPeriodType.equals("WEEK") ? getString(R.string.budget_main_card_title_weekly) : getString(R.string.budget_main_card_title_monthly));
                updateToggleColors();
                triggerLoadData();
            }
        });
        updateToggleColors();

        // Click hiện bảng chọn Tháng/Tuần
        cardMonthSelector.setOnClickListener(v -> showMonthSelectorDialog());
        cardWeekSelector.setOnClickListener(v -> showWeekSelectorDialog());

        // Lắng nghe dữ liệu (UI State) trả về từ ViewModel để vẽ
        budgetViewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            masterBudgetCache = state.masterBudget; // Lưu cache cho nút bấm
            isReadOnly = state.isReadOnly;

            tvMonth.setText(state.monthLabel);
            tvWeek.setText(state.weekLabel);
            updateToggleColors();

            adapter.setBudgets(state.displayList);
            drawMasterCard(state.masterBudget, state.masterSpent, state.totalCatLimits);
        });

        // Click Master Card
        CardView cardMaster = view.findViewById(R.id.cardMaster);
        if (cardMaster != null) {
            cardMaster.setOnClickListener(v -> {
                if (isReadOnly) { showToastOnUI("Past months are read-only!"); return; }
                if (android.os.SystemClock.elapsedRealtime() - mLastClickTime < 1000) return;
                mLastClickTime = android.os.SystemClock.elapsedRealtime();
                double limit = masterBudgetCache != null ? masterBudgetCache.limitAmount : 0;
                openNumpadToEditBudget(-1, limit);
            });
        }

        // Click (+) Thêm mới
        if (btnAddBudget != null) {
            btnAddBudget.setOnClickListener(v -> {
                if (isReadOnly) { showToastOnUI("Past months are read-only!"); return; }
                if (android.os.SystemClock.elapsedRealtime() - mLastClickTime < 1000) return;
                mLastClickTime = android.os.SystemClock.elapsedRealtime();
                List<Integer> existingIds = new ArrayList<>();
                if (adapter != null && adapter.getBudgets() != null) {
                    for (BudgetWithSpent b : adapter.getBudgets()) existingIds.add(b.categoryId);
                }
                BudgetBottomSheetDialog dialog = new BudgetBottomSheetDialog(0, "Add Budget", 0, 0, "", "", new BudgetBottomSheetDialog.OnBudgetActionListener() {
                    @Override public void onSave(int id, double limit) { triggerSaveBudget(id, limit); }
                    @Override public void onDelete(int id) {}
                });
                dialog.setDisabledCategoryIds(existingIds);
                dialog.show(getParentFragmentManager(), "AddBudget");
            });
        }

        // Danh sách RecyclerView
        RecyclerView rvBudgets = view.findViewById(R.id.rvCategoryBudgets);
        rvBudgets.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BudgetAdapter(item -> {
            if (isReadOnly) { showToastOnUI("Past months are read-only!"); return; }
            if (android.os.SystemClock.elapsedRealtime() - mLastClickTime < 1000) return;
            mLastClickTime = android.os.SystemClock.elapsedRealtime();

            if (isLinkedMode && currentPeriodType.equals("MONTH")) {
                showToastOnUI("Please edit category budgets in the Weekly tab!");
                return;
            }

            String title = (item.categoryName != null) ? item.categoryName : "Category " + item.categoryId;

            BudgetBottomSheetDialog dialog = new BudgetBottomSheetDialog(item.categoryId, title, item.spentAmount, item.limitAmount, item.categoryIcon, item.categoryColor, new BudgetBottomSheetDialog.OnBudgetActionListener() {
                @Override public void onSave(int id, double limit) { triggerSaveBudget(id, limit); }
                @Override public void onDelete(int id) { triggerDeleteBudget(id); }
            });
            dialog.show(getParentFragmentManager(), "EditBudget");
        });
        rvBudgets.setAdapter(adapter);

        // Kích nổ lần đầu
        triggerLoadData();
    }

    private void triggerLoadData() {
        String monthFormat = getString(R.string.budget_month_format);
        String weekFormat = getString(R.string.budget_week_format);
        budgetViewModel.loadBudgetsData(isLinkedMode, currentPeriodType, monthFormat, weekFormat);
    }

    private void showMonthSelectorDialog() {
        String monthFormat = getString(R.string.budget_month_format);
        List<String> months = budgetViewModel.getAvailableMonths(monthFormat);
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.budget_date_picker_month))
                .setItems(months.toArray(new String[0]), (dialog, which) -> {
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.MONTH, -24 + which);
                    budgetViewModel.updateSelectedMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));
                    triggerLoadData();
                }).show();
    }

    private void showWeekSelectorDialog() {
        String weekFormat = getString(R.string.budget_week_format);
        List<String> weeks = budgetViewModel.getAvailableWeeks(weekFormat);
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.budget_date_picker_week))
                .setItems(weeks.toArray(new String[0]), (dialog, which) -> {
                    budgetViewModel.updateSelectedWeek(which + 1);
                    triggerLoadData();
                }).show();
    }

    // --- HÀM GỌI VIEWMODEL ĐỂ XỬ LÝ LOGIC ---
    private void triggerSaveBudget(int categoryId, double limitAmount) {
        budgetViewModel.validateAndSaveBudget(categoryId, limitAmount, currentPeriodType, isLinkedMode, new BudgetViewModel.BudgetActionCallback() {
            @Override
            public void onSuccess(String message) {
                showToastOnUI(message);
                triggerLoadData();
            }
            @Override
            public void onError(String error) {
                showToastOnUI(error);
            }
        });
    }

    private void triggerDeleteBudget(int categoryId) {
        budgetViewModel.deleteBudget(categoryId, currentPeriodType, isLinkedMode, new BudgetViewModel.BudgetActionCallback() {
            @Override
            public void onSuccess(String message) {
                showToastOnUI(message);
                triggerLoadData();
            }
            @Override
            public void onError(String error) {
                showToastOnUI(error);
            }
        });
    }

    // --- HÀM VẼ GIAO DIỆN CHUẨN ---
    private void drawMasterCard(Budget masterBudget, long masterSpent, long totalCatLimits) {
        if (masterBudget != null) {
            long limit = masterBudget.limitAmount;
            long remaining = limit - masterSpent;
            int percent = limit > 0 ? (int) ((masterSpent * 100) / limit) : 0;

            tvMasterLimit.setText(CurrencyFormatter.formatFullVND(limit));
            tvMasterSpent.setText(CurrencyFormatter.formatFullVND(masterSpent));
            tvMasterRemaining.setText(CurrencyFormatter.formatFullVND(remaining));
            pbMaster.setProgress(Math.min(percent, 100));

            tvMasterAlert.setVisibility(View.VISIBLE);
            if (totalCatLimits < limit && !(isLinkedMode && currentPeriodType.equals("MONTH"))) {
                tvMasterAlert.setText("Ngân sách tổng còn trống " + CurrencyFormatter.formatCompactVND(limit - totalCatLimits));
                tvMasterAlert.setTextColor(Color.WHITE);
            } else if (totalCatLimits > limit && !(isLinkedMode && currentPeriodType.equals("MONTH"))) {
                tvMasterAlert.setText("Ngân sách danh mục vượt mức tổng " + CurrencyFormatter.formatCompactVND(totalCatLimits - limit));
                tvMasterAlert.setTextColor(Color.YELLOW);
            } else {
                tvMasterAlert.setText(CurrencyFormatter.formatCompactVND(Math.abs(remaining)) + (remaining >= 0 ? " left" : " over"));
                tvMasterAlert.setTextColor(Color.GREEN);
            }
        } else {
            tvMasterLimit.setText(CurrencyFormatter.formatFullVND(0));
            tvMasterSpent.setText(CurrencyFormatter.formatFullVND(masterSpent));
            tvMasterRemaining.setText(CurrencyFormatter.formatFullVND(0));
            pbMaster.setProgress(0);
            tvMasterAlert.setVisibility(View.GONE);
        }
    }

    private void updateToggleColors() {
        if (getContext() == null) return;
        int colorGreen = ContextCompat.getColor(requireContext(), R.color.status_green);
        int colorWhite = ContextCompat.getColor(requireContext(), R.color.white);
        int colorBrand = ContextCompat.getColor(requireContext(), R.color.brand_primary);
        int colorTransparent = Color.TRANSPARENT;

        if (btnAddBudget != null) {
            boolean hideAddBtn = (isLinkedMode && currentPeriodType.equals("MONTH")) || isReadOnly;
            btnAddBudget.setVisibility(hideAddBtn ? View.GONE : View.VISIBLE);
            btnAddBudget.setAlpha(isReadOnly ? 0.5f : 1.0f);
        }

        if (cardWeekSelector != null) {
            cardWeekSelector.setVisibility(currentPeriodType.equals("WEEK") ? View.VISIBLE : View.GONE);
        }

        btnWeekly.setBackgroundTintList(ColorStateList.valueOf(currentPeriodType.equals("WEEK") ? colorGreen : colorTransparent));
        btnWeekly.setTextColor(currentPeriodType.equals("WEEK") ? colorWhite : colorBrand);

        btnMonthly.setBackgroundTintList(ColorStateList.valueOf(currentPeriodType.equals("MONTH") ? colorGreen : colorTransparent));
        btnMonthly.setTextColor(currentPeriodType.equals("MONTH") ? colorWhite : colorBrand);
    }

    private void openNumpadToEditBudget(int categoryId, double currentLimit) {
        NumpadBottomSheet numpad = new NumpadBottomSheet();
        numpad.setInitialAmount(String.valueOf((long) currentLimit));
        numpad.setListener((raw, formatted) -> triggerSaveBudget(categoryId, Double.parseDouble(raw)));
        numpad.show(getChildFragmentManager(), "NumpadBottomSheet");
    }

    private void showToastOnUI(String msg) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (mCurrentToast != null) {
                    mCurrentToast.cancel();
                }
                mCurrentToast = Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT);
                mCurrentToast.show();
            });
        }    }
}