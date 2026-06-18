package com.example.cashify.ui.workspace;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.cashify.R;
import com.example.cashify.data.model.User;
import com.example.cashify.data.model.Workspace;
import com.example.cashify.ui.main.MainActivity;
import com.example.cashify.utils.DialogHelper;
import com.example.cashify.utils.ToastHelper;

import java.util.List;

public class WorkspaceSettingsFragment extends Fragment {

    private String workspaceId;
    private WorkspaceViewModel workspaceViewModel;

    private LinearLayout btnManageCategories, btnManageMembers, btnLeaveWorkspace;
    private TextView tvLeaveDeleteTitle, tvLeaveDeleteDesc;

    public WorkspaceSettingsFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_workspace_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Traverse fragment tree to extract workspaceId from container arguments
        Fragment parent = getParentFragment();
        while (parent != null) {
            if (parent instanceof WorkspaceContainerFragment) {
                if (parent.getArguments() != null) workspaceId = parent.getArguments().getString("WORKSPACE_ID");
                break;
            }
            parent = parent.getParentFragment();
        }

        initViews(view);
        initViewModel();
    }

    private void initViews(View view) {
        btnManageCategories = view.findViewById(R.id.btn_manage_categories);
        btnManageMembers = view.findViewById(R.id.btn_manage_members);
        btnLeaveWorkspace = view.findViewById(R.id.btn_leave_workspace);
        tvLeaveDeleteTitle = view.findViewById(R.id.tv_leave_delete_title);
        tvLeaveDeleteDesc = view.findViewById(R.id.tv_leave_delete_desc);
    }

    private void initViewModel() {
        workspaceViewModel = new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);

        // 1. Kick Trap: Automatically redirect if the user leaves or is kicked
        workspaceViewModel.isKickedOut.observe(getViewLifecycleOwner(), isKicked -> {
            if (Boolean.TRUE.equals(isKicked)) {
                ToastHelper.show(requireContext(), "You are no longer in this workspace.");
                forceQuitToPersonal();
            }
        });

        // 2. Real-time UI updates based on workspace state
        workspaceViewModel.getWorkspaceLiveData().observe(getViewLifecycleOwner(), workspace -> {
            if (workspace != null) updateUI(workspace);
        });

        // 3. Real-time UI updates based on member count (e.g., Leave vs Delete)
        workspaceViewModel.getMembersLiveData().observe(getViewLifecycleOwner(), users -> {
            Workspace ws = workspaceViewModel.getWorkspaceLiveData().getValue();
            if (ws != null) updateUI(ws);
        });

        // 4. Observe general network/action errors
        workspaceViewModel.errorMessage.observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null) {
                DialogHelper.showAlert(requireContext(), "Error", errorMsg, null);
            }
        });
    }

    private void updateUI(Workspace workspace) {
        String currentUserId = workspaceViewModel.getCurrentUserId();
        boolean isOwner = currentUserId != null && currentUserId.equals(workspace.getOwnerId());

        List<User> members = workspaceViewModel.getMembersLiveData().getValue();
        int memberCount = (members != null) ? members.size() : 1;

        if (isOwner) {
            btnManageCategories.setAlpha(1.0f);
            btnManageCategories.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), WorkspaceCategoryActivity.class);
                intent.putExtra("WORKSPACE_ID", workspaceId);
                startActivity(intent);
            });

            // Dynamic UI for Leave/Delete context
            if (memberCount <= 1) {
                tvLeaveDeleteTitle.setText("Delete Workspace");
                tvLeaveDeleteDesc.setText("Permanently delete this fund and all data");
            } else {
                tvLeaveDeleteTitle.setText("Leave Workspace");
                tvLeaveDeleteDesc.setText("Transfer ownership and leave this fund");
            }
        } else {
            btnManageCategories.setAlpha(0.5f);
            btnManageCategories.setOnClickListener(v ->
                    ToastHelper.show(requireContext(), "Access denied! Only the owner can manage categories.")
            );

            tvLeaveDeleteTitle.setText("Leave Workspace");
            tvLeaveDeleteDesc.setText("Remove yourself from this fund");
        }

        btnManageMembers.setOnClickListener(v -> {
            int mode = isOwner ? WorkspaceMemberListAdapter.MODE_MANAGE_KICK : WorkspaceMemberListAdapter.MODE_VIEW_ONLY;
            MemberBottomSheetFragment bottomSheet = MemberBottomSheetFragment.newInstance(workspaceId, mode);
            bottomSheet.show(getChildFragmentManager(), "MemberBottomSheet");
        });

        btnLeaveWorkspace.setOnClickListener(v -> {
            if (isOwner && memberCount > 1) {
                DialogHelper.showAlert(
                        requireContext(),
                        "Action Required",
                        "You cannot leave while you are the owner. Please open 'Manage Members' and transfer ownership first.",
                        null
                );
            } else {
                String title = isOwner ? "Delete Workspace?" : "Leave Workspace?";
                String msg = isOwner ? "Are you sure you want to delete this workspace forever?" : "Are you sure you want to leave?";

                DialogHelper.showCustomDialog(
                        requireContext(),
                        title,
                        msg,
                        "Confirm",
                        "Cancel",
                        DialogHelper.DialogType.DANGER,
                        true,
                        () -> workspaceViewModel.leaveWorkspace(workspaceId), // Delegate to ViewModel
                        null
                );
            }
        });
    }

    private void forceQuitToPersonal() {
        if (getActivity() != null) {
            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }
}