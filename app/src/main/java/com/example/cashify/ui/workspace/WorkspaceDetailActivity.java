package com.example.cashify.ui.workspace;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.Transaction;
import com.example.cashify.ui.transactions.AddTransactionActivity;
import com.example.cashify.ui.transactions.HistoryAdapter;
import com.example.cashify.ui.transactions.TransactionViewModel;
import com.example.cashify.utils.CurrencyFormatter;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;

public class WorkspaceDetailActivity extends AppCompatActivity {

    private RecyclerView rvMembers, rvTransactions;
    private WorkspaceMemberAdapter memberAdapter;
    private HistoryAdapter historyAdapter;
    private TextView tvBalance, tvIncome, tvExpense;
    private MaterialToolbar toolbar;

    private WorkspaceViewModel workspaceViewModel;
    private String workspaceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workspace_detail);

        workspaceId = getIntent().getStringExtra("WORKSPACE_ID");

        if (workspaceId == null || workspaceId.isEmpty()) {
            ToastHelper.show(this, "Error: Fund information not found!");
            finish();
            return;
        }

        initViewModel();
        initViews();
        observeViewModel();
    }

    private void initViewModel() {
        workspaceViewModel = new ViewModelProvider(this).get(WorkspaceViewModel.class);

        workspaceViewModel.loadWorkspaceDetails(workspaceId);
        workspaceViewModel.loadWorkspaceMembers(workspaceId);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarWorkspaceDetail);
        tvBalance = findViewById(R.id.tvWorkspaceBalance);
        tvIncome = findViewById(R.id.tvWorkspaceIncome);
        tvExpense = findViewById(R.id.tvWorkspaceExpense);
        rvMembers = findViewById(R.id.rvWorkspaceMembers);
        rvTransactions = findViewById(R.id.rvWorkspaceTransactions);

        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvMembers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        memberAdapter = new WorkspaceMemberAdapter(new ArrayList<>());
        rvMembers.setAdapter(memberAdapter);

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter();

        // Bắt sự kiện bấm vào 1 dòng giao dịch để mở màn hình chỉnh sửa
        historyAdapter.setOnTransactionClickListener(transaction -> {
            Intent intent = new Intent(this, AddTransactionActivity.class);
            intent.putExtra("TRANSACTION_ID", transaction.id);
            intent.putExtra("WORKSPACE_ID", workspaceId);
            startActivity(intent);
        });

        rvTransactions.setAdapter(historyAdapter);
        // =======================================================

        findViewById(R.id.tvAddMember).setOnClickListener(v -> {
            // Truyền cái workspaceId của màn hình hiện tại vào Bottom Sheet
            AddMemberBottomSheet bottomSheet = AddMemberBottomSheet.newInstance(workspaceId);
            bottomSheet.show(getSupportFragmentManager(), "AddMemberBottomSheet");
        });

        findViewById(R.id.fabAddWorkspaceTransaction).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddTransactionActivity.class);
            intent.putExtra("WORKSPACE_ID", workspaceId);
            startActivity(intent);
        });
    }

    @SuppressLint("SetTextI18n")
    private void observeViewModel() {
        workspaceViewModel.getWorkspaceLiveData().observe(this, workspace -> {
            if (workspace != null) {
                toolbar.setTitle(workspace.getName());
            }
        });

        workspaceViewModel.getMembersLiveData().observe(this, members -> {
            if (members != null) {
                memberAdapter.setMembers(members);
            }
        });

        workspaceViewModel.getTransactionsLiveData().observe(this, historyItems -> {
            if (historyItems != null) {
                historyAdapter.setHistoryData(historyItems);

                // Tính lại Số dư, Thu, Chi mỗi khi có cục data mới (Real-time)
                long totalIncome = 0;
                long totalExpense = 0;

                for (TransactionViewModel.HistoryItem item : historyItems) {
                    if (item.getType() == TransactionViewModel.HistoryItem.TYPE_TRANSACTION) {
                        Transaction t = item.getTransaction();
                        if (t != null) {
                            if (t.type == 1) totalIncome += t.amount;
                            else if (t.type == 0) totalExpense += t.amount;
                        }
                    }
                }

                long actualBalance = totalIncome - totalExpense;

                tvBalance.setText(CurrencyFormatter.formatFullVND(actualBalance));
                tvIncome.setText(CurrencyFormatter.formatFullVND(totalIncome));
                tvExpense.setText(CurrencyFormatter.formatFullVND(totalExpense));
            }
        });

        workspaceViewModel.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null) {
                ToastHelper.show(this, errorMsg);
            }
        });

        workspaceViewModel.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null) {
                ToastHelper.show(this, errorMsg);
            }
        });
    }
}