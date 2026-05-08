package com.example.cashify.ui.workspace;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.Category;
import com.example.cashify.ui.category.PopupAdapter;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.Serializable;

public class WorkspaceCategoryBottomSheet extends BottomSheetDialogFragment {

    private String workspaceId;
    private Category editCat;

    // View components (giữ nguyên của bồ)
    private EditText edtName;
    private ImageView imgPreview;
    private TextView tvTitle, btnTabChi, btnTabThu;
    private Button btnSave;
    private RecyclerView gIcon, gColor;

    private String selectedIconName = "ic_other";
    private String selectedColorCode = "#313B60";
    private boolean isExpense = true;
    private String[] colorHexRepo;
    private final int[] allIconsRepo = {
            R.drawable.ic_salary, R.drawable.ic_family, R.drawable.ic_freelance, R.drawable.ic_bonus, R.drawable.ic_gift,
            R.drawable.ic_food, R.drawable.ic_cafe, R.drawable.ic_transport, R.drawable.ic_fuel, R.drawable.ic_shopping,
            R.drawable.ic_house, R.drawable.ic_bill, R.drawable.ic_electricity, R.drawable.ic_gas, R.drawable.ic_health,
            R.drawable.ic_education, R.drawable.ic_vacation, R.drawable.ic_entertain, R.drawable.ic_gym, R.drawable.ic_other
    };

    public static WorkspaceCategoryBottomSheet newInstance(String workspaceId, Category category) {
        WorkspaceCategoryBottomSheet fragment = new WorkspaceCategoryBottomSheet();
        Bundle args = new Bundle();
        args.putString("WORKSPACE_ID", workspaceId);
        if (category != null) {
            args.putSerializable("edit_category", (Serializable) category);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            workspaceId = getArguments().getString("WORKSPACE_ID");
            if (getArguments().containsKey("edit_category")) {
                editCat = (Category) getArguments().getSerializable("edit_category");
            }
        }
        // 2. Chuẩn bị tài nguyên màu sắc
        prepareColorResources();
        // 3. Ánh xạ View
        initViews(view);

        if (editCat != null) {
            setupEditMode();
        }
        // 5. Cập nhật UI ban đầu
        updatePopupUI();
        setupEventListeners();

        btnSave.setOnClickListener(v -> handleSaveToFirestore());
    }

    private void handleSaveToFirestore() {
        String name = edtName.getText().toString().trim();
        if (name.isEmpty()) {
            ToastHelper.show(getContext(), "Please enter a category name");
            return;
        }

        Category c = (editCat != null) ? editCat : new Category();
        c.name = name;
        c.iconName = selectedIconName;
        c.colorCode = selectedColorCode;
        c.type = isExpense ? 0 : 1;
        c.workspaceId = workspaceId;

        // THÊM 2 DÒNG NÀY VÀO ĐỂ ĐỒNG BỘ CẤU TRÚC:
        c.isDefault = 0;
        c.isDeleted = 0;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        btnSave.setEnabled(false); // Tránh bấm 2 lần

        if (editCat != null && editCat.firestoreId != null) {
            // EDIT Mode
            db.collection("workspaces").document(workspaceId)
                    .collection("categories").document(editCat.firestoreId)
                    .set(c)
                    .addOnSuccessListener(aVoid -> {
                        ToastHelper.show(getContext(), "Category updated successfully");
                        dismiss();
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        ToastHelper.show(getContext(), "Failed to update: " + e.getMessage());
                    });
        } else {
            // ADD NEW Mode
            db.collection("workspaces").document(workspaceId)
                    .collection("categories")
                    .add(c)
                    .addOnSuccessListener(documentReference -> {
                        ToastHelper.show(getContext(), "Category added successfully");
                        dismiss();
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        ToastHelper.show(getContext(), "Failed to add: " + e.getMessage());
                    });
        }
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
        gIcon = v.findViewById(R.id.recyclerIconPicker);
        gColor = v.findViewById(R.id.recyclerColorPicker);
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

        // Setup RecyclerView cho Icon (5 cột)
        gIcon.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 5));
        gIcon.setAdapter(new PopupAdapter.IconAdapter(requireContext(), allIconsRepo, position -> {
            selectedIconName = getResources().getResourceEntryName(allIconsRepo[position]);
            updatePopupUI();
        }));

        // Setup RecyclerView cho Màu (5 cột)
        gColor.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 5));
        gColor.setAdapter(new PopupAdapter.ColorAdapter(requireContext(), colorHexRepo, position -> {
            selectedColorCode = colorHexRepo[position];
            updatePopupUI();
        }));

        // Bùa chuẩn cho RecyclerView
        RecyclerView.OnItemTouchListener bulletproofScrollLock = new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                int action = e.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    // Ngón tay chạm vào -> Khóa BottomSheet
                    setBottomSheetDraggable(false);
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    // Ngón tay nhấc lên -> Nhả BottomSheet
                    setBottomSheetDraggable(true);
                }
                return false; // Phải return false để RecyclerView còn cuộn được
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        };

        // Gắn bùa vào 2 danh sách
        gIcon.addOnItemTouchListener(bulletproofScrollLock);
        gColor.addOnItemTouchListener(bulletproofScrollLock);
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

    // Hàm này dùng để Bật/Tắt khả năng trượt của BottomSheet
    private void setBottomSheetDraggable(boolean isDraggable) {
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();

            // Lấy thẳng Behavior từ Dialog luôn, không cần tìm View, không dính dáng chữ R nào nữa!
            dialog.getBehavior().setDraggable(isDraggable);
        }
    }
}