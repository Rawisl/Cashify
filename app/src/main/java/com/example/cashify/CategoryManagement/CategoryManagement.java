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
    // Two separate lists and adapters for Expense (Chi) and Income (Thu)
    private RecyclerView rvChi, rvThu;
    private CategoryAdapter adapterChi, adapterThu;
    private List<Category> listChi = new ArrayList<>();
    private List<Category> listThu = new ArrayList<>();
    private AppDatabase db;

    // Repositories for available icons and colors in the picker
    private final int[] allIconsRepo = {R.drawable.ic_food, R.drawable.ic_car, R.drawable.ic_home, R.drawable.ic_bill, R.drawable.ic_cafe, R.drawable.ic_shopping};
    private final String[] colorRepo = {"#F44336", "#E91E63", "#9C27B0", "#2196F3", "#4CAF50", "#FFC107", "#FF5722", "#607D8B"};

    // Temporary states for the category currently being created or edited
    private String selectedIconName = "ic_food";
    private String selectedColorCode = "#F44336";
    private boolean isExpense = true; // true = Chi (0), false = Thu (1)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);
        db = AppDatabase.getInstance(this);

        // Ensure default categories exist if the app is newly installed
        DatabaseSeeder.seedIfEmpty(this);

        rvChi = findViewById(R.id.recyclerChiRa);
        rvThu = findViewById(R.id.recyclerThuVao);
        ImageButton btnAddCategory = findViewById(R.id.btnAddCategory);

        loadData();

        // --- UI Collapsible Sections ---
        // Uses TransitionManager for a smooth sliding effect when showing/hiding lists
        findViewById(R.id.layoutHeaderChi).setOnClickListener(v -> toggleRecyclerView(rvChi, (TextView) findViewById(R.id.tvArrowChi)));
        findViewById(R.id.layoutHeaderThu).setOnClickListener(v -> toggleRecyclerView(rvThu, (TextView) findViewById(R.id.tvArrowThu)));

        btnAddCategory.setOnClickListener(v -> showCategoryPopup(null));
    }

    private void toggleRecyclerView(RecyclerView rv, TextView arrow) {
        TransitionManager.beginDelayedTransition((ViewGroup) findViewById(R.id.content_layout));
        if (rv.getVisibility() == View.VISIBLE) {
            rv.setVisibility(View.GONE);
            arrow.setText("▶ ");
        } else {
            rv.setVisibility(View.VISIBLE);
            arrow.setText("▼ ");
        }
    }

    /**
     * Fetches categories from Room DB on a background thread and updates UI.
     */
    private void loadData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Category> chi = db.categoryDao().getCategoriesByType(0);
            List<Category> thu = db.categoryDao().getCategoriesByType(1);

            runOnUiThread(() -> {
                listChi.clear(); listChi.addAll(chi);
                listThu.clear(); listThu.addAll(thu);

                // Shared listener for Edit and Delete actions within the Adapter
                CategoryAdapter.OnCategoryListener listener = new CategoryAdapter.OnCategoryListener() {
                    @Override public void onDeleteSuccess() { loadData(); }
                    @Override public void onEditClick(Category c) { showCategoryPopup(c); }
                };

                // Initialize or refresh adapters for both types
                if (adapterChi == null) {
                    adapterChi = new CategoryAdapter(this, listChi, listener);
                    rvChi.setLayoutManager(new LinearLayoutManager(this));
                    rvChi.setAdapter(adapterChi);
                    setupSwipeToDelete(rvChi, adapterChi, listChi);
                } else adapterChi.notifyDataSetChanged();

                if (adapterThu == null) {
                    adapterThu = new CategoryAdapter(this, listThu, listener);
                    rvThu.setLayoutManager(new LinearLayoutManager(this));
                    rvThu.setAdapter(adapterThu);
                    setupSwipeToDelete(rvThu, adapterThu, listThu);
                } else adapterThu.notifyDataSetChanged();
            });
        });
    }

    /**
     * Shows a Dialog to Create or Edit a category.
     * @param editCat If null, creates new. If not null, fills fields for editing.
     */
    private void showCategoryPopup(Category editCat) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_category);

        // Make dialog background transparent to allow for rounded corners in XML
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        }

        EditText edtName = dialog.findViewById(R.id.edtCategoryName);
        ImageView imgPreview = dialog.findViewById(R.id.imgSelectedIcon);
        Button btnSave = dialog.findViewById(R.id.btnSave);

        // Pre-fill data if we are in Edit Mode
        if (editCat != null) {
            edtName.setText(editCat.name);
            selectedIconName = editCat.icon_name;
            selectedColorCode = editCat.color_code;
            isExpense = (editCat.type == 0);
        } else {
            // Default values for new category
            selectedIconName = "ic_food";
            selectedColorCode = "#F44336";
            isExpense = true;
        }

        updatePopupUI(dialog, imgPreview, editCat != null);

        btnSave.setOnClickListener(v -> {
            String name = edtName.getText().toString().trim();
            if (name.isEmpty()) return;

            Category c = (editCat != null) ? editCat : new Category();
            c.name = name;
            c.icon_name = selectedIconName;
            c.color_code = selectedColorCode;
            c.type = isExpense ? 0 : 1;
            c.is_deleted = 0;

            Executors.newSingleThreadExecutor().execute(() -> {
                if (editCat != null) db.categoryDao().update(c);
                else db.categoryDao().insert(c);
                runOnUiThread(() -> { dialog.dismiss(); loadData(); });
            });
        });
        dialog.show();
    }

    /**
     * Synchronizes the Popup's appearance (Icon, Colors, Tabs) with current state.
     */
    private void updatePopupUI(Dialog dialog, ImageView preview, boolean isEditMode) {
        // Update Preview Icon & Background
        int resId = getResources().getIdentifier(selectedIconName, "drawable", getPackageName());
        preview.setImageResource(resId != 0 ? resId : R.drawable.ic_food);

        try {
            int originColor = Color.parseColor(selectedColorCode);
            int pastelColor = Color.argb(51, Color.red(originColor), Color.green(originColor), Color.blue(originColor));

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(25f);
            shape.setColor(pastelColor);

            preview.setBackground(shape);
            preview.setImageTintList(ColorStateList.valueOf(originColor));
        } catch (Exception e) {
            preview.setImageTintList(ColorStateList.valueOf(Color.GRAY));
        }

        // --- Tab Selection Logic ---
        TextView btnChi = dialog.findViewById(R.id.btnTabChi);
        TextView btnThu = dialog.findViewById(R.id.btnTabThu);
        btnChi.setTextColor(isExpense ? Color.BLACK : Color.GRAY);
        btnThu.setTextColor(!isExpense ? Color.BLACK : Color.GRAY);

        if (isEditMode) {
            // Disable switching type (Expense/Income) during edit to avoid database confusion
            btnChi.setOnClickListener(null);
            btnThu.setOnClickListener(null);
            btnChi.setAlpha(0.6f);
            btnThu.setAlpha(0.6f);
        } else {
            btnChi.setAlpha(1.0f);
            btnThu.setAlpha(1.0f);
            btnChi.setOnClickListener(v -> { isExpense = true; updatePopupUI(dialog, preview, false); });
            btnThu.setOnClickListener(v -> { isExpense = false; updatePopupUI(dialog, preview, false); });
        }
        setupPickers(dialog, preview, isEditMode);
    }

    /**
     * Initializes the GridViews for choosing icons and colors within the popup.
     */
    private void setupPickers(Dialog dialog, ImageView preview, boolean isEditMode) {
        GridView gIcon = dialog.findViewById(R.id.gridIconPicker);
        GridView gColor = dialog.findViewById(R.id.gridColorPicker);

        gIcon.setAdapter(new PopupAdapter.IconGridAdapter(this, allIconsRepo));
        gColor.setAdapter(new PopupAdapter.ColorGridAdapter(this, colorRepo));

        gIcon.setOnItemClickListener((p, v, pos, id) -> {
            // Convert resource ID back to entry name (e.g., "ic_car") for storage
            selectedIconName = getResources().getResourceEntryName(allIconsRepo[pos]);
            updatePopupUI(dialog, preview, isEditMode);
        });

        gColor.setOnItemClickListener((p, v, pos, id) -> {
            selectedColorCode = colorRepo[pos];
            updatePopupUI(dialog, preview, isEditMode);
        });
    }

    /**
     * Adds "Swipe to Left" gesture for quick deletion.
     */
    private void setupSwipeToDelete(RecyclerView rv, CategoryAdapter adapter, List<Category> list) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) { return false; }

            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Category category = list.get(position);

                new AlertDialog.Builder(CategoryManagement.this)
                        .setTitle("Xác nhận xóa")
                        .setMessage("Xóa '" + category.name + "'?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            Executors.newSingleThreadExecutor().execute(() -> {
                                db.categoryDao().softDelete(category.id);
                                runOnUiThread(() -> { loadData(); Toast.makeText(CategoryManagement.this, "Đã xóa!", Toast.LENGTH_SHORT).show(); });
                            });
                        })
                        .setNegativeButton("Hủy", (dialog, which) -> {
                            // Important: Restore item view if deletion is cancelled
                            adapter.notifyItemChanged(position);
                        })
                        .setCancelable(false).show();
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(rv);
    }
}