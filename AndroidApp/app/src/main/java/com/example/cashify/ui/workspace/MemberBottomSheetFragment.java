package com.example.cashify.ui.workspace;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.User;
import com.example.cashify.data.remote.FirebaseManager;
import com.example.cashify.utils.DialogHelper;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;

public class MemberBottomSheetFragment extends BottomSheetDialogFragment implements WorkspaceMemberListAdapter.OnMemberActionListener {

    private String workspaceId;
    private int mode;
    private WorkspaceViewModel viewModel;
    private WorkspaceMemberListAdapter adapter;

    public static MemberBottomSheetFragment newInstance(String workspaceId, int mode) {
        MemberBottomSheetFragment fragment = new MemberBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("WORKSPACE_ID", workspaceId);
        args.putInt("MODE", mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_workspace_members, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            workspaceId = getArguments().getString("WORKSPACE_ID");
            mode = getArguments().getInt("MODE", WorkspaceMemberListAdapter.MODE_VIEW_ONLY);
        }

        TextView tvTitle = view.findViewById(R.id.tvSheetTitle);
        RecyclerView rvMembers = view.findViewById(R.id.rvSheetMembers);
        rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));

        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        adapter = new WorkspaceMemberListAdapter(new java.util.ArrayList<>(), mode, currentUid, this);
        rvMembers.setAdapter(adapter);

        viewModel = new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);

        // 1. Lắng nghe danh sách thành viên (Real-time)
        viewModel.getMembersLiveData().observe(getViewLifecycleOwner(), users -> {
            if (users != null) adapter.setMembers(users);
        });

        // 2. PHÉP THUẬT Ở ĐÂY: Lắng nghe chức vụ (Real-time)
        viewModel.getWorkspaceLiveData().observe(getViewLifecycleOwner(), workspace -> {
            if (workspace != null) {
                String ownerId = workspace.getOwnerId();

                // Trừ trường hợp đang ở mode TRANSFER_OWNER thì không đổi UI
                if (mode != WorkspaceMemberListAdapter.MODE_TRANSFER_OWNER) {
                    if (currentUid.equals(ownerId)) {
                        tvTitle.setText("Manage Members");
                        adapter.updateMode(WorkspaceMemberListAdapter.MODE_MANAGE_KICK); // Lên làm vua -> Hiện thùng rác
                    } else {
                        tvTitle.setText("Workspace Members");
                        adapter.updateMode(WorkspaceMemberListAdapter.MODE_VIEW_ONLY); // Bị giáng cấp -> Ẩn thùng rác
                    }
                }
            }
        });
    }

    @Override
    public void onKickClicked(User targetUser) {
        DialogHelper.showCustomDialog(
                requireContext(),
                "Kick Member",
                "Remove " + targetUser.getDisplayName() + " from this workspace?",
                "Remove",
                "Cancel",
                DialogHelper.DialogType.DANGER,
                true,
                () -> {
                    FirebaseManager.getInstance().kickMember(workspaceId, targetUser.getUid(), new FirebaseManager.DataCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            if (getActivity() != null) getActivity().runOnUiThread(() -> {
                                DialogHelper.showSuccess(requireContext(), "Done", "Member removed!", () ->
                                        viewModel.loadWorkspaceMembers(workspaceId)
                                );
                            });
                        }
                        @Override
                        public void onError(String message) {
                            if (getActivity() != null) getActivity().runOnUiThread(() ->
                                    DialogHelper.showAlert(requireContext(), "Error", message, null)
                            );
                        }
                    });
                },
                null
        );
    }

    @Override
    public void onTransferClicked(User targetUser) {
        DialogHelper.showCustomDialog(
                requireContext(),
                "Transfer Ownership",
                "Make " + targetUser.getDisplayName() + " the new owner? You will become a regular member.",
                "Confirm",
                "Cancel",
                DialogHelper.DialogType.NORMAL,
                true,
                () -> {
                    FirebaseManager.getInstance().transferOwnership(workspaceId, targetUser.getUid(), new FirebaseManager.DataCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            if (getActivity() != null) getActivity().runOnUiThread(() -> {
                                DialogHelper.showSuccess(requireContext(), "Done", "You are no longer the owner.", () -> {
                                    viewModel.loadWorkspaceDetails(workspaceId);
                                    dismiss();
                                });
                            });
                        }
                        @Override
                        public void onError(String message) {
                            if (getActivity() != null) getActivity().runOnUiThread(() ->
                                    DialogHelper.showAlert(requireContext(), "Error", message, null)
                            );
                        }
                    });
                },
                null
        );
    }
}