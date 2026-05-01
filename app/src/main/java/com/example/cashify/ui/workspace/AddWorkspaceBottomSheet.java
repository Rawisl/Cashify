package com.example.cashify.ui.workspace;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.cashify.R;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AddWorkspaceBottomSheet extends BottomSheetDialogFragment {

    private EditText etWorkspaceName;
    private Button btnCreate;
    private WorkspaceViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_workspace, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etWorkspaceName = view.findViewById(R.id.etWorkspaceName);
        btnCreate = view.findViewById(R.id.btnCreateWorkspace);

        // Dùng ViewModel của Activity cha để khi tạo xong, Activity cha tự biết đường update danh sách
        viewModel = new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);

        btnCreate.setOnClickListener(v -> {
            String name = etWorkspaceName.getText().toString().trim();
            if (name.isEmpty()) {
                etWorkspaceName.setError("Please fill Group Name!");
                return;
            }

            // Gọi hàm tạo quỹ ở ViewModel
            viewModel.createNewWorkspace(name, "GROUP"); // Truyền Type nếu UI có chỗ chọn
        });

        // Lắng nghe tín hiệu tạo thành công từ ViewModel
        viewModel.actionSuccess.observe(getViewLifecycleOwner(), isSuccess -> {
            if (isSuccess) {
                ToastHelper.show(getContext(), "Create group successfully!");
                viewModel.resetActionStatus(); // Reset cờ để không bị nháy lại lần sau
                dismiss(); // Đóng BottomSheet
            }
        });

        // Lắng nghe lỗi
        viewModel.errorMessage.observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                ToastHelper.show(getContext(), error);
                viewModel.resetActionStatus();
            }
        });
    }
}