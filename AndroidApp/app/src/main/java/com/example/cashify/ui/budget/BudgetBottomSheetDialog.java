package com.example.cashify.ui.budget;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.local.AppDatabase;
import com.example.cashify.data.model.Category;
import com.example.cashify.utils.CurrencyFormatter;
import com.example.cashify.utils.CurrencyManager;
import com.example.cashify.utils.NumpadBottomSheet;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class BudgetBottomSheetDialog extends BottomSheetDialogFragment {

    private final int categoryId;
    private final String title;
    private final double currentSpending;
    private final double currentLimit;
    private final String iconName;
    private final String colorCode;

    // Đã trả lại biến cũ
    private List<Integer> disabledCategoryIds = new ArrayList<>();
    private OnBudgetActionListener actionListener;

    private TextInputEditText edtBudgetAmount;
    private TextInputLayout layoutInputAmount;
    private LinearLayout layoutCategorySelector;
    private Button btnSaveBudget, btnDeleteBudget;
    private ImageView ivIcon;

    private int selectedCategoryIdFromDropdown = -1;

    public interface OnBudgetActionListener {
        void onSave(int selectedCategoryId, double limitAmount);
        void onDelete(int categoryId);
    }

    public BudgetBottomSheetDialog(int categoryId, String title, double currentSpending,
                                   double currentLimit, String iconName, String colorCode,
                                   OnBudgetActionListener listener) {
        this.categoryId = categoryId;
        this.title = title;
        this.currentSpending = currentSpending;
        this.currentLimit = currentLimit;
        this.iconName = iconName;
        this.colorCode = colorCode;
        this.actionListener = listener;
    }

    // Đã trả lại hàm cũ để Fragment gọi không bị lỗi
    public void setDisabledCategoryIds(List<Integer> ids) {
        this.disabledCategoryIds = ids != null ? ids : new ArrayList<>();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
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

        initViews(view);
        setupHeaderUI();
        setupInputHandlers();
        setupCategoryPickerUI();
        setupActionButtons();
    }

    private void initViews(View view) {
        ivIcon = view.findViewById(R.id.ivIcon);
        edtBudgetAmount = view.findViewById(R.id.edtBudgetAmount);
        layoutInputAmount = view.findViewById(R.id.layoutInputAmount);
        layoutCategorySelector = view.findViewById(R.id.layoutCategorySelector);
        btnSaveBudget = view.findViewById(R.id.btnSaveBudget);
        btnDeleteBudget = view.findViewById(R.id.btnDeleteBudget);

        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvCurrentSpending = view.findViewById(R.id.tvCurrentSpending);
        ImageButton btnClose = view.findViewById(R.id.btnClose);

        tvTitle.setText(title);
        tvCurrentSpending.setText(CurrencyFormatter.formatFullVND(currentSpending));
        layoutInputAmount.setPrefixText(CurrencyManager.isUsd() ? "$" : "\u0111");
        layoutInputAmount.setSuffixText(null);

        btnClose.setOnClickListener(v -> dismiss());
    }

    private void setupHeaderUI() {
        if (categoryId > 0) {
            btnDeleteBudget.setVisibility(View.VISIBLE);
            String currentIconName = (iconName != null && !iconName.isEmpty()) ? iconName : "ic_food";
            int currentResId = requireContext().getResources().getIdentifier(currentIconName, "drawable", requireContext().getPackageName());
            ivIcon.setImageResource(currentResId != 0 ? currentResId : R.drawable.ic_food);

            try {
                String cCode = (colorCode != null && !colorCode.isEmpty()) ? colorCode : "#4C6FFF";
                ivIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor(cCode)));
            } catch (Exception e) {
                ivIcon.setImageTintList(ColorStateList.valueOf(Color.GRAY));
            }
        } else {
            btnDeleteBudget.setVisibility(View.GONE);
            ivIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FF9800")));
        }

        if (currentLimit > 0) {
            edtBudgetAmount.setText(CurrencyFormatter.formatDoubleToVND(currentLimit));
        }
    }

    private void setupInputHandlers() {
        edtBudgetAmount.setFocusable(false);
        edtBudgetAmount.setFocusableInTouchMode(false);

        edtBudgetAmount.setOnClickListener(v -> {
            NumpadBottomSheet numpad = new NumpadBottomSheet();
            String currentAmt = edtBudgetAmount.getText() != null ? edtBudgetAmount.getText().toString() : "0";
            if (currentAmt.isEmpty()) currentAmt = "0";

            numpad.setInitialAmount(currentAmt);
            numpad.setListener((rawAmount, formattedAmount) -> edtBudgetAmount.setText(rawAmount));
            numpad.show(getParentFragmentManager(), "NumpadFromBudget");
        });
    }

    private void setupCategoryPickerUI() {
        if (categoryId == 0) {
            layoutCategorySelector.setVisibility(View.VISIBLE);
            loadCategoriesSafely(); // Gọi hàm load DB an toàn
        } else {
            layoutCategorySelector.setVisibility(View.GONE);
        }
    }

    // =========================================================================
    // DATABASE SAFE LOADING
    // =========================================================================
    private void loadCategoriesSafely() {
        new Thread(() -> {
            // Check context null safety before querying DB
            if (getContext() == null) return;

            List<Category> allCategories = AppDatabase.getInstance(requireContext())
                    .categoryDao().getCategoriesByType(0);

            List<Category> available = new ArrayList<>();
            for (Category c : allCategories) {
                if (!disabledCategoryIds.contains(c.id)) {
                    available.add(c);
                }
            }

            // UI Thread null safety check
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    View view = getView();
                    if (view != null) {
                        RecyclerView rvCategoryPicker = view.findViewById(R.id.rvCategoryPicker);
                        InnerCategoryGridAdapter adapter = new InnerCategoryGridAdapter(available);
                        rvCategoryPicker.setAdapter(adapter);
                    }
                });
            }
        }).start();
    }

    private void setupActionButtons() {
        btnSaveBudget.setText(R.string.action_save);
        btnSaveBudget.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.brand_primary)));

        btnDeleteBudget.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onDelete(categoryId);
            dismiss();
        });

        btnSaveBudget.setOnClickListener(v -> {
            String amountVal = edtBudgetAmount.getText() != null ? edtBudgetAmount.getText().toString().trim() : "";

            if (TextUtils.isEmpty(amountVal)) {
                layoutInputAmount.setBoxStrokeColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                ToastHelper.show(getContext(), "Please enter a valid money amount.");
                return;
            }

            if (categoryId == 0 && selectedCategoryIdFromDropdown == -1) {
                ToastHelper.show(getContext(), "Please select a category.");
                return;
            }

            double finalLimit = CurrencyFormatter.parseVNDToDouble(amountVal);
            if (actionListener != null) {
                int idToSave = (categoryId == 0) ? selectedCategoryIdFromDropdown : categoryId;
                actionListener.onSave(idToSave, finalLimit);
            }
            dismiss();
        });
    }

    // =========================================================================
    // INNER ADAPTER FOR CATEGORY SELECTION
    // =========================================================================

    private class InnerCategoryGridAdapter extends RecyclerView.Adapter<InnerCategoryGridAdapter.ViewHolder> {
        private final List<Category> categories;
        private int selectedPos = -1;

        public InnerCategoryGridAdapter(List<Category> categories) {
            this.categories = categories;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_picker, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Category cat = categories.get(position);
            holder.tvCatName.setText(cat.name);

            String iconName = (cat.iconName != null && !cat.iconName.isEmpty()) ? cat.iconName : "ic_food";
            int resId = holder.itemView.getContext().getResources().getIdentifier(iconName, "drawable", holder.itemView.getContext().getPackageName());
            holder.imgCatIcon.setImageResource(resId != 0 ? resId : R.drawable.ic_food);

            try {
                String colorStr = (cat.colorCode != null && !cat.colorCode.isEmpty()) ? cat.colorCode : "#4C6FFF";
                holder.imgCatIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor(colorStr)));
            } catch (Exception e) {
                holder.imgCatIcon.setImageTintList(ColorStateList.valueOf(Color.GRAY));
            }

            holder.imgCatIcon.setBackgroundResource(position == selectedPos ? R.drawable.bg_circle_button : R.drawable.bg_circle_icon);
            holder.itemView.setAlpha(1.0f);

            holder.itemView.setOnClickListener(v -> {
                int oldPos = selectedPos;
                selectedPos = holder.getAdapterPosition();
                selectedCategoryIdFromDropdown = cat.id;

                TextView tvTitle = getDialog() != null ? getDialog().findViewById(R.id.tvTitle) : null;
                if (tvTitle != null) {
                    tvTitle.setText("Add " + cat.name);
                }

                if (ivIcon != null) {
                    ivIcon.setImageResource(resId != 0 ? resId : R.drawable.ic_food);
                    try {
                        String colorStr = (cat.colorCode != null && !cat.colorCode.isEmpty()) ? cat.colorCode : "#4C6FFF";
                        ivIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor(colorStr)));
                    } catch (Exception e) {
                        ivIcon.setImageTintList(ColorStateList.valueOf(Color.GRAY));
                    }
                }

                notifyItemChanged(oldPos);
                notifyItemChanged(selectedPos);
            });
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgCatIcon;
            TextView tvCatName;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                imgCatIcon = itemView.findViewById(R.id.imgCatIcon);
                tvCatName = itemView.findViewById(R.id.tvCatName);
            }
        }
    }
}