package com.example.cashify.ui.category;

import android.graphics.Canvas;
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
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.Category;
import com.example.cashify.utils.DialogHelper;
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
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

        viewModel = new ViewModelProvider(this).get(CategoryViewModel.class);

        initViews();
        setupRecyclerViews();
        observeData();
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
                DialogHelper.showCustomDialog(
                        CategoryManagement.this,
                        getString(R.string.action_delete_category),
                        getString(R.string.confirm_delete, category.name),
                        getString(R.string.action_delete),
                        getString(R.string.action_cancel),
                        DialogHelper.DialogType.DANGER,
                        true,
                        () -> viewModel.deleteCategory(category.id),
                        null
                );
            }

            @Override
            public void onEditClick(Category category) {
                CategoryBottomSheet.newInstance(category).show(getSupportFragmentManager(), "EditCategory");
            }

            @Override
            public void onRestoreClick(Category category) {
                DialogHelper.showCustomDialog(
                        CategoryManagement.this,
                        "Restore Category",
                        "Are you sure you want to restore " + category.name + "?",
                        "Restore",
                        "Cancel",
                        DialogHelper.DialogType.NORMAL,
                        true,
                        () -> viewModel.restoreCategory(category.id),
                        null
                );
            }
        };

        // Initialize with empty lists; data will be injected via ViewModel
        adapterChi = new CategoryAdapter(this, new ArrayList<>(), listener);
        rvChi.setLayoutManager(new LinearLayoutManager(this));
        rvChi.setAdapter(adapterChi);
        setupSwipeToDelete(rvChi, adapterChi);

        adapterThu = new CategoryAdapter(this, new ArrayList<>(), listener);
        rvThu.setLayoutManager(new LinearLayoutManager(this));
        rvThu.setAdapter(adapterThu);
        setupSwipeToDelete(rvThu, adapterThu);
    }

    private void observeData() {
        viewModel.getExpenseCategories().observe(this, categories -> {
            listChiOriginal = categories != null ? categories : new ArrayList<>();
            filterCategories(edtSearch.getText().toString());
        });

        viewModel.getIncomeCategories().observe(this, categories -> {
            listThuOriginal = categories != null ? categories : new ArrayList<>();
            filterCategories(edtSearch.getText().toString());
        });
    }

    private void setupActions() {
        FloatingActionButton fabAddCategory = findViewById(R.id.fab_add_category);
        fabAddCategory.setOnClickListener(v ->
                CategoryBottomSheet.newInstance(null).show(getSupportFragmentManager(), "CategoryBottomSheet")
        );

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCategories(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Filters the original category lists based on the search query
     * and delegates UI updates to the adapters.
     */
    private void filterCategories(String query) {
        String pattern = query.toLowerCase().trim();
        filteredChi.clear();
        filteredThu.clear();
        for (Category c : listChiOriginal) {
            if (c.name.toLowerCase().contains(pattern)) filteredChi.add(c);
        }
        for (Category c : listThuOriginal) {
            if (c.name.toLowerCase().contains(pattern)) filteredThu.add(c);
        }

        // Delegate update to adapter instead of modifying the underlying list directly
        adapterChi.updateData(filteredChi);
        adapterThu.updateData(filteredThu);

        // Auto-expand lists if the user is actively searching
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

    /**
     * Forces the FAB to slide back up if it was hidden by a nested scroll event
     * prior to the list being collapsed.
     */
    private void rescueFloatingActionButton() {
        try {
            FloatingActionButton fab = findViewById(R.id.fab_add_category);
            if (fab != null && fab.getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
                CoordinatorLayout.Behavior<?> behavior = ((CoordinatorLayout.LayoutParams) fab.getLayoutParams()).getBehavior();
                if (behavior instanceof HideBottomViewOnScrollBehavior) {
                    ((HideBottomViewOnScrollBehavior<FloatingActionButton>) behavior).slideUp(fab);
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Attaches a swipe-to-delete behavior to the provided RecyclerView.
     */
    private void setupSwipeToDelete(RecyclerView rv, CategoryAdapter targetAdapter) {
        // Pre-allocate drawing tools to optimize onChildDraw performance (60fps)
        float density = getResources().getDisplayMetrics().density;

        GradientDrawable background = new GradientDrawable();
        background.setColor(ContextCompat.getColor(this, R.color.status_red));
        background.setCornerRadius(16 * density);

        Drawable deleteIcon = ContextCompat.getDrawable(this, R.drawable.ic_trash_regular);
        if (deleteIcon != null) {
            deleteIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        }

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder v, @NonNull RecyclerView.ViewHolder t) {
                return false;
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                                    float dX, float dY, int action, boolean active) {

                // Mandatory call to ensure the ItemView translates with the swipe gesture
                super.onChildDraw(c, rv, vh, dX, dY, action, active);

                if (dX < 0) { // Swiping Left
                    View itemView = vh.itemView;

                    // 1. Draw expanding red background
                    background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    background.draw(c);

                    // 2. Draw trash icon once the swipe passes a threshold
                    if (deleteIcon != null && dX < -100) {
                        int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + iconMargin;
                        int iconRight = itemView.getRight() - (int) (16 * density);

                        deleteIcon.setBounds(
                                iconRight - deleteIcon.getIntrinsicWidth(),
                                iconTop,
                                iconRight,
                                iconTop + deleteIcon.getIntrinsicHeight()
                        );
                        deleteIcon.draw(c);
                    }
                }
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Defensive check to handle rapid swiping issues
                int pos = viewHolder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                // We need to fetch the item from the adapter to ensure accuracy,
                // however, we cannot retrieve it directly as the adapter doesn't expose a 'getItem(int)' method yet.
                // Assuming you will implement `public Category getItem(int pos)` in CategoryAdapter,
                // or just leave it as it was if you are fine using the internal list of the Activity.

                // Let's rely on the lists maintained by the activity for simplicity.
                Category cat = (targetAdapter == adapterChi) ? filteredChi.get(pos) : filteredThu.get(pos);

                DialogHelper.showCustomDialog(
                        CategoryManagement.this,
                        getString(R.string.action_delete_category),
                        getString(R.string.confirm_delete, cat.name),
                        getString(R.string.action_delete),
                        getString(R.string.action_cancel),
                        DialogHelper.DialogType.DANGER,
                        true,
                        () -> viewModel.deleteCategory(cat.id),
                        () -> {
                            // On Cancel/Dismiss: Post to UI thread to smoothly animate the item bouncing back
                            rv.post(() -> targetAdapter.notifyItemChanged(pos));
                        }
                );
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(rv);
    }
}