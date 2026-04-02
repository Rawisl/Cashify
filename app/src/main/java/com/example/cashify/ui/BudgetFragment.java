package com.example.cashify.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.database.AppDatabase;
import com.example.cashify.database.Budget;
import com.example.cashify.database.BudgetDao;
import com.example.cashify.database.BudgetWithSpent;
import com.example.cashify.utils.CurrencyFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class BudgetFragment extends Fragment {

    private BudgetAdapter adapter;
    private BudgetDao budgetDao;

    // Các View của thẻ Master Budget
    private TextView tvMasterTitle, tvMasterLimit, tvMasterSpent, tvMasterRemaining, tvMasterAlert;
    private ProgressBar pbMaster;
    private MaterialButtonToggleGroup toggleGroupPeriod;
    private MaterialButton btnWeekly, btnMonthly; // Ánh xạ 2 nút để đổi màu

    private Budget masterBudgetCache; // Lưu tạm dữ liệu Master
    private double currentMasterSpent = 0.0; // Lưu tạm tiền Master đã tiêu

    // Biến lưu trạng thái tab hiện tại: "WEEK" hoặc "MONTH"
    private String currentPeriodType = "MONTH";
    private long mLastClickTime = 0;

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

        // Ánh xạ thẻ Master và Toggle Group
        tvMasterTitle = view.findViewById(R.id.tvMasterTitle);
        tvMasterLimit = view.findViewById(R.id.tvMasterLimit);
        tvMasterSpent = view.findViewById(R.id.tvMasterSpent);
        tvMasterRemaining = view.findViewById(R.id.tvMasterRemaining);
        pbMaster = view.findViewById(R.id.pbMaster);
        tvMasterAlert = view.findViewById(R.id.tvMasterAlert); // Ánh xạ chữ cảnh báo

        toggleGroupPeriod = view.findViewById(R.id.toggleGroupPeriod);
        btnWeekly = view.findViewById(R.id.btnWeekly);
        btnMonthly = view.findViewById(R.id.btnMonthly);

        // --- LẮNG NGHE SỰ KIỆN CHUYỂN TAB WEEKLY / MONTHLY ---
        toggleGroupPeriod.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnWeekly) {
                    currentPeriodType = "WEEK";
                    tvMasterTitle.setText("Weekly Master Budget");
                } else if (checkedId == R.id.btnMonthly) {
                    currentPeriodType = "MONTH";
                    tvMasterTitle.setText("Monthly Master Budget");
                }

                // Gọi hàm đổi màu xanh lá khi người dùng chọn tab
                updateToggleColors();

                // Khi đổi tab thì tải lại dữ liệu tương ứng
                loadBudgetsData();
            }
        });

        // Gọi hàm này lần đầu tiên để set màu lúc vừa mở màn hình (mặc định là Monthly)
        updateToggleColors();

        // 1. Click Master Budget
        CardView cardMaster = view.findViewById(R.id.cardMaster);
        if (cardMaster != null) {
            cardMaster.setOnClickListener(v -> {
                if (android.os.SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = android.os.SystemClock.elapsedRealtime();
                double limit = masterBudgetCache != null ? masterBudgetCache.limitAmount : 0;
                //mở numpad, truyền id = -1 (master) vô
                openNumpadToEditBudget(-1, limit);
                /*BudgetBottomSheetDialog bottomSheet = new BudgetBottomSheetDialog(
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
                bottomSheet.show(getParentFragmentManager(), "BudgetBottomSheet");*/
            });
        }

        // 2. Click Thêm ngân sách (+)
        ImageButton btnAddBudget = view.findViewById(R.id.btnAddBudget);
        if (btnAddBudget != null) {
            btnAddBudget.setOnClickListener(v -> {
                if (android.os.SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return;
                }
                mLastClickTime = android.os.SystemClock.elapsedRealtime();
                //  LOGIC LÀM MỜ
                List<Integer> existingIds = new ArrayList<>();
                // Lấy danh sách ID đã có budget từ adapter
                if (adapter != null && adapter.getBudgets() != null) {
                    for (BudgetWithSpent b : adapter.getBudgets()) {
                        existingIds.add(b.categoryId);
                    }
                }
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
                bottomSheet.setDisabledCategoryIds(existingIds);
                bottomSheet.show(getParentFragmentManager(), "AddBudgetBottomSheet");
            });
        }

        // 3. Click Sửa/Xóa Danh mục
        RecyclerView rvBudgets = view.findViewById(R.id.rvCategoryBudgets);
        rvBudgets.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BudgetAdapter(item -> {
            if (android.os.SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = android.os.SystemClock.elapsedRealtime();
            String titleName = (item.categoryName != null) ? item.categoryName : "Danh mục " + item.categoryId;
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

    private void updateToggleColors() {
        if (getContext() == null) return;

        int colorGreen = ContextCompat.getColor(requireContext(), R.color.status_green);
        int colorWhite = ContextCompat.getColor(requireContext(), R.color.white);
        int colorBrand = ContextCompat.getColor(requireContext(), R.color.brand_primary);
        int colorTransparent = Color.TRANSPARENT;

        if (currentPeriodType.equals("WEEK")) {
            btnWeekly.setBackgroundTintList(ColorStateList.valueOf(colorGreen));
            btnWeekly.setTextColor(colorWhite);

            btnMonthly.setBackgroundTintList(ColorStateList.valueOf(colorTransparent));
            btnMonthly.setTextColor(colorBrand);
        } else {
            btnMonthly.setBackgroundTintList(ColorStateList.valueOf(colorGreen));
            btnMonthly.setTextColor(colorWhite);

            btnWeekly.setBackgroundTintList(ColorStateList.valueOf(colorTransparent));
            btnWeekly.setTextColor(colorBrand);
        }
    }


    // CÁC HÀM XỬ LÝ THỜI GIAN (HỖ TRỢ CHO VIỆC NỐI DÂY)
    private long[] calculateTimeRange() {
        Calendar cal = Calendar.getInstance();
        long startTime, endTime;

        if (currentPeriodType.equals("WEEK")) {
            // Tính Thứ 2 -> Chủ Nhật
            cal.setFirstDayOfWeek(Calendar.MONDAY);
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
            startTime = cal.getTimeInMillis();

            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
            endTime = cal.getTimeInMillis();
        } else {
            // Tính Mùng 1 -> Ngày cuối tháng
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
            startTime = cal.getTimeInMillis();

            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
            endTime = cal.getTimeInMillis();
        }
        return new long[]{startTime, endTime};
    }

    // --- CÁC HÀM XỬ LÝ DATABASE ---

    private void saveBudgetToDatabase(int categoryId, double limitAmount) {
        new Thread(() -> {
            // Thay thế đoạn code fix cứng bằng hàm tính thời gian
            long[] timeRange = calculateTimeRange();
            long startTime = timeRange[0];
            long endTime = timeRange[1];

            // BẮT ĐẦU LOGIC KIỂM TRA TỔNG HẠN MỨC (TASK MỚI)
            // ĐÃ THÊM: Kiểm tra nếu sửa Master Budget mà nhỏ hơn tổng các mục con
            if (categoryId == -1) {
                long totalCatLimits = budgetDao.getTotalCategoryLimits(currentPeriodType);
                if (limitAmount < totalCatLimits) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            String diff = CurrencyFormatter.formatCompactVND(totalCatLimits - limitAmount);
                            Toast.makeText(getContext(),
                                    "Master Budget không được nhỏ hơn tổng danh mục con (Thiếu " + diff + ")!",
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                    return; // Chặn không cho lưu
                }
            } else { // Chỉ check nếu đây không phải là đang sửa chính thẻ Master
                Budget master = budgetDao.getMasterBudget(System.currentTimeMillis(), currentPeriodType);
                if (master != null) {
                    // Lấy tổng hạn mức của các category hiện tại (trừ category đang sửa ra để tránh cộng dồn chính nó)
                    long totalOthersLimit = budgetDao.getTotalCategoryLimitExcluding(categoryId, currentPeriodType);

                    if (totalOthersLimit + limitAmount > master.limitAmount) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                // Format số tiền thừa để báo cho người dùng dễ hiểu
                                String overAmount = CurrencyFormatter.formatCompactVND((totalOthersLimit + limitAmount) - master.limitAmount);
                                Toast.makeText(getContext(),
                                        "Tổng ngân sách danh mục vượt quá Master Budget (" + overAmount + ")!",
                                        Toast.LENGTH_LONG).show();
                            });
                        }
                        return; // Dừng lại, không cho save vào DB
                    }
                }
            }
            // KẾT THÚC LOGIC KIỂM TRA

            Budget budget = new Budget();
            budget.categoryId = categoryId;
            budget.limitAmount = (long) limitAmount;

            // Gán loại kỳ hạn theo Tab đang chọn (WEEK hoặc MONTH)
            budget.periodType = currentPeriodType;
            budget.startDate = startTime;
            budget.endDate = endTime;

            Budget existingBudget = budgetDao.getBudgetByCategory(categoryId, System.currentTimeMillis(), currentPeriodType);
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
            // Tìm ngân sách hiện tại của danh mục này (dựa trên thời gian thực tế)
            Budget existingBudget = budgetDao.getBudgetByCategory(categoryId, System.currentTimeMillis(), currentPeriodType);

            if (existingBudget != null) {
                budgetDao.delete(existingBudget);
            }

            if (existingBudget != null) {
                // Thực hiện xóa khỏi bảng budgets
                budgetDao.delete(existingBudget);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // Thông báo cho người dùng
                    Toast.makeText(getContext(), "Đã xóa hạn mức ngân sách!", Toast.LENGTH_SHORT).show();

                    // LOAD LẠI DỮ LIỆU
                    // Lúc này, danh mục vừa xóa sẽ không còn trong plannedData
                    // nhưng nếu nó có chi tiêu, nó sẽ tự lọt vào unplannedData và hiện thẻ xám.
                    loadBudgetsData();
                });
            }
        }).start();
    }

    private void loadBudgetsData() {
        new Thread(() -> {
            long now = System.currentTimeMillis();
            long[] timeRange = calculateTimeRange();
            long startTime = timeRange[0];
            long endTime = timeRange[1];

            // 1. Lấy toàn bộ dữ liệu ĐÃ LÊN KẾ HOẠCH từ DB
            // TODO: NỐI DÂY DB - Truyền thêm currentPeriodType vào các truy vấn này
            List<BudgetWithSpent> plannedData = budgetDao.getActiveBudgetsWithSpent(now, currentPeriodType);

            // 2. Lấy toàn bộ dữ liệu NGOÀI KẾ HOẠCH (ĐÃ MỞ KHÓA)
            // ĐÃ MỞ KHÓA: Gọi hàm lấy unplannedExpenses
            List<BudgetWithSpent> unplannedData = budgetDao.getUnplannedExpenses(startTime, endTime, now, currentPeriodType);

            masterBudgetCache = budgetDao.getMasterBudget(now, currentPeriodType);
            long masterSpent = budgetDao.getMasterSpentAmount(startTime, endTime);
            currentMasterSpent = masterSpent;

            // ĐÃ THÊM: Tính tổng hạn mức con để hiện trạng thái Master
            long totalCatLimits = budgetDao.getTotalCategoryLimits(currentPeriodType);

            List<BudgetWithSpent> displayList = new ArrayList<>();
            // Thêm các mục có Budget (trừ Master ID = -1)
            if (plannedData != null) {
                for (BudgetWithSpent b : plannedData) {
                    if (b.categoryId != -1) {
                        displayList.add(b);
                    }
                }
            }
            // Thêm các mục ngoài kế hoạch vào cuối danh sách
            if (unplannedData != null) {
                displayList.addAll(unplannedData);
            }

            // Đẩy lên giao diện UI
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // Truyền cái danh sách đã gộp vào Adapter
                    adapter.setBudgets(displayList);

                    // Cập nhật thẻ Master UI (Format VNĐ chuẩn)
                    if(masterBudgetCache != null) {
                        long limit = masterBudgetCache.limitAmount;
                        long remaining = limit - masterSpent;
                        int percent = limit > 0 ? (int) ((masterSpent * 100) / limit) : 0;

                        tvMasterLimit.setText(CurrencyFormatter.formatFullVND(limit));
                        tvMasterSpent.setText(CurrencyFormatter.formatFullVND(masterSpent));
                        tvMasterRemaining.setText(CurrencyFormatter.formatFullVND(remaining));
                        pbMaster.setProgress(Math.min(percent, 100));

                        // LOGIC ĐỔI MÀU VÀ CHỮ CẢNH BÁO CHO MASTER
                        tvMasterAlert.setVisibility(View.VISIBLE);

                        // ĐÃ THÊM: Logic hiển thị trạng thái "Còn trống" hoặc "Vượt mức" của Master
                        if (totalCatLimits < limit) {
                            String freeSpace = CurrencyFormatter.formatCompactVND(limit - totalCatLimits);
                            tvMasterAlert.setText("Ngân sách tổng còn trống " + freeSpace);
                            tvMasterAlert.setTextColor(Color.WHITE);
                        } else if (totalCatLimits > limit) {
                            String exceeded = CurrencyFormatter.formatCompactVND(totalCatLimits - limit);
                            tvMasterAlert.setText("Ngân sách danh mục vượt mức tổng " + exceeded);
                            tvMasterAlert.setTextColor(Color.YELLOW);
                        } else {
                            // Nếu khít thì dùng lại logic cảnh báo theo % chi tiêu của bạn
                            String formattedRemaining = CurrencyFormatter.formatCompactVND(Math.abs(remaining));
                            if (percent > 100) {
                                int colorCoral = ContextCompat.getColor(requireContext(), R.color.cat_pastel_coral);
                                pbMaster.setProgressTintList(ColorStateList.valueOf(colorCoral));
                                tvMasterAlert.setText("Ahh c'mon man...");
                                tvMasterAlert.setTextColor(colorCoral);
                            } else if (percent >= 80) {
                                int colorCoral = ContextCompat.getColor(requireContext(), R.color.cat_pastel_coral);
                                pbMaster.setProgressTintList(ColorStateList.valueOf(colorCoral));
                                tvMasterAlert.setText("Just " + formattedRemaining + " left!");
                                tvMasterAlert.setTextColor(colorCoral);
                            } else if (percent >= 60) {
                                int colorOrange = ContextCompat.getColor(requireContext(), R.color.cat_pastel_orange);
                                pbMaster.setProgressTintList(ColorStateList.valueOf(colorOrange));
                                tvMasterAlert.setText(formattedRemaining + " available");
                                tvMasterAlert.setTextColor(colorOrange);
                            } else {
                                int colorGreen = ContextCompat.getColor(requireContext(), R.color.cat_pastel_green);
                                pbMaster.setProgressTintList(ColorStateList.valueOf(colorGreen));
                                tvMasterAlert.setText(formattedRemaining + " left to spend");
                                tvMasterAlert.setTextColor(colorGreen);
                            }
                        }

                    }
                    else
                    {
                        tvMasterLimit.setText(CurrencyFormatter.formatFullVND(0));
                        tvMasterSpent.setText(CurrencyFormatter.formatFullVND(masterSpent));
                        tvMasterRemaining.setText(CurrencyFormatter.formatFullVND(0));
                        pbMaster.setProgress(0);

                        // GIẤU CẢNH BÁO NẾU CHƯA CÓ MASTER BUDGET
                        tvMasterAlert.setVisibility(View.GONE);
                        pbMaster.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.cat_pastel_green)));
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