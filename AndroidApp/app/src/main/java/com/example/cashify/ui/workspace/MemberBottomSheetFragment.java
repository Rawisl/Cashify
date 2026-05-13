package com.example.cashify.ui.workspace;

import android.app.AlertDialog;
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
        new AlertDialog.Builder(requireContext())
                .setTitle("Kick Member")
                .setMessage("Remove " + targetUser.getDisplayName() + " from this workspace?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    ToastHelper.show(requireContext(), "Processing...");
                    FirebaseManager.getInstance().kickMember(workspaceId, targetUser.getUid(), new FirebaseManager.DataCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            if (getActivity() != null) getActivity().runOnUiThread(() -> {
                                ToastHelper.show(requireContext(), "Member removed!");
                                viewModel.loadWorkspaceMembers(workspaceId); // Tự động load lại List trên Android
                            });
                        }
                        @Override
                        public void onError(String message) {
                            if (getActivity() != null) getActivity().runOnUiThread(() -> ToastHelper.show(requireContext(), message));
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onTransferClicked(User targetUser) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Transfer Ownership")
                .setMessage("Make " + targetUser.getDisplayName() + " the new owner? You will become a regular member.")
                .setPositiveButton("Confirm", (dialog, which) -> {
                    ToastHelper.show(requireContext(), "Transferring...");
                    FirebaseManager.getInstance().transferOwnership(workspaceId, targetUser.getUid(), new FirebaseManager.DataCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            if (getActivity() != null) getActivity().runOnUiThread(() -> {
                                ToastHelper.show(requireContext(), "You are no longer the owner.");
                                viewModel.loadWorkspaceDetails(workspaceId); //sync giao diện ngay
                                dismiss(); // Chỉ cần đóng Sheet, ViewModel sẽ tự động làm mờ các nút của sếp ở ngoài!
                            });
                        }
                        @Override
                        public void onError(String message) {
                            if (getActivity() != null) getActivity().runOnUiThread(() -> ToastHelper.show(requireContext(), message));
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}