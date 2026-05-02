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
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AddWorkspaceBottomSheet extends BottomSheetDialogFragment {

    private EditText etWorkspaceName;
    private Button btnCreate;
    private WorkspaceViewModel viewModel;
    private RecyclerView rvIcons;
    private IconAdapter iconAdapter;

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
        rvIcons = view.findViewById(R.id.rvIcons);

        java.util.List<String> myIcons = java.util.Arrays.asList(
                "ic_house", "ic_gift", "ic_food", "ic_salary", "ic_family",
                "ic_entertain", "ic_transport", "ic_vacation", "ic_freelance", "ic_cafe",
                "ic_shopping", "ic_health", "ic_education", "ic_bill", "ic_bonus", "ic_other"
        );

        iconAdapter = new IconAdapter(requireContext(), myIcons);
        rvIcons.setAdapter(iconAdapter);

        // Dùng ViewModel của Activity cha để khi tạo xong, Activity cha tự biết đường update danh sách
        viewModel = new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);

        btnCreate.setOnClickListener(v -> {
            if (!isNetworkConnected()) {
                ToastHelper.show(getContext(), "Please connect to the internet to create a Group Fund!!");
                return;
            }
            String name = etWorkspaceName.getText().toString().trim();
            if (name.isEmpty()) {
                etWorkspaceName.setError("Please fill Group Name!");
                return;
            }

            viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
                if (isLoading) {
                    btnCreate.setEnabled(false);
                    btnCreate.setText("Creating..."); // Đổi chữ cho ngầu
                } else {
                    btnCreate.setEnabled(true);  // Mở khóa nút
                    btnCreate.setText("Create"); // Trả lại chữ cũ
                }
            });

            String selectedIconName = iconAdapter.getSelectedIconName();

            // Gọi hàm tạo quỹ ở ViewModel
            viewModel.createNewWorkspace(name, "GROUP", selectedIconName); // Truyền Type nếu UI có chỗ chọn
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

    // Hàm check kết nối Internet
    private boolean isNetworkConnected() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }
}