package com.example.cashify.ui.transactions;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;

import androidx.core.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
//TODO: Nếu user đổi sang "Quỹ Nhóm", History phải tự load lại data của Quỹ đó.
public class TransactionFragment extends Fragment {

    private TransactionViewModel viewModel;
    private HistoryAdapter historyAdapter;
    private FilterChipAdapter chipAdapter;
    private RecyclerView rvHistory, rvFilterChips;
    private LinearLayout layoutEmpty;

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
        setupListeners(view);
        setupSwipeToDelete();
    }

    private void initViews(View view) {
        rvHistory = view.findViewById(R.id.rvHistory);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);

        // CHÚ Ý: Bạn nhớ thêm RecyclerView này vào fragment_transaction.xml nhé
        rvFilterChips = view.findViewById(R.id.rvFilterChips);
    }

    private void setupRecyclerViews() {
        // 1. Setup History List
        historyAdapter = new HistoryAdapter();
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        rvHistory.setAdapter(historyAdapter);

        historyAdapter.setOnTransactionClickListener(transaction -> {
            openEditScreen(transaction.id);
        });

        // 2. Setup Filter Chips
        chipAdapter = new FilterChipAdapter(new FilterChipAdapter.OnChipClickListener() {
            @Override
            public void onChipClick(FilterChip chip, int position, View anchorView) {
                switch (chip.getType()) {
                    case DATE:
                        showDatePicker(chip, position);
                        break;
                    case TYPE:
                        showTypeFilterPopup(anchorView, chip, position);
                        break;
                    case METHOD:
                        showMethodFilterPopup(anchorView, chip, position);
                        break;
                    case CATEGORY:
                        showCategoryBottomSheet(chip, position);
                        break;
                }
            }

            @Override
            public void onChipClearClick(FilterChip chip, int position) {
                // Hủy filter trên ViewModel tương ứng
                switch (chip.getType()) {
                    case DATE: viewModel.selectedDateRange.setValue(null); break;
                    case TYPE: viewModel.selectedType.setValue(null); break;
                    case METHOD: viewModel.selectedMethod.setValue(null); break;
                    case CATEGORY: viewModel.selectedCategoryId.setValue(null); break;
                }

                // Trả UI Chip về trạng thái cũ
                chip.setActive(false);
                chip.setActiveLabel(chip.getFilLabel());
                chipAdapter.notifyItemChanged(position);

                // Lấy lại danh sách với query (nếu đang search dở)
                EditText etSearch = getView().findViewById(R.id.etSearch);
                String query = (etSearch != null) ? etSearch.getText().toString().trim() : "";
                viewModel.fetchHistoryData(currentWorkspaceId, query);
            }
        });

        rvFilterChips.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvFilterChips.setAdapter(chipAdapter);
    }

    private void setupObservers() {
        viewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);

        // Lắng nghe dữ liệu Giao dịch
        viewModel.getGroupedTransactions().observe(getViewLifecycleOwner(), items -> {
            if (items != null && !items.isEmpty()) {
                historyAdapter.setHistoryData(items);
                rvHistory.setVisibility(View.VISIBLE);
                layoutEmpty.setVisibility(View.GONE);
            } else {
                rvHistory.setVisibility(View.GONE);
                layoutEmpty.setVisibility(View.VISIBLE);
            }
        });

        // Lắng nghe dữ liệu danh sách Chip
        viewModel.getFilterChips().observe(getViewLifecycleOwner(), chips -> {
            chipAdapter.setChips(chips);
        });

        MainViewModel mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        mainViewModel.syncCompleted.observe(getViewLifecycleOwner(), isDone -> {
            if (isDone != null && isDone) {
                EditText etSearch = getView().findViewById(R.id.etSearch);
                String query = (etSearch != null) ? etSearch.getText().toString().trim() : "";

                viewModel.fetchHistoryData(currentWorkspaceId, query);
            }
        });
    }

    private void setupListeners(View view) {
        // Cập nhật lại Search logic: Chỉ cần truyền query, ViewModel tự kết hợp với các Filter state
        EditText etSearch = view.findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.fetchHistoryData(currentWorkspaceId, s.toString().trim());
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    // --- POPUP MENU CHO CHIP ---
    private void showDatePicker(FilterChip chip, int position) {
        // Khởi tạo MaterialDatePicker cho Date Range
        MaterialDatePicker<Pair<Long, Long>> datePicker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Select Date Range")
                        .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            Long startDate = selection.first;
            Long endDate = selection.second;

            if (startDate != null && endDate != null) {
                // Format ngày để hiển thị trên Chip (VD: "Oct 01 - Oct 31")
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.ENGLISH);
                String label = sdf.format(new Date(startDate)) + " - " + sdf.format(new Date(endDate));

                // Cập nhật UI của Chip
                chip.setActive(true);
                chip.setActiveLabel(label);
                chipAdapter.notifyItemChanged(position);

                // Cập nhật ViewModel & Load data
                viewModel.selectedDateRange.setValue(new long[]{startDate, endDate});
                viewModel.fetchHistoryData(currentWorkspaceId);
            }
        });

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }
    private void showTypeFilterPopup(View anchorView, FilterChip chip, int position) {
        PopupMenu popup = new PopupMenu(requireContext(), anchorView);
        popup.getMenu().add(0, 1, 0, "💰 Income");
        popup.getMenu().add(0, 0, 0, "💸 Expense");

        popup.setOnMenuItemClickListener(item -> {
            int typeId = item.getItemId(); // 1 là Thu, 0 là Chi

            // Cập nhật UI Chip
            chip.setActive(true);
            chip.setActiveLabel(item.getTitle().toString());
            chipAdapter.notifyItemChanged(position);

            // Cập nhật ViewModel & Load data
            viewModel.selectedType.setValue(typeId);
            viewModel.fetchHistoryData(currentWorkspaceId);
            return true;
        });
        popup.show();
    }

    private void showMethodFilterPopup(View anchorView, FilterChip chip, int position) {
        PopupMenu popup = new PopupMenu(requireContext(), anchorView);
        // Tham số thứ 2 chính là itemId (0, 1, 2)
        popup.getMenu().add(0, 0, 0, "💵 Cash");
        popup.getMenu().add(0, 1, 0, "💳 Card");
        popup.getMenu().add(0, 2, 0, "🏦 Bank");

        popup.setOnMenuItemClickListener(item -> {
            // Chuỗi này có chứa Emoji, dùng để hiển thị cho đẹp trên UI
            String displayLabel = item.getTitle().toString();

            // Chuỗi này nguyên bản, dùng để truy vấn Database
            String filterValue = "";
            switch (item.getItemId()) {
                case 0: filterValue = "Cash"; break;
                case 1: filterValue = "Card"; break;
                case 2: filterValue = "Bank"; break;
            }

            // 1. Cập nhật UI Chip (Sẽ hiện "💵 Cash")
            chip.setActive(true);
            chip.setActiveLabel(displayLabel);
            chipAdapter.notifyItemChanged(position);

            // 2. Cập nhật ViewModel & Load data (Chỉ truyền "Cash" xuống)
            viewModel.selectedMethod.setValue(filterValue);
            viewModel.fetchHistoryData(currentWorkspaceId);
            return true;
        });
        popup.show();
    }
    private void showCategoryBottomSheet(FilterChip chip, int position) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());

        // 1. Gọi giao diện XML
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_filter, null);

        // Ánh xạ RecyclerView thay vì ListView
        RecyclerView rvCategories = bottomSheetView.findViewById(R.id.rvCategoryFilter);

        // Cài đặt dạng Grid 4 cột giống y hệt màn Add Transaction
        rvCategories.setLayoutManager(new GridLayoutManager(requireContext(), 4));

        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<Category> categories = db.categoryDao().getAll();

            requireActivity().runOnUiThread(() -> {
                if (categories == null || categories.isEmpty()) return;

                // 2. TÁI SỬ DỤNG CategoryPickerAdapter đã có sẵn màu và hiệu ứng siêu đẹp
                CategoryPickerAdapter adapter = new CategoryPickerAdapter(requireContext(), categories, selectedCat -> {
                    // Xử lý khi user bấm chọn 1 danh mục
                    chip.setActive(true);
                    chip.setActiveLabel(selectedCat.name);

                    int resId = getContext().getResources().getIdentifier(selectedCat.iconName, "drawable", getContext().getPackageName());
                    if (resId != 0) chip.setIconRes(resId);

                    chipAdapter.notifyItemChanged(position);

                    viewModel.selectedCategoryId.setValue(selectedCat.id);
                    viewModel.fetchHistoryData(currentWorkspaceId);

                    // Đóng Bottom Sheet tự động
                    bottomSheetDialog.dismiss();
                });

                rvCategories.setAdapter(adapter);

                // Gắn View vào Bottom Sheet và hiển thị
                bottomSheetDialog.setContentView(bottomSheetView);
                bottomSheetDialog.show();
            });
        }).start();
    }
    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                TransactionViewModel.HistoryItem item = historyAdapter.getItemAt(position);

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
                TransactionViewModel.HistoryItem item = historyAdapter.getItemAt(position);

                if (item.getType() == TransactionViewModel.HistoryItem.TYPE_TRANSACTION) {
                    Transaction deletedTrans = item.getTransaction();

                    // 1. Hiện Dialog Confirm hỏi người dùng (Nút Xóa màu Đỏ)
                    DialogHelper.showCustomDialog(
                            requireContext(),
                            getString(R.string.action_delete), // Bạn có thể thay bằng "Xóa giao dịch"
                            "Bạn có chắc chắn muốn xóa giao dịch này không? Hành động này không thể hoàn tác.",
                            "Xóa",
                            "Hủy",
                            DialogHelper.DialogType.DANGER, // DANGER để hiện nút đỏ
                            true, // Cho phép hiện nút Hủy
                            () -> {
                                // Sự kiện khi bấm "Xóa":
                                // A. Tiến hành xóa trong Database
                                viewModel.deleteOnly(deletedTrans);

                                // B. Xóa xong thì gọi Dialog 1 nút (showSuccess) báo thành công
                                DialogHelper.showSuccess(
                                        requireContext(),
                                        "Thành công",
                                        "Đã xóa giao dịch thành công!",
                                        null // Bấm OK tự tắt, không cần làm gì thêm
                                );
                            },
                            () -> {
                                // Sự kiện khi bấm "Hủy":
                                // Cập nhật lại UI để item vừa vuốt nảy ngược trở lại vị trí cũ
                                historyAdapter.notifyItemChanged(position);
                            }
                    );

                } else {
                    // Nếu lỡ vuốt trúng cục Header Ngày Tháng thì cũng nảy ngược lại (ko cho xóa header)
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
        EditText etSearch = getView().findViewById(R.id.etSearch);
        String query = (etSearch != null) ? etSearch.getText().toString().trim() : "";
        viewModel.fetchHistoryData(currentWorkspaceId, query);
    }
}
//huhu