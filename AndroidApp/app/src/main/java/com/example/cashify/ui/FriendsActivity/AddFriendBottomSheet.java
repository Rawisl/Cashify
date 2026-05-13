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

        // Móc lấy ViewModel CHUNG với thằng Activity cha
        socialViewModel = new ViewModelProvider(requireActivity()).get(SocialViewModel.class);

        btnSendRequest.setOnClickListener(v -> {
            String email = edtEmailInput.getText() != null ? edtEmailInput.getText().toString().trim() : "";

            if (email.isEmpty()) {
                edtEmailInput.setError("Vui lòng nhập Email!");
                return;
            }

            // Gọi hàm check và bắn Request lên mây
            socialViewModel.searchAndSendRequestByEmail(email);

            // Đóng BottomSheet vuốt xuống cái vèo cho ngầu
            dismiss();
        });
    }
}