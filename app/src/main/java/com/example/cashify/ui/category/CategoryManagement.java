package com.example.cashify.ui.category;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.database.Category;
import com.example.cashify.viewmodel.CategoryViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class CategoryManagement extends AppCompatActivity {
    private RecyclerView rvChi, rvThu;
    private CategoryAdapter adapterChi, adapterThu;
    private CategoryViewModel viewModel;
    private EditText edtSearch;

    private List<Category> listChiOriginal = new ArrayList<>();
    private List<Category> listThuOriginal = new ArrayList<>();
    private List<Category> filteredChi = new ArrayList<>();
    private List<Category> filteredThu = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        // 1. Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(CategoryViewModel.class);

        // 2. Ánh xạ View & Sự kiện cơ bản
        initViews();

        // 3. Thiết lập RecyclerView
        setupRecyclerViews();

        // 4. Quan sát dữ liệu (Đây là linh hồn của MVVM)
        observeData();

        // 5. Thiết lập Search và FAB
        setupActions();
    }

    private void initViews() {
        rvChi = findViewById(R.id.recyclerChiRa);
        rvThu = findViewById(R.id.recyclerThuVao);
        edtSearch = findViewById(R.id.edtSearchCategory);
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        findViewById(R.id.layoutHeaderChi).setOnClickListener(v -> toggleRecyclerView(rvChi, findViewById(R.id.tvArrowChi)));
        findViewById(R.id.layoutHeaderThu).setOnClickListener(v -> toggleRecyclerView(rvThu, findViewById(R.id.tvArrowThu)));
    }

    private void setupRecyclerViews() {
        CategoryAdapter.OnCategoryListener listener = new CategoryAdapter.OnCategoryListener() {
            @Override
            public void onDeleteClick(Category category) {
                // Hiện Dialog xác nhận ở ĐÂY (Activity)
                new AlertDialog.Builder(CategoryManagement.this)
                        .setTitle(getString(R.string.action_delete_category))
                        .setMessage(getString(R.string.confirm_delete,category.name))
                        .setPositiveButton(getString(R.string.action_delete), (d, w) -> {
                            // GỌI VIEWMODEL THỰC HIỆN LỆNH XÓA
                            viewModel.deleteCategory(category.id);
                        })
                        .setNegativeButton(getString(R.string.action_cancel), null)
                        .show();
            }

            @Override
            public void onEditClick(Category category) {
                CategoryBottomSheet.newInstance(category).show(getSupportFragmentManager(), "EditCategory");
            }
        };

        adapterChi = new CategoryAdapter(this, filteredChi, listener);
        rvChi.setLayoutManager(new LinearLayoutManager(this));
        rvChi.setAdapter(adapterChi);
        setupSwipeToDelete(rvChi, adapterChi, filteredChi);

        adapterThu = new CategoryAdapter(this, filteredThu, listener);
        rvThu.setLayoutManager(new LinearLayoutManager(this));
        rvThu.setAdapter(adapterThu);
        setupSwipeToDelete(rvThu, adapterThu, filteredThu);
    }

    private void observeData() {
        viewModel.getExpenseCategories().observe(this, categories -> {
            listChiOriginal = categories;
            filterCategories(edtSearch.getText().toString());
        });

        viewModel.getIncomeCategories().observe(this, categories -> {
            listThuOriginal = categories;
            filterCategories(edtSearch.getText().toString());
        });
    }

    private void setupActions() {
        FloatingActionButton fabAddCategory = findViewById(R.id.fab_add_category);
        fabAddCategory.setOnClickListener(v -> {
            // GỌI BOTTOM SHEET ĐỂ THÊM MỚI
            CategoryBottomSheet.newInstance(null).show(getSupportFragmentManager(), "CategoryBottomSheet");
        });

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCategories(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filterCategories(String query) {
        String pattern = query.toLowerCase().trim();
        filteredChi.clear();
        filteredThu.clear();
        for (Category c : listChiOriginal) if (c.name.toLowerCase().contains(pattern)) filteredChi.add(c);
        for (Category c : listThuOriginal) if (c.name.toLowerCase().contains(pattern)) filteredThu.add(c);

        adapterChi.notifyDataSetChanged();
        adapterThu.notifyDataSetChanged();

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
            rescueFloatingActionButton();
        } else {
            rv.setVisibility(View.VISIBLE);
            if (arrow != null) arrow.animate().rotation(0).setDuration(200).start();
        }
    }

    private void rescueFloatingActionButton() {
        try {
            FloatingActionButton fab = findViewById(R.id.fab_add_category);
            if (fab != null && fab.getLayoutParams() instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                androidx.coordinatorlayout.widget.CoordinatorLayout.Behavior behavior =
                        ((androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) fab.getLayoutParams()).getBehavior();
                if (behavior instanceof com.google.android.material.behavior.HideBottomViewOnScrollBehavior) {
                    ((com.google.android.material.behavior.HideBottomViewOnScrollBehavior<FloatingActionButton>) behavior).slideUp(fab);
                }
            }
        } catch (Exception ignored) {}
    }

    private void setupSwipeToDelete(RecyclerView rv, CategoryAdapter adapter, List<Category> list) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder v, @NonNull RecyclerView.ViewHolder t) { return false; }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, float dX, float dY, int action, boolean active) {
                if (dX < 0) {
                    float density = getResources().getDisplayMetrics().density;
                    View itemView = vh.itemView;
                    GradientDrawable background = new GradientDrawable();
                    background.setColor(ContextCompat.getColor(CategoryManagement.this, R.color.status_red));
                    background.setCornerRadius(16 * density);
                    background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    background.draw(c);

                    Drawable deleteIcon = ContextCompat.getDrawable(CategoryManagement.this, R.drawable.trash_regularsvg);
                    if (deleteIcon != null && dX < -100) {
                        deleteIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                        int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + iconMargin;
                        int iconRight = itemView.getRight() - (int) (16 * density);
                        deleteIcon.setBounds(iconRight - deleteIcon.getIntrinsicWidth(), iconTop, iconRight, iconTop + deleteIcon.getIntrinsicHeight());
                        deleteIcon.draw(c);
                    }
                }
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                Category cat = list.get(pos);
                new AlertDialog.Builder(CategoryManagement.this)
                        .setTitle(R.string.action_delete_category)
                        .setMessage(getString(R.string.confirm_delete, cat.name))
                        .setPositiveButton(R.string.action_delete, (d, w) -> viewModel.deleteCategory(cat.id))
                        .setNegativeButton(R.string.action_cancel, (d, w) -> adapter.notifyItemChanged(pos))
                        .setOnDismissListener(dialogInterface -> adapter.notifyItemChanged(pos))
                        .show();
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(rv);
    }
}