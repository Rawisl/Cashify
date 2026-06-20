package com.example.cashify.ui.workspace;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.ui.transactions.AddTransactionActivity;
import com.example.cashify.utils.CurrencyFormatter;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class WorkspaceHomeFragment extends Fragment {

    private RecyclerView rvMembers, rvTransactions;
    private WorkspaceMemberAdapter memberAdapter;
    private WorkspaceTransactionAdapter historyAdapter;
    private TextView tvBalance, tvIncome, tvExpense, tvNotificationBadge;
    private MaterialToolbar toolbar;
    private NestedScrollView scrollView;

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

        if (this.workspaceId != null && !this.workspaceId.isEmpty()) {
            initViewModel();
            initViews(view);
            observeViewModel();
        }
    }

    private void initViewModel() {
        workspaceViewModel = new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);
        workspaceViewModel.loadWorkspaceDetails(workspaceId);
        workspaceViewModel.loadWorkspaceMembers(workspaceId);

        // Chỉ cần gắn cò súng Realtime, nó sẽ tự auto-load trang đầu và tự tính số dư
        workspaceViewModel.startRealtimeSyncTrigger(workspaceId);
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbarWorkspaceDetail);
        tvBalance = view.findViewById(R.id.tvWorkspaceBalance);
        tvIncome = view.findViewById(R.id.tvWorkspaceIncome);
        tvExpense = view.findViewById(R.id.tvWorkspaceExpense);
        tvNotificationBadge = view.findViewById(R.id.tvBellBadge);
        rvMembers = view.findViewById(R.id.rvWorkspaceMembers);
        rvTransactions = view.findViewById(R.id.rvWorkspaceTransactions);
        scrollView = view.findViewById(R.id.workspaceScrollView);
        View bellIcon = view.findViewById(R.id.imgBellIcon);

        if (getActivity() instanceof com.example.cashify.ui.main.BaseActivity) {
            ((com.example.cashify.ui.main.BaseActivity) getActivity())
                    .setupCommonHeader(toolbar, bellIcon, tvNotificationBadge);
        }

        rvMembers.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        memberAdapter = new WorkspaceMemberAdapter(new ArrayList<>());
        rvMembers.setAdapter(memberAdapter);

        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));

        String currentUserUid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        historyAdapter = new WorkspaceTransactionAdapter(
                requireContext(),
                this.workspaceId,
                currentUserUid,
                "",
                new ArrayList<>(),
                transaction -> {
                    if (getContext() != null) {
                        Intent intent = new Intent(getContext(), AddTransactionActivity.class);
                        intent.putExtra("TRANSACTION_ID", transaction.id);
                        intent.putExtra("WORKSPACE_ID", this.workspaceId);
                        startActivity(intent);
                    }
                }
        );
        rvTransactions.setAdapter(historyAdapter);

        view.findViewById(R.id.tvAddMember).setOnClickListener(v -> {
            AddMemberBottomSheet bottomSheet = AddMemberBottomSheet.newInstance(workspaceId);
            bottomSheet.show(getChildFragmentManager(), "AddMemberBottomSheet");
        });

        if (scrollView != null) {
            scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (!v.canScrollVertically(1)) {
                    workspaceViewModel.loadWorkspaceTransactions(workspaceId, false);
                }
            });
        }
    }

    @SuppressLint("SetTextI18n")
    private void observeViewModel() {
        View progressBar = getView() != null ? getView().findViewById(R.id.progressBarWorkspace) : null;

        workspaceViewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (progressBar != null) {
                progressBar.setVisibility(isLoading != null && isLoading ? View.VISIBLE : View.GONE);
            }
        });

        workspaceViewModel.getWorkspaceLiveData().observe(getViewLifecycleOwner(), workspace -> {
            if (workspace != null) {
                toolbar.setTitle(workspace.getName());
                if (historyAdapter != null && workspace.getOwnerId() != null) {
                    historyAdapter.setOwnerId(workspace.getOwnerId());
                }
            }
        });

        workspaceViewModel.getMembersLiveData().observe(getViewLifecycleOwner(), members -> {
            if (members != null && memberAdapter != null) {
                memberAdapter.setMembers(members);
            }
        });

        workspaceViewModel.getTransactionsLiveData().observe(getViewLifecycleOwner(), historyItems -> {
            if (historyItems != null && historyAdapter != null) {
                historyAdapter.setHistoryData(historyItems);
            }
        });

        workspaceViewModel.getTotalIncomeLiveData().observe(getViewLifecycleOwner(), income -> {
            tvIncome.setText(CurrencyFormatter.formatFullVND(income != null ? income : 0L));
        });

        workspaceViewModel.getTotalExpenseLiveData().observe(getViewLifecycleOwner(), expense -> {
            tvExpense.setText(CurrencyFormatter.formatFullVND(expense != null ? expense : 0L));
        });

        workspaceViewModel.getActualBalanceLiveData().observe(getViewLifecycleOwner(), balance -> {
            tvBalance.setText(CurrencyFormatter.formatFullVND(balance != null ? balance : 0L));
        });

        workspaceViewModel.errorMessage.observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null) {
                ToastHelper.show(requireContext(), errorMsg);
            }
        });
    }
}