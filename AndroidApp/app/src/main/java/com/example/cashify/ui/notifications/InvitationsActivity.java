package com.example.cashify.ui.notifications;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.WorkspaceInvitation;
import com.example.cashify.data.remote.FirebaseManager;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class InvitationsActivity extends AppCompatActivity {

    private InvitationAdapter adapter;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitations);

        firebaseManager = FirebaseManager.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbarInvitations);
        toolbar.setNavigationOnClickListener(v -> finish()); // Nút back

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
    }

    private void loadInvitations() {
        firebaseManager.listenToWorkspaceInvitations(new FirebaseManager.DataCallback<List<WorkspaceInvitation>>() {
            @Override
            public void onSuccess(List<WorkspaceInvitation> data) {
                adapter.setData(data);

                // Nếu rỗng thì có thể hiện một TextView "Không có lời mời nào" (Tài tự custom thêm UI chỗ này nếu muốn nhé)
            }

            @Override
            public void onError(String message) {
                ToastHelper.show(InvitationsActivity.this, "Downloading invitations failed: " + message);
            }
        });
    }
}