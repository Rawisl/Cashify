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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.Transaction;
import com.example.cashify.data.model.WorkspaceInvitation;
import com.example.cashify.ui.transactions.AddTransactionActivity;
import com.example.cashify.ui.transactions.TransactionViewModel;
import com.example.cashify.utils.CurrencyFormatter;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class WorkspaceHomeFragment extends Fragment { // Vẫn giữ nguyên extends Fragment

    private RecyclerView rvMembers, rvTransactions;
    private WorkspaceMemberAdapter memberAdapter;
    private WorkspaceTransactionAdapter historyAdapter;
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
        }
    }

    private void initViewModel() {
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

        // ============================================================
        // VỚI TAY RA MÀN HÌNH CHÍNH (MAIN ACTIVITY) ĐỂ MỞ SIDEBAR
        // ============================================================
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                androidx.drawerlayout.widget.DrawerLayout drawer = getActivity().findViewById(R.id.drawerLayout);
                if (drawer != null) {
                    drawer.openDrawer(androidx.core.view.GravityCompat.START);
                }
            }
        });

        // Setup RecyclerView Members
        rvMembers.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        memberAdapter = new WorkspaceMemberAdapter(new ArrayList<>());
        rvMembers.setAdapter(memberAdapter);

        // Setup RecyclerView Transactions
        rvTransactions.setLayoutManager(new LinearLayoutManager(requireContext()));

        String currentUserUid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        String finalWorkspaceId = this.workspaceId;

        // Khởi tạo Adapter an toàn
        historyAdapter = new WorkspaceTransactionAdapter(
                requireContext(),
                finalWorkspaceId,
                currentUserUid,
                "",
                new ArrayList<>(),
                transaction -> {
                    // Check getContext để không bị văng app
                    if (getContext() != null) {
                        Intent intent = new Intent(getContext(), AddTransactionActivity.class);
                        intent.putExtra("TRANSACTION_ID", transaction.id);
                        intent.putExtra("WORKSPACE_ID", finalWorkspaceId);
                        startActivity(intent);
                    }
                }
        );
        rvTransactions.setAdapter(historyAdapter);

        view.findViewById(R.id.tvAddMember).setOnClickListener(v -> {
            AddMemberBottomSheet bottomSheet = AddMemberBottomSheet.newInstance(workspaceId);
            bottomSheet.show(getChildFragmentManager(), "AddMemberBottomSheet");
        });

        // ============================================================
        // MẶC ÁO GIÁP CHỐNG VĂNG APP KHI CHUYỂN TAB ĐANG LOAD DỞ
        // ============================================================
        TextView tvNotificationBadge = view.findViewById(R.id.tvNotificationBadge);

        com.example.cashify.data.remote.FirebaseManager.getInstance()
                .listenToUnreadNotifications(new com.example.cashify.data.remote.FirebaseManager.DataCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer count) {
                        // CHỐT CHẶN: Nếu Fragment bị giấu đi rồi thì nghỉ vẽ
                        if (!isAdded() || getView() == null) return;

                        if (count != null && count > 0) {
                            tvNotificationBadge.setVisibility(View.VISIBLE);
                            tvNotificationBadge.setText(count > 9 ? "9+" : String.valueOf(count));
                        } else {
                            tvNotificationBadge.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded() || getView() == null) return;
                        tvNotificationBadge.setVisibility(View.GONE);
                    }
                });

        view.findViewById(R.id.btnWorkspaceNotifications).setOnClickListener(v -> {
            new com.example.cashify.ui.notifications.NotificationBottomSheet()
                    .show(getChildFragmentManager(), "NotificationBottomSheet");
        });
    }

    @SuppressLint("SetTextI18n")
    private void observeViewModel() {

        View progressBar = getView().findViewById(R.id.progressBarWorkspace);

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

        // ============================================================
        // MẶC ÁO GIÁP CHO PHẦN BÁO LỖI (CHỐNG TOAST BÓNG MA)
        // ============================================================
        workspaceViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null) {
                // Phải check getContext() != null thay vì xài requireContext()
                if (getContext() != null) {
                    ToastHelper.show(getContext(), errorMsg);
                }
                // Xóa thông báo lỗi ngay lập tức để lần sau lật tab nó không tự bung Toast ảo ra nữa
                workspaceViewModel.resetActionStatus();
            }
        });
    }
}