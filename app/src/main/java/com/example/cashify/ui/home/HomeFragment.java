package com.example.cashify.ui.home;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cashify.R;
import com.example.cashify.database.AppDatabase;
import com.example.cashify.database.CategorySum;
import com.example.cashify.database.TransactionDao;
import com.example.cashify.database.TransactionWithCategory;
import com.example.cashify.utils.CurrencyFormatter;
import com.example.cashify.viewmodel.HomeViewModel;
import com.example.cashify.viewmodel.TransactionViewModel;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    // Khai báo Executor để quản lý luồng truy vấn DB
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private PieChart pieChart;
    private TextView tvDate;
    // Khai báo các TextView cho thẻ Tổng quan
    private TextView tvTotalBalance, tvIncome, tvExpense;
    private Calendar currentCalendar;

    private TextView tvSeeAll;

    private RecyclerView rvRecentTransactions;
    private RecentTransactionAdapter adapter;

    // Bộ màu chuẩn cho Rule of 5 (5 màu nổi + 1 màu xám)
    private final int[] CHART_COLORS = {
            Color.parseColor("#00BCD4"), // Cyan (Màu 1)
            Color.parseColor("#FF9800"), // Orange (Màu 2)
            Color.parseColor("#AB47BC"), // Purple (Màu 3)
            Color.parseColor("#FF4081"), // Pink (Màu 4)
            Color.parseColor("#4CAF50")  // Green (Màu 5)
    };
    private final int COLOR_OTHERS = Color.parseColor("#BDBDBD"); // Grey (Mục Khác)

    public HomeFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvSeeAll = view.findViewById(R.id.tvSeeAll);
        pieChart = view.findViewById(R.id.pieChart);
        tvDate = view.findViewById(R.id.tvDate);

        View cardDateSelector = view.findViewById(R.id.cardDateSelector);

        // Ánh xạ View cho Thẻ số dư
        tvTotalBalance = view.findViewById(R.id.total_money_amount);
        tvIncome = view.findViewById(R.id.income_money_amount);
        tvExpense = view.findViewById(R.id.expenses_money_amount);

        rvRecentTransactions = view.findViewById(R.id.rvRecentTransactions);

        // Cấu hình cuộn (Cuộn dọc, nếu muốn cuộn ngang thì đổi VERTICAL thành HORIZONTAL)
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        // Khởi tạo Adapter mới
        adapter = new RecentTransactionAdapter();
        rvRecentTransactions.setAdapter(adapter);

        // Setup Click cho từng dòng (Mới học được từ bên History)
        adapter.setOnItemClickListener(transaction -> {
            // Tạm thời hiện Toast, sau này bác có thể mở Dialog Edit hoặc xem chi tiết
            Toast.makeText(getContext(), "Giao dịch: " + transaction.amount, Toast.LENGTH_SHORT).show();
        });

        //Setup giao diện chuẩn Donut Chart
        setupDonutChart();

        //Setup thời gian mặc định (Tháng hiện tại)
        currentCalendar = Calendar.getInstance();

        View.OnClickListener showDialogListener = v -> showMonthSelectorDialog();
        tvDate.setOnClickListener(showDialogListener);
        if (cardDateSelector != null) {
            cardDateSelector.setOnClickListener(showDialogListener);
        }

// Khởi tạo lại bằng HomeViewModel
        HomeViewModel viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

// Dùng câu query lấy sẵn 5 giao dịch + Category từ Room (KHÔNG CẦN VÒNG LẶP NẶNG MÁY)
        viewModel.getRecentTransactionsWithCategory().observe(getViewLifecycleOwner(), transWithCatList -> {
            if (transWithCatList != null && !transWithCatList.isEmpty()) {
                List<TransactionViewModel.HistoryItem> recentItems = new ArrayList<>();

                for (TransactionWithCategory item : transWithCatList) {
                    String catName = (item.category != null) ? item.category.name : "Chưa phân loại";
                    String catIcon = (item.category != null) ? item.category.iconName : "ic_food";

                    recentItems.add(new TransactionViewModel.HistoryItem(item.transaction, catName, catIcon));
                }

                adapter.updateData(recentItems);
                rvRecentTransactions.setVisibility(View.VISIBLE);
            }
        });

        tvSeeAll.setOnClickListener(v -> {
            // Tìm cái thanh Bottom Navigation nằm ngoài MainActivity
            com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                    requireActivity().findViewById(R.id.bottom_navigation);

            if (bottomNav != null) {
                // Lệnh này tương đương với việc người dùng lấy tay bấm vào tab Transaction
                bottomNav.setSelectedItemId(R.id.nav_transaction);
            }
        });

