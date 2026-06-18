package com.example.cashify.ui.workspace;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

    // Search optimization (Debounce)
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

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

        RecyclerView rvFriendSelect = view.findViewById(R.id.rvFriendSelect);
        btnInvite = view.findViewById(R.id.btnInvite);
        EditText etSearchFriend = view.findViewById(R.id.etSearchFriend);

        // Bind to Activity-scoped ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);

        // Initialize Adapter with dynamic counter update
        adapter = new SelectFriendAdapter(new ArrayList<>(), selectedUids, selectedCount -> {
            btnInvite.setText(getString(R.string.invite_count_format, selectedCount));
            // Fallback if string resource is missing: btnInvite.setText("Invite (" + selectedCount + ")");
        });

        rvFriendSelect.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFriendSelect.setAdapter(adapter);

        // Load and observe available friends
        viewModel.loadAvailableFriends(workspaceId);
        viewModel.availableFriends.observe(getViewLifecycleOwner(), users -> {
            if (users != null) {
                originalList = users;
                adapter.updateList(users);
            }
        });

        // Setup Debounced Search functionality
        etSearchFriend.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);

                String query = s == null ? "" : s.toString().trim();
                searchRunnable = () -> filter(query);

                // Wait 300ms after user stops typing before filtering
                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        // Handle Invitation Action
        btnInvite.setOnClickListener(v -> {
            if (selectedUids.isEmpty()) {
                ToastHelper.show(getContext(), "Please select at least one friend!");
                return;
            }

            // Retrieve current workspace name from ViewModel
            String wsName = "a workspace";
            if (viewModel.getWorkspaceLiveData().getValue() != null) {
                wsName = viewModel.getWorkspaceLiveData().getValue().getName();
            }

            // Dispatch invite request
            viewModel.addSelectedMembers(workspaceId, wsName, new ArrayList<>(selectedUids));
        });

        // Observe Success State
        viewModel.actionSuccess.observe(getViewLifecycleOwner(), isSuccess -> {
            if (Boolean.TRUE.equals(isSuccess)) {
                ToastHelper.show(getContext(), "Invitations sent successfully!");
                viewModel.resetActionStatus();
                dismiss();
            }
        });

        // Observe Error State
        viewModel.errorMessage.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                ToastHelper.show(getContext(), error);
                viewModel.resetActionStatus();
            }
        });
    }

    private void filter(String query) {
        if (query.isEmpty()) {
            adapter.updateList(originalList);
            return;
        }

        List<User> filtered = new ArrayList<>();
        String lowerCaseQuery = query.toLowerCase();

        for (User u : originalList) {
            if (u.getNameToShow() != null && u.getNameToShow().toLowerCase().contains(lowerCaseQuery)) {
                filtered.add(u);
            }
        }
        adapter.updateList(filtered);
    }
}