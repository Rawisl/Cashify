package com.example.cashify.ui.FriendsActivity;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cashify.R;
import com.example.cashify.ui.workspace.ChatAdapter;
import com.example.cashify.utils.CloudinaryHelper;
import com.example.cashify.utils.DialogHelper;
import com.example.cashify.utils.ImageHelper;
import com.example.cashify.utils.ToastHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FriendChatActivity extends AppCompatActivity {
    public static final String EXTRA_FRIEND_UID = "friend_uid";
    public static final String EXTRA_FRIEND_NAME = "friend_name";
    public static final String EXTRA_FRIEND_AVATAR = "friend_avatar";

    private final List<String> pendingImageUrls = new ArrayList<>();
    private ImageButton btnSendMessage;

    private ActivityResultLauncher<String> pickImageLauncher;

    private FriendChatViewModel viewModel;
    private ChatAdapter chatAdapter;
    private RecyclerView rvChatMessages;
    private EditText edtMessageInput;
    private String friendUid;

    private LinearLayout layoutImagePreview;
    private LinearLayout layoutPreviewImages;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_chat);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(), uris -> {
                    if (uris == null || uris.isEmpty()) return;
                    for (Uri uri : uris) {
                        long size = getFileSizeFromUri(uri);
                        if (size > 10 * 1024 * 1024) {
                            DialogHelper.showCustomDialog(this, "Image too large",
                                    "Please select an image under 10MB.", "Choose again", "Cancel",
                                    DialogHelper.DialogType.DANGER, true,
                                    () -> pickImageLauncher.launch("image/*"), null);
                            return;
                        }
                        uploadAndPreview(uri);
                    }
                });

        friendUid = getIntent().getStringExtra(EXTRA_FRIEND_UID);
        String friendName = getIntent().getStringExtra(EXTRA_FRIEND_NAME);
        String friendAvatar = getIntent().getStringExtra(EXTRA_FRIEND_AVATAR);

        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageView imgFriendAvatar = findViewById(R.id.imgFriendAvatar);
        TextView tvFriendName = findViewById(R.id.tvFriendName);
        rvChatMessages = findViewById(R.id.rvChatMessages);
        edtMessageInput = findViewById(R.id.edtMessageInput);
        btnSendMessage = findViewById(R.id.btnSendMessage);

        layoutImagePreview = findViewById(R.id.layoutImagePreview);
        layoutPreviewImages = findViewById(R.id.layoutPreviewImages);

        btnBack.setOnClickListener(v -> finish());
        tvFriendName.setText(friendName != null ? friendName : "Friend");
        ImageHelper.loadAvatar(friendAvatar, imgFriendAvatar, friendName);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(layoutManager);
