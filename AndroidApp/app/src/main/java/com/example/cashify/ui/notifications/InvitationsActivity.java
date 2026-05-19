package com.example.cashify.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.WorkspaceInvitation;
import com.example.cashify.data.remote.FirebaseManager;
import com.example.cashify.ui.main.BaseActivity;
import com.example.cashify.ui.main.MainActivity;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

public class InvitationsActivity extends BaseActivity { // Đổi sang kế thừa BaseActivity

    private InvitationAdapter adapter;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitations);

        firebaseManager = FirebaseManager.getInstance();

        // 1. GỌI HÀM CỦA CHA ĐỂ SETUP SIDEBAR
        setupBaseSidebar();

        MaterialToolbar toolbar = findViewById(R.id.toolbarInvitations);

        // 2. MỞ CỬA SIDEBAR THAY VÌ FINISH()
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        RecyclerView rvInvitations = findViewById(R.id.rvInvitations);
        rvInvitations.setLayoutManager(new LinearLayoutManager(this));

        adapter = new InvitationAdapter(new InvitationAdapter.OnInviteClickListener() {
            @Override
            public void onAccept(WorkspaceInvitation invitation) {
                firebaseManager.acceptWorkspaceInvitation(invitation, new FirebaseManager.DataCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        ToastHelper.show(InvitationsActivity.this, "You have joined the workspace: " + invitation.getWorkspaceName());
                    }

                    @Override
                    public void onError(String message) {
                        ToastHelper.show(InvitationsActivity.this, "Error: " + message);
                    }
                });
            }

            @Override
            public void onDecline(WorkspaceInvitation invitation) {
                firebaseManager.declineWorkspaceInvitation(invitation.getId(), new FirebaseManager.DataCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        ToastHelper.show(InvitationsActivity.this, "You have declined the invitation");
                    }

                    @Override
                    public void onError(String message) {
                        ToastHelper.show(InvitationsActivity.this, "Error: " + message);
                    }
                });
            }
        });

        rvInvitations.setAdapter(adapter);

        // Kéo dữ liệu từ Firebase về
        loadInvitations();

        // 3. SETUP CHUÔNG THÔNG BÁO VÀ CHẤM ĐỎ
        ImageButton btnNotifications = findViewById(R.id.btnNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                new com.example.cashify.ui.notifications.NotificationBottomSheet()
                        .show(getSupportFragmentManager(), "NotificationBottomSheet");
            });
        }

        TextView tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        if (tvNotificationBadge != null) {
            FirebaseManager.getInstance().listenToUnreadNotifications(new FirebaseManager.DataCallback<Integer>() {
                @Override
                public void onSuccess(Integer count) {
                    if (count != null && count > 0) {
                        tvNotificationBadge.setVisibility(View.VISIBLE);
                        tvNotificationBadge.setText(count > 9 ? "9+" : String.valueOf(count));
                    } else {
                        tvNotificationBadge.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(String message) {
                    tvNotificationBadge.setVisibility(View.GONE);
                }
            });
        }
    }

    // 4. XỬ LÝ LOGIC KHI BẤM VÀO ITEM QUỸ TRÊN SIDEBAR (BẮT BUỘC VÌ IMPLEMENTS)
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

    private void loadInvitations() {
        firebaseManager.listenToWorkspaceInvitations(new FirebaseManager.DataCallback<List<WorkspaceInvitation>>() {
            @Override
            public void onSuccess(List<WorkspaceInvitation> data) {
                adapter.setData(data);
            }

            @Override
            public void onError(String message) {
                ToastHelper.show(InvitationsActivity.this, "Downloading invitations failed: " + message);
            }
        });
    }
}