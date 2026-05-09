package com.example.cashify.ui.workspace;

import android.os.Bundle;
import android.util.Log;
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
 * WorkspaceLogFragment.java — tầng View trong MVVM.
 *
 * Trách nhiệm của Fragment này:
 *  - Khởi tạo ViewModel (qua Factory truyền workspaceId)
 *  - Observe LiveData từ ViewModel
 *  - Cập nhật RecyclerView khi data thay đổi
 *  - KHÔNG gọi Firestore trực tiếp
 *  - KHÔNG chứa business logic
 *
 * Nhận workspaceId qua newInstance() / Bundle.
 */
public class WorkspaceLogFragment extends Fragment {

    private static final String ARG_WORKSPACE_ID = "WORKSPACE_ID";

    // ── UI ────────────────────────────────────────────────────────────────────
    private RecyclerView         recyclerView;
    private WorkspaceLogAdapter  adapter;

    // ── ViewModel ─────────────────────────────────────────────────────────────
    private WorkspaceLogViewModel viewModel;

    // ── Required empty constructor ────────────────────────────────────────────
    public WorkspaceLogFragment() {}

    // ── Factory ───────────────────────────────────────────────────────────────
    public static WorkspaceLogFragment newInstance(String workspaceId) {
        WorkspaceLogFragment fragment = new WorkspaceLogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_WORKSPACE_ID, workspaceId);
        fragment.setArguments(args);
        return fragment;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_workspace_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String workspaceId = "";

        if (getArguments() != null) {
            workspaceId = getArguments().getString("WORKSPACE_ID", "");
        }

        // 3. Kiểm tra logic
        if (workspaceId == null || workspaceId.trim().isEmpty()) {
            // Chỉ hiện Toast nếu THỰC SỰ không có ID
            android.util.Log.e("DEBUG_FLOW", "=> LỖI: workspaceId rỗng, dừng setup.");
            Toast.makeText(requireContext(), "Lỗi: Không tìm thấy nhóm!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. Nếu đi đến đây nghĩa là ID đã ngon lành
        setupRecyclerView(view);
        setupViewModel(workspaceId);
    }

    // ── Setup RecyclerView ────────────────────────────────────────────────────
    private void setupRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.recyclerViewLogs);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setHasFixedSize(false);

        adapter = new WorkspaceLogAdapter(requireContext());
        recyclerView.setAdapter(adapter);
    }

    // ── Setup ViewModel + Observe ─────────────────────────────────────────────
    private void setupViewModel(String workspaceId) {
        WorkspaceLogViewModel.Factory factory =
                new WorkspaceLogViewModel.Factory(workspaceId);

        viewModel = new ViewModelProvider(this, factory)
                .get(WorkspaceLogViewModel.class);

        // Observe danh sách log → đẩy vào adapter (DiffUtil xử lý diff)
        viewModel.getLogs().observe(getViewLifecycleOwner(), logItems -> {
            if (logItems != null) {
                adapter.submitList(logItems);
            }
        });

        // Observe lỗi → hiển thị Toast đơn giản
        viewModel.getError().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(requireContext(),
                        "Lỗi tải log: " + errorMsg,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}