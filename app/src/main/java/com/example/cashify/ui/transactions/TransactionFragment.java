    package com.example.cashify.ui.transactions;

    import android.content.res.ColorStateList;
    import android.graphics.Color;
    import android.graphics.drawable.ColorDrawable;
    import android.os.Bundle;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.ArrayAdapter;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.LinearLayout;
    import android.widget.Spinner;
    import android.widget.TextView;

    import androidx.annotation.NonNull;
    import androidx.annotation.Nullable;
    import androidx.appcompat.app.AlertDialog;
    import androidx.core.content.ContextCompat;
    import androidx.fragment.app.Fragment;
    import androidx.lifecycle.ViewModelProvider;
    import androidx.recyclerview.widget.ItemTouchHelper;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

    import com.example.cashify.R;
    import com.example.cashify.database.AppDatabase;
    import com.example.cashify.database.Category;
    import com.example.cashify.database.CategoryDao;
    import com.example.cashify.database.Transaction;
    import com.example.cashify.viewmodel.TransactionViewModel;

    import java.util.ArrayList;
    import java.util.List;

    public class TransactionFragment extends Fragment {

        private TransactionViewModel viewModel;
        private HistoryAdapter adapter;
        private RecyclerView rvHistory;
        private LinearLayout layoutEmpty;

        // Khai báo các nút lọc
        private TextView filterAll, filterIncome, filterExpense;
        private String currentFilter = "ALL"; // ALL, INCOME, EXPENSE

        public TransactionFragment() {}

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_transaction, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            // 1. Ánh xạ View
            rvHistory = view.findViewById(R.id.rvHistory);
            layoutEmpty = view.findViewById(R.id.layoutEmpty);
            filterAll = view.findViewById(R.id.filterAll);
            filterIncome = view.findViewById(R.id.filterIncome);
            filterExpense = view.findViewById(R.id.filterExpense);

            View btnEmptyAdd = view.findViewById(R.id.btnEmptyAdd);
            if (btnEmptyAdd != null) {
                btnEmptyAdd.setOnClickListener(v -> {
                    // Mở màn hình thêm giao dịch của bạn ở đây
                    // Ví dụ: NavHostFragment.findNavController(this).navigate(R.id.action_to_addTransaction);
                });
            }
            // 2. Cấu hình Adapter
            adapter = new HistoryAdapter();
            rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
            rvHistory.setAdapter(adapter);

            // 3. Sự kiện Filter Click
            filterAll.setOnClickListener(v -> handleFilterClick("ALL"));
            filterIncome.setOnClickListener(v -> handleFilterClick("INCOME"));
            filterExpense.setOnClickListener(v -> handleFilterClick("EXPENSE"));

            // 4. Kết nối ViewModel & Quan sát dữ liệu
            viewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);

            // Quan sát dữ liệu đã được lọc từ ViewModel
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

            // 5. Các cài đặt khác
            adapter.setOnItemLongClickListener(this::showEditDialog);
            view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());
            setupSwipeToDelete();
            // 6. Xử lý tìm kiếm (Search)
            EditText etSearch = view.findViewById(R.id.etSearch);
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString().trim();
                    // Gọi ViewModel để lọc dữ liệu dựa trên Filter hiện tại và từ khóa tìm kiếm
                    viewModel.fetchHistoryData(currentFilter, query);
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });

            // Mặc định load dữ liệu "All"
            viewModel.fetchHistoryData("ALL");
        }
        private void handleFilterClick(String type) {
            if (currentFilter.equals(type)) return;

            currentFilter = type;
            updateFilterUI();

            // Gọi ViewModel lọc dữ liệu
            // Bạn cần cập nhật ViewModel để nhận tham số type (0 cho Expense, 1 cho Income)
            viewModel.fetchHistoryData(type);
        }

        private void updateFilterUI() {
            // Reset tất cả về Inactive style
            setFilterStyle(filterAll, currentFilter.equals("ALL"));
            setFilterStyle(filterIncome, currentFilter.equals("INCOME"));
            setFilterStyle(filterExpense, currentFilter.equals("EXPENSE"));
        }

        private void setFilterStyle(TextView view, boolean isActive) {
            if (isActive) {
                view.setBackgroundResource(R.drawable.bg_filter_active);
                view.setTextColor(Color.WHITE);
                // Nếu dùng Material3 có thể dùng setTextAppearance(R.style.filter_tab_item_Active)
            } else {
                view.setBackgroundResource(R.drawable.bg_filter_inactive);
                view.setTextColor(ContextCompat.getColor(requireContext(), R.color.item_title));
            }
        }
        private void setupSwipeToDelete() {
            ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                @Override
                public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    int position = viewHolder.getAdapterPosition();
                    TransactionViewModel.HistoryItem item = adapter.getItemAt(position);

                    // Chỉ cho phép xóa nếu là item giao dịch (không xóa header ngày)
                    if (item.getType() == TransactionViewModel.HistoryItem.TYPE_TRANSACTION) {
                        showDeleteDialog(item.getTransaction(), position);
                    } else {
                        adapter.notifyItemChanged(position); // Trả lại vị trí cũ nếu là header
                    }
                }
            };
            new ItemTouchHelper(callback).attachToRecyclerView(rvHistory);
        }

        private void showDeleteDialog(Transaction transaction, int position) {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_confirm, null);
            AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            // Nút Xóa
            dialogView.findViewById(R.id.btnConfirmDelete).setOnClickListener(v -> {
                viewModel.deleteAndRefresh(transaction);
                dialog.dismiss();
            });

            // Nút Hủy
            dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> {
                adapter.notifyItemChanged(position); // Reset lại trạng thái quẹt của item
                dialog.dismiss();
            });

            dialog.setOnCancelListener(d -> adapter.notifyItemChanged(position));
            dialog.show();
        }

        private boolean isExpenseType = true; // Biến tạm để check loại trong Dialog

        private void showEditDialog(Transaction transaction) {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_transaction, null);
            AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            // --- Ánh xạ View ---
            Button btnTabExpense = dialogView.findViewById(R.id.btnTabExpense);
            Button btnTabIncome = dialogView.findViewById(R.id.btnTabIncome);
            EditText editAmount = dialogView.findViewById(R.id.editAmount);
            EditText editNote = dialogView.findViewById(R.id.editNote);
            Spinner spCategory = dialogView.findViewById(R.id.spCategory);
            Button btnUpdate = dialogView.findViewById(R.id.btnUpdate);

            // --- Gán dữ liệu ban đầu từ giao dịch cũ ---
            isExpenseType = (transaction.type == 0);
            editAmount.setText(String.valueOf(transaction.amount));
            editNote.setText(transaction.note);

            // Hàm phụ để load lại Spinner & màu sắc Tab
            refreshEditDialogUI(btnTabExpense, btnTabIncome, btnUpdate, spCategory, transaction.categoryId);

            // --- Sự kiện nhấn Tab Expense ---
            btnTabExpense.setOnClickListener(v -> {
                if (!isExpenseType) {
                    isExpenseType = true;
                    refreshEditDialogUI(btnTabExpense, btnTabIncome, btnUpdate, spCategory, -1);
                }
            });

            // --- Sự kiện nhấn Tab Income ---
            btnTabIncome.setOnClickListener(v -> {
                if (isExpenseType) {
                    isExpenseType = false;
                    refreshEditDialogUI(btnTabExpense, btnTabIncome, btnUpdate, spCategory, -1);
                }
            });

            // --- Nút Cập nhật ---
            btnUpdate.setOnClickListener(v -> {
                String amt = editAmount.getText().toString();
                if (amt.isEmpty()) {
                    editAmount.setError("Nhập số tiền");
                    return;
                }

                // Lấy lại danh sách Category object từ Tag của Spinner
                List<Category> currentCats = (List<Category>) spCategory.getTag();
                if (currentCats == null || currentCats.isEmpty()) return;

                int pos = spCategory.getSelectedItemPosition();
                Category selectedCat = currentCats.get(pos); // Đây mới là object thật

                // Cập nhật
                transaction.amount = Long.parseLong(amt);
                transaction.type = isExpenseType ? 0 : 1;
                transaction.categoryId = selectedCat.id;
                transaction.note = editNote.getText().toString();

                viewModel.updateAndRefresh(transaction);
                dialog.dismiss();
            });

            dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        }
        private void refreshEditDialogUI(Button btnEx, Button btnIn, Button btnUpdate, Spinner sp, int oldCategoryId) {
            int type = isExpenseType ? 0 : 1;
            // Lấy màu từ file colors.xml của bạn cho đồng bộ
            int activeColor = isExpenseType ? ContextCompat.getColor(requireContext(), R.color.status_red)
                    : ContextCompat.getColor(requireContext(), R.color.status_green);

            String btnText = isExpenseType ? "Update Expense" : "Update Income";

            // 1. Cập nhật UI ngay lập tức
            btnEx.setBackgroundTintList(ColorStateList.valueOf(isExpenseType ? activeColor : Color.TRANSPARENT));
            btnEx.setTextColor(isExpenseType ? Color.WHITE : Color.GRAY);

            btnIn.setBackgroundTintList(ColorStateList.valueOf(!isExpenseType ? activeColor : Color.TRANSPARENT));
            btnIn.setTextColor(!isExpenseType ? Color.WHITE : Color.GRAY);

            btnUpdate.setText(btnText);
            btnUpdate.setBackgroundTintList(ColorStateList.valueOf(activeColor));

            // 2. Xóa adapter cũ để tránh chọn nhầm Category của type cũ
            sp.setAdapter(null);

            // 3. Load Category mới
            new Thread(() -> {
                CategoryDao categoryDao = AppDatabase.getInstance(requireContext()).categoryDao();
                List<Category> categories = categoryDao.getCategoriesByType(type);

                List<String> categoryNames = new ArrayList<>();
                int selectedPosition = 0;
                for (int i = 0; i < categories.size(); i++) {
                    categoryNames.add(categories.get(i).name);
                    // Nếu là lần đầu mở (oldCategoryId != -1), tìm vị trí cũ
                    if (oldCategoryId != -1 && categories.get(i).id == oldCategoryId) {
                        selectedPosition = i;
                    }
                }

                if (getActivity() != null) {
                    int finalPos = selectedPosition;
                    getActivity().runOnUiThread(() -> {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                                android.R.layout.simple_spinner_item, categoryNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        sp.setAdapter(adapter);

                        // Chỉ set selection nếu tìm thấy vị trí phù hợp
                        if (finalPos < categories.size()) {
                            sp.setSelection(finalPos);
                        }

                        sp.setTag(categories); // Lưu lại list object để lấy ID khi bấm Update
                    });
                }
            }).start();
        }

        @Override
        public void onResume() {
            super.onResume();
            viewModel.fetchHistoryData();
        }
    }