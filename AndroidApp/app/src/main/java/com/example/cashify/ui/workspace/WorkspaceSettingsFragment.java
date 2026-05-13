package com.example.cashify.ui.workspace;

import android.app.AlertDialog;
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
import com.example.cashify.data.remote.FirebaseManager;
import com.example.cashify.utils.ToastHelper;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class WorkspaceSettingsFragment extends Fragment {

    private String workspaceId;
    private WorkspaceViewModel workspaceViewModel;

    private LinearLayout btnManageCategories, btnManageMembers, btnLeaveWorkspace;
    private TextView tvLeaveDeleteTitle, tvLeaveDeleteDesc;

    public WorkspaceSettingsFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_workspace_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        // 1. HỨNG BẪY KICK: Nếu ViewModel báo bị Kick -> Văng ngay lập tức!
        workspaceViewModel.isKickedOut.observe(getViewLifecycleOwner(), isKicked -> {
            if (isKicked != null && isKicked) {
                ToastHelper.show(requireContext(), "You are no longer in this workspace.");
                forceQuitToPersonal();
            }
        });

        // 2. THEO DÕI REAL-TIME SỐ LƯỢNG THÀNH VIÊN ĐỂ ĐỔI UI NÚT
        workspaceViewModel.getWorkspaceLiveData().observe(getViewLifecycleOwner(), workspace -> {
            if (workspace != null) updateUI(workspace);
        });

        workspaceViewModel.getMembersLiveData().observe(getViewLifecycleOwner(), users -> {
            com.example.cashify.data.model.Workspace ws = workspaceViewModel.getWorkspaceLiveData().getValue();
            if (ws != null) updateUI(ws); // Kích hoạt lại UI mỗi khi có người ra/vào
        });
    }

    private void updateUI(com.example.cashify.data.model.Workspace workspace) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        boolean isOwner = currentUserId.equals(workspace.getOwnerId());

        List<User> members = workspaceViewModel.getMembersLiveData().getValue();
        int memberCount = (members != null) ? members.size() : 1;

        if (isOwner) {
            btnManageCategories.setAlpha(1.0f);
            btnManageCategories.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(requireContext(), WorkspaceCategoryActivity.class);
                intent.putExtra("WORKSPACE_ID", workspaceId);
                startActivity(intent);
            });

            // XỬ LÝ NÚT LEAVE/DELETE ĐỘNG MƯỢT MÀ
            if (memberCount <= 1) {
                tvLeaveDeleteTitle.setText("Delete Workspace");
                tvLeaveDeleteDesc.setText("Permanently delete this fund and all data");
            } else {
                tvLeaveDeleteTitle.setText("Leave Workspace");
                tvLeaveDeleteDesc.setText("Transfer ownership and leave this fund");
            }
        } else {
            btnManageCategories.setAlpha(0.5f);
            btnManageCategories.setOnClickListener(v -> ToastHelper.show(requireContext(), "Access denied! Only the owner can manage categories."));

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
                new AlertDialog.Builder(requireContext())
                        .setTitle("Action Required")
                        .setMessage("You cannot leave while you are the owner. Please open 'Manage Members' and transfer ownership first.")
                        .setPositiveButton("OK", null).show();
            } else {
                String title = isOwner ? "Delete Workspace?" : "Leave Workspace?";
                String msg = isOwner ? "Are you sure you want to delete this workspace forever?" : "Are you sure you want to leave?";

                new AlertDialog.Builder(requireContext())
                        .setTitle(title).setMessage(msg)
                        .setPositiveButton("Confirm", (dialog, which) -> {
                            FirebaseManager.getInstance().leaveWorkspace(workspaceId, new FirebaseManager.DataCallback<Void>() {
                                @Override
                                public void onSuccess(Void data) { } // Firebase snapshot sẽ tự động xử lý và văng app
                                @Override
                                public void onError(String message) {
                                    if (getActivity() != null) getActivity().runOnUiThread(() -> ToastHelper.show(requireContext(), message));
                                }
                            });
                        }).setNegativeButton("Cancel", null).show();
            }
        });
    }

    private void forceQuitToPersonal() {
        if (getActivity() != null) {
            android.content.Intent intent = new android.content.Intent(requireContext(), com.example.cashify.ui.main.MainActivity.class);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }
}