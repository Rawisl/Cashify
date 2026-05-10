package com.example.cashify.ui.workspace;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
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

public class WorkspaceHomeFragment extends Fragment {

    private RecyclerView rvMembers, rvTransactions;
    private WorkspaceMemberAdapter memberAdapter;
    private WorkspaceTransactionAdapter historyAdapter; // Bỏ cái cũ đi
    private TextView tvBalance, tvIncome, tvExpense;
    private MaterialToolbar toolbar;

    private WorkspaceViewModel workspaceViewModel;
    private String workspaceId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_workspace_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. LẤY ID TỪ PARENT
        // Đảm bảo dùng biến toàn cục 'this.workspaceId', KHÔNG khai báo 'String workspaceId' ở đây
        this.workspaceId = "";
        Fragment parent = getParentFragment();
        while (parent != null) {
            if (parent instanceof WorkspaceContainerFragment) {
                if (parent.getArguments() != null) {
                    this.workspaceId = parent.getArguments().getString("WORKSPACE_ID", "");
                }
                break;
            }
            parent = parent.getParentFragment();
        }


        // 2. CHỈ KHỞI TẠO KHI CÓ ID
        if (this.workspaceId != null && !this.workspaceId.isEmpty()) {
            initViewModel();
            initViews(view);
            observeViewModel();
        } else {
        }
    }

    private void initViewModel() {
        // Dùng requireActivity() để xài chung ViewModel với Activity nếu cần
        workspaceViewModel = new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);

        workspaceViewModel.loadWorkspaceDetails(workspaceId);
        workspaceViewModel.loadWorkspaceMembers(workspaceId);
        workspaceViewModel.loadWorkspaceTransactions(workspaceId);
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbarWorkspaceDetail);
        tvBalance = view.findViewById(R.id.tvWorkspaceBalance);
        tvIncome = view.findViewById(R.id.tvWorkspaceIncome);
        tvExpense = view.findViewById(R.id.tvWorkspaceExpense);
        rvMembers = view.findViewById(R.id.rvWorkspaceMembers);
        rvTransactions = view.findViewById(R.id.rvWorkspaceTransactions);

        // Mở Sidebar của MainActivity từ bên trong Fragment
        toolbar.setNavigationOnClickListener(v -> {
            androidx.drawerlayout.widget.DrawerLayout drawer = requireActivity().findViewById(R.id.drawerLayout);
            if (drawer != null) {
                drawer.openDrawer(androidx.core.view.GravityCompat.START);
            }
        });
        rvMembers.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        memberAdapter = new WorkspaceMemberAdapter(new ArrayList<>());
        rvMembers.setAdapter(memberAdapter);

        rvTransactions = view.findViewById(R.id.rvWorkspaceTransactions);

        // Đảm bảo setup RecyclerView LayoutManager trước
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));

        String finalWorkspaceId = this.workspaceId;

        historyAdapter = new WorkspaceTransactionAdapter(
                requireContext(),
                finalWorkspaceId,
                new ArrayList<>(),
                transaction -> {
                    // Click transaction → mở edit
                    Intent intent = new Intent(requireContext(), AddTransactionActivity.class);
                    intent.putExtra("TRANSACTION_ID", transaction.id);
                    intent.putExtra("WORKSPACE_ID", finalWorkspaceId);
                    startActivity(intent);
                }
        );
        rvTransactions.setAdapter(historyAdapter);

        view.findViewById(R.id.tvAddMember).setOnClickListener(v -> {
            AddMemberBottomSheet bottomSheet = AddMemberBottomSheet.newInstance(workspaceId);
            bottomSheet.show(getChildFragmentManager(), "AddMemberBottomSheet");
        });

    }

    @SuppressLint("SetTextI18n")
    private void observeViewModel() {
        workspaceViewModel.getWorkspaceLiveData().observe(getViewLifecycleOwner(), workspace -> {
            if (workspace != null) {
                toolbar.setTitle(workspace.getName());
            }
        });

        workspaceViewModel.getMembersLiveData().observe(getViewLifecycleOwner(), members -> {
            if (members != null) {
                memberAdapter.setMembers(members);
            }
        });

        workspaceViewModel.getTransactionsLiveData().observe(getViewLifecycleOwner(), historyItems -> {
            if (historyItems != null) {
                historyAdapter.setHistoryData(historyItems);

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

        workspaceViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null) {
                ToastHelper.show(requireContext(), errorMsg);
            }
        });
    }
}