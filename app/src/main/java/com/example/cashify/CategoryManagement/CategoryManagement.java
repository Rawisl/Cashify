package com.example.cashify.CategoryManagement;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.database.AppDatabase;
import com.example.cashify.database.Category;
import com.example.cashify.database.DatabaseSeeder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class CategoryManagement extends AppCompatActivity {
    private RecyclerView rvChi, rvThu;
    private CategoryAdapter adapterChi, adapterThu;
    private AppDatabase db;
    private EditText edtSearch;

    private List<Category> listChi = new ArrayList<>();
    private List<Category> listThu = new ArrayList<>();
    private List<Category> filteredChi = new ArrayList<>();
    private List<Category> filteredThu = new ArrayList<>();

    private final int[] allIconsRepo = {
            R.drawable.ic_salary, R.drawable.ic_family, R.drawable.ic_freelance, R.drawable.ic_bonus, R.drawable.ic_gift,
            R.drawable.ic_food, R.drawable.ic_cafe, R.drawable.ic_transport, R.drawable.ic_fuel, R.drawable.ic_shopping,
            R.drawable.ic_house, R.drawable.ic_bill, R.drawable.ic_electricity, R.drawable.ic_gas, R.drawable.ic_health,
            R.drawable.ic_education, R.drawable.ic_vacation, R.drawable.ic_entertain, R.drawable.ic_gym, R.drawable.ic_other
    };

    private final String[] colorRepo = {
            "#E96565", "#FDA664", "#F675A1", "#559DE4", "#6CD0D0", "#847FF0", "#87D18C", "#93CE9D",
            "#9B8077", "#F6DA88", "#AD78B4", "#81949D", "#313B60", "#D14040", "#1DB424"
    };

    private String selectedIconName = "ic_other";
    private String selectedColorCode = "#313B60";
    private boolean isExpense = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);
        db = AppDatabase.getInstance(this);
        DatabaseSeeder.seedIfEmpty(this);

        rvChi = findViewById(R.id.recyclerChiRa);
        rvThu = findViewById(R.id.recyclerThuVao);
        edtSearch = findViewById(R.id.edtSearchCategory);
        ImageButton btnAddCategory = findViewById(R.id.btnAddCategory);
        ImageView btnBack = findViewById(R.id.btnBack);

        loadData();

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        if (edtSearch != null) {
            edtSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterCategories(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        findViewById(R.id.layoutHeaderChi).setOnClickListener(v -> toggleRecyclerView(rvChi, findViewById(R.id.tvArrowChi)));
        findViewById(R.id.layoutHeaderThu).setOnClickListener(v -> toggleRecyclerView(rvThu, findViewById(R.id.tvArrowThu)));

        btnAddCategory.setOnClickListener(v -> showCategoryPopup(null));
    }

    private void filterCategories(String query) {
        String pattern = query.toLowerCase().trim();
        filteredChi.clear(); filteredThu.clear();
        for (Category c : listChi) if (c.name.toLowerCase().contains(pattern)) filteredChi.add(c);
        for (Category c : listThu) if (c.name.toLowerCase().contains(pattern)) filteredThu.add(c);
        if (adapterChi != null) adapterChi.notifyDataSetChanged();
        if (adapterThu != null) adapterThu.notifyDataSetChanged();
        if (!pattern.isEmpty()) {
            rvChi.setVisibility(View.VISIBLE);
            rvThu.setVisibility(View.VISIBLE);
        }
    }

    private void toggleRecyclerView(RecyclerView rv, TextView arrow) {
        ViewGroup root = findViewById(R.id.content_layout);
        if (root != null) TransitionManager.beginDelayedTransition(root);
        if (rv.getVisibility() == View.VISIBLE) {
            rv.setVisibility(View.GONE);
            if (arrow != null) arrow.animate().rotation(-90).setDuration(200).start();
        } else {
            rv.setVisibility(View.VISIBLE);
            if (arrow != null) arrow.animate().rotation(0).setDuration(200).start();
        }
    }

    private void loadData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Category> chi = db.categoryDao().getCategoriesByType(0);
            List<Category> thu = db.categoryDao().getCategoriesByType(1);
            runOnUiThread(() -> {
                listChi.clear(); listChi.addAll(chi);
                listThu.clear(); listThu.addAll(thu);
                filterCategories(edtSearch != null ? edtSearch.getText().toString() : "");
                CategoryAdapter.OnCategoryListener listener = new CategoryAdapter.OnCategoryListener() {
                    @Override public void onDeleteSuccess() { loadData(); }
                    @Override public void onEditClick(Category c) { showCategoryPopup(c); }
                };
                if (adapterChi == null) {
                    adapterChi = new CategoryAdapter(this, filteredChi, listener);
                    rvChi.setLayoutManager(new LinearLayoutManager(this));
                    rvChi.setAdapter(adapterChi);
                    setupSwipeToDelete(rvChi, adapterChi, filteredChi);
                } else adapterChi.notifyDataSetChanged();
                if (adapterThu == null) {
                    adapterThu = new CategoryAdapter(this, filteredThu, listener);
                    rvThu.setLayoutManager(new LinearLayoutManager(this));
                    rvThu.setAdapter(adapterThu);
                    setupSwipeToDelete(rvThu, adapterThu, filteredThu);
                } else adapterThu.notifyDataSetChanged();
            });
        });
    }

    private void showCategoryPopup(Category editCat) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_category);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }
        EditText edtName = dialog.findViewById(R.id.edtCategoryName);
        ImageView imgPreview = dialog.findViewById(R.id.imgSelectedIcon);
        Button btnSave = dialog.findViewById(R.id.btnSave);
