package com.example.cashify.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.database.AppDatabase;
import com.example.cashify.database.Budget;
import com.example.cashify.database.BudgetDao;
import com.example.cashify.database.BudgetWithSpent;
import com.example.cashify.utils.CurrencyFormatter;

import java.util.Calendar;
import java.util.List;

public class BudgetFragment extends Fragment {

    private BudgetAdapter adapter;
    private BudgetDao budgetDao;

    // Các View của thẻ Master Budget
    private TextView tvMasterLimit, tvMasterSpent, tvMasterRemaining;
    private ProgressBar pbMaster;
    private Budget masterBudgetCache; // Lưu tạm dữ liệu Master
    private double currentMasterSpent = 0.0; // Lưu tạm tiền Master đã tiêu

    public BudgetFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_budget, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppDatabase db = AppDatabase.getInstance(requireContext());
        budgetDao = db.budgetDao();

        // Ánh xạ thẻ Master
        tvMasterLimit = view.findViewById(R.id.tvMasterLimit);
        tvMasterSpent = view.findViewById(R.id.tvMasterSpent);
        tvMasterRemaining = view.findViewById(R.id.tvMasterRemaining);
        pbMaster = view.findViewById(R.id.pbMaster);

        // 1. Click Master Budget
        CardView cardMaster = view.findViewById(R.id.cardMaster);
        if (cardMaster != null) {
            cardMaster.setOnClickListener(v -> {
                double limit = masterBudgetCache != null ? masterBudgetCache.limitAmount : 0;
                //mở numpad, truyền id = -1 (master) vô
                openNumpadToEditBudget(-1, limit);
                BudgetBottomSheetDialog bottomSheet = new BudgetBottomSheetDialog(
                        -1, "Master Budget", currentMasterSpent, limit,
                        new BudgetBottomSheetDialog.OnBudgetActionListener()
                        {
                            @Override
                            public void onSave(int selectedId, double newLimit) {
                                saveBudgetToDatabase(-1, newLimit);
                            }

                            @Override
                            public void onDelete(int categoryId) {
                                deleteBudgetFromDatabase(categoryId);
                            }
                        }
                );
                bottomSheet.show(getParentFragmentManager(), "BudgetBottomSheet");
            });
        }

        // 2. Click Thêm ngân sách (+)
        ImageButton btnAddBudget = view.findViewById(R.id.btnAddBudget);
        if (btnAddBudget != null) {
            btnAddBudget.setOnClickListener(v -> {
                BudgetBottomSheetDialog bottomSheet = new BudgetBottomSheetDialog(
                        0, "Thêm ngân sách mới", 0.0, 0.0,
                        new BudgetBottomSheetDialog.OnBudgetActionListener() {
                            @Override
                            public void onSave(int selectedId, double newLimit) {
                                saveBudgetToDatabase(selectedId, newLimit);
                            }

                            @Override
                            public void onDelete(int categoryId) {
                                // Thêm mới thì không có nút Xóa
                            }
                        }
                );
                bottomSheet.show(getParentFragmentManager(), "AddBudgetBottomSheet");
            });
        }

        // 3. Click Sửa/Xóa Danh mục
        RecyclerView rvBudgets = view.findViewById(R.id.rvCategoryBudgets);
        rvBudgets.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BudgetAdapter(item -> {
            String titleName = (item.categoryName != null) ? item.categoryName : "Danh mục " + item.categoryId;
            // Gọi Numpad, truyền ID của danh mục và số tiền limit hiện tại vào
            openNumpadToEditBudget(item.categoryId, item.limitAmount);
            BudgetBottomSheetDialog bottomSheet = new BudgetBottomSheetDialog(
                    item.categoryId, titleName, item.spentAmount, item.limitAmount,
                    new BudgetBottomSheetDialog.OnBudgetActionListener()
                    {
                        @Override
                        public void onSave(int selectedId, double newLimit) {
                            saveBudgetToDatabase(selectedId, newLimit);
                        }

                        @Override
                        public void onDelete(int categoryIdToDelete) {
                            deleteBudgetFromDatabase(categoryIdToDelete);
                        }
                    }
            );
            bottomSheet.show(getParentFragmentManager(), "EditBudgetBottomSheet");
        });
        rvBudgets.setAdapter(adapter);

