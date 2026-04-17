package com.example.cashify.ui.transactions;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.database.Transaction;
import com.example.cashify.viewmodel.TransactionViewModel;
import com.google.android.material.snackbar.Snackbar;

public class TransactionFragment extends Fragment {

    private TransactionViewModel viewModel;
    private HistoryAdapter adapter;
    private RecyclerView rvHistory;
    private LinearLayout layoutEmpty;

    private TextView filterAll, filterIncome, filterExpense;
    private String currentFilter = "ALL";

    public TransactionFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupObservers();
        setupListeners(view);
        setupSwipeToDelete();
    }

    private void initViews(View view) {
        rvHistory = view.findViewById(R.id.rvHistory);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        filterAll = view.findViewById(R.id.filterAll);
        filterIncome = view.findViewById(R.id.filterIncome);
        filterExpense = view.findViewById(R.id.filterExpense);
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter();
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvHistory.setAdapter(adapter);


        adapter.setOnTransactionClickListener(transaction -> {
            openEditScreen(transaction.id);
        });
    }

    private void setupObservers() {
        viewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);

        viewModel.getGroupedTransactions().observe(getViewLifecycleOwner(), items -> {
            if (items != null && !items.isEmpty()) {
                adapter.setHistoryData(items);
                rvHistory.setVisibility(View.VISIBLE);
                layoutEmpty.setVisibility(View.GONE);
            } else {
                rvHistory.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setupListeners(View view) {
        filterAll.setOnClickListener(v -> handleFilterClick("ALL"));
        filterIncome.setOnClickListener(v -> handleFilterClick("INCOME"));
        filterExpense.setOnClickListener(v -> handleFilterClick("EXPENSE"));


        // Search logic
        EditText etSearch = view.findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.fetchHistoryData(currentFilter, s.toString().trim());
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                TransactionViewModel.HistoryItem item = adapter.getItemAt(position);

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
                TransactionViewModel.HistoryItem item = adapter.getItemAt(position);

                if (item.getType() == TransactionViewModel.HistoryItem.TYPE_TRANSACTION) {
                    Transaction deletedTrans = item.getTransaction();

                    // Xóa trong Database và UI ngay lập tức
                    viewModel.deleteOnly(deletedTrans);

                    // Hiển thị Snackbar với nút Undo (Yêu cầu 3)
                    Snackbar.make(rvHistory, "Transaction deleted", Snackbar.LENGTH_LONG)
                            .setAction("Undo", v -> {
                                viewModel.insertOnly(deletedTrans); // Khôi phục lại
                            }).show();
                } else {
                    adapter.notifyItemChanged(position); // Không cho xóa header ngày
                }
            }
        }).attachToRecyclerView(rvHistory);
    }

    private void openEditScreen(int transactionId) {
        // Chuyển hướng sang AddTransactionActivity với ID để kích hoạt Edit Mode
        Intent intent = new Intent(getContext(), AddTransactionActivity.class);
        intent.putExtra("TRANSACTION_ID", transactionId);
        startActivity(intent);
    }

    private void handleFilterClick(String type) {
        if (currentFilter.equals(type)) return;
        currentFilter = type;
        updateFilterUI();
        viewModel.fetchHistoryData(type);
    }

    private void updateFilterUI() {
        setFilterStyle(filterAll, currentFilter.equals("ALL"));
        setFilterStyle(filterIncome, currentFilter.equals("INCOME"));
        setFilterStyle(filterExpense, currentFilter.equals("EXPENSE"));
    }

    private void setFilterStyle(TextView view, boolean isActive) {
        if (isActive) {
            view.setBackgroundResource(R.drawable.bg_filter_active);
            view.setTextColor(Color.WHITE);
        } else {
            view.setBackgroundResource(R.drawable.bg_filter_inactive);
            view.setTextColor(ContextCompat.getColor(requireContext(), R.color.item_title));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Luôn làm mới dữ liệu khi quay lại từ màn hình Sửa
        EditText etSearch = getView().findViewById(R.id.etSearch);
        String query = (etSearch != null) ? etSearch.getText().toString().trim() : "";
        viewModel.fetchHistoryData(currentFilter, query);
    }
}