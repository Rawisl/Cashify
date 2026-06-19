package com.example.cashify.ui.home;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.local.CategorySum;
import com.example.cashify.data.local.TransactionWithCategory;
import com.example.cashify.ui.common.BaseFragment;
import com.example.cashify.ui.main.BaseActivity;
import com.example.cashify.ui.main.MainViewModel;
import com.example.cashify.ui.notifications.NotificationBottomSheet;
import com.example.cashify.ui.transactions.TransactionViewModel;
import com.example.cashify.utils.CurrencyFormatter;
import com.example.cashify.utils.ToastHelper;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends BaseFragment {

    private HomeViewModel viewModel;
    private MainViewModel mainViewModel;

    // UI Components
    private PieChart pieChart;
    private TextView tvDate, tvTotalBalance, tvIncome, tvExpense, tvSeeAll, tvNotificationBadge;
    private RecyclerView rvRecentTransactions, rvLegend;

    // Adapters
    private RecentTransactionAdapter transactionAdapter;
    private LegendAdapter legendAdapter;

    // State
    private Calendar currentCalendar;
    private String currentWorkspaceId = "PERSONAL"; // TODO: Update dynamically when switching workspaces

    public HomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        currentCalendar = Calendar.getInstance();

        initViews(view);
        setupRecyclerViews();
        setupDonutChart();
        setupListeners(view);
        setupObservers();

    }

    @Override
    public void onResume() {
        super.onResume();
        // Auto-refresh data when fragment becomes visible
        updateMonthTextAndLoadData();
    }

    private void initViews(View view) {
        pieChart = view.findViewById(R.id.pieChart);
        tvDate = view.findViewById(R.id.tvDate);
        tvTotalBalance = view.findViewById(R.id.total_money_amount);
        tvIncome = view.findViewById(R.id.income_money_amount);
        tvExpense = view.findViewById(R.id.expenses_money_amount);
        tvSeeAll = view.findViewById(R.id.tvSeeAll);
        tvNotificationBadge = view.findViewById(R.id.tvNotificationBadge);
        rvRecentTransactions = view.findViewById(R.id.rvRecentTransactions);
        rvLegend = view.findViewById(R.id.rvLegend);
    }

    private void setupRecyclerViews() {
        // Legend setup
        rvLegend.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        legendAdapter = new LegendAdapter();
        legendAdapter.setOnItemClickListener(item -> {
            if ("Others".equals(item.getName())) {
                showOthersDetailBottomSheet();
            }
        });
        rvLegend.setAdapter(legendAdapter);

        // Recent transactions setup
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        transactionAdapter = new RecentTransactionAdapter();
        rvRecentTransactions.setAdapter(transactionAdapter);
    }

    private void setupListeners(View view) {
        // Date Selector
        View.OnClickListener showDialogListener = v -> viewModel.loadAvailableMonths(currentWorkspaceId, getString(R.string.dashboard_month_format));
        tvDate.setOnClickListener(showDialogListener);
        View cardDateSelector = view.findViewById(R.id.cardDateSelector);
        if (cardDateSelector != null) cardDateSelector.setOnClickListener(showDialogListener);

        // "See All" Navigation
        tvSeeAll.setOnClickListener(v -> {
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.nav_transaction);
            }
        });

        // Sidebar Navigation
        MaterialToolbar toolbarPersonal = view.findViewById(R.id.toolbarPersonal);
        View bellIcon = view.findViewById(R.id.imgBellIcon);
        TextView bellBadge = view.findViewById(R.id.tvNotificationBadge);
        TextView tvTitle = view.findViewById(R.id.tvToolbarTitle);

        if (tvTitle != null) {
            tvTitle.setText("Dashboard");
        }
        if (getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).setupCommonHeader(toolbarPersonal, bellIcon, bellBadge);
        }

    }

    private void setupObservers() {
        // Observe Recent Transactions
        viewModel.getRecentTransactionsWithCategory(currentWorkspaceId).observe(getViewLifecycleOwner(), transWithCatList -> {
            List<TransactionViewModel.HistoryItem> recentItems = new ArrayList<>();
            if (transWithCatList != null) {
                for (TransactionWithCategory item : transWithCatList) {
                    if (item == null || item.transaction == null) continue;

                    String catName = (item.category != null && item.category.name != null) ? item.category.name : "Syncing...";
                    String catIcon = (item.category != null && item.category.iconName != null) ? item.category.iconName : "ic_other";
                    String catColor = (item.category != null && item.category.colorCode != null) ? item.category.colorCode : "#A9A9A9";

                    recentItems.add(new TransactionViewModel.HistoryItem(item.transaction, catName, catIcon, catColor));
                }
            }
            transactionAdapter.updateData(recentItems);
            rvRecentTransactions.setVisibility(recentItems.isEmpty() ? View.GONE : View.VISIBLE);
        });

        // Observe Dashboard Data (Chart & Balances)
        viewModel.getDashboardData().observe(getViewLifecycleOwner(), state -> {
            tvTotalBalance.setText(CurrencyFormatter.formatFullVND(state.actualBalance));
            tvIncome.setText(CurrencyFormatter.formatCompactAmount(state.totalIncome));
            tvExpense.setText(CurrencyFormatter.formatCompactAmount(state.totalExpense));

            legendAdapter.updateData(state.legendItems);
            renderPieChart(state);
        });

        // Observe Available Months for Dialog
        viewModel.getAvailableMonths().observe(getViewLifecycleOwner(), monthMap -> {
            if (monthMap == null || monthMap.isEmpty()) {
                ToastHelper.show(getContext(), "No transaction data available.");
                return;
            }

            String[] displayArray = monthMap.keySet().toArray(new String[0]);

            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Select Month")
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

        // Observe Sync Status
        mainViewModel.syncCompleted.observe(getViewLifecycleOwner(), isDone -> {
            if (Boolean.TRUE.equals(isDone)) {
                updateMonthTextAndLoadData();
            }
        });

    }

    private void setupDonutChart() {
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(0f);
        pieChart.setHoleRadius(70f);
        pieChart.animateY(700);

        pieChart.setDrawEntryLabels(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setMinAngleForSlices(17f); // Minimum touchable area
        pieChart.setUsePercentValues(true);
        pieChart.getLegend().setEnabled(false);

        String currentMonth = new SimpleDateFormat("MMMM", Locale.ENGLISH).format(currentCalendar.getTime());
        pieChart.setCenterText(getString(R.string.chart_center_text, currentMonth));
        pieChart.setCenterTextSize(15f);

        Typeface tf = ResourcesCompat.getFont(requireContext(), R.font.inter_bold);
        pieChart.setCenterTextColor(ContextCompat.getColor(requireContext(), R.color.brand_primary));
        pieChart.setCenterTextTypeface(tf);

        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, Highlight h) {
                if (e instanceof PieEntry) {
                    PieEntry pe = (PieEntry) e;
                    float percentage = (pe.getValue() / pieChart.getData().getYValueSum()) * 100f;
                    String centerText = pe.getLabel() + "\n" + String.format(Locale.US, "%.1f%%", percentage);

                    pieChart.setCenterText(centerText);
                    pieChart.setCenterTextSize(15f);
                }
            }

            @Override
            public void onNothingSelected() {
                String currentMonth = new SimpleDateFormat("MMMM", Locale.ENGLISH).format(currentCalendar.getTime());
                pieChart.setCenterText(getString(R.string.chart_center_text, currentMonth));
                pieChart.setCenterTextSize(15f);
            }
        });
    }

    private void renderPieChart(HomeViewModel.DashboardState state) {
        ArrayList<PieEntry> entries = new ArrayList<>();

        for (CategorySum cat : state.top5Categories) {
            if (cat.total > 0) entries.add(new PieEntry((float) cat.total, cat.categoryName));
        }

        if (state.othersTotal > 0) {
            entries.add(new PieEntry((float) state.othersTotal, "Others"));
        }

        if (entries.isEmpty()) {
            pieChart.clear();
            pieChart.setCenterText("No Expenses");
        } else {
            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setColors(state.pieColors);
            dataSet.setSliceSpace(0f);
            dataSet.setSelectionShift(2f);

            PieData data = new PieData(dataSet);
            data.setDrawValues(false);

            pieChart.setData(data);
            String currentMonth = new SimpleDateFormat("MMMM", Locale.ENGLISH).format(currentCalendar.getTime());
            pieChart.setCenterText(getString(R.string.chart_center_text, currentMonth));
            pieChart.setCenterTextSize(15f);
        }
        pieChart.invalidate();
    }

    private void showOthersDetailBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_others, null);

        RecyclerView rvOthers = view.findViewById(R.id.rvOthersDetail);
        rvOthers.setLayoutManager(new LinearLayoutManager(getContext()));
        LegendAdapter othersAdapter = new LegendAdapter();
        rvOthers.setAdapter(othersAdapter);

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
        tvDate.setText(getString(R.string.dashboard_month_format, month, year));

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

        viewModel.loadDashboardData(currentWorkspaceId, startOfMonth, endOfMonth);
    }
}