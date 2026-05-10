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
import com.example.cashify.data.repository.WorkspaceLogRepository;

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

        // Lấy workspaceId từ Bundle hoặc parent
        String workspaceId = null;
        Bundle args = getArguments();
        if (args != null) workspaceId = args.getString("WORKSPACE_ID");

        if (workspaceId == null || workspaceId.isEmpty()) {
            Fragment parent = getParentFragment();
            while (parent != null) {
                if (parent instanceof WorkspaceContainerFragment) {
                    if (parent.getArguments() != null) {
                        workspaceId = parent.getArguments().getString("WORKSPACE_ID", "");
                    }
                    break;
                }
                parent = parent.getParentFragment();
            }
        }
        

        setupRecyclerView(view);
        setupViewModel(workspaceId);

        // FAB seed mock data
        String finalWorkspaceId = workspaceId;
        view.findViewById(R.id.fabSeedLog).setOnClickListener(v -> {
            seedMockLogs(finalWorkspaceId);
            Toast.makeText(requireContext(), "Đã tạo mock log!", Toast.LENGTH_SHORT).show();
        });
    }

    private void seedMockLogs(String workspaceId) {
        String uid = com.example.cashify.data.remote.FirebaseManager.getInstance().getCurrentUserId();

        String[][] mocks = {
                {"CREATE_WORKSPACE",   "đã khởi tạo quỹ nhóm này"},
                {"ADD_TRANSACTION",    "đã thêm giao dịch Chi 200,000đ - Ăn uống"},
                {"ADD_TRANSACTION",    "đã thêm giao dịch Thu 1,500,000đ - Lương"},
                {"EDIT_TRANSACTION",   "đã chỉnh sửa giao dịch 300,000đ - Mua sắm"},
                {"DELETE_TRANSACTION", "đã xóa giao dịch 50,000đ - Di chuyển"},
                {"ADD_MEMBER",         "đã thêm thành viên mới vào quỹ"},
                {"ADD_BUDGET",         "đã tạo ngân sách Ăn uống 2,000,000đ/tháng"},
                {"EDIT_BUDGET",        "đã cập nhật ngân sách Di chuyển lên 500,000đ"},
        };

        // Thêm delay nhỏ giữa các log để timestamp khác nhau
        android.os.Handler handler = new android.os.Handler();
        for (int i = 0; i < mocks.length; i++) {
            final int index = i;
            handler.postDelayed(() ->
                            WorkspaceLogRepository.pushLog(
                                    workspaceId,
                                    uid,
                                    mocks[index][0],
                                    mocks[index][1]
                            ),
                    index * 100L); // cách nhau 100ms để timestamp khác nhau
        }
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