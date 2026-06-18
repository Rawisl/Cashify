package com.example.cashify.ui.FriendsActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
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
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cashify.R;
import com.example.cashify.ui.workspace.ChatAdapter;
import com.example.cashify.utils.DialogHelper;
import com.example.cashify.utils.FileUtils;
import com.example.cashify.utils.ImageHelper;
import com.example.cashify.utils.ToastHelper;

import java.io.File;
import java.util.ArrayList;

public class FriendChatActivity extends AppCompatActivity {
    public static final String EXTRA_FRIEND_UID = "friend_uid";
    public static final String EXTRA_FRIEND_NAME = "friend_name";
    public static final String EXTRA_FRIEND_AVATAR = "friend_avatar";
    private static final int PERMISSION_REQUEST_CODE = 101;

    private FriendChatViewModel viewModel;
    private ChatAdapter chatAdapter;
    private RecyclerView rvChatMessages;
    private EditText edtMessageInput;
    private ImageButton btnSendMessage;
    private LinearLayout layoutImagePreview;
    private LinearLayout layoutPreviewImages;

    private String friendUid;
    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_chat);

        friendUid = getIntent().getStringExtra(EXTRA_FRIEND_UID);
        String friendName = getIntent().getStringExtra(EXTRA_FRIEND_NAME);
        String friendAvatar = getIntent().getStringExtra(EXTRA_FRIEND_AVATAR);

        viewModel = new ViewModelProvider(this).get(FriendChatViewModel.class);

        initViews();
        setupToolbar(friendName, friendAvatar);
        setupRecyclerView();
        setupImagePicker();
        setupObservers();
        setupListeners();

        viewModel.startListeningMessages(friendUid);
    }

    private void initViews() {
        rvChatMessages = findViewById(R.id.rvChatMessages);
        edtMessageInput = findViewById(R.id.edtMessageInput);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        layoutImagePreview = findViewById(R.id.layoutImagePreview);
        layoutPreviewImages = findViewById(R.id.layoutPreviewImages);
    }

    private void setupToolbar(String friendName, String friendAvatar) {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvFriendName)).setText(friendName != null ? friendName : "Friend");
        ImageHelper.loadAvatar(friendAvatar, findViewById(R.id.imgFriendAvatar), friendName);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(layoutManager);

        chatAdapter = new ChatAdapter(new ArrayList<>(), message ->
                DialogHelper.showCustomDialog(this, "Recall Message",
                        "Are you sure you want to recall this message?",
                        "Recall", "Cancel", DialogHelper.DialogType.DANGER, true,
                        () -> viewModel.recallMessage(friendUid, message.getMessageId()), null)
        );
        rvChatMessages.setAdapter(chatAdapter);
    }

    private void setupImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(), uris -> {
                    if (uris == null || uris.isEmpty()) return;
                    for (Uri uri : uris) {
                        long size = FileUtils.getFileSizeFromUri(this, uri);
                        if (size > 10 * 1024 * 1024) {
                            DialogHelper.showCustomDialog(this, "Image too large",
                                    "Please select an image under 10MB.", "Choose again", "Cancel",
                                    DialogHelper.DialogType.DANGER, true,
                                    () -> pickImageLauncher.launch("image/*"), null);
                            return;
                        }
                        processImageUpload(uri);
                    }
                });
    }

    private void setupObservers() {
        viewModel.getChatMessages().observe(this, messages -> {
            chatAdapter.setMessages(messages);
            if (messages != null && !messages.isEmpty()) {
                rvChatMessages.scrollToPosition(messages.size() - 1);
            }
        });

        viewModel.getLoadErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                ToastHelper.show(this, "Failed to load messages: " + message);
            }
        });

        viewModel.getSendErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                ToastHelper.show(this, "Failed to send message: " + message);
            }
        });

        // Delegate UI blocking to ViewModel state
        viewModel.getIsUploading().observe(this, isUploading -> {
            btnSendMessage.setEnabled(!isUploading);
            btnSendMessage.setAlpha(isUploading ? 0.5f : 1.0f);
        });

        // Auto-render previews driven by ViewModel state
        viewModel.getPendingImages().observe(this, images -> {
            layoutPreviewImages.removeAllViews();
            if (images == null || images.isEmpty()) {
                layoutImagePreview.setVisibility(View.GONE);
            } else {
                layoutImagePreview.setVisibility(View.VISIBLE);
                for (String url : images) {
                    renderImagePreview(url);
                }
            }
        });
    }

    private void setupListeners() {
        btnSendMessage.setOnClickListener(v -> {
            String text = edtMessageInput.getText() != null ? edtMessageInput.getText().toString().trim() : "";

            // ViewModel handles the check if text/images are empty
            boolean hasStartedSending = viewModel.submitMessages(friendUid, text);
            if (hasStartedSending) {
                edtMessageInput.setText("");
            }
        });

        findViewById(R.id.btnAttachImage).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_CODE);
                    return;
                }
            }
            pickImageLauncher.launch("image/*");
        });
    }

    private void processImageUpload(Uri uri) {
        File file = FileUtils.getFileFromUri(this, uri);
        if (file == null) {
            ToastHelper.show(this, "Failed to process image file.");
            return;
        }
        viewModel.uploadChatImage(file);
    }

    private void renderImagePreview(String imageUrl) {
        FrameLayout container = new FrameLayout(this);
        int size = (int) (64 * getResources().getDisplayMetrics().density);
        int margin = (int) (6 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMarginEnd(margin);
        container.setLayoutParams(params);

        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundResource(R.drawable.bg_image_preview);
        imageView.setClipToOutline(true);
        Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_camera).into(imageView);

        ImageButton btnRemove = new ImageButton(this);
        int btnSize = (int) (18 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(btnSize, btnSize);
        btnParams.gravity = Gravity.TOP | Gravity.END;
        btnRemove.setLayoutParams(btnParams);
        btnRemove.setImageResource(R.drawable.ic_arrow_left_back);
        btnRemove.setBackground(null);
        btnRemove.setColorFilter(ContextCompat.getColor(this, R.color.brand_primary));
        btnRemove.setOnClickListener(v -> viewModel.removePendingImage(imageUrl));

        container.addView(imageView);
        container.addView(btnRemove);
        layoutPreviewImages.addView(container);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickImageLauncher.launch("image/*");
        }
    }
}