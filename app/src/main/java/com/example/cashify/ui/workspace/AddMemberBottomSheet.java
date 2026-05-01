package com.example.cashify.ui.workspace;

import android.os.Bundle;
import android.util.Patterns;
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

public class AddMemberBottomSheet extends BottomSheetDialogFragment {

    private String workspaceId;
    private WorkspaceViewModel viewModel;

    // Cách chuẩn để truyền ID vào Fragment
    public static AddMemberBottomSheet newInstance(String workspaceId) {
        AddMemberBottomSheet fragment = new AddMemberBottomSheet();
        Bundle args = new Bundle();
        args.putString("WORKSPACE_ID", workspaceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_member, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            workspaceId = getArguments().getString("WORKSPACE_ID");
        }

        EditText etMemberEmail = view.findViewById(R.id.etMemberEmail);
        Button btnAddMember = view.findViewById(R.id.btnAddMember);

        // Lấy ViewModel của Activity cha
        viewModel = new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);

        btnAddMember.setOnClickListener(v -> {
            String email = etMemberEmail.getText().toString().trim();

            if (email.isEmpty()) {
                etMemberEmail.setError("Please enter email!");
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etMemberEmail.setError("Invalid email address!");
                return;
            }

            // Bắn email sang ViewModel để xử lý
            viewModel.addMemberByEmail(workspaceId, email);
        });

        // Lắng nghe kết quả
        viewModel.actionSuccess.observe(getViewLifecycleOwner(), isSuccess -> {
            if (isSuccess) {
                ToastHelper.show(getContext(), "Member added successfully!");
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
    }
}