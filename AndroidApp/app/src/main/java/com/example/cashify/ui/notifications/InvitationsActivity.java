package com.example.cashify.ui.notifications;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
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
    private View layoutInvitationsEmpty;   // UI-consistency: empty state
    private RecyclerView rvInvitations;    // UI-consistency: field level để dùng trong observeViewModel

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyLightSystemBars();            // UI-consistency
        setContentView(R.layout.activity_invitations);
        setupBaseSidebar();
        if (drawerLayout != null) {
            drawerLayout.setStatusBarBackgroundColor(
                ContextCompat.getColor(this, R.color.bg_budget_screen)); // UI-consistency
        }
        viewModel = new ViewModelProvider(this).get(InvitationsViewModel.class); // master
        setupHeader();
        setupRecyclerView();
        observeViewModel();
    }

    private void setupHeader() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarInvitations);
        View btnNotifications = findViewById(R.id.btnNotifications);
        TextView tvNotificationBadge = findViewById(R.id.tvBellBadge);
        setupCommonHeader(toolbar, btnNotifications, tvNotificationBadge);

        // UI-consistency: nav icon mở sidebar thay vì finish()
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }
    }

    private void setupRecyclerView() {
        rvInvitations = findViewById(R.id.rvInvitations);
        layoutInvitationsEmpty = findViewById(R.id.layoutInvitationsEmpty); // UI-consistency
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

    // UI-consistency: custom status bar color
    private void applyLightSystemBars() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.bg_budget_screen));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private void observeViewModel() {
        // master: dùng ViewModel, nhưng thêm xử lý empty state của UI-consistency
        viewModel.getInvitations().observe(this, data -> {
            adapter.setData(data);
            boolean isEmpty = data == null || data.isEmpty();
            if (rvInvitations != null)
                rvInvitations.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            if (layoutInvitationsEmpty != null)
                layoutInvitationsEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        });

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