package com.example.cashify.ui.FriendsActivity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.cashify.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AddFriendBottomSheet extends BottomSheetDialogFragment {

    private TextInputEditText edtEmailInput;
    private MaterialButton btnSendRequest;
    private SocialViewModel socialViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_friend, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        edtEmailInput = view.findViewById(R.id.edtEmailInput);
        btnSendRequest = view.findViewById(R.id.btnSendRequest);

        // Bind to Activity-scoped ViewModel to share data/state with the parent Activity
        socialViewModel = new ViewModelProvider(requireActivity()).get(SocialViewModel.class);

        btnSendRequest.setOnClickListener(v -> {
            String email = edtEmailInput.getText() != null ? edtEmailInput.getText().toString().trim() : "";

            if (email.isEmpty()) {
                edtEmailInput.setError("Please enter an email address!");
                return;
            }

            // Delegate network request and validation to the ViewModel
            socialViewModel.searchAndSendRequestByEmail(email);

            // Dismiss the BottomSheet immediately for a snappy UX
            dismiss();
        });
    }
}