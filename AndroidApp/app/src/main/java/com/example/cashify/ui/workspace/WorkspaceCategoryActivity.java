package com.example.cashify.ui.workspace;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.Category;
import com.example.cashify.ui.category.CategoryAdapter;
import com.example.cashify.utils.DialogHelper;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
    private FirebaseFirestore db;
    private ListenerRegistration categoryListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        workspaceId = getIntent().getStringExtra("WORKSPACE_ID");
        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerViews();
        setupActions();

        // Lắng nghe data trực tiếp từ Firestore thay vì ViewModel
        observeFirestoreData();
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
                        WorkspaceCategoryActivity.this, "Hide Category", "Are you sure you want to hide '" + category.name + "'? Future transactions cannot use this.", "Hide", "Cancel", DialogHelper.DialogType.DANGER, true,
                        () -> {
                            // GỌI CÁP XUỐNG C# ĐỂ KIỂM DUYỆT VÀ SOFT DELETE
                            com.example.cashify.data.remote.FirebaseManager.getInstance().deleteCategory(workspaceId, category.firestoreId, new com.example.cashify.data.remote.FirebaseManager.DataCallback<Void>() {
                                @Override
                                public void onSuccess(Void data) {
                                    runOnUiThread(() -> ToastHelper.show(WorkspaceCategoryActivity.this, "Category hidden successfully"));
                                }
                                @Override
                                public void onError(String message) {
                                    runOnUiThread(() -> ToastHelper.show(WorkspaceCategoryActivity.this, message));
                                }
                            });
                        }, null
                );
            }

            @Override
            public void onEditClick(Category category) {
                WorkspaceCategoryBottomSheet.newInstance(workspaceId, category).show(getSupportFragmentManager(), "EditCategory");
            }
        };

        adapterChi = new CategoryAdapter(this, filteredChi, listener);
        rvChi.setLayoutManager(new LinearLayoutManager(this));
        rvChi.setAdapter(adapterChi);

        adapterThu = new CategoryAdapter(this, filteredThu, listener);
        rvThu.setLayoutManager(new LinearLayoutManager(this));
        rvThu.setAdapter(adapterThu);

        // Bồ gọi lại hàm setupSwipeToDelete của bồ ở đây nhé
    }

    private void observeFirestoreData() {
        if (workspaceId == null) return;
        // GÁN NÓ VÀO BIẾN ĐỂ LÁT NỮA HỦY
        categoryListener = db.collection("workspaces").document(workspaceId).collection("categories")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        // Tùy chọn: Log ra logcat thay vì Toast để user đỡ hoảng nếu lỡ có rớt mạng xíu
                        android.util.Log.e("FIRESTORE", "Listen failed.", e);
                        // ToastHelper.show(this, "Error loading categories");
                        return;
                    }

                    listChiOriginal.clear();
                    listThuOriginal.clear();

                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Category cat = doc.toObject(Category.class);
                            cat.firestoreId = doc.getId(); // Gán Document ID vào model

                            if (cat.type == 0) listChiOriginal.add(cat);
                            else listThuOriginal.add(cat);
                        }
                    }
                    filterCategories(edtSearch.getText().toString());
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

    //Cứu nút FAB khỏi bị kẹt lúc cuộn
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
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy lắng nghe Firestore khi thoát màn hình để tránh leak memory và lỗi lúc Logout
        if (categoryListener != null) {
            categoryListener.remove();
        }
    }
}