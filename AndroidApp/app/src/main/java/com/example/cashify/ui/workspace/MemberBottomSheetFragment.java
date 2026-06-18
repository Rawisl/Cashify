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
import com.example.cashify.utils.DialogHelper;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

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

        viewModel = new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);

        //Lấy UID từ ViewModel thay vì chọc thẳng FirebaseAuth
        String currentUid = viewModel.getCurrentUserId();

        TextView tvTitle = view.findViewById(R.id.tvSheetTitle);
        RecyclerView rvMembers = view.findViewById(R.id.rvSheetMembers);
        rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new WorkspaceMemberListAdapter(new java.util.ArrayList<>(), mode, currentUid, this);
        rvMembers.setAdapter(adapter);

        observeViewModel(tvTitle, currentUid);
    }

    private void observeViewModel(TextView tvTitle, String currentUid) {
        //Lắng nghe danh sách thành viên (Real-time)
        viewModel.getMembersLiveData().observe(getViewLifecycleOwner(), users -> {
            if (users != null) adapter.setMembers(users);
        });

        //Lắng nghe chức vụ (Real-time)
        viewModel.getWorkspaceLiveData().observe(getViewLifecycleOwner(), workspace -> {
            if (workspace != null) {
                String ownerId = workspace.getOwnerId();

                if (mode != WorkspaceMemberListAdapter.MODE_TRANSFER_OWNER) {
                    if (currentUid.equals(ownerId)) {
                        tvTitle.setText("Manage Members");
                        adapter.updateMode(WorkspaceMemberListAdapter.MODE_MANAGE_KICK);
                    } else {
                        tvTitle.setText("Workspace Members");
                        adapter.updateMode(WorkspaceMemberListAdapter.MODE_VIEW_ONLY);
                    }
                }
            }
        });

        //Lắng nghe kết quả hành động từ ViewModel (Kick/Transfer)
        viewModel.getMemberActionResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                if (result.isSuccess) {
                    DialogHelper.showSuccess(requireContext(), "Done", result.message, () -> {
                        if (result.actionType.equals("TRANSFER")) {
                            dismiss(); // Thoát BottomSheet nếu đã nhường quyền thành công
                        }
                    });
                } else {
                    DialogHelper.showAlert(requireContext(), "Error", result.message, null);
                }
                viewModel.clearMemberActionResult();
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
                    viewModel.kickMember(workspaceId, targetUser.getUid());
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
                    viewModel.transferOwnership(workspaceId, targetUser.getUid());
                },
                null
        );
    }
}