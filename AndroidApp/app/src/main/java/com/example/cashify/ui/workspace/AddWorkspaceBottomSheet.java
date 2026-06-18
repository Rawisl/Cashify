package com.example.cashify.ui.workspace;

import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
    private IconAdapter iconAdapter;

    // =========================================================================
    // UX FIX 1: Enforce Expanded State
    // Forces the BottomSheet to open fully expanded instead of half-way.
    // =========================================================================
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

    // =========================================================================
    // UX FIX 2: Keyboard Handling
    // Ensures the BottomSheet is pushed up by the soft keyboard.
    // =========================================================================
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
        RecyclerView rvIcons = view.findViewById(R.id.rvIcons);

        // Pre-defined set of workspace icons
        List<String> myIcons = Arrays.asList(
                "ic_house", "ic_gift", "ic_food", "ic_salary", "ic_family",
                "ic_entertain", "ic_transport", "ic_vacation", "ic_freelance", "ic_cafe",
                "ic_shopping", "ic_health", "ic_education", "ic_bill", "ic_bonus", "ic_other"
        );

        iconAdapter = new IconAdapter(requireContext(), myIcons);
        rvIcons.setAdapter(iconAdapter);

        // Bind to Activity-scoped ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);

        // =========================================================================
        // UX FIX 3: Lifecycle-aware Observers
        // Prevents memory leaks by separating state observation from click events.
        // =========================================================================

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (Boolean.TRUE.equals(isLoading)) {
                btnCreate.setEnabled(false);
                btnCreate.setText("Creating...");
            } else {
                btnCreate.setEnabled(true);
                btnCreate.setText("Create");
            }
        });

        viewModel.actionSuccess.observe(getViewLifecycleOwner(), isSuccess -> {
            if (Boolean.TRUE.equals(isSuccess)) {
                ToastHelper.show(getContext(), "Group created successfully!");
                viewModel.resetActionStatus();
                dismiss();
            }
        });

        viewModel.errorMessage.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                ToastHelper.show(getContext(), error);
                viewModel.resetActionStatus();
            }
        });

        // =========================================================================
        // ACTION: CREATE WORKSPACE
        // =========================================================================
        btnCreate.setOnClickListener(v -> {
            if (!isNetworkConnected()) {
                ToastHelper.show(getContext(), "Please connect to the internet to create a Group Fund!");
                return;
            }

            String name = etWorkspaceName.getText() != null ? etWorkspaceName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                etWorkspaceName.setError("Please enter a Group Name!");
                return;
            }

            String selectedIconName = iconAdapter.getSelectedIconName();
            viewModel.createNewWorkspace(name, "GROUP", selectedIconName);
        });
    }

    /**
     * Checks if the device has an active network connection.
     */
    @SuppressWarnings("deprecation")
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }
}