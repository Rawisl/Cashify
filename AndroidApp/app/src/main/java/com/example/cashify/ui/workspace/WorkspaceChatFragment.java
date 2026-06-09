package com.example.cashify.ui.workspace;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.utils.ToastHelper;

import java.util.ArrayList;
import java.util.List;

public class WorkspaceChatFragment extends Fragment {


    // === CÁC BIẾN MỚI THÊM CHO TÍNH NĂNG CHAT ===
    private String workspaceId;
    private WorkspaceViewModel workspaceViewModel;
    private ChatAdapter chatAdapter;
    private RecyclerView rvChatMessages;
    private EditText edtMessageInput;
    private ImageButton btnSendMessage;

    private final List<String> pendingImageUrls = new ArrayList<>();
    private ActivityResultLauncher<String> pickImageLauncher;

    private LinearLayout layoutImagePreview;
    private LinearLayout layoutPreviewImages;

    public WorkspaceChatFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(), uris -> {
                    if (uris == null || uris.isEmpty()) return;
                    for (Uri uri : uris) {
                        long size = getFileSizeFromUri(uri);
                        if (size > 10 * 1024 * 1024) {
                            com.example.cashify.utils.DialogHelper.showCustomDialog(
                                    requireContext(), "Ảnh quá lớn",
                                    "Vui lòng chọn ảnh dưới 10MB.", "Chọn lại", "Huỷ",
                                    com.example.cashify.utils.DialogHelper.DialogType.DANGER, true,
                                    () -> pickImageLauncher.launch("image/*"), null);
                            return;
                        }
                        uploadAndPreview(uri);
                    }
                });
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

        ImageButton btnAttachImage = view.findViewById(R.id.btnAttachImage);
        btnAttachImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        layoutImagePreview = view.findViewById(R.id.layoutImagePreview);
        layoutPreviewImages = view.findViewById(R.id.layoutPreviewImages);

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true); // Để tin nhắn đẩy từ dưới lên
        rvChatMessages.setLayoutManager(layoutManager);
