package com.example.cashify.ui.transactions;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.local.AppDatabase;
import com.example.cashify.data.model.Category;
import com.example.cashify.data.model.Transaction;
import com.example.cashify.ui.main.MainViewModel;
import com.example.cashify.ui.main.PersonalWorkspaceHeader;
import com.example.cashify.utils.DialogHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionFragment extends Fragment {

    private TransactionViewModel viewModel;
    private HistoryAdapter historyAdapter;
    private FilterChipAdapter chipAdapter;
    private RecyclerView rvHistory, rvFilterChips;
    private LinearLayout layoutEmpty;
    private EditText etSearch;

    private String currentWorkspaceId = "PERSONAL";

    public TransactionFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerViews();
        setupObservers();
        setupListeners();
        setupSwipeToDelete();
        PersonalWorkspaceHeader.bind(this, view);
    }

    private void initViews(View view) {
        rvHistory = view.findViewById(R.id.rvHistory);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        etSearch = view.findViewById(R.id.etSearch);
        rvFilterChips = view.findViewById(R.id.rvFilterChips);
    }

    private void setupRecyclerViews() {
        // 1. Setup History List
        historyAdapter = new HistoryAdapter();
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvHistory.setAdapter(historyAdapter);

        historyAdapter.setOnTransactionClickListener(transaction -> openEditScreen(transaction.id));

        // 2. Setup Filter Chips
        chipAdapter = new FilterChipAdapter(new FilterChipAdapter.OnChipClickListener() {
            @Override
            public void onChipClick(FilterChip chip, int position, View anchorView) {
                switch (chip.getType()) {
                    case DATE: showDatePicker(chip, position); break;
                    case TYPE: showTypeFilterPopup(anchorView, chip, position); break;
                    case METHOD: showMethodFilterPopup(anchorView, chip, position); break;
                    case CATEGORY: showCategoryBottomSheet(chip, position); break;
                }
            }

            @Override
            public void onChipClearClick(FilterChip chip, int position) {
                // Remove filter state from ViewModel
                switch (chip.getType()) {
                    case DATE: viewModel.selectedDateRange.setValue(null); break;
                    case TYPE: viewModel.selectedType.setValue(null); break;
                    case METHOD: viewModel.selectedMethod.setValue(null); break;
                    case CATEGORY: viewModel.selectedCategoryId.setValue(null); break;
                }

                // Reset Chip UI to default inactive state
                chip.setActive(false);
                chip.setActiveLabel(chip.getFilLabel());
                chipAdapter.notifyItemChanged(position);

                // Fetch data with current search query applied
                String query = (etSearch != null) ? etSearch.getText().toString().trim() : "";
                viewModel.fetchHistoryData(currentWorkspaceId, query);
            }
        });

        rvFilterChips.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvFilterChips.setAdapter(chipAdapter);
    }

    private void setupObservers() {
        viewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);

        // Observe Grouped Transaction Data
        viewModel.getGroupedTransactions().observe(getViewLifecycleOwner(), items -> {
            if (items != null && !items.isEmpty()) {
                historyAdapter.setHistoryData(items);
                rvHistory.setVisibility(View.VISIBLE);
                layoutEmpty.setVisibility(View.GONE);
            } else {
                historyAdapter.setHistoryData(null);
                rvHistory.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
            }
        });

        // Observe Chip Configurations
        viewModel.getFilterChips().observe(getViewLifecycleOwner(), chips -> chipAdapter.setChips(chips));

        // Sync observation
        MainViewModel mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        mainViewModel.syncCompleted.observe(getViewLifecycleOwner(), isDone -> {
            if (Boolean.TRUE.equals(isDone)) {
                String query = (etSearch != null) ? etSearch.getText().toString().trim() : "";
                viewModel.fetchHistoryData(currentWorkspaceId, query);
            }
        });
    }

    private void setupListeners() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.fetchHistoryData(currentWorkspaceId, s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // =========================================================================
    // FILTER POPUPS & MENUS
    // =========================================================================

    private void showDatePicker(FilterChip chip, int position) {
        MaterialDatePicker<Pair<Long, Long>> datePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Date Range")
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Long startDate = selection.first;
            Long endDate = selection.second;

            if (startDate != null && endDate != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.ENGLISH);
                String label = sdf.format(new Date(startDate)) + " - " + sdf.format(new Date(endDate));

                chip.setActive(true);
                chip.setActiveLabel(label);
                chipAdapter.notifyItemChanged(position);

                viewModel.selectedDateRange.setValue(new long[]{startDate, endDate});
                viewModel.fetchHistoryData(currentWorkspaceId);
            }
        });

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void showTypeFilterPopup(View anchorView, FilterChip chip, int position) {
        PopupMenu popup = new PopupMenu(requireContext(), anchorView);
        popup.getMenu().add(0, 1, 0, R.string.income_chip);
        popup.getMenu().add(0, 0, 0, R.string.expense_chip);

        popup.setOnMenuItemClickListener(item -> {
            int typeId = item.getItemId(); // 1 = Income, 0 = Expense

            chip.setActive(true);
            chip.setActiveLabel(item.getTitle().toString());
            chipAdapter.notifyItemChanged(position);

            viewModel.selectedType.setValue(typeId);
            viewModel.fetchHistoryData(currentWorkspaceId);
            return true;
        });
        popup.show();
    }

    private void showMethodFilterPopup(View anchorView, FilterChip chip, int position) {
        PopupMenu popup = new PopupMenu(requireContext(), anchorView);
        popup.getMenu().add(0, 0, 0, R.string.cash_chip);
        popup.getMenu().add(0, 1, 0, R.string.card_chip);
        popup.getMenu().add(0, 2, 0, R.string.bank_chip);

        popup.setOnMenuItemClickListener(item -> {
            String displayLabel = item.getTitle().toString();
            String filterValue = "";
            switch (item.getItemId()) {
                case 0: filterValue = "Cash"; break;
                case 1: filterValue = "Card"; break;
                case 2: filterValue = "Bank"; break;
            }

            chip.setActive(true);
            chip.setActiveLabel(displayLabel);
            chipAdapter.notifyItemChanged(position);

            viewModel.selectedMethod.setValue(filterValue);
            viewModel.fetchHistoryData(currentWorkspaceId);
            return true;
        });
        popup.show();
    }

    private void showCategoryBottomSheet(FilterChip chip, int position) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        Context appContext = requireContext().getApplicationContext();

        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_filter, null);
        RecyclerView rvCategories = bottomSheetView.findViewById(R.id.rvCategoryFilter);
        rvCategories.setLayoutManager(new GridLayoutManager(requireContext(), 4));

        // Note: Ideally, this DB call should be delegated to the ViewModel.
        // Kept here to preserve existing application flow.
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(appContext);
            List<Category> categories = db.categoryDao().getAll();

            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || getContext() == null || categories == null || categories.isEmpty()) return;

                CategoryPickerAdapter adapter = new CategoryPickerAdapter(requireContext(), categories, selectedCat -> {
                    chip.setActive(true);
                    chip.setActiveLabel(selectedCat.name);

                    String iconName = selectedCat.iconName != null ? selectedCat.iconName : "";
                    int resId = requireContext().getResources().getIdentifier(iconName, "drawable", requireContext().getPackageName());
                    if (resId != 0) chip.setIconRes(resId);

                    chipAdapter.notifyItemChanged(position);

                    viewModel.selectedCategoryId.setValue(selectedCat.id);
                    viewModel.fetchHistoryData(currentWorkspaceId);

                    bottomSheetDialog.dismiss();
                });

                rvCategories.setAdapter(adapter);
                bottomSheetDialog.setContentView(bottomSheetView);
                bottomSheetDialog.show();
            });
        }).start();
    }

    // =========================================================================
    // SWIPE ACTIONS
    // =========================================================================

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return makeMovementFlags(0, 0);

                TransactionViewModel.HistoryItem item = historyAdapter.getItemAt(position);

                // Prevent swiping the Date Header
                if (item.getType() == TransactionViewModel.HistoryItem.TYPE_DATE_HEADER) {
                    return makeMovementFlags(0, 0);
                }
                return makeMovementFlags(0, ItemTouchHelper.LEFT);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView r, @NonNull RecyclerView.ViewHolder v, @NonNull RecyclerView.ViewHolder t) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                TransactionViewModel.HistoryItem item = historyAdapter.getItemAt(position);

                if (item.getType() == TransactionViewModel.HistoryItem.TYPE_TRANSACTION) {
                    Transaction deletedTrans = item.getTransaction();

                    DialogHelper.showCustomDialog(
                            requireContext(),
                            getString(R.string.action_delete),
                            "Are you sure? This action cannot be undone.",
                            "Delete",
                            "Cancel",
                            DialogHelper.DialogType.DANGER,
                            true,
                            () -> {
                                // ACTION: DELETE
                                viewModel.deleteOnly(deletedTrans);
                                DialogHelper.showSuccess(
                                        requireContext(),
                                        "Success",
                                        "Transaction deleted successfully",
                                        null
                                );
                            },
                            () -> {
                                // ACTION: CANCEL (Restore item position)
                                historyAdapter.notifyItemChanged(position);
                            }
                    );
                } else {
                    historyAdapter.notifyItemChanged(position);
                }
            }
        }).attachToRecyclerView(rvHistory);
    }

    private void openEditScreen(String transactionId) {
        Intent intent = new Intent(getContext(), AddTransactionActivity.class);
        intent.putExtra("TRANSACTION_ID", transactionId);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        String query = (etSearch != null) ? etSearch.getText().toString().trim() : "";
        viewModel.fetchHistoryData(currentWorkspaceId, query);
    }
}