package com.example.cashify.ui.workspace;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.utils.ToastHelper;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WorkspaceChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WorkspaceChatFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    // === CÁC BIẾN MỚI THÊM CHO TÍNH NĂNG CHAT ===
    private String workspaceId;
    private WorkspaceViewModel workspaceViewModel;
    private ChatAdapter chatAdapter;
    private RecyclerView rvChatMessages;
    private EditText edtMessageInput;
    private ImageButton btnSendMessage;

    public WorkspaceChatFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment WorkspaceChatFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static WorkspaceChatFragment newInstance(String param1, String param2) {
        WorkspaceChatFragment fragment = new WorkspaceChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_workspace_chat, container, false);
    }

    // === PHẦN LOGIC CHAT MỚI THÊM ===
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Kỹ thuật leo cây tìm workspaceId từ Fragment cha
        Fragment parent = getParentFragment();
        while (parent != null) {
            if (parent instanceof WorkspaceContainerFragment) {
                if (parent.getArguments() != null) {
                    workspaceId = parent.getArguments().getString("WORKSPACE_ID");
                }
                break;
            }
            parent = parent.getParentFragment();
        }

        // Ánh xạ
        rvChatMessages = view.findViewById(R.id.rvChatMessages);
        edtMessageInput = view.findViewById(R.id.edtMessageInput);
        btnSendMessage = view.findViewById(R.id.btnSendMessage);
        ImageView imgWorkspaceIcon = view.findViewById(R.id.imgWorkspaceIcon);
        TextView tvWorkspaceName = view.findViewById(R.id.tvWorkspaceName);
        View layoutInput = view.findViewById(R.id.layoutInput);

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true); // Để tin nhắn đẩy từ dưới lên
        rvChatMessages.setLayoutManager(layoutManager);

        // Khởi tạo Adapter kèm theo sự kiện nhấn giữ tin nhắn
        chatAdapter = new ChatAdapter(new ArrayList<>(), message -> {
            // Sử dụng DialogHelper xịn xò của Tài (Nút đỏ DANGER)
            com.example.cashify.utils.DialogHelper.showCustomDialog(
                    requireContext(),
                    "Delete message?",
                    "Are you sure deleting this message?",
                    "Delete",
                    "Cancel",
                    com.example.cashify.utils.DialogHelper.DialogType.DANGER,
                    true,
                    () -> {
                        // Sự kiện khi bấm nút "Thu hồi"
                        workspaceViewModel.deleteChatMessage(workspaceId, message.getMessageId());
                    },
                    null
            );
        });
        rvChatMessages.setAdapter(chatAdapter);

        // Setup ViewModel
        workspaceViewModel = new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);

        workspaceViewModel.getWorkspaceLiveData().observe(getViewLifecycleOwner(), workspace -> {
            if (workspace != null) {
                tvWorkspaceName.setText(workspace.getName());

                int resId = requireContext().getResources().getIdentifier(workspace.getIconName(), "drawable", requireContext().getPackageName());
                if (resId != 0) {
                    imgWorkspaceIcon.setImageResource(resId);
                } else {
                    imgWorkspaceIcon.setImageResource(R.drawable.ic_other);
                }
            }
        });

        // Bắt đầu lắng nghe tin nhắn Realtime
        workspaceViewModel.listenForChatMessages(workspaceId);

        // Cập nhật giao diện khi có tin nhắn mới
        workspaceViewModel.getChatMessages().observe(getViewLifecycleOwner(), messages -> {
            if (messages != null) {
                chatAdapter.setMessages(messages);
                // Cuộn xuống dòng cuối cùng cực mượt
                if (!messages.isEmpty()) {
                    rvChatMessages.smoothScrollToPosition(messages.size() - 1);
                }
            }
        });

        // Xử lý nút Gửi
        btnSendMessage.setOnClickListener(v -> {
            String text = edtMessageInput.getText().toString();
            if (!text.trim().isEmpty()) {
                workspaceViewModel.sendChatMessage(workspaceId, text);
                edtMessageInput.setText(""); // Xóa trắng ô nhập sau khi gửi thành công
            }
        });

        // Bắt lỗi nếu gửi xịt
        workspaceViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null) {
                ToastHelper.show(requireContext(), errorMsg);
            }
        });


        float density = getResources().getDisplayMetrics().density;

        view.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Tính toán độ chênh lệch chiều cao màn hình để biết bàn phím có bật hay không
                android.graphics.Rect r = new android.graphics.Rect();
                view.getWindowVisibleDisplayFrame(r);
                int screenHeight = view.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                android.view.ViewGroup.MarginLayoutParams inputParams =
                        (android.view.ViewGroup.MarginLayoutParams) layoutInput.getLayoutParams();

                View bottomNav = null;
                View fab = null;
                if (getActivity() != null) {
                    bottomNav = getActivity().findViewById(R.id.bottom_navigation_workspace);
                    fab = getActivity().findViewById(R.id.fabAddWorkspaceTransaction);
                }

                // Nếu độ chênh lệch > 15% màn hình -> Bàn phím đang mở
                if (keypadHeight > screenHeight * 0.15) {

                    if (bottomNav != null) bottomNav.setVisibility(View.GONE);
                    if (fab != null) fab.setVisibility(View.GONE);

                    // Chỉ cập nhật nếu margin hiện tại chưa phải là 16dp (tránh bị lặp vô tận)
                    if (inputParams.bottomMargin != (int) (16 * density)) {
                        inputParams.bottomMargin = (int) (16 * density);
                        layoutInput.setLayoutParams(inputParams);

                        rvChatMessages.setPadding(
                                rvChatMessages.getPaddingLeft(),
                                rvChatMessages.getPaddingTop(),
                                rvChatMessages.getPaddingRight(),
                                (int) (16 * density)
                        );

                        // Cuộn mượt xuống tin nhắn cuối cùng khi bàn phím mở
                        if (chatAdapter.getItemCount() > 0) {
                            rvChatMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                        }
                    }
                }
                // Ngược lại -> Bàn phím đang đóng
                else {
                    if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
                    if (fab != null) fab.setVisibility(View.VISIBLE);

                    if (inputParams.bottomMargin != (int) (110 * density)) {
                        inputParams.bottomMargin = (int) (110 * density);
                        layoutInput.setLayoutParams(inputParams);

                        rvChatMessages.setPadding(
                                rvChatMessages.getPaddingLeft(),
                                rvChatMessages.getPaddingTop(),
                                rvChatMessages.getPaddingRight(),
                                (int) (180 * density)
                        );
                    }
                }
            }
        });

    }


}