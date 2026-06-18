package com.example.cashify.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.WorkspaceInvitation;
import com.example.cashify.ui.main.BaseActivity;
import com.example.cashify.ui.main.MainActivity;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.appbar.MaterialToolbar;

public class InvitationsActivity extends BaseActivity {

    private InvitationAdapter adapter;
    private InvitationsViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitations);

        // 1. SETUP SIDEBAR TỪ BASE
        setupBaseSidebar();

        // 2. KHỞI TẠO VIEWMODEL RIÊNG CHO MÀN HÌNH NÀY
        viewModel = new ViewModelProvider(this).get(InvitationsViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbarInvitations);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
            });
        }

        setupRecyclerView();
        setupNotifications();
        observeViewModel();
    }

    private void setupRecyclerView() {
        RecyclerView rvInvitations = findViewById(R.id.rvInvitations);
        rvInvitations.setLayoutManager(new LinearLayoutManager(this));

        adapter = new InvitationAdapter(new InvitationAdapter.OnInviteClickListener() {
            @Override
            public void onAccept(WorkspaceInvitation invitation) {
                // Đẩy logic xuống ViewModel xử lý
                viewModel.acceptInvitation(invitation);
            }

            @Override
            public void onDecline(WorkspaceInvitation invitation) {
                // Đẩy logic xuống ViewModel xử lý
                viewModel.declineInvitation(invitation.getId());
            }
        });

        rvInvitations.setAdapter(adapter);
    }

    private void setupNotifications() {
        ImageButton btnNotifications = findViewById(R.id.btnNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v ->
                    new com.example.cashify.ui.notifications.NotificationBottomSheet()
                            .show(getSupportFragmentManager(), "NotificationBottomSheet"));
        }
    }

    private void observeViewModel() {
        // Hóng danh sách lời mời
        viewModel.getInvitations().observe(this, data -> adapter.setData(data));

        // Hóng kết quả hành động để hiện thông báo
        viewModel.getActionResult().observe(this, result -> {
            if (result != null) {
                ToastHelper.show(this, result.message);
                viewModel.clearActionResult();
            }
        });

        // TÁI SỬ DỤNG LẠI MAIN_VIEW_MODEL TỪ BASE_ACTIVITY ĐỂ LẤY SỐ THÔNG BÁO!
        TextView tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        if (tvNotificationBadge != null && mainViewModel != null) {
            mainViewModel.getUnreadNotificationCount().observe(this, count -> {
                if (count != null && count > 0) {
                    tvNotificationBadge.setVisibility(View.VISIBLE);
                    tvNotificationBadge.setText(count > 9 ? "9+" : String.valueOf(count));
                } else {
                    tvNotificationBadge.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    protected void onNavigationItemSelected(int itemId) {
        if (menuIdToWorkspaceIdMap.containsKey(itemId)) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("OPEN_WORKSPACE_ID", menuIdToWorkspaceIdMap.get(itemId));
            startActivity(intent);
            finish();
        } else if (itemId == R.id.nav_workspace_personal) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}