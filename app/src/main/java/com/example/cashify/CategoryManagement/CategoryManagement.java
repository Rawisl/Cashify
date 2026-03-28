package com.example.cashify.CategoryManagement;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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
    private List<Category> listChi = new ArrayList<>();
    private List<Category> listThu = new ArrayList<>();
    private AppDatabase db;

    private final int[] allIconsRepo = {
            //income
            R.drawable.ic_salary,
            R.drawable.ic_family,
            R.drawable.ic_freelance,
            R.drawable.ic_bonus,
            R.drawable.ic_gift,
            //expense
            R.drawable.ic_food,
            R.drawable.ic_cafe,
            R.drawable.ic_transport,
            R.drawable.ic_fuel,
            R.drawable.ic_shopping,
            R.drawable.ic_house,
            R.drawable.ic_bill,
            R.drawable.ic_electricity,
            R.drawable.ic_gas,
            R.drawable.ic_health,
            R.drawable.ic_education,
            R.drawable.ic_vacation,
            R.drawable.ic_entertain,
            R.drawable.ic_gym,
            R.drawable.ic_other};
    private final String[] colorRepo = {
            "#F44336", // đỏ
            "#E91E63", // hồng
            "#9C27B0", // tím
            "#673AB7", // tím đậm
            "#3F51B5", // indigo
            "#2196F3", // xanh dương
            "#03A9F4", // xanh da trời
            "#00BCD4", // cyan
            "#009688", // teal
            "#4CAF50", // xanh lá
            "#8BC34A", // xanh lá nhạt (vẫn ổn)
            "#FFC107", // vàng (đậm, vẫn thấy được)
            "#FF9800", // cam
            "#FF5722", // cam đỏ
            "#795548"  // nâu
    };

    private String selectedIconName = "ic_other";
    private String selectedColorCode = "#F44336";
    private boolean isExpense = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);
        db = AppDatabase.getInstance(this);

        DatabaseSeeder.seedIfEmpty(this);

        // Ánh xạ View
        rvChi = findViewById(R.id.recyclerChiRa);
        rvThu = findViewById(R.id.recyclerThuVao);
        ImageButton btnAddCategory = findViewById(R.id.btnAddCategory);
        ImageView btnBack = findViewById(R.id.btnBack);

        loadData();

        // Xử lý nút Back
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // --- UI Collapsible Sections ---
        findViewById(R.id.layoutHeaderChi).setOnClickListener(v -> toggleRecyclerView(rvChi, findViewById(R.id.tvArrowChi)));
        findViewById(R.id.layoutHeaderThu).setOnClickListener(v -> toggleRecyclerView(rvThu, findViewById(R.id.tvArrowThu)));

        btnAddCategory.setOnClickListener(v -> showCategoryPopup(null));
    }

    private void toggleRecyclerView(RecyclerView rv, TextView arrow) {
        ViewGroup root = findViewById(R.id.content_layout);
        if (root != null) {
            TransitionManager.beginDelayedTransition(root);
        }

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

                CategoryAdapter.OnCategoryListener listener = new CategoryAdapter.OnCategoryListener() {
                    @Override public void onDeleteSuccess() { loadData(); }
                    @Override public void onEditClick(Category c) { showCategoryPopup(c); }
                };

                // Adapter Chi
                if (adapterChi == null) {
                    adapterChi = new CategoryAdapter(this, listChi, listener);
                    rvChi.setLayoutManager(new LinearLayoutManager(this));
                    rvChi.setAdapter(adapterChi);
                    rvChi.setNestedScrollingEnabled(false); // Quan trọng: Tắt scroll để mượt hơn
                    setupSwipeToDelete(rvChi, adapterChi, listChi);
                } else adapterChi.notifyDataSetChanged();

                // Adapter Thu
                if (adapterThu == null) {
                    adapterThu = new CategoryAdapter(this, listThu, listener);
                    rvThu.setLayoutManager(new LinearLayoutManager(this));
                    rvThu.setAdapter(adapterThu);
                    rvThu.setNestedScrollingEnabled(false); // Quan trọng: Tắt scroll để mượt hơn
                    setupSwipeToDelete(rvThu, adapterThu, listThu);
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

        if (editCat != null) {
            edtName.setText(editCat.name);
            selectedIconName = editCat.iconName;
            selectedColorCode = editCat.colorCode;
            isExpense = (editCat.type == 0);
        } else {
            selectedIconName = "ic_other";
            selectedColorCode = "#4C6FFF";
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
            c.name = name;
            c.iconName = selectedIconName;
            c.colorCode = selectedColorCode;
            c.type = isExpense ? 0 : 1;
            c.isDeleted = 0;

            Executors.newSingleThreadExecutor().execute(() -> {
                if (editCat != null) db.categoryDao().update(c);
                else db.categoryDao().insert(c);
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
            // Tạo màu pastel (độ trong suốt 20%) cho background icon
            int pastelColor = Color.argb(51, Color.red(originColor), Color.green(originColor), Color.blue(originColor));

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL); // Đổi thành OVAL cho đúng chuẩn Material
            shape.setColor(pastelColor);

            preview.setBackground(shape);
            preview.setImageTintList(ColorStateList.valueOf(originColor));
        } catch (Exception e) {
            preview.setImageTintList(ColorStateList.valueOf(Color.GRAY));
        }

        TextView btnChi = dialog.findViewById(R.id.btnTabChi);
        TextView btnThu = dialog.findViewById(R.id.btnTabThu);

        // Cập nhật style cho tab được chọn
        btnChi.setTextColor(isExpense ? Color.parseColor("#4C6FFF") : Color.GRAY);
        btnThu.setTextColor(!isExpense ? Color.parseColor("#4C6FFF") : Color.GRAY);

        if (isEditMode) {
            btnChi.setAlpha(0.5f);
            btnThu.setAlpha(0.5f);
        } else {
            btnChi.setAlpha(1.0f);
            btnThu.setAlpha(1.0f);
            btnChi.setOnClickListener(v -> { isExpense = true; updatePopupUI(dialog, preview, false); });
            btnThu.setOnClickListener(v -> { isExpense = false; updatePopupUI(dialog, preview, false); });
        }
        setupPickers(dialog, preview, isEditMode);
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

    private void setupSwipeToDelete(RecyclerView rv, CategoryAdapter adapter, List<Category> list) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Category category = list.get(position);

                new AlertDialog.Builder(CategoryManagement.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Bạn có chắc muốn xóa '" + category.name + "'?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                db.categoryDao().softDelete(category.id);
                                runOnUiThread(() -> { loadData(); });
                            });
                        })
                        .setNegativeButton("Hủy", (dialog, which) -> {
                            adapter.notifyItemChanged(position);
                        })
                        .setCancelable(false).show();
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(rv);
    }
}