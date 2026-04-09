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
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.cashify.R;
import com.example.cashify.database.AppDatabase;
import com.example.cashify.database.Category;
import com.example.cashify.utils.NumpadBottomSheet;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class BudgetBottomSheetDialog extends BottomSheetDialogFragment {

    private int categoryId;
    private String title;
    private double currentSpending;
    private double currentLimit;

    private List<Integer> disabledCategoryIds = new ArrayList<>();
    private OnBudgetActionListener actionListener;
    public interface OnBudgetActionListener {
        void onSave(int selectedCategoryId, double limitAmount);
        void onDelete(int categoryId);
    }

    private TextInputEditText edtBudgetAmount;
    private TextInputLayout layoutInputAmount;
    private LinearLayout layoutCategorySelector, layoutMasterInfo;
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

    public void setDisabledCategoryIds(List<Integer> ids) {
        this.disabledCategoryIds = ids;
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

        // Ánh xạ views
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvCurrentSpending = view.findViewById(R.id.tvCurrentSpending);
        ImageButton btnClose = view.findViewById(R.id.btnClose);
        ivIcon = view.findViewById(R.id.ivIcon);

        edtBudgetAmount = view.findViewById(R.id.edtBudgetAmount);
        layoutInputAmount = view.findViewById(R.id.layoutInputAmount);

        layoutCategorySelector = view.findViewById(R.id.layoutCategorySelector);
        layoutMasterInfo = view.findViewById(R.id.layoutMasterInfo);

        btnSaveBudget = view.findViewById(R.id.btnSaveBudget);
        btnDeleteBudget = view.findViewById(R.id.btnDeleteBudget);

        if (categoryId > 0) {
            btnDeleteBudget.setVisibility(View.VISIBLE);
        } else {
            btnDeleteBudget.setVisibility(View.GONE);
        }

        btnDeleteBudget.setOnClickListener(v -> {
            if (actionListener != null) actionListener.onDelete(categoryId);
            dismiss();
        });

        // Khóa không cho hiện bàn phím hệ thống
        edtBudgetAmount.setFocusable(false);
        edtBudgetAmount.setFocusableInTouchMode(false);

        // Bắt sự kiện click để gọi Numpad
        edtBudgetAmount.setOnClickListener(v -> {
            NumpadBottomSheet numpad = new NumpadBottomSheet();

            String currentAmt = edtBudgetAmount.getText().toString();
            if (currentAmt.isEmpty()) {
                currentAmt = "0";
            }
            numpad.setInitialAmount(currentAmt);

            numpad.setListener((rawAmount, formattedAmount) -> {
                edtBudgetAmount.setText(rawAmount);
            });

            numpad.show(getParentFragmentManager(), "NumpadFromCategory");
        });

        if (categoryId == -1) {
            // Giao diện Master Budget
            layoutCategorySelector.setVisibility(View.GONE);
            layoutMasterInfo.setVisibility(View.VISIBLE);
            ivIcon.setImageResource(android.R.drawable.ic_menu_sort_by_size);
            ivIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#E91E63")));
            btnSaveBudget.setText("Save Master Budget");
            btnSaveBudget.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E91E63")));
        } else {
            // Giao diện Category Budget
            layoutMasterInfo.setVisibility(View.GONE);

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
            edtBudgetAmount.setText(String.format("%.0f", currentLimit));
        }

        btnClose.setOnClickListener(v -> dismiss());

        btnSaveBudget.setOnClickListener(v -> {
            String amountVal = edtBudgetAmount.getText().toString().trim();
            if (TextUtils.isEmpty(amountVal)) {
                layoutInputAmount.setBoxStrokeColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                Toast.makeText(getContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                return;
            }

            if (categoryId == 0 && selectedCategoryIdFromDropdown == -1) {
                Toast.makeText(getContext(), "Vui lòng chọn một danh mục!", Toast.LENGTH_SHORT).show();
                return;
            }

            double finalLimit = Double.parseDouble(amountVal);
            if (actionListener != null) {
                int idToSave = (categoryId == 0) ? selectedCategoryIdFromDropdown : categoryId;
                actionListener.onSave(idToSave, finalLimit);
            }
            dismiss();
        });
    }

    private void loadCategoriesFromDB() {
        AppDatabase db = AppDatabase.getInstance(requireContext());
        new Thread(() -> {
            // Lấy tất cả danh mục chi tiêu từ DB
            List<Category> allCategories = db.categoryDao().getCategoriesByType(0);

            // Lọc: Chỉ giữ lại những danh mục CHƯA có budget
            List<Category> availableCategories = new ArrayList<>();
            for (Category c : allCategories) {
                if (!disabledCategoryIds.contains(c.id)) {
                    availableCategories.add(c);
                }
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // Ánh xạ lại RecyclerView mới tạo bên giao diện
                    RecyclerView rvCategoryPicker = getDialog().findViewById(R.id.rvCategoryPicker);

                    InnerCategoryGridAdapter adapter = new InnerCategoryGridAdapter(availableCategories);
                    rvCategoryPicker.setAdapter(adapter);
                });
            }
        }).start();
    }

    private class InnerCategoryGridAdapter extends RecyclerView.Adapter<InnerCategoryGridAdapter.ViewHolder> {
        private final List<Category> categories;
        private int selectedPos = -1; // Lưu vị trí đang được bấm chọn

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

            // Load Icon
            String iconName = (cat.iconName != null && !cat.iconName.isEmpty()) ? cat.iconName : "ic_food";
            int resId = holder.itemView.getContext().getResources().getIdentifier(iconName, "drawable", holder.itemView.getContext().getPackageName());
            holder.imgCatIcon.setImageResource(resId != 0 ? resId : R.drawable.ic_food);

            // Load Color
            try {
                String colorStr = (cat.colorCode != null && !cat.colorCode.isEmpty()) ? cat.colorCode : "#4C6FFF";
                int color = Color.parseColor(colorStr);
                holder.imgCatIcon.setImageTintList(ColorStateList.valueOf(color));
            } catch (Exception e) {
                holder.imgCatIcon.setImageTintList(ColorStateList.valueOf(Color.GRAY));
            }

            if (position == selectedPos) {
                // Khi được chọn: Dùng file bg_circle_button (có viền đen)
                holder.imgCatIcon.setBackgroundResource(R.drawable.bg_circle_button);
            } else {
                // Khi không được chọn: Dùng file bg_circle_icon (nền xám bình thường)
                holder.imgCatIcon.setBackgroundResource(R.drawable.bg_circle_icon);
            }

            // Xóa hiệu ứng làm mờ để các icon luôn rõ nét 100% giống hình mẫu
            holder.itemView.setAlpha(1.0f);
            // =========================================================================

            // Sự kiện bấm vào
            holder.itemView.setOnClickListener(v -> {
                int oldPos = selectedPos;
                selectedPos = holder.getAdapterPosition();
                selectedCategoryIdFromDropdown = cat.id; // Gán ID để lưu Database

                // Cập nhật lại UI để hiện viền đen cho cục mới
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