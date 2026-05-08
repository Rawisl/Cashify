package com.example.cashify.ui.workspace;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.cashify.R;
import com.example.cashify.utils.ToastHelper;
import com.google.firebase.auth.FirebaseAuth;

public class WorkspaceSettingsFragment extends Fragment {

    private String workspaceId;
    private WorkspaceViewModel workspaceViewModel;

    private LinearLayout btnManageCategories, btnManageMembers, btnLeaveWorkspace;
    private TextView tvLeaveDeleteTitle, tvLeaveDeleteDesc;

    public WorkspaceSettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_workspace_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Lấy ID Quỹ từ arguments (được truyền từ NavHost bên ngoài)
        Fragment parent = getParentFragment();
        while (parent != null) {
            if (parent instanceof WorkspaceContainerFragment) {
                if (parent.getArguments() != null) {
                    workspaceId = parent.getArguments().getString("WORKSPACE_ID");
                }
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
        // Xài chung ViewModel của Activity để không phải load lại data
        workspaceViewModel = new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);

        // Không cần gọi loadWorkspaceDetails nữa vì màn Home đã gọi rồi, chỉ cần observe thôi
        workspaceViewModel.getWorkspaceLiveData().observe(getViewLifecycleOwner(), workspace -> {
            if (workspace != null) {
                setupPermissions(workspace);
            }
        });
    }

    private void setupPermissions(com.example.cashify.data.model.Workspace workspace) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        boolean isOwner = currentUserId.equals(workspace.getOwnerId());

        // ==========================================
        // PHÂN QUYỀN QUẢN LÝ CATEGORY
        // ==========================================
        if (isOwner) {
            // Là Chủ Quỹ: Sáng bừng, cho phép click
            btnManageCategories.setAlpha(1.0f);
            btnManageCategories.setOnClickListener(v -> {
                // LỆNH CHUYỂN TRANG
                android.content.Intent intent = new android.content.Intent(requireContext(), WorkspaceCategoryActivity.class);
                intent.putExtra("WORKSPACE_ID", workspaceId);
                startActivity(intent);
            });

            // Đổi UI Nút nguy hiểm thành Xóa Quỹ
            tvLeaveDeleteTitle.setText("Delete Workspace");
            tvLeaveDeleteDesc.setText("Permanently delete this fund and all data");
            btnLeaveWorkspace.setOnClickListener(v -> {
                ToastHelper.show(requireContext(), "Action: Delete Workspace");
            });

        } else {
            // Là Thành viên: Làm mờ 50%, cấm vào
            btnManageCategories.setAlpha(0.5f);
            btnManageCategories.setOnClickListener(v -> {
                // Báo lỗi bằng tiếng Anh
                ToastHelper.show(requireContext(), "Access denied! Only the workspace owner can manage categories.");
            });

            // UI Nút nguy hiểm là Rời Quỹ
            tvLeaveDeleteTitle.setText("Leave Workspace");
            tvLeaveDeleteDesc.setText("Remove yourself from this fund");
            btnLeaveWorkspace.setOnClickListener(v -> {
                ToastHelper.show(requireContext(), "Action: Leave Workspace");
            });
        }

        // Quản lý thành viên (Tạm thời ai cũng xem được list, phân quyền xóa sau)
        btnManageMembers.setOnClickListener(v -> {
            ToastHelper.show(requireContext(), "Navigating to Members List...");
        });
    }
}