//        rvChatMessages.setItemAnimator(null);

        // Khởi tạo Adapter kèm theo sự kiện nhấn giữ tin nhắn
        chatAdapter = new ChatAdapter(new ArrayList<>(), message -> {
            com.example.cashify.utils.DialogHelper.showCustomDialog(
                    requireContext(), "Recall Message", "Are you sure you want to recall this message?", "Recall", "Cancel", com.example.cashify.utils.DialogHelper.DialogType.DANGER, true,
                    () -> {
                        // GỌI QUA VIEWMODEL (CHUẨN MVVM)
                        workspaceViewModel.deleteChatMessage(workspaceId, message.getMessageId());
                    }, null
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
                // PHÉP THUẬT GOD MODE REAL-TIME
                if (chatAdapter != null) chatAdapter.setOwnerId(workspace.getOwnerId());
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
                    rvChatMessages.scrollToPosition(messages.size() - 1);
                }
            }
        });

        // Xử lý nút Gửi
        btnSendMessage.setOnClickListener(v -> {
            String text = edtMessageInput.getText().toString();
            if (text.trim().isEmpty() && pendingImageUrls.isEmpty()) return;

            // Copy list ra để tránh bị clear trước khi gửi xong
            List<String> urlsToSend = new ArrayList<>(pendingImageUrls);

            sendMessagesSequentially(workspaceId, text, urlsToSend, 0);

            pendingImageUrls.clear();
            edtMessageInput.setText("");
            layoutImagePreview.setVisibility(View.GONE);
            layoutPreviewImages.removeAllViews();
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

    private void uploadAndPreview(Uri uri) {
        btnSendMessage.setEnabled(false);
        java.io.File file = getFileFromUri(uri);
        if (file == null) { btnSendMessage.setEnabled(true); return; }

        com.example.cashify.utils.CloudinaryHelper.uploadImage(file,
                new com.example.cashify.utils.CloudinaryHelper.UploadCallback() {
                    @Override public void onProgress(int percent) {}

                    @Override
                    public void onSuccess(String imageUrl) {
                        pendingImageUrls.add(imageUrl);
                        if (getActivity() != null)
                            getActivity().runOnUiThread(() -> {
                                btnSendMessage.setEnabled(true);
                                addImagePreview(imageUrl); // ← gọi hàm preview mới
                            });
                    }

                    @Override
                    public void onFailure(String error) {
                        if (getActivity() != null)
                            getActivity().runOnUiThread(() -> {
                                btnSendMessage.setEnabled(true);
                                ToastHelper.show(requireContext(), "Tải ảnh thất bại: " + error);
                            });
                    }
                });
    }

    private void addImagePreview(String imageUrl) {
        layoutImagePreview.setVisibility(View.VISIBLE);

        android.widget.FrameLayout container = new android.widget.FrameLayout(requireContext());
        int size = (int) (64 * getResources().getDisplayMetrics().density);
        int margin = (int) (6 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMarginEnd(margin);
        container.setLayoutParams(params);

        ImageView imageView = new ImageView(requireContext());
        imageView.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundResource(R.drawable.bg_image_preview);
        imageView.setClipToOutline(true);
        com.bumptech.glide.Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_camera)
                .into(imageView);

        ImageButton btnRemove = new ImageButton(requireContext());
        int btnSize = (int) (18 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout.LayoutParams btnParams =
                new android.widget.FrameLayout.LayoutParams(btnSize, btnSize);
        btnParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        btnRemove.setLayoutParams(btnParams);
        btnRemove.setImageResource(R.drawable.ic_arrow_left_back);
        btnRemove.setBackground(null);
        btnRemove.setColorFilter(
                requireContext().getResources().getColor(R.color.brand_primary, null));
        btnRemove.setOnClickListener(v -> {
            pendingImageUrls.remove(imageUrl);
            layoutPreviewImages.removeView(container);
            if (pendingImageUrls.isEmpty()) {
                layoutImagePreview.setVisibility(View.GONE);
            }
        });

        container.addView(imageView);
        container.addView(btnRemove);
        layoutPreviewImages.addView(container);
    }

    private long getFileSizeFromUri(Uri uri) {
        try (android.database.Cursor cursor = requireContext().getContentResolver().query(
                uri, new String[]{android.provider.OpenableColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                if (idx != -1 && !cursor.isNull(idx)) return cursor.getLong(idx);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private java.io.File getFileFromUri(Uri uri) {
        try {
            String fileName = "ws_img_" + System.currentTimeMillis() + ".jpg";
            try (android.database.Cursor cursor = requireContext().getContentResolver().query(
                    uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME},
                    null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIdx != -1) fileName = cursor.getString(nameIdx);
                }
            }

            java.io.File outputFile = new java.io.File(
                    requireContext().getCacheDir(),
                    "ws_" + System.currentTimeMillis() + "_" + fileName);

            try (java.io.InputStream is = requireContext().getContentResolver().openInputStream(uri);
                 java.io.OutputStream os = new java.io.FileOutputStream(outputFile)) {
                if (is == null) return null;
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            return outputFile.length() > 0 ? outputFile : null;
        } catch (Exception e) {
            Log.e("WS_IMG", "getFileFromUri lỗi: " + e.getMessage(), e);
            return null;
        }
    }

    private void sendMessagesSequentially(String workspaceId, String text,
                                          List<String> urls, int index) {
        if (index >= urls.size()) {
            // Hết ảnh rồi, nếu không có ảnh nào thì gửi text thuần
            if (urls.isEmpty()) {
                workspaceViewModel.sendChatMessage(workspaceId, text, null);
            }
            return;
        }

        String imgUrl = urls.get(index);
        String msgText = (index == 0) ? text : ""; // text chỉ đính kèm ảnh đầu

        workspaceViewModel.sendChatMessage(workspaceId, msgText, imgUrl,
                () -> sendMessagesSequentially(workspaceId, text, urls, index + 1) // callback khi xong
        );
    }
}