        loadBudgetsData();
    }

    // --- CÁC HÀM XỬ LÝ DATABASE ---

    private void saveBudgetToDatabase(int categoryId, double limitAmount) {
        new Thread(() -> {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
            long startOfMonth = cal.getTimeInMillis();

            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
            long endOfMonth = cal.getTimeInMillis();

            Budget budget = new Budget();
            budget.categoryId = categoryId;
            budget.limitAmount = (long) limitAmount;
            budget.periodType = "MONTH";
            budget.startDate = startOfMonth;
            budget.endDate = endOfMonth;

            Budget existingBudget = budgetDao.getBudgetByCategory(categoryId, System.currentTimeMillis());
            if (existingBudget != null) {
                budget.id = existingBudget.id;
                budgetDao.update(budget);
            } else {
                budgetDao.insert(budget);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Đã lưu thành công!", Toast.LENGTH_SHORT).show();
                    loadBudgetsData();
                });
            }
        }).start();
    }

    private void deleteBudgetFromDatabase(int categoryId) {
        new Thread(() -> {
            Budget existingBudget = budgetDao.getBudgetByCategory(categoryId, System.currentTimeMillis());
            if (existingBudget != null) {
                budgetDao.delete(existingBudget);
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Đã xóa ngân sách!", Toast.LENGTH_SHORT).show();
                    loadBudgetsData();
                });
            }
        }).start();
    }

    private void loadBudgetsData() {
        new Thread(() -> {
            long now = System.currentTimeMillis();

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
            long startOfMonth = cal.getTimeInMillis();

            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
            long endOfMonth = cal.getTimeInMillis();

            // Lấy toàn bộ dữ liệu từ DB
            List<BudgetWithSpent> allData = budgetDao.getActiveBudgetsWithSpent(now);
            masterBudgetCache = budgetDao.getMasterBudget(now);
            long masterSpent = budgetDao.getMasterSpentAmount(startOfMonth, endOfMonth);
            currentMasterSpent = masterSpent;

            // ĐÃ SỬA: Lọc bỏ Master Budget (ID = -1) ra khỏi danh sách hiển thị
            List<BudgetWithSpent> categoryBudgetsOnly = new java.util.ArrayList<>();
            if (allData != null) {
                for (BudgetWithSpent b : allData) {
                    if (b.categoryId != -1) {
                        categoryBudgetsOnly.add(b);
                    }
                }
            }

            // Đẩy lên giao diện UI
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // Truyền cái danh sách đã lọc (không có -1) vào Adapter
                    adapter.setBudgets(categoryBudgetsOnly);

                    // Cập nhật thẻ Master UI (Format VNĐ chuẩn)
                    if(masterBudgetCache != null) {
                        long limit = masterBudgetCache.limitAmount;
                        long remaining = limit - masterSpent;
                        int percent = limit > 0 ? (int) ((masterSpent * 100) / limit) : 0;

                        tvMasterLimit.setText(CurrencyFormatter.formatFullVND(limit));
                        tvMasterSpent.setText(CurrencyFormatter.formatFullVND(masterSpent));
                        tvMasterRemaining.setText(CurrencyFormatter.formatFullVND(remaining));
                        pbMaster.setProgress(Math.min(percent, 100));
                    }
                    else
                    {
                        tvMasterLimit.setText("0 VNĐ");
                        tvMasterSpent.setText(CurrencyFormatter.formatFullVND(masterSpent));
                        tvMasterRemaining.setText("0 VNĐ");
                        pbMaster.setProgress(0);
                    }
                });
            }
        }).start();
    }

    // Hàm này sẽ được gọi khi người dùng bấm vào thẻ Master hoặc 1 thẻ Category Budget
    private void openNumpadToEditBudget(int categoryId, double currentLimit) {
        NumpadBottomSheet numpad = new NumpadBottomSheet();

        // 1. Ép số tiền Limit hiện tại thành chuỗi để truyền vào Numpad
        numpad.setInitialAmount(String.valueOf((long) currentLimit));

        // 2. Gắn bộ đàm để nghe ngóng kết quả trả về
        numpad.setListener(new NumpadBottomSheet.OnNumpadListener() {
            @Override
            public void onAmountConfirmed(String rawAmount, String formattedAmount) {
                // Người dùng đã bấm "Xác nhận" (Continue)
                double newLimit = Double.parseDouble(rawAmount);

                // Dùng luôn hàm lưu Database thần thánh của ông (Nó đã có sẵn logic Update/Insert và tự load lại UI)
                saveBudgetToDatabase(categoryId, newLimit);
            }
        });

        // 3. Kéo cái Numpad từ dưới đáy màn hình lên
        numpad.show(getChildFragmentManager(), "NumpadBottomSheet");
    }
}