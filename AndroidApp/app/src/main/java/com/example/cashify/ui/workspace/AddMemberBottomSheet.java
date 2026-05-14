package com.example.cashify.ui.workspace;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.User;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddMemberBottomSheet extends BottomSheetDialogFragment {

    private String workspaceId;
    private WorkspaceViewModel viewModel;
    private MaterialButton btnInvite;

    private SelectFriendAdapter adapter;
    private List<User> originalList = new ArrayList<>();
    private final Set<String> selectedUids = new HashSet<>();

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
        if (getArguments() != null) workspaceId = getArguments().getString("WORKSPACE_ID");

        RecyclerView rvFriendSelect = view.findViewById(R.id.rvFriendSelect);
        btnInvite = view.findViewById(R.id.btnInvite);
        EditText etSearchFriend = view.findViewById(R.id.etSearchFriend);
        viewModel = new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);

        // Khởi tạo Adapter ĐÃ TÁCH
        adapter = new SelectFriendAdapter(new ArrayList<>(), selectedUids, selectedCount -> {
            btnInvite.setText("Invite (" + selectedCount + ")");
        });

        rvFriendSelect.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFriendSelect.setAdapter(adapter);

        viewModel.loadAvailableFriends(workspaceId);
        viewModel.availableFriends.observe(getViewLifecycleOwner(), users -> {
            if (users != null) {
                originalList = users;
                adapter.updateList(users);
            }
        });

        etSearchFriend.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString().trim()); }
            @Override public void afterTextChanged(Editable s) {}
        });
        btnInvite.setOnClickListener(v -> {
            if (selectedUids.isEmpty()) {
                ToastHelper.show(getContext(), "Please select at least one friend!");
                return;
            }

            // Lấy tên Quỹ hiện tại từ ViewModel đang chạy
            String wsName = "a workspace";
            if (viewModel.getWorkspaceLiveData().getValue() != null) {
                wsName = viewModel.getWorkspaceLiveData().getValue().getName();
            }

            // Gửi lời mời với ĐẦY ĐỦ 3 tham số
            viewModel.addSelectedMembers(workspaceId, wsName, new ArrayList<>(selectedUids));
        });


        // Lắng nghe khi gửi mời thành công
        viewModel.actionSuccess.observe(getViewLifecycleOwner(), isSuccess -> {
            if (isSuccess != null && isSuccess) {
                ToastHelper.show(getContext(), "You have invited selected friends!");
                viewModel.resetActionStatus(); // Reset lại cờ để lần sau bấm tiếp được
                dismiss(); // Đóng BottomSheet
            }
        });

        // Lắng nghe nếu có lỗi xảy ra (Ví dụ: Server C# chưa bật)
        viewModel.errorMessage.observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                ToastHelper.show(getContext(), error);
                viewModel.resetActionStatus();
            }
        });
    }

    private void filter(String query) {
        if (query.isEmpty()) { adapter.updateList(originalList); return; }
        List<User> filtered = new ArrayList<>();
        for (User u : originalList) {
            if (u.getNameToShow().toLowerCase().contains(query.toLowerCase())) filtered.add(u);
        }
        adapter.updateList(filtered);
    }
}