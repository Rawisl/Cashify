package com.example.cashify.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.cashify.R;
import com.example.cashify.database.AppDatabase;
import com.example.cashify.database.Category;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class BudgetBottomSheetDialog extends BottomSheetDialogFragment {

    private int categoryId;
    private String title;
    private double currentSpending;
    private double currentLimit;

    private OnBudgetActionListener actionListener;
    public interface OnBudgetActionListener {
        void onSave(int selectedCategoryId, double limitAmount);
        void onDelete(int categoryId);
    }

    private TextInputEditText edtMonthlyBudget, edtWeeklyBudget, edtYearlyBudget;
    private TextInputLayout layoutMonthly;
    private LinearLayout layoutCategorySelector, layoutMasterInfo, layoutMasterPresets;
    private MaterialAutoCompleteTextView actvCategoryDropdown;
    private Button btnSaveBudget, btnDeleteBudget;
    private ImageView ivIcon;

    private int selectedCategoryIdFromDropdown = -1;
    private List<Category> expenseCategories;

    public BudgetBottomSheetDialog(int categoryId, String title, double currentSpending, double currentLimit, OnBudgetActionListener listener) {
        this.categoryId = categoryId;
        this.title = title;
        this.currentSpending = currentSpending;
        this.currentLimit = currentLimit;
        this.actionListener = listener;
    }

    @Override
    public void onStart() {
        super.onStart();
        View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_budget, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ánh xạ views
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvCurrentSpending = view.findViewById(R.id.tvCurrentSpending);
        ImageButton btnClose = view.findViewById(R.id.btnClose);
        ivIcon = view.findViewById(R.id.ivIcon);
        edtWeeklyBudget = view.findViewById(R.id.edtWeeklyBudget);
        edtMonthlyBudget = view.findViewById(R.id.edtMonthlyBudget);
        edtYearlyBudget = view.findViewById(R.id.edtYearlyBudget);
        layoutMonthly = view.findViewById(R.id.layoutInputMonthly);
        layoutCategorySelector = view.findViewById(R.id.layoutCategorySelector);
        actvCategoryDropdown = view.findViewById(R.id.actvCategoryDropdown);
        layoutMasterInfo = view.findViewById(R.id.layoutMasterInfo);
        layoutMasterPresets = view.findViewById(R.id.layoutMasterPresets);
        Button btnQuickFill = view.findViewById(R.id.btnQuickFill);
        btnSaveBudget = view.findViewById(R.id.btnSaveBudget);
        btnDeleteBudget = view.findViewById(R.id.btnDeleteBudget);

        // hiển thị nút xóa nếu là chỉnh sửa ngân sách cũ
        if (categoryId > 0) {
            btnDeleteBudget.setVisibility(View.VISIBLE);
        } else {
            btnDeleteBudget.setVisibility(View.GONE);
        }

        btnDeleteBudget.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onDelete(categoryId);
            dismiss();
        });

        // thiết lập giao diện dựa trên loại ngân sách
        if (categoryId == -1) {
            layoutCategorySelector.setVisibility(View.GONE);
            layoutMasterInfo.setVisibility(View.VISIBLE);
            layoutMasterPresets.setVisibility(View.VISIBLE);
            ivIcon.setImageResource(android.R.drawable.ic_menu_sort_by_size);
            ivIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#E91E63")));
            btnSaveBudget.setText("Save Master Budget");
            btnSaveBudget.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E91E63")));

            // thiết lập sự kiện cho các nút gợi ý master budget
            setupMasterPresets();
        } else {
            layoutMasterInfo.setVisibility(View.GONE);
            layoutMasterPresets.setVisibility(View.GONE);
            if (categoryId == 0) {
                layoutCategorySelector.setVisibility(View.VISIBLE);
                loadCategoriesFromDB();
            } else {
                layoutCategorySelector.setVisibility(View.GONE);
            }
            btnSaveBudget.setText("Save Budget");
            btnSaveBudget.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4C6FFF")));
            ivIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FF9800")));
        }

        tvTitle.setText(title);
        tvCurrentSpending.setText(String.format("%,.0f VNĐ", currentSpending));
        if (currentLimit > 0) {
            edtMonthlyBudget.setText(String.format("%.0f", currentLimit));
        }

        btnClose.setOnClickListener(v -> dismiss());

        btnQuickFill.setOnClickListener(v -> {
            calculateFromMonthly();
        });

        btnSaveBudget.setOnClickListener(v -> {
            String monthlyVal = edtMonthlyBudget.getText().toString().trim();
            if (TextUtils.isEmpty(monthlyVal)) {
                layoutMonthly.setBoxStrokeColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                Toast.makeText(getContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                return;
            }

            if (categoryId == 0 && selectedCategoryIdFromDropdown == -1) {
                Toast.makeText(getContext(), "Vui lòng chọn một danh mục!", Toast.LENGTH_SHORT).show();
                return;
            }

            double finalLimit = Double.parseDouble(monthlyVal);
            if (actionListener != null) {
                int idToSave = (categoryId == 0) ? selectedCategoryIdFromDropdown : categoryId;
                actionListener.onSave(idToSave, finalLimit);
            }
            dismiss();
        });
    }

    // xử lý sự kiện click cho các nút tiền nhanh 2tr, 3tr, 5tr
    private void setupMasterPresets() {
        for (int i = 0; i < layoutMasterPresets.getChildCount(); i++) {
            View child = layoutMasterPresets.getChildAt(i);
            if (child instanceof Button) {
                Button btnPreset = (Button) child;
                btnPreset.setOnClickListener(v -> {
                    String btnText = btnPreset.getText().toString().toLowerCase();
                    double amount = 0;
                    if (btnText.contains("2tr")) amount = 2000000;
                    else if (btnText.contains("3tr")) amount = 3000000;
                    else if (btnText.contains("5tr")) amount = 5000000;

                    if (amount > 0) {
                        edtMonthlyBudget.setText(String.valueOf((long) amount));
                        calculateFromMonthly(); // tự động cập nhật tuần và năm
                    }
                });
            }
        }
    }

    // tính toán ngân sách tuần và năm dựa trên giá trị tháng
    private void calculateFromMonthly() {
        String monthStr = edtMonthlyBudget.getText().toString();
        if (!TextUtils.isEmpty(monthStr)) {
            try {
                double monthly = Double.parseDouble(monthStr);
                edtWeeklyBudget.setText(String.format("%.0f", monthly / 4));
                edtYearlyBudget.setText(String.format("%.0f", monthly * 12));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadCategoriesFromDB() {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        new Thread(() -> {
            expenseCategories = db.categoryDao().getCategoriesByType(0);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    List<String> categoryNames = new ArrayList<>();
                    for (Category c : expenseCategories) {
                        categoryNames.add(c.name);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames);
                    actvCategoryDropdown.setAdapter(adapter);
                    actvCategoryDropdown.setOnItemClickListener((parent, view, position, id) -> {
                        selectedCategoryIdFromDropdown = expenseCategories.get(position).id;
                    });
                });
            }
        }).start();
    }
}