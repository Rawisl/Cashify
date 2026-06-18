package com.example.cashify.ui.category;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.Category;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
    private RecyclerView gIcon, gColor;

    // Default State Variables
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

    /**
     * Factory method to create a new instance of the BottomSheet.
     * Passes the Category object if opening in Edit Mode.
     */
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
        return inflater.inflate(R.layout.bottom_sheet_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind ViewModel to the Activity scope to share data with the parent screen
        viewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);

        prepareColorResources();
        initViews(view);

        // Check for existing data (Edit Mode)
        if (getArguments() != null) {
            editCat = (Category) getArguments().getSerializable("edit_category");
            setupEditMode();
        }

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

            // Lock the Income/Expense tabs to prevent changing category type during edit
            btnTabChi.setAlpha(0.5f);
            btnTabThu.setAlpha(0.5f);
            btnTabChi.setEnabled(false);
            btnTabThu.setEnabled(false);
        }
    }

    private void setupEventListeners() {
        // Tab Listeners (Only active in Add Mode)
        if (editCat == null) {
            btnTabChi.setOnClickListener(v -> {
                isExpense = true;
                updatePopupUI();
            });
            btnTabThu.setOnClickListener(v -> {
                isExpense = false;
                updatePopupUI();
            });
        }

        // Setup Icon Grid
        gIcon.setLayoutManager(new GridLayoutManager(requireContext(), 5));
        gIcon.setAdapter(new PopupAdapter.IconAdapter(requireContext(), allIconsRepo, position -> {
            selectedIconName = getResources().getResourceEntryName(allIconsRepo[position]);
            updatePopupUI();
        }));

        // Setup Color Grid
        gColor.setLayoutManager(new GridLayoutManager(requireContext(), 5));
        gColor.setAdapter(new PopupAdapter.ColorAdapter(requireContext(), colorHexRepo, position -> {
            selectedColorCode = colorHexRepo[position];
            updatePopupUI();
        }));

        btnSave.setOnClickListener(v -> handleSave());

        // =========================================================================
        // SCROLL CONFLICT RESOLUTION
        // =========================================================================
        // Prevents the BottomSheet from dragging down when scrolling the inner RecyclerViews
        RecyclerView.OnItemTouchListener bulletproofScrollLock = new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                int action = e.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN) {
                    setBottomSheetDraggable(false);
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    setBottomSheetDraggable(true);
                }
                return false; // Must return false to allow the RecyclerView to process the scroll
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {}

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        };

        gIcon.addOnItemTouchListener(bulletproofScrollLock);
        gColor.addOnItemTouchListener(bulletproofScrollLock);
    }

    private void updatePopupUI() {
        // Render Icon Preview
        int resId = getResources().getIdentifier(selectedIconName, "drawable", requireContext().getPackageName());
        imgPreview.setImageResource(resId != 0 ? resId : R.drawable.ic_food);

        // Render Pastel Background for Preview
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

        // Toggle Active Tab Style
        if (isExpense) {
            updateTabStyles(btnTabChi, btnTabThu, true);
        } else {
            updateTabStyles(btnTabThu, btnTabChi, false);
        }
    }

    private void updateTabStyles(TextView selected, TextView unselected, boolean isExpenseTab) {
        GradientDrawable selectedBg = new GradientDrawable();
        selectedBg.setColor(ContextCompat.getColor(requireContext(), R.color.tab_active_bg));
        selectedBg.setCornerRadius(10 * getResources().getDisplayMetrics().density);

        selected.setBackground(selectedBg);
        selected.setTextColor(isExpenseTab ? Color.parseColor("#D14040") : Color.parseColor("#1DB424"));
        selected.setTypeface(null, Typeface.BOLD);

        unselected.setBackground(null);
        unselected.setTextColor(Color.parseColor("#9CA3AF"));
        unselected.setTypeface(null, Typeface.NORMAL);
    }

    private void handleSave() {
        String name = edtName.getText() != null ? edtName.getText().toString().trim() : "";
        if (name.isEmpty()) {
            ToastHelper.show(getContext(), R.string.error_empty_category_name);
            return;
        }

        // Apply edits to existing object, or create a new one
        Category c = (editCat != null) ? editCat : new Category();
        c.name = name;
        c.iconName = selectedIconName;
        c.colorCode = selectedColorCode;
        c.type = isExpense ? 0 : 1;

        // Ensure standard states are enforced on creation
        if (editCat == null) {
            c.isDefault = 0;
            c.isDeleted = 0;
        }

        // Delegate persistence to ViewModel
        if (editCat != null) {
            viewModel.update(c);
        } else {
            viewModel.insert(c);
        }

        // Job done, dismiss the sheet
        dismiss();
    }

    /**
     * Toggles the swipe-to-dismiss behavior of the BottomSheet.
     * Essential for nested scrolling structures.
     */
    private void setBottomSheetDraggable(boolean isDraggable) {
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            if (dialog.getBehavior() != null) {
                dialog.getBehavior().setDraggable(isDraggable);
            }
        }
    }
}