//        //Bắt sự kiện click chọn tháng (Tạm thời để Toast, Khang ráp DatePicker vào sau nhé)
//        tvDate.setOnClickListener(v -> {
//            Toast.makeText(getContext(), "Tính năng chọn tháng sẽ mở BottomSheet!", Toast.LENGTH_SHORT).show();
//            // Test lùi lại 1 tháng:
//            // currentCalendar.add(Calendar.MONTH, -1);
//            // updateMonthTextAndLoadData();
//        });

    }

    // Viết đè hàm này để fragment tự kéo dữ liệu từ database
    @Override
    public void onResume() {
        super.onResume();
        // Mỗi khi màn hình này hiện lên (kể cả lúc vừa mở app hay vừa đóng màn hình Thêm),
        // nó sẽ tự động tính lại ngày tháng và kéo dữ liệu mới nhất từ DB.
        updateMonthTextAndLoadData();
    }

    private void showMonthSelectorDialog() {
        databaseExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());

            // Kéo toàn bộ mốc thời gian từ DB về (nhờ câu Query Khang vừa thêm bên DAO)
            List<Long> timestamps = db.transactionDao().getAllTimestamps();

            if (timestamps == null || timestamps.isEmpty()) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Chưa có dữ liệu giao dịch nào!", Toast.LENGTH_SHORT).show()
                    );
                }
                return; // Dừng lại, không mở bảng chọn nếu DB trống
            }

            // Gom nhóm các tháng (loại bỏ trùng lặp) và giữ nguyên thứ tự Mới -> Cũ
            java.util.LinkedHashMap<String, Calendar> monthMap = new java.util.LinkedHashMap<>();

            for (Long ts : timestamps) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(ts);
                int m = cal.get(Calendar.MONTH) + 1;
                int y = cal.get(Calendar.YEAR);

                // Format lại để có chữ giống strings.xml
                String label = getString(R.string.dashboard_month_format, m, y);

                if (!monthMap.containsKey(label)) {
                    // Reset ngày giờ về đầu tháng để lát nữa lọc DB cho chuẩn
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    monthMap.put(label, cal);
                }
            }

            //Ép danh sách Map thành Mảng để ném vào AlertDialog
            List<String> displayList = new ArrayList<>(monthMap.keySet());
            String[] displayArray = displayList.toArray(new String[0]);

            //Mở Popup hiển thị cho người dùng chọn
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    new android.app.AlertDialog.Builder(requireContext())
                            .setTitle("Chọn tháng")
                            .setItems(displayArray, (dialog, which) -> {
                                // Khi người dùng bấm chọn 1 dòng
                                String selectedLabel = displayArray[which];
                                Calendar selectedCal = monthMap.get(selectedLabel);

                                if (selectedCal != null) {
                                    // Cập nhật lại mốc thời gian và load lại toàn bộ màn hình
                                    currentCalendar.setTimeInMillis(selectedCal.getTimeInMillis());
                                    updateMonthTextAndLoadData();
                                }
                            })
                            .show();
                });
            }
        });
    }

    private void setupDonutChart() {
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(0f);
        pieChart.setHoleRadius(65f); // Kích thước lỗ tròn ở giữa

        pieChart.setDrawEntryLabels(false); // Tắt tên dán đè lên miếng bánh cho đỡ rối
        pieChart.getDescription().setEnabled(false); // Tắt dòng chữ Description góc dưới

        // Text ở giữa cái lỗ
        pieChart.setCenterText("Total\nExpenses");
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(Color.parseColor("#111827"));

        // Chú thích (Legend) nằm ở dưới cùng
        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setWordWrapEnabled(true);
        legend.setDrawInside(false);
        legend.setTextSize(12f);
    }

    private void updateMonthTextAndLoadData() {
        int month = currentCalendar.get(Calendar.MONTH) + 1;
        int year = currentCalendar.get(Calendar.YEAR);
        String dateText = getString(R.string.dashboard_month_format, month, year);
        tvDate.setText(dateText);

        //tui đổi tên cho n thích hợp cái bro tại h hàm này gánh cả card với chart
        loadDashboardData();
    }

    private void loadDashboardData() {
        databaseExecutor.execute(() -> {
            // Tính mốc thời gian: Đầu tháng -> Cuối tháng
            Calendar cal = (Calendar) currentCalendar.clone();
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
            long startOfMonth = cal.getTimeInMillis();

            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59);
            long endOfMonth = cal.getTimeInMillis();

            // Gọi Database
            AppDatabase db = AppDatabase.getInstance(requireContext());
            TransactionDao dao = db.transactionDao();

            // Lấy tổng số dư (Actual Balance lấy trọn đời, không phụ thuộc tháng) --mốt cái này chỉnh theo tháng sau
            long actualBalance = dao.getMonthlyBalance(startOfMonth, endOfMonth);;
            // Lấy tổng thu & chi (Chỉ lấy trong tháng hiện tại)
            long totalIncome = dao.getTotalIncome(startOfMonth, endOfMonth);
            long totalExpense = dao.getTotalExpense(startOfMonth, endOfMonth);

            // Lấy Top 5 và Lấy tổng "Khác" bằng 2 câu Query của Khang
            List<CategorySum> top5Categories = dao.getTop5ExpenseCategories(startOfMonth, endOfMonth);
            long othersTotal = dao.getOtherExpenseTotal(startOfMonth, endOfMonth);

            ArrayList<PieEntry> entries = new ArrayList<>();
            ArrayList<Integer> colors = new ArrayList<>();

            // Đổ Top 5 vào biểu đồ
            for (int i = 0; i < top5Categories.size(); i++) {
                CategorySum cat = top5Categories.get(i);
                if (cat.total > 0) {
                    // Dùng class CurrencyFormatter của Khang hoặc bỏ qua nếu biểu đồ tự format số
                    entries.add(new PieEntry((float) cat.total, cat.categoryName));
                    colors.add(CHART_COLORS[i % CHART_COLORS.length]);
                }
            }

            // Nếu phần "Khác" > 0 thì thêm 1 cục xám vào cuối
            if (othersTotal > 0) {
                entries.add(new PieEntry((float) othersTotal, "Khác"));
                colors.add(COLOR_OTHERS);
            }

            // Đẩy lên UI
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // --- Cập nhật Thẻ Số Dư ---
                    tvTotalBalance.setText(CurrencyFormatter.formatFullVND(actualBalance));
                    tvIncome.setText(CurrencyFormatter.formatFullVND(totalIncome));
                    tvExpense.setText(CurrencyFormatter.formatFullVND(totalExpense));

                    //sửa logic chỗ này để không bị kẹt UI khi xóa hết giao dịch
                    if (entries.isEmpty()) {
                        pieChart.clear();
                        pieChart.setCenterText("No Expenses");
                    } else {
                        PieDataSet dataSet = new PieDataSet(entries, "");
                        dataSet.setColors(colors);
                        dataSet.setSliceSpace(3f);
                        dataSet.setSelectionShift(5f);

                        PieData data = new PieData(dataSet);
                        data.setDrawValues(false);

                        pieChart.setData(data);
                        pieChart.setCenterText("Chi tiêu\nTháng " + (currentCalendar.get(Calendar.MONTH) + 1));
                        pieChart.animateY(700);
                    }
                    pieChart.invalidate();
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Không shutdown databaseExecutor ở đây vì Fragment có thể được tạo lại
        // Executor này sẽ tồn tại theo vòng đời của HomeFragment instance.
    }
}