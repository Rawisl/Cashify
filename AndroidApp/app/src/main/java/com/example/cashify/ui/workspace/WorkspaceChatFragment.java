package com.example.cashify.ui.workspace;

import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cashify.R;
import com.example.cashify.utils.DialogHelper;
import com.example.cashify.utils.FileUtils;
import com.example.cashify.utils.ToastHelper;

import java.io.File;
import java.util.ArrayList;

public class WorkspaceChatFragment extends Fragment {

    private String workspaceId;
    private WorkspaceViewModel workspaceViewModel;

    // UI Components
    private ChatAdapter chatAdapter;
    private RecyclerView rvChatMessages;
    private EditText edtMessageInput;
    private ImageButton btnSendMessage;
    private LinearLayout layoutImagePreview;
    private LinearLayout layoutPreviewImages;
    private View layoutInput;

    private ActivityResultLauncher<String> pickImageLauncher;

    public WorkspaceChatFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(), uris -> {
                    if (uris == null || uris.isEmpty()) return;
                    for (Uri uri : uris) {
                        long size = FileUtils.getFileSizeFromUri(requireContext(), uri);
                        if (size > 10 * 1024 * 1024) {
                            DialogHelper.showCustomDialog(requireContext(), "Image too large",
                                    "Please select an image under 10MB.", "Choose again", "Cancel",
                                    DialogHelper.DialogType.DANGER, true,
                                    () -> pickImageLauncher.launch("image/*"), null);
                            return;
                        }
                        processImageUpload(uri);
                    }
                });
        return inflater.inflate(R.layout.fragment_workspace_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Retrieve workspaceId from the parent container
        Fragment parent = getParentFragment();
        while (parent != null) {
            if (parent instanceof WorkspaceContainerFragment && parent.getArguments() != null) {
                workspaceId = parent.getArguments().getString("WORKSPACE_ID");
                break;
            }
            parent = parent.getParentFragment();
        }

        // Initialize ViewModel using the parent activity scope
        workspaceViewModel = new ViewModelProvider(requireActivity()).get(WorkspaceViewModel.class);

        initViews(view);
        setupRecyclerView();
        setupObservers();
        setupListeners();
        setupKeyboardListener(view);

        workspaceViewModel.listenForChatMessages(workspaceId);
    }

    private void initViews(View view) {
        rvChatMessages = view.findViewById(R.id.rvChatMessages);
        edtMessageInput = view.findViewById(R.id.edtMessageInput);
        btnSendMessage = view.findViewById(R.id.btnSendMessage);
        layoutInput = view.findViewById(R.id.layoutInput);
        layoutImagePreview = view.findViewById(R.id.layoutImagePreview);
        layoutPreviewImages = view.findViewById(R.id.layoutPreviewImages);

        ImageButton btnAttachImage = view.findViewById(R.id.btnAttachImage);
        btnAttachImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        rvChatMessages.setLayoutManager(layoutManager);

        chatAdapter = new ChatAdapter(new ArrayList<>(), message ->
                DialogHelper.showCustomDialog(requireContext(), "Recall Message",
                        "Are you sure you want to recall this message?", "Recall", "Cancel",
                        DialogHelper.DialogType.DANGER, true,
                        () -> workspaceViewModel.deleteChatMessage(workspaceId, message.getMessageId()), null)
        );
        rvChatMessages.setAdapter(chatAdapter);
    }

    private void setupObservers() {
        TextView tvWorkspaceName = getView().findViewById(R.id.tvWorkspaceName);
        ImageView imgWorkspaceIcon = getView().findViewById(R.id.imgWorkspaceIcon);

        workspaceViewModel.getWorkspaceLiveData().observe(getViewLifecycleOwner(), workspace -> {
            if (workspace != null) {
                if (tvWorkspaceName != null) {
                    tvWorkspaceName.setText(workspace.getName());
                }
                if (imgWorkspaceIcon != null) {
                    int resId = requireContext().getResources().getIdentifier(workspace.getIconName(), "drawable", requireContext().getPackageName());
                    imgWorkspaceIcon.setImageResource(resId != 0 ? resId : R.drawable.ic_other);
                }

                if (chatAdapter != null) chatAdapter.setOwnerId(workspace.getOwnerId());
            }
        });

        workspaceViewModel.getChatMessages().observe(getViewLifecycleOwner(), messages -> {
            if (messages != null) {
                chatAdapter.setMessages(messages);
                if (!messages.isEmpty()) {
                    rvChatMessages.scrollToPosition(messages.size() - 1);
                }
            }
        });

        workspaceViewModel.errorMessage.observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null) ToastHelper.show(requireContext(), errorMsg);
        });

        workspaceViewModel.getIsUploading().observe(getViewLifecycleOwner(), isUploading -> {
            btnSendMessage.setEnabled(!isUploading);
            btnSendMessage.setAlpha(isUploading ? 0.5f : 1.0f);
        });

        workspaceViewModel.getPendingImages().observe(getViewLifecycleOwner(), images -> {
            layoutPreviewImages.removeAllViews();
            if (images == null || images.isEmpty()) {
                layoutImagePreview.setVisibility(View.GONE);
            } else {
                layoutImagePreview.setVisibility(View.VISIBLE);
                for (String url : images) renderImagePreview(url);
            }
        });
    }

    private void setupListeners() {
        btnSendMessage.setOnClickListener(v -> {
            String text = edtMessageInput.getText() != null ? edtMessageInput.getText().toString().trim() : "";
            boolean hasStartedSending = workspaceViewModel.submitChatMessages(workspaceId, text);
            if (hasStartedSending) {
                edtMessageInput.setText("");
            }
        });
    }

    private void processImageUpload(Uri uri) {
        File file = FileUtils.getFileFromUri(requireContext(), uri);
        if (file == null) {
            ToastHelper.show(requireContext(), "Failed to process image file.");
            return;
        }
        workspaceViewModel.uploadChatImage(file);
    }

    private void renderImagePreview(String imageUrl) {
        FrameLayout container = new FrameLayout(requireContext());
        int size = (int) (64 * getResources().getDisplayMetrics().density);
        int margin = (int) (6 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMarginEnd(margin);
        container.setLayoutParams(params);

        ImageView imageView = new ImageView(requireContext());
        imageView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundResource(R.drawable.bg_image_preview);
        imageView.setClipToOutline(true);
        Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_camera).into(imageView);

        ImageButton btnRemove = new ImageButton(requireContext());
        int btnSize = (int) (18 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(btnSize, btnSize);
        btnParams.gravity = Gravity.TOP | Gravity.END;
        btnRemove.setLayoutParams(btnParams);
        btnRemove.setImageResource(R.drawable.ic_arrow_left_back);
        btnRemove.setBackground(null);
        btnRemove.setColorFilter(ContextCompat.getColor(requireContext(), R.color.brand_primary));
        btnRemove.setOnClickListener(v -> workspaceViewModel.removePendingImage(imageUrl));

        container.addView(imageView);
        container.addView(btnRemove);
        layoutPreviewImages.addView(container);
    }

    private void setupKeyboardListener(View view) {
        float density = getResources().getDisplayMetrics().density;
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                android.graphics.Rect r = new android.graphics.Rect();
                view.getWindowVisibleDisplayFrame(r);
                int screenHeight = view.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                ViewGroup.MarginLayoutParams inputParams = (ViewGroup.MarginLayoutParams) layoutInput.getLayoutParams();
                View bottomNav = getActivity() != null ? getActivity().findViewById(R.id.bottom_navigation_workspace) : null;

                if (keypadHeight > screenHeight * 0.15) {
                    if (bottomNav != null) bottomNav.setVisibility(View.GONE);

                    if (inputParams.bottomMargin != (int) (16 * density)) {
                        inputParams.bottomMargin = (int) (16 * density);
                        layoutInput.setLayoutParams(inputParams);
                        rvChatMessages.setPadding(rvChatMessages.getPaddingLeft(), rvChatMessages.getPaddingTop(), rvChatMessages.getPaddingRight(), (int) (16 * density));
                        if (chatAdapter.getItemCount() > 0) rvChatMessages.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
                    }
                } else {
                    if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);

                    if (inputParams.bottomMargin != (int) (110 * density)) {
                        inputParams.bottomMargin = (int) (110 * density);
                        layoutInput.setLayoutParams(inputParams);
                        rvChatMessages.setPadding(rvChatMessages.getPaddingLeft(), rvChatMessages.getPaddingTop(), rvChatMessages.getPaddingRight(), (int) (180 * density));
                    }
                }
            }
        });
    }
}