<<<<<<< AddTransaction
        if (editCat != null) {
=======
        TextView tvTitle = dialog.findViewById(R.id.tvPopupTitle);

        if (editCat != null)
        {
            if (tvTitle != null) tvTitle.setText(R.string.category_popup_edit);
            btnSave.setText(R.string.action_update);
>>>>>>> master
            edtName.setText(editCat.name);
            selectedIconName = editCat.iconName;
            selectedColorCode = editCat.colorCode;
            isExpense = (editCat.type == 0);
        } else {
            if (tvTitle != null) tvTitle.setText(R.string.category_popup_add);
            btnSave.setText(R.string.action_save);
            selectedIconName = "ic_other";
            selectedColorCode = "#313B60";
            isExpense = true;
        }
        updatePopupUI(dialog, imgPreview, editCat != null);
        btnSave.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên!", Toast.LENGTH_SHORT).show();
                return;
            }
            Category c = (editCat != null) ? editCat : new Category();
            c.name = name; c.iconName = selectedIconName; c.colorCode = selectedColorCode;
            c.type = isExpense ? 0 : 1; c.isDeleted = 0;
            Executors.newSingleThreadExecutor().execute(() -> {
                if (editCat != null) db.categoryDao().update(c); else db.categoryDao().insert(c);
                runOnUiThread(() -> { dialog.dismiss(); loadData(); });
            });
        });
        dialog.show();
    }

    private void updatePopupUI(Dialog dialog, ImageView preview, boolean isEditMode) {
        int resId = getResources().getIdentifier(selectedIconName, "drawable", getPackageName());
        preview.setImageResource(resId != 0 ? resId : R.drawable.ic_food);
        try {
            int originColor = Color.parseColor(selectedColorCode);
            int pastelColor = Color.argb(51, Color.red(originColor), Color.green(originColor), Color.blue(originColor));
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(14 * getResources().getDisplayMetrics().density);
            shape.setColor(pastelColor);
            preview.setBackground(shape);
            preview.setImageTintList(ColorStateList.valueOf(originColor));
        } catch (Exception e) { preview.setImageTintList(ColorStateList.valueOf(Color.GRAY)); }

        TextView btnChi = dialog.findViewById(R.id.btnTabChi);
        TextView btnThu = dialog.findViewById(R.id.btnTabThu);
        if (isExpense) updateTabStyles(btnChi, btnThu, true); else updateTabStyles(btnThu, btnChi, false);
        if (!isEditMode) {
            btnChi.setOnClickListener(v -> { isExpense = true; updatePopupUI(dialog, preview, false); });
            btnThu.setOnClickListener(v -> { isExpense = false; updatePopupUI(dialog, preview, false); });
        } else { btnChi.setAlpha(0.5f); btnThu.setAlpha(0.5f); }
        setupPickers(dialog, preview, isEditMode);
    }

    private void updateTabStyles(TextView selected, TextView unselected, boolean isExpenseTab) {
        GradientDrawable selectedBg = new GradientDrawable();
        selectedBg.setColor(Color.parseColor("#E5E7EB"));
        selectedBg.setCornerRadius(10 * getResources().getDisplayMetrics().density);
        selected.setBackground(selectedBg);
        selected.setTextColor(isExpenseTab ? Color.parseColor("#D14040") : Color.parseColor("#1DB424"));
        selected.setTypeface(null, android.graphics.Typeface.BOLD);
        unselected.setBackground(null);
        unselected.setTextColor(Color.parseColor("#9CA3AF"));
        unselected.setTypeface(null, android.graphics.Typeface.NORMAL);
    }

    private void setupPickers(Dialog dialog, ImageView preview, boolean isEditMode) {
        GridView gIcon = dialog.findViewById(R.id.gridIconPicker);
        GridView gColor = dialog.findViewById(R.id.gridColorPicker);
        if (gIcon != null) {
            gIcon.setAdapter(new PopupAdapter.IconGridAdapter(this, allIconsRepo));
            gIcon.setOnItemClickListener((p, v, pos, id) -> {
                selectedIconName = getResources().getResourceEntryName(allIconsRepo[pos]);
                updatePopupUI(dialog, preview, isEditMode);
            });
        }
        if (gColor != null) {
            gColor.setAdapter(new PopupAdapter.ColorGridAdapter(this, colorRepo));
            gColor.setOnItemClickListener((p, v, pos, id) -> {
                selectedColorCode = colorRepo[pos];
                updatePopupUI(dialog, preview, isEditMode);
            });
        }
    }

    // --- CẬP NHẬT PHẦN SWIPE Ở ĐÂY ---
    private void setupSwipeToDelete(RecyclerView rv, CategoryAdapter adapter, List<Category> list) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder v, @NonNull RecyclerView.ViewHolder t) { return false; }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                float density = getResources().getDisplayMetrics().density;

                // 1. Vẽ nền đỏ bo góc
                GradientDrawable background = new GradientDrawable();
                background.setColor(Color.parseColor("#D14040"));
                background.setCornerRadius(16 * density);
                background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                background.draw(c);

                // 2. Vẽ icon Thùng rác màu trắng
                Drawable deleteIcon = ContextCompat.getDrawable(CategoryManagement.this, R.drawable.trash_regularsvg); // Đảm bảo có ic_delete trong drawable
                if (deleteIcon != null && dX < -100) { // Chỉ hiện icon khi swipe đủ xa
                    deleteIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);

                    int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int iconTop = itemView.getTop() + iconMargin;
                    int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

                    // Căn icon nằm bên phải (trong vùng đỏ)
                    int iconRight = itemView.getRight() - (int)(16 * density);
                    int iconLeft = iconRight - deleteIcon.getIntrinsicWidth();

                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    deleteIcon.draw(c);
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                Category cat = list.get(pos);
                new AlertDialog.Builder(CategoryManagement.this)
                        .setTitle("Xóa danh mục")
                        .setMessage("Bạn có chắc muốn xóa '" + cat.name + "'?")
                        .setPositiveButton("Xóa", (d, w) -> Executors.newSingleThreadExecutor().execute(() -> {
                            db.categoryDao().softDelete(cat.id);
                            runOnUiThread(() -> loadData());
                        }))
                        .setNegativeButton("Hủy", (d, w) -> adapter.notifyItemChanged(pos))
                        .setCancelable(false)
                        .show();
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(rv);
    }
}