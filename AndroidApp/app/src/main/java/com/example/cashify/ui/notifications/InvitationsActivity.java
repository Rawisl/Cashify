package com.example.cashify.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

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
        setupBaseSidebar();
        viewModel = new ViewModelProvider(this).get(InvitationsViewModel.class);
        setupHeader();
        setupRecyclerView();
        observeViewModel();
    }

    private void setupHeader() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarInvitations);
        View btnNotifications = findViewById(R.id.btnNotifications);
        TextView tvNotificationBadge = findViewById(R.id.tvBellBadge);
        setupCommonHeader(toolbar, btnNotifications, tvNotificationBadge);
    }

    private void setupRecyclerView() {
        RecyclerView rvInvitations = findViewById(R.id.rvInvitations);
        rvInvitations.setLayoutManager(new LinearLayoutManager(this));

        adapter = new InvitationAdapter(new InvitationAdapter.OnInviteClickListener() {
            @Override
            public void onAccept(WorkspaceInvitation invitation) {
                viewModel.acceptInvitation(invitation);
            }

            @Override
            public void onDecline(WorkspaceInvitation invitation) {
                viewModel.declineInvitation(invitation.getId());
            }
        });

        rvInvitations.setAdapter(adapter);
    }

    private void observeViewModel() {
        // danh sách lời mời
        viewModel.getInvitations().observe(this, data -> adapter.setData(data));

        //kết quả hành động để hiện thông báo
        viewModel.getActionResult().observe(this, result -> {
            if (result != null) {
                ToastHelper.show(this, result.message);
                viewModel.clearActionResult();
            }
        });

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