package com.example.cashify.ui.category;


import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.cashify.R;
import com.example.cashify.database.Category;
import com.example.cashify.viewmodel.CategoryViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.Serializable;

public class CategoryBottomSheet extends BottomSheetDialogFragment {

    private CategoryViewModel viewModel;
    private Category editCat;

    // View components
    private EditText edtName;
    private ImageView imgPreview;
    private TextView tvTitle, btnTabChi, btnTabThu;
    private Button btnSave;
    private GridView gIcon, gColor;

    // State variables (Bốc từ Activity sang)
    private String selectedIconName = "ic_other";
    private String selectedColorCode = "#313B60";
    private boolean isExpense = true;

    private final int[] allIconsRepo = {
            R.drawable.ic_salary, R.drawable.ic_family, R.drawable.ic_freelance, R.drawable.ic_bonus, R.drawable.ic_gift,
            R.drawable.ic_food, R.drawable.ic_cafe, R.drawable.ic_transport, R.drawable.ic_fuel, R.drawable.ic_shopping,
            R.drawable.ic_house, R.drawable.ic_bill, R.drawable.ic_electricity, R.drawable.ic_gas, R.drawable.ic_health,
            R.drawable.ic_education, R.drawable.ic_vacation, R.drawable.ic_entertain, R.drawable.ic_gym, R.drawable.ic_other
    };

    private String[] colorHexRepo;

    // Hàm static để khởi tạo BottomSheet kèm dữ liệu (nếu là Edit)
    public static CategoryBottomSheet newInstance(Category category) {
        CategoryBottomSheet fragment = new CategoryBottomSheet();
        if (category != null) {
            Bundle args = new Bundle();
            args.putSerializable("edit_category", (Serializable) category);
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.popup_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Kết nối ViewModel (Dùng requireActivity để xài chung data với màn hình chính)
        viewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        // 2. Chuẩn bị tài nguyên màu sắc
        prepareColorResources();

        // 3. Ánh xạ View
        initViews(view);

        // 4. Kiểm tra xem có phải mode Edit không
        if (getArguments() != null) {
            editCat = (Category) getArguments().getSerializable("edit_category");
            setupEditMode();
        }

        // 5. Cập nhật UI ban đầu
        updatePopupUI();
        setupEventListeners();
    }

    private void prepareColorResources() {
        int[] colorRepo = getResources().getIntArray(R.array.category_color_repo);
        colorHexRepo = new String[colorRepo.length];
        for (int i = 0; i < colorRepo.length; i++) {
            colorHexRepo[i] = String.format("#%06X", (0xFFFFFF & colorRepo[i]));
        }
    }

    private void initViews(View v) {
        edtName = v.findViewById(R.id.edtCategoryName);
        imgPreview = v.findViewById(R.id.imgSelectedIcon);
        tvTitle = v.findViewById(R.id.tvPopupTitle);
        btnTabChi = v.findViewById(R.id.btnTabChi);
        btnTabThu = v.findViewById(R.id.btnTabThu);
        btnSave = v.findViewById(R.id.btnSave);
        gIcon = v.findViewById(R.id.gridIconPicker);
        gColor = v.findViewById(R.id.gridColorPicker);
    }

    private void setupEditMode() {
        if (editCat != null) {
            tvTitle.setText(R.string.category_popup_edit);
            btnSave.setText(R.string.action_update);
            edtName.setText(editCat.name);
            selectedIconName = editCat.iconName;
            selectedColorCode = editCat.colorCode;
            isExpense = (editCat.type == 0);

            // Khóa Tab nếu đang edit (giống logic cũ của bạn)
            btnTabChi.setAlpha(0.5f);
            btnTabThu.setAlpha(0.5f);
        }
    }

    private void setupEventListeners() {
        // Tab Chi
        if (editCat == null) { // Chỉ cho chọn tab khi thêm mới
            btnTabChi.setOnClickListener(v -> {
                isExpense = true;
                updatePopupUI();
            });
            btnTabThu.setOnClickListener(v -> {
                isExpense = false;
                updatePopupUI();
            });
        }

        // Picker Icon
        gIcon.setAdapter(new PopupAdapter.IconGridAdapter(requireContext(), allIconsRepo));
        gIcon.setOnItemClickListener((p, v, pos, id) -> {
            selectedIconName = getResources().getResourceEntryName(allIconsRepo[pos]);
            updatePopupUI();
        });

        // Picker Color
        gColor.setAdapter(new PopupAdapter.ColorGridAdapter(requireContext(), colorHexRepo));
        gColor.setOnItemClickListener((p, v, pos, id) -> {
            selectedColorCode = colorHexRepo[pos];
            updatePopupUI();
        });

        // Nút Save
        btnSave.setOnClickListener(v -> handleSave());
    }

    private void updatePopupUI() {
        // Update Icon Preview
        int resId = getResources().getIdentifier(selectedIconName, "drawable", requireContext().getPackageName());
        imgPreview.setImageResource(resId != 0 ? resId : R.drawable.ic_food);

        try {
            int originColor = Color.parseColor(selectedColorCode);
            int pastelColor = Color.argb(51, Color.red(originColor), Color.green(originColor), Color.blue(originColor));
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(14 * getResources().getDisplayMetrics().density);
            shape.setColor(pastelColor);
            imgPreview.setBackground(shape);
            imgPreview.setImageTintList(ColorStateList.valueOf(originColor));
        } catch (Exception e) {
            imgPreview.setImageTintList(ColorStateList.valueOf(Color.GRAY));
        }

        // Update Tabs
        if (isExpense) updateTabStyles(btnTabChi, btnTabThu, true);
        else updateTabStyles(btnTabThu, btnTabChi, false);
    }

    private void updateTabStyles(TextView selected, TextView unselected, boolean isExpenseTab) {
        GradientDrawable selectedBg = new GradientDrawable();
        selectedBg.setColor(ContextCompat.getColor(requireContext(), R.color.tab_active_bg));
        selectedBg.setCornerRadius(10 * getResources().getDisplayMetrics().density);
        selected.setBackground(selectedBg);
        selected.setTextColor(isExpenseTab ? Color.parseColor("#D14040") : Color.parseColor("#1DB424"));
        selected.setTypeface(null, android.graphics.Typeface.BOLD);

        unselected.setBackground(null);
        unselected.setTextColor(Color.parseColor("#9CA3AF"));
        unselected.setTypeface(null, android.graphics.Typeface.NORMAL);
    }

    private void handleSave() {
        String name = edtName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(getContext(), R.string.error_empty_category_name, Toast.LENGTH_SHORT).show();
            return;
        }

        Category c = (editCat != null) ? editCat : new Category();
        c.name = name;
        c.iconName = selectedIconName;
        c.colorCode = selectedColorCode;
        c.type = isExpense ? 0 : 1;
        c.isDeleted = 0;

        if (editCat != null) {
            viewModel.update(c);
        } else {
            viewModel.insert(c);
        }

        dismiss(); // Xong việc thì cút (đóng sheet)
    }
}