//        rvChatMessages.setItemAnimator(null);
        chatAdapter = new ChatAdapter(new ArrayList<>(), null);
        rvChatMessages.setAdapter(chatAdapter);

        viewModel = new ViewModelProvider(this).get(FriendChatViewModel.class);
        viewModel.startListeningMessages(friendUid);
        viewModel.getChatMessages().observe(this, messages -> {
            chatAdapter.setMessages(messages);
            if (messages != null && !messages.isEmpty()) {
                rvChatMessages.scrollToPosition(messages.size() - 1);
            }
        });
        viewModel.getLoadErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                ToastHelper.show(this, "Load message failed");
                ToastHelper.show(this, message);
            }
        });
        viewModel.getSendErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                ToastHelper.show(this, "Send message failed");
                ToastHelper.show(this, message);
            }
        });

        btnSendMessage.setOnClickListener(v -> {
            String text = edtMessageInput.getText() != null
                    ? edtMessageInput.getText().toString() : "";
            if (text.trim().isEmpty() && pendingImageUrls.isEmpty()) return;

            List<String> urlsToSend = new ArrayList<>(pendingImageUrls);

            sendMessagesSequentially(friendUid, text, urlsToSend, 0);

            pendingImageUrls.clear();
            edtMessageInput.setText("");
            layoutImagePreview.setVisibility(View.GONE);
            layoutPreviewImages.removeAllViews();
        });

        findViewById(R.id.btnAttachImage).setOnClickListener(v -> {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(
                            new String[]{android.Manifest.permission.READ_MEDIA_IMAGES},
                            101);
                    return;
                }
            }
            pickImageLauncher.launch("image/*");
        });
    }

    private void uploadAndPreview(Uri uri) {
        btnSendMessage.setEnabled(false);
        File file = getFileFromUri(uri);
        if (file == null) { btnSendMessage.setEnabled(true); return; }

        CloudinaryHelper.uploadImage(file, new CloudinaryHelper.UploadCallback() {
            @Override public void onProgress(int percent) {}

            @Override
            public void onSuccess(String imageUrl) {
                pendingImageUrls.add(imageUrl);
                runOnUiThread(() -> {
                    btnSendMessage.setEnabled(true);
                    addImagePreview(imageUrl);
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    btnSendMessage.setEnabled(true);
                    DialogHelper.showAlert(FriendChatActivity.this,
                            "Tải ảnh thất bại", error, null);
                });
            }
        });
    }

    private long getFileSizeFromUri(Uri uri) {
        try (android.database.Cursor cursor = getContentResolver().query(
                uri, new String[]{android.provider.OpenableColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                if (idx != -1 && !cursor.isNull(idx)) return cursor.getLong(idx);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private File getFileFromUri(Uri uri) {
        try {
            // Lấy tên file gốc nếu có
            String fileName = "chat_img_" + System.currentTimeMillis() + ".jpg";
            try (android.database.Cursor cursor = getContentResolver().query(
                    uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME},
                    null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIdx != -1) fileName = cursor.getString(nameIdx);
                }
            }

            File outputFile = new File(getCacheDir(), "chat_" + System.currentTimeMillis() + "_" + fileName);

            try (java.io.InputStream is = getContentResolver().openInputStream(uri);
                 java.io.OutputStream os = new java.io.FileOutputStream(outputFile)) {
                if (is == null) return null;
                byte[] buffer = new byte[4096]; // 4KB buffer — nhanh hơn 1KB
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            return outputFile.length() > 0 ? outputFile : null;
        } catch (Exception e) {
            Log.e("CHAT_IMG", "getFileFromUri lỗi: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0
                && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            pickImageLauncher.launch("image/*");
        }
    }

    private void addImagePreview(String imageUrl) {
        layoutImagePreview.setVisibility(View.VISIBLE);

        // Container cho ảnh + nút X
        FrameLayout container = new FrameLayout(this);
        int size = (int) (64 * getResources().getDisplayMetrics().density);
        int margin = (int) (6 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMarginEnd(margin);
        container.setLayoutParams(params);

        // ImageView bo tròn
        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundResource(R.drawable.bg_image_preview);
        imageView.setClipToOutline(true);
        Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_camera).into(imageView);

        // Nút X nhỏ góc trên phải
        ImageButton btnRemove = new ImageButton(this);
        int btnSize = (int) (18 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(btnSize, btnSize);
        btnParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        btnRemove.setLayoutParams(btnParams);
        btnRemove.setImageResource(R.drawable.ic_arrow_left_back); // hoặc ic_close nếu có
        btnRemove.setBackground(null);
        btnRemove.setColorFilter(getResources().getColor(R.color.brand_primary, null));
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

    private void sendMessagesSequentially(String friendUid, String text,
                                          List<String> urls, int index) {
        if (index >= urls.size()) {
            if (urls.isEmpty()) {
                viewModel.sendMessage(friendUid, text, null);
            }
            return;
        }

        String imgUrl = urls.get(index);
        String msgText = (index == 0) ? text : "";

        viewModel.sendMessage(friendUid, msgText, imgUrl,
                () -> sendMessagesSequentially(friendUid, text, urls, index + 1));
    }
}
