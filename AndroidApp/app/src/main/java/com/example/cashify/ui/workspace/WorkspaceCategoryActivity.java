package com.example.cashify.ui.workspace;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.Category;
import com.example.cashify.ui.category.CategoryAdapter;
import com.example.cashify.utils.DialogHelper;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class WorkspaceCategoryActivity extends AppCompatActivity {
    private RecyclerView rvChi, rvThu;
    private CategoryAdapter adapterChi, adapterThu;
    private EditText edtSearch;

    private List<Category> listChiOriginal = new ArrayList<>();
    private List<Category> listThuOriginal = new ArrayList<>();
    private List<Category> filteredChi = new ArrayList<>();
    private List<Category> filteredThu = new ArrayList<>();

    private String workspaceId;
    private WorkspaceCategoryViewModel viewModel; // BỔ SUNG VIEWMODEL

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        workspaceId = getIntent().getStringExtra("WORKSPACE_ID");

        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(WorkspaceCategoryViewModel.class);

        initViews();
        setupRecyclerViews();
        setupActions();

        // Quan sát dữ liệu an toàn qua ViewModel
        observeViewModel();

        // Kích hoạt việc tải danh sách từ Firebase
        viewModel.loadCategories(workspaceId);
    }

    private void initViews() {
        rvChi = findViewById(R.id.recyclerChiRa);
        rvThu = findViewById(R.id.recyclerThuVao);
        edtSearch = findViewById(R.id.edtSearchCategory);
        ImageView btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        findViewById(R.id.layoutHeaderChi).setOnClickListener(v -> toggleRecyclerView(rvChi, findViewById(R.id.tvArrowChi)));
        findViewById(R.id.layoutHeaderThu).setOnClickListener(v -> toggleRecyclerView(rvThu, findViewById(R.id.tvArrowThu)));

        rvChi.setVisibility(View.VISIBLE);
        rvThu.setVisibility(View.VISIBLE);
    }

    private void setupRecyclerViews() {
        CategoryAdapter.OnCategoryListener listener = new CategoryAdapter.OnCategoryListener() {
            @Override
            public void onDeleteClick(Category category) {
                DialogHelper.showCustomDialog(
                        WorkspaceCategoryActivity.this, "Hide Category",
                        "Are you sure you want to hide '" + category.name + "'? Future transactions cannot use this.",
                        "Hide", "Cancel", DialogHelper.DialogType.DANGER, true,
                        () -> viewModel.deleteCategory(workspaceId, category.firestoreId),
                        null
                );
            }

            @Override
            public void onEditClick(Category category) {
                WorkspaceCategoryBottomSheet.newInstance(workspaceId, category).show(getSupportFragmentManager(), "EditCategory");
            }

            @Override
            public void onRestoreClick(Category category) {
                DialogHelper.showCustomDialog(
                        WorkspaceCategoryActivity.this, "Restore Category",
                        "Are you sure you want to restore " + category.name + "?",
                        "Restore", "Cancel", DialogHelper.DialogType.NORMAL, true,
                        () -> viewModel.restoreCategory(workspaceId, category.firestoreId),
                        null
                );
            }
        };

        adapterChi = new CategoryAdapter(this, filteredChi, listener);
        rvChi.setLayoutManager(new LinearLayoutManager(this));
        rvChi.setAdapter(adapterChi);

        adapterThu = new CategoryAdapter(this, filteredThu, listener);
        rvThu.setLayoutManager(new LinearLayoutManager(this));
        rvThu.setAdapter(adapterThu);
    }

    private void observeViewModel() {
        // Hóng danh sách danh mục Real-time
        viewModel.getCategoriesLiveData().observe(this, categories -> {
            listChiOriginal.clear();
            listThuOriginal.clear();

            if (categories != null) {
                for (Category cat : categories) {
                    if (cat.type == 0) listChiOriginal.add(cat);
                    else listThuOriginal.add(cat);
                }
            }
            filterCategories(edtSearch.getText().toString());
        });

        // Hóng kết quả Xóa/Khôi phục để hiện Toast
        viewModel.getActionResult().observe(this, result -> {
            if (result != null) {
                if (result.isSuccess) {
                    ToastHelper.show(this, result.message);
                } else {
                    ToastHelper.show(this, "Error: " + result.message);
                }
                viewModel.clearActionResult();
            }
        });
    }

    private void setupActions() {
        FloatingActionButton fabAddCategory = findViewById(R.id.fab_add_category);
        fabAddCategory.setOnClickListener(v -> {
            WorkspaceCategoryBottomSheet.newInstance(workspaceId, null).show(getSupportFragmentManager(), "WorkspaceCategoryBottomSheet");
        });

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterCategories(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filterCategories(String query) {
        String pattern = query.toLowerCase().trim();
        filteredChi.clear();
        filteredThu.clear();

        for (Category c : listChiOriginal)
            if (c.name.toLowerCase().contains(pattern)) filteredChi.add(c);

        for (Category c : listThuOriginal)
            if (c.name.toLowerCase().contains(pattern)) filteredThu.add(c);

        adapterChi.notifyDataSetChanged();
        adapterThu.notifyDataSetChanged();
    }

    private void toggleRecyclerView(RecyclerView rv, TextView arrow) {
        ViewGroup root = findViewById(R.id.content_layout);
        if (root != null) android.transition.TransitionManager.beginDelayedTransition(root);

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
}