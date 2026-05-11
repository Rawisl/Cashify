package com.example.cashify.ui.workspace;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.Arrays;
import java.util.List;

public class AddWorkspaceBottomSheet extends BottomSheetDialogFragment {

    private EditText etWorkspaceName;
    private Button btnCreate;
    private WorkspaceViewModel viewModel;
    private RecyclerView rvIcons;
    private IconAdapter iconAdapter;

    // =========================================================
    // FIX BỆNH 1: ÉP BOTTOM SHEET MỞ BUNG FULL SIZE NGAY TỪ ĐẦU
    // =========================================================
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(d -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) d;
            View bottomSheetInternal = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheetInternal != null) {
                BottomSheetBehavior.from(bottomSheetInternal).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        return dialog;
    }

    // =========================================================
    // FIX BỆNH 2: ÉP BÀN PHÍM PHẢI ĐẨY BOTTOM SHEET TRƯỢT LÊN
    // =========================================================
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

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

        List<String> myIcons = Arrays.asList(
                "ic_house", "ic_gift", "ic_food", "ic_salary", "ic_family",
                "ic_entertain", "ic_transport", "ic_vacation", "ic_freelance", "ic_cafe",
                "ic_shopping", "ic_health", "ic_education", "ic_bill", "ic_bonus", "ic_other"
        );

        iconAdapter = new IconAdapter(requireContext(), myIcons);
        rvIcons.setAdapter(iconAdapter);

        // Dùng ViewModel của Activity cha
        viewModel = new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);

        // =========================================================
        // FIX BỆNH 3: MANG OBSERVER RA KHỎI NÚT BẤM (TRÁNH TRÀN RAM)
        // =========================================================
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                btnCreate.setEnabled(false);
                btnCreate.setText("Creating..."); // Đổi chữ cho ngầu
            } else {
                btnCreate.setEnabled(true);  // Mở khóa nút
                btnCreate.setText("Create"); // Trả lại chữ cũ
            }
        });

        viewModel.actionSuccess.observe(getViewLifecycleOwner(), isSuccess -> {
            if (isSuccess) {
                ToastHelper.show(getContext(), "Create group successfully!");
                viewModel.resetActionStatus();
                dismiss();
            }
        });

        viewModel.errorMessage.observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                ToastHelper.show(getContext(), error);
                viewModel.resetActionStatus();
            }
        });

        // =========================================================
        // SỰ KIỆN CLICK CHỈ LÀM ĐÚNG NHIỆM VỤ CỦA NÓ: KIỂM TRA & GỬI
        // =========================================================
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

            String selectedIconName = iconAdapter.getSelectedIconName();
            viewModel.createNewWorkspace(name, "GROUP", selectedIconName);
        });
    }

    private boolean isNetworkConnected() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }
}