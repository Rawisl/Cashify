package com.example.cashify.ui.workspace;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;

/**
 * WorkspaceLogFragment.java — Tầng View thuần túy trong kiến trúc MVVM.
 */
public class WorkspaceLogFragment extends Fragment {

    private static final String ARG_WORKSPACE_ID = "WORKSPACE_ID";

    private RecyclerView recyclerView;
    private WorkspaceLogAdapter adapter;
    private WorkspaceLogViewModel viewModel;

    public WorkspaceLogFragment() {}

    public static WorkspaceLogFragment newInstance(String workspaceId) {
        WorkspaceLogFragment fragment = new WorkspaceLogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_WORKSPACE_ID, workspaceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_workspace_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String workspaceId = null;
        Bundle args = getArguments();
        if (args != null) workspaceId = args.getString(ARG_WORKSPACE_ID);

        if (workspaceId == null || workspaceId.isEmpty()) {
            Fragment parent = getParentFragment();
            while (parent != null) {
                if (parent instanceof WorkspaceContainerFragment) {
                    if (parent.getArguments() != null) {
                        workspaceId = parent.getArguments().getString(ARG_WORKSPACE_ID, "");
                    }
                    break;
                }
                parent = parent.getParentFragment();
            }
        }

        setupRecyclerView(view);
        setupViewModel(workspaceId);

        // Nút bấm chỉ ra lệnh, mọi nghiệp vụ để ViewModel lo
        view.findViewById(R.id.fabSeedLog).setOnClickListener(v -> {
            if (viewModel != null) {
                viewModel.seedMockLogs();
                Toast.makeText(requireContext(), "Generating mock logs...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewLogs);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(false);

        adapter = new WorkspaceLogAdapter(requireContext());
        recyclerView.setAdapter(adapter);
    }

    private void setupViewModel(String workspaceId) {
        WorkspaceLogViewModel.Factory factory = new WorkspaceLogViewModel.Factory(workspaceId);
        viewModel = new ViewModelProvider(this, factory).get(WorkspaceLogViewModel.class);

        viewModel.getLogs().observe(getViewLifecycleOwner(), logItems -> {
            if (logItems != null) {
                adapter.submitList(logItems);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(requireContext(), "Failed to load logs: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}