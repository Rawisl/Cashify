package com.example.cashify.ui.home;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
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
import com.example.cashify.data.local.CategorySum;
import com.example.cashify.data.local.TransactionWithCategory;
import com.example.cashify.utils.CurrencyFormatter;
import com.example.cashify.ui.transactions.TransactionViewModel;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
//TODO: Nếu user đổi sang "Quỹ Nhóm", Home phải tự load lại data của Quỹ đó.
public class HomeFragment extends Fragment {

    private PieChart pieChart;
    private TextView tvDate;
    // Khai báo các TextView cho thẻ Tổng quan
    private TextView tvTotalBalance, tvIncome, tvExpense;
    private Calendar currentCalendar;

    private TextView tvSeeAll;

    private RecyclerView rvRecentTransactions;
    private RecentTransactionAdapter adapter;

    private HomeViewModel viewModel;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //ánh xạ dùng cho chart
        RecyclerView rvLegend = view.findViewById(R.id.rvLegend);
        rvLegend.setLayoutManager(new LinearLayoutManager(getContext()));
        LegendAdapter legendAdapter = new LegendAdapter();
        rvLegend.setAdapter(legendAdapter);

        //Nghe ngóng click từ Legend
        legendAdapter.setOnItemClickListener(item -> {
            if ("Others".equals(item.getName())) {
                showOthersDetailBottomSheet();
            }
        });

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
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // Dùng câu query lấy sẵn 5 giao dịch + Category từ Room (KHÔNG CẦN VÒNG LẶP NẶNG MÁY)
        viewModel.getRecentTransactionsWithCategory().observe(getViewLifecycleOwner(), transWithCatList -> {
            if (transWithCatList != null && !transWithCatList.isEmpty()) {
                List<TransactionViewModel.HistoryItem> recentItems = new ArrayList<>();

                for (TransactionWithCategory item : transWithCatList) {
                    String catName = (item.category != null) ? item.category.name : "Chưa phân loại";
                    String catIcon = (item.category != null) ? item.category.iconName : "ic_food";
                    String catColor = (item.category != null) ? item.category.colorCode : "#000000";

                    recentItems.add(new TransactionViewModel.HistoryItem(item.transaction, catName, catIcon, catColor));
                }

                adapter.updateData(recentItems);
                rvRecentTransactions.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getDashboardData().observe(getViewLifecycleOwner(), state -> {
            // Cập nhật text số dư
            tvTotalBalance.setText(CurrencyFormatter.formatFullVND(state.actualBalance));
            tvIncome.setText(CurrencyFormatter.formatFullVND(state.totalIncome));
            tvExpense.setText(CurrencyFormatter.formatFullVND(state.totalExpense));

            //Bơm data cho Legend Adapter (MỚI THÊM)
            if (state.legendItems != null) {
                legendAdapter.updateData(state.legendItems);
            }

            //Vẽ biểu đồ gọn
            ArrayList<PieEntry> entries = new ArrayList<>();

            for (CategorySum cat : state.top5Categories) {
                if (cat.total > 0) {
                    entries.add(new PieEntry((float) cat.total, cat.categoryName));
                }
            }

            if (state.othersTotal > 0) {
                entries.add(new PieEntry((float) state.othersTotal, "Others"));
            }

            if (entries.isEmpty()) {
                pieChart.clear();
                pieChart.setCenterText("No Expenses");
            } else {
                PieDataSet dataSet = new PieDataSet(entries, "");

                // Lấy thẳng mảng màu từ ViewModel ném vào
                dataSet.setColors(state.pieColors);
                dataSet.setSliceSpace(3f);
                dataSet.setSelectionShift(5f);

                PieData data = new PieData(dataSet);
                data.setDrawValues(false); // Vẫn tắt value mặc định

                pieChart.setData(data);
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM", Locale.ENGLISH);
                String currentMonth = sdf.format(currentCalendar.getTime());
                pieChart.setCenterText(getString(R.string.chart_center_text, currentMonth));
                pieChart.setCenterTextSize(20f);
            }
            pieChart.invalidate();
        });

        viewModel.getAvailableMonths().observe(getViewLifecycleOwner(), monthMap -> {
            if (monthMap == null || monthMap.isEmpty()) {
                Toast.makeText(getContext(), "Chưa có dữ liệu giao dịch nào!", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> displayList = new ArrayList<>(monthMap.keySet());
            String[] displayArray = displayList.toArray(new String[0]);

            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Chọn tháng")
                    .setItems(displayArray, (dialog, which) -> {
                        String selectedLabel = displayArray[which];
                        Calendar selectedCal = monthMap.get(selectedLabel);

                        if (selectedCal != null) {
                            currentCalendar.setTimeInMillis(selectedCal.getTimeInMillis());
                            updateMonthTextAndLoadData();
                        }
                    })
                    .show();
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
        viewModel.loadAvailableMonths(getString(R.string.dashboard_month_format));
    }

    private void setupDonutChart() {
        pieChart.setDrawHoleEnabled(true); //bật cái lỗ tròn ở giữa mới vẽ được center text
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(0f);
        pieChart.setHoleRadius(65f); // Kích thước lỗ tròn ở giữa
        pieChart.animateY(700);

        pieChart.setDrawEntryLabels(false); // Tắt tên dán đè lên miếng bánh cho đỡ rối
        pieChart.getDescription().setEnabled(false); // Tắt dòng chữ Description góc dưới

        //Ép mọi miếng bánh phải rộng ít nhất 15 độ (tầm này là ngón tay người bấm vừa)
        pieChart.setMinAngleForSlices(15f);

        pieChart.setUsePercentValues(true);

        // Lấy tên tháng hiện tại (ví dụ: "April")
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM", Locale.ENGLISH);
        String monthName = sdf.format(Calendar.getInstance().getTime());

        // Text ở giữa cái lỗ
        pieChart.setCenterText(getString(R.string.chart_center_text, monthName));
        pieChart.setCenterTextSize(20f);
        // Khởi tạo Typeface từ font chữ và màu, không dùng mã font và mã màu thường mà phải dùng resources/context compat
        Typeface tf = ResourcesCompat.getFont(getContext(), R.font.inter_bold);
        pieChart.setCenterTextColor(ContextCompat.getColor(getContext(), R.color.brand_primary));
        pieChart.setCenterTextTypeface(tf);

        //tắt chú thích legend mặc định của piechart đi vì n xấu vkl
        pieChart.getLegend().setEnabled(false);

        //Bắt sự kiện người dùng lấy tay chọt vào miếng bánh
        pieChart.setOnChartValueSelectedListener(new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                if (e instanceof PieEntry) {
                    PieEntry pe = (PieEntry) e;

                    String categoryName = pe.getLabel();

                    float rawAmount = pe.getValue();

                    // CHUẨN CÔNG THỨC: Lấy số tiền miếng bánh chia cho (Tổng tất cả các bánh) * 100
                    float percentage = (rawAmount / pieChart.getData().getYValueSum()) * 100f;

                    // Chỉ ghép Tên danh mục và %
                    String centerText = categoryName + "\n" +
                            String.format(Locale.getDefault(), "%.1f%%", percentage);

                    pieChart.setCenterText(centerText);
                    pieChart.setCenterTextSize(18f); // Chữ ngắn rồi thì buff size lên xíu cho đẹp
                }
            }

            @Override
            public void onNothingSelected() {
                // Lấy lại tên tháng từ currentCalendar (tháng người dùng đang xem)
                SimpleDateFormat sdf = new SimpleDateFormat("MMMM", Locale.ENGLISH);
                String currentMonth = sdf.format(currentCalendar.getTime());

                // Trả lại nguyên trạng
                pieChart.setCenterText(getString(R.string.chart_center_text, currentMonth));
                pieChart.setCenterTextSize(20f);
            }
        });
    }

    // Cục Bottom Sheet
    private void showOthersDetailBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());

        View view = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_others, null);

        RecyclerView rvOthers = view.findViewById(R.id.rvOthersDetail);
        rvOthers.setLayoutManager(new LinearLayoutManager(getContext()));

        LegendAdapter othersAdapter = new LegendAdapter();
        rvOthers.setAdapter(othersAdapter);

        // Kéo data từ ViewModel ra
        HomeViewModel.DashboardState state = viewModel.getDashboardData().getValue();
        if (state != null && state.subOthersList != null) {
            othersAdapter.updateData(state.subOthersList);
        }

        dialog.setContentView(view);
        dialog.show();
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
        Calendar cal = (Calendar) currentCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long startOfMonth = cal.getTimeInMillis();

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long endOfMonth = cal.getTimeInMillis();

        // Kêu ViewModel lấy dữ liệu
        viewModel.loadDashboardData(startOfMonth, endOfMonth);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Không shutdown databaseExecutor ở đây vì Fragment có thể được tạo lại
        // Executor này sẽ tồn tại theo vòng đời của HomeFragment instance.
    }
}