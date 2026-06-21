package com.example.cashify.ui.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.model.WorkspaceInvitation;
import com.example.cashify.data.remote.FirebaseManager;

import java.util.List;

public class InvitationsViewModel extends ViewModel {

    private final FirebaseManager firebaseManager = FirebaseManager.getInstance();

    private final MutableLiveData<List<WorkspaceInvitation>> _invitations = new MutableLiveData<>();
    public LiveData<List<WorkspaceInvitation>> getInvitations() { return _invitations; }

    // Helper class để bọc kết quả báo về View
    public static class ActionResult {
        public boolean isSuccess;
        public String message;
        public ActionResult(boolean isSuccess, String message) {
            this.isSuccess = isSuccess;
            this.message = message;
        }
    }

    private final MutableLiveData<ActionResult> _actionResult = new MutableLiveData<>();
    public LiveData<ActionResult> getActionResult() { return _actionResult; }
    public void clearActionResult() { _actionResult.setValue(null); }

    public InvitationsViewModel() {
        loadInvitations();
    }

    private void loadInvitations() {
        firebaseManager.listenToWorkspaceInvitations(new FirebaseManager.DataCallback<List<WorkspaceInvitation>>() {
            @Override
            public void onSuccess(List<WorkspaceInvitation> data) {
                _invitations.setValue(data);
            }
            @Override
            public void onError(String message) {
                _actionResult.setValue(new ActionResult(false, "Failed to load invitations: " + message));
            }
        });
    }

    public void acceptInvitation(WorkspaceInvitation invitation) {
        firebaseManager.acceptWorkspaceInvitation(invitation, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                _actionResult.setValue(new ActionResult(true, "Joined workspace: " + invitation.getWorkspaceName()));
            }
            @Override
            public void onError(String message) {
                _actionResult.setValue(new ActionResult(false, "Error: " + message));
            }
        });
    }

    public void declineInvitation(String invitationId) {
        firebaseManager.declineWorkspaceInvitation(invitationId, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                _actionResult.setValue(new ActionResult(true, "Invitation declined"));
            }
            @Override
            public void onError(String message) {
                _actionResult.setValue(new ActionResult(false, "Error: " + message));
            }
        });
    }
}