package com.example.cashify.ui.budget;

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
import java.util.List;

public class BudgetFragment extends Fragment {

    private BudgetAdapter adapter;
    private BudgetViewModel budgetViewModel;

    private TextView tvMasterTitle, tvMasterLimit, tvMasterSpent, tvMasterRemaining, tvMasterAlert;
    private ProgressBar pbMaster;
    private MaterialButtonToggleGroup toggleGroupPeriod;
    private MaterialButton btnWeekly, btnMonthly;

    private MaterialSwitch switchLinkedMode;
    private boolean isLinkedMode = false;
    private ImageButton btnAddBudget;

    private Budget masterBudgetCache; // Lưu tạm để truyền vào Numpad
    private String currentPeriodType = "MONTH";
    private long mLastClickTime = 0;

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

        // 1. Quản lý Linked Mode Switch
        SharedPreferences prefs = requireContext().getSharedPreferences("BudgetPrefs", Context.MODE_PRIVATE);
        isLinkedMode = prefs.getBoolean("isLinkedMode", false);
        if (switchLinkedMode != null) {
            switchLinkedMode.setChecked(isLinkedMode);
            switchLinkedMode.setOnCheckedChangeListener((button, isChecked) -> {
                isLinkedMode = isChecked;
                prefs.edit().putBoolean("isLinkedMode", isLinkedMode).apply();
                updateToggleColors();
                budgetViewModel.loadBudgetsData(isLinkedMode, currentPeriodType);
            });
        }

        // 2. Chuyển Tab Tuần/Tháng
        toggleGroupPeriod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                currentPeriodType = (checkedId == R.id.btnWeekly) ? "WEEK" : "MONTH";
                tvMasterTitle.setText(currentPeriodType.equals("WEEK") ? "Weekly Master Budget" : "Monthly Master Budget");
                updateToggleColors();
                budgetViewModel.loadBudgetsData(isLinkedMode, currentPeriodType);
            }
        });
        updateToggleColors();

        // 3. Lắng nghe dữ liệu (UI State) trả về từ ViewModel để vẽ
        budgetViewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            masterBudgetCache = state.masterBudget; // Lưu cache cho nút bấm
            adapter.setBudgets(state.displayList);
            drawMasterCard(state.masterBudget, state.masterSpent, state.totalCatLimits);
        });

        // 4. Click Master Card
        CardView cardMaster = view.findViewById(R.id.cardMaster);
        if (cardMaster != null) {
            cardMaster.setOnClickListener(v -> {
                if (android.os.SystemClock.elapsedRealtime() - mLastClickTime < 1000) return;
                mLastClickTime = android.os.SystemClock.elapsedRealtime();
                double limit = masterBudgetCache != null ? masterBudgetCache.limitAmount : 0;
                openNumpadToEditBudget(-1, limit);
            });
        }

        // 5. Click (+) Thêm mới
        if (btnAddBudget != null) {
            btnAddBudget.setOnClickListener(v -> {
                if (android.os.SystemClock.elapsedRealtime() - mLastClickTime < 1000) return;
                mLastClickTime = android.os.SystemClock.elapsedRealtime();
                List<Integer> existingIds = new ArrayList<>();
                if (adapter != null && adapter.getBudgets() != null) {
                    for (BudgetWithSpent b : adapter.getBudgets()) existingIds.add(b.categoryId);
                }
                BudgetBottomSheetDialog dialog = new BudgetBottomSheetDialog(0, "Thêm ngân sách", 0, 0, new BudgetBottomSheetDialog.OnBudgetActionListener() {
                    @Override public void onSave(int id, double limit) { triggerSaveBudget(id, limit); }
                    @Override public void onDelete(int id) {}
                });
                dialog.setDisabledCategoryIds(existingIds);
                dialog.show(getParentFragmentManager(), "AddBudget");
            });
        }

        // 6. Danh sách RecyclerView
        RecyclerView rvBudgets = view.findViewById(R.id.rvCategoryBudgets);
        rvBudgets.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BudgetAdapter(item -> {
            if (android.os.SystemClock.elapsedRealtime() - mLastClickTime < 1000) return;
            mLastClickTime = android.os.SystemClock.elapsedRealtime();

            if (isLinkedMode && currentPeriodType.equals("MONTH")) {
                showToastOnUI("Hãy sửa chi tiết ngân sách trong tab Weekly!");
                return;
            }

            String title = (item.categoryName != null) ? item.categoryName : "Danh mục " + item.categoryId;
            BudgetBottomSheetDialog dialog = new BudgetBottomSheetDialog(item.categoryId, title, item.spentAmount, item.limitAmount, new BudgetBottomSheetDialog.OnBudgetActionListener() {
                @Override public void onSave(int id, double limit) { triggerSaveBudget(id, limit); }
                @Override public void onDelete(int id) { triggerDeleteBudget(id); }
            });
            dialog.show(getParentFragmentManager(), "EditBudget");
        });
        rvBudgets.setAdapter(adapter);

        // Khởi động load lần đầu
        budgetViewModel.loadBudgetsData(isLinkedMode, currentPeriodType);
    }

    // --- HÀM GỌI VIEWMODEL ĐỂ XỬ LÝ LOGIC ---
    private void triggerSaveBudget(int categoryId, double limitAmount) {
        budgetViewModel.validateAndSaveBudget(categoryId, limitAmount, currentPeriodType, isLinkedMode, new BudgetViewModel.BudgetActionCallback() {
            @Override
            public void onSuccess(String message) {
                showToastOnUI(message);
                budgetViewModel.loadBudgetsData(isLinkedMode, currentPeriodType);
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
                budgetViewModel.loadBudgetsData(isLinkedMode, currentPeriodType);
            }
            @Override
            public void onError(String error) {}
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
            btnAddBudget.setVisibility((isLinkedMode && currentPeriodType.equals("MONTH")) ? View.GONE : View.VISIBLE);
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
        numpad.show(getChildFragmentManager(), "Numpad");
    }

    private void showToastOnUI(String msg) {
        if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show());
    }
}