package com.example.cashify.ui.feed;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.cashify.R;
import com.example.cashify.utils.ImageHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.Locale;

public class CommunityFeedFragment extends Fragment {
    private final Handler handler = new Handler(Looper.getMainLooper());

    private EditText editPostContent;
    private TextView txtComposerCount;
    private TextView txtComposerHint;
    private TextView btnAudience;
    private TextView actionMilestone;
    private TextView actionPhoto;
    private MaterialButton btnSubmitPost;
    private ProgressBar progressPosting;
    private FrameLayout imagePreviewContainer;
    private ImageView imgPostPreview;
    private ImageView imgComposerAvatar;
    private ChipGroup chipGroupTopics;

    private Uri selectedImageUri;
    private boolean milestoneMode;
    private String selectedAudience = "Bạn bè";
    private PopupWindow audiencePopup;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    return;
                }
                selectedImageUri = uri;
                imgPostPreview.setImageURI(uri);
                imagePreviewContainer.setVisibility(View.VISIBLE);
                txtComposerHint.setText("Đã thêm ảnh. Bạn có thể viết thêm mô tả hoặc đăng ngay.");
                updateSubmitState();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupToolbar(view);
        setupComposer(view);
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    private void bindViews(View view) {
        editPostContent = view.findViewById(R.id.editPostContent);
        txtComposerCount = view.findViewById(R.id.txtComposerCount);
        txtComposerHint = view.findViewById(R.id.txtComposerHint);
        btnAudience = view.findViewById(R.id.btnAudience);
        actionMilestone = view.findViewById(R.id.actionMilestone);
        actionPhoto = view.findViewById(R.id.actionPhoto);
        btnSubmitPost = view.findViewById(R.id.btnSubmitPost);
        progressPosting = view.findViewById(R.id.progressPosting);
        imagePreviewContainer = view.findViewById(R.id.imagePreviewContainer);
        imgPostPreview = view.findViewById(R.id.imgPostPreview);
        imgComposerAvatar = view.findViewById(R.id.imgComposerAvatar);
        chipGroupTopics = view.findViewById(R.id.chipGroupTopics);
    }

    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbarCreatePost);
        toolbar.setNavigationOnClickListener(v -> navigateBack());
    }

    private void setupComposer(View view) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        TextView txtComposerName = view.findViewById(R.id.txtComposerName);
        if (user != null) {
            txtComposerName.setText(cleanDisplayName(user.getDisplayName()));
            if (user.getPhotoUrl() != null) {
                ImageHelper.loadAvatar(user.getPhotoUrl(), imgComposerAvatar);
            }
            FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> bindCurrentUserProfile(doc, txtComposerName));
        }

        editPostContent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                txtComposerCount.setText(String.format(Locale.US, "%d/280", s.length()));
                updateSubmitState();
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        editPostContent.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                txtComposerHint.setText(milestoneMode
                        ? "Bài cột mốc sẽ hay hơn khi có một chiến thắng thật rõ."
                        : "Bắt đầu một câu chuyện tài chính nhỏ.");
            }
        });

        updateAudienceButton();
        btnAudience.setOnClickListener(this::showAudienceMenu);
        actionPhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        actionMilestone.setOnClickListener(v -> setMilestoneMode(!milestoneMode));
        view.findViewById(R.id.btnRemoveImage).setOnClickListener(v -> clearSelectedImage());
        btnSubmitPost.setOnClickListener(v -> submitPost());
        updateSubmitState();
    }

    private void showAudienceMenu(View anchor) {
        if (audiencePopup != null && audiencePopup.isShowing()) {
            audiencePopup.dismiss();
            return;
        }

        LinearLayout menu = new LinearLayout(requireContext());
        menu.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(8);
        menu.setPadding(padding, padding, padding, padding);
        menu.setBackgroundResource(R.drawable.bg_privacy_menu);

        addAudienceMenuItem(menu, "Công khai", R.drawable.ic_privacy_public);
        addAudienceMenuItem(menu, "Bạn bè", R.drawable.ic_friends);
        addAudienceMenuItem(menu, "Chỉ mình tôi", R.drawable.ic_privacy_lock);

        audiencePopup = new PopupWindow(
                menu,
                Math.max(anchor.getWidth(), dp(190)),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        audiencePopup.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        audiencePopup.setOutsideTouchable(true);
        audiencePopup.setElevation(dp(6));
        audiencePopup.showAsDropDown(anchor, 0, dp(6));
    }

    private void addAudienceMenuItem(LinearLayout menu, String label, int iconRes) {
        TextView item = new TextView(requireContext());
        item.setText(label);
        item.setTextSize(14);
        item.setTextColor(ContextCompat.getColor(requireContext(), R.color.item_title));
        item.setGravity(android.view.Gravity.CENTER_VERTICAL);
        item.setMinHeight(dp(46));
        item.setPadding(dp(12), 0, dp(12), 0);
        item.setCompoundDrawablePadding(dp(10));
        item.setCompoundDrawablesRelativeWithIntrinsicBounds(tintedDrawable(iconRes), null, null, null);
        if (label.equals(selectedAudience)) {
            item.setBackgroundResource(R.drawable.bg_privacy_menu_item_selected);
            item.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_primary));
            item.setTypeface(item.getTypeface(), android.graphics.Typeface.BOLD);
        }
        item.setOnClickListener(v -> {
            selectedAudience = label;
            updateAudienceButton();
            if (audiencePopup != null) {
                audiencePopup.dismiss();
            }
        });
        menu.addView(item, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
    }

    private void updateAudienceButton() {
        btnAudience.setText(selectedAudience);
        btnAudience.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_primary));
        btnAudience.setCompoundDrawablePadding(dp(8));
        btnAudience.setCompoundDrawablesRelativeWithIntrinsicBounds(
                tintedDrawable(iconForAudience(selectedAudience)),
                null,
                tintedDrawable(R.drawable.ic_angle_down_regular),
                null
        );
        btnAudience.setCompoundDrawableTintList(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.brand_primary)));
        btnAudience.setContentDescription("Quyền riêng tư: " + selectedAudience);
    }

    private int iconForAudience(String audience) {
        if ("Công khai".equals(audience)) {
            return R.drawable.ic_privacy_public;
        }
        if ("Chỉ mình tôi".equals(audience)) {
            return R.drawable.ic_privacy_lock;
        }
        return R.drawable.ic_friends;
    }

    private Drawable tintedDrawable(int iconRes) {
        Drawable drawable = ContextCompat.getDrawable(requireContext(), iconRes);
        if (drawable == null) {
            return null;
        }
        Drawable wrapped = DrawableCompat.wrap(drawable.mutate());
        DrawableCompat.setTint(wrapped, ContextCompat.getColor(requireContext(), R.color.brand_primary));
        return wrapped;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void setMilestoneMode(boolean enabled) {
        milestoneMode = enabled;
        actionMilestone.setBackgroundResource(enabled
                ? R.drawable.bg_composer_action_selected
                : R.drawable.bg_action_button);
        txtComposerHint.setText(enabled
                ? "Chế độ cột mốc: chia sẻ mục tiêu, chuỗi ngày tốt hoặc một chiến thắng nhỏ."
                : "Bắt đầu một câu chuyện tài chính nhỏ.");
        editPostContent.setHint(enabled
                ? "Bạn vừa đạt cột mốc nào?"
                : "Bạn muốn chia sẻ chuyện tiền bạc gì hôm nay?");
        updateSubmitState();
    }

    private void clearSelectedImage() {
        selectedImageUri = null;
        imgPostPreview.setImageDrawable(null);
        imagePreviewContainer.setVisibility(View.GONE);
        txtComposerHint.setText("Đã xoá ảnh.");
        updateSubmitState();
    }

    // =========================================================================
    // XỬ LÝ ĐĂNG BÀI: TÍCH HỢP CLOUDINARY & C# BACKEND
    // =========================================================================
    private void submitPost() {
        String content = editPostContent.getText().toString().trim();

        if (content.isEmpty() && selectedImageUri == null) {
            Toast.makeText(requireContext(), "Hãy viết nội dung hoặc thêm ảnh trước nhé.", Toast.LENGTH_SHORT).show();
            editPostContent.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editPostContent, InputMethodManager.SHOW_IMPLICIT);
            }
            return;
        }

        setPosting(true);
        String type = milestoneMode ? "MILESTONE_POST" : "USER_POST";
        String milestoneData = null; // TODO: Khang ráp chuỗi JSON của Auto-Milestone vào đây sau

        if (selectedImageUri != null) {
            // TRƯỜNG HỢP CÓ ẢNH: Đẩy qua Cloudinary trước
            txtComposerHint.setText("Đang tải ảnh lên máy chủ...");
            File imageFile = getFileFromUri(selectedImageUri);

            if (imageFile == null) {
                setPosting(false);
                Toast.makeText(requireContext(), "Lỗi đọc file ảnh!", Toast.LENGTH_SHORT).show();
                return;
            }

            com.example.cashify.utils.CloudinaryHelper.uploadImage(imageFile, new com.example.cashify.utils.CloudinaryHelper.UploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    // Up ảnh xong, lấy URL gọi Backend
                    callBackendToCreatePost(content, type, imageUrl, milestoneData);
                }

                @Override
                public void onFailure(String error) {
                    requireActivity().runOnUiThread(() -> {
                        setPosting(false);
                        Toast.makeText(requireContext(), "Lỗi tải ảnh: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            // TRƯỜNG HỢP KHÔNG ẢNH: Đẩy thẳng lên Backend
            callBackendToCreatePost(content, type, "", milestoneData);
        }
    }

    // Hàm gọi C# API để lưu Post vào Firestore
    private void callBackendToCreatePost(String content, String type, String imageUrl, String milestoneData) {
        requireActivity().runOnUiThread(() -> txtComposerHint.setText("Đang lưu bài viết..."));

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            requireActivity().runOnUiThread(() -> {
                setPosting(false);
                Toast.makeText(requireContext(), "Chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        user.getIdToken(true).addOnSuccessListener(getTokenResult -> {
            String token = "Bearer " + getTokenResult.getToken();
            com.example.cashify.utils.ApiService apiService = com.example.cashify.utils.ApiClient.getClient().create(com.example.cashify.utils.ApiService.class);

            // Khởi tạo Request Model (Nhớ đảm bảo ApiService đã có class này)
            com.example.cashify.utils.ApiService.CreatePostRequest request =
                    new com.example.cashify.utils.ApiService.CreatePostRequest(content, type, imageUrl, milestoneData);

            apiService.createPost(token, request).enqueue(new retrofit2.Callback<Object>() {
                @Override
                public void onResponse(@NonNull retrofit2.Call<Object> call, @NonNull retrofit2.Response<Object> response) {
                    requireActivity().runOnUiThread(() -> {
                        setPosting(false);
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                            resetComposer();
                            navigateBack(); // Quay lại trang Feed
                        } else {
                            Toast.makeText(requireContext(), "Lỗi tạo bài: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onFailure(@NonNull retrofit2.Call<Object> call, @NonNull Throwable t) {
                    requireActivity().runOnUiThread(() -> {
                        setPosting(false);
                        Toast.makeText(requireContext(), "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }).addOnFailureListener(e -> {
            requireActivity().runOnUiThread(() -> {
                setPosting(false);
                Toast.makeText(requireContext(), "Lỗi xác thực Firebase", Toast.LENGTH_SHORT).show();
            });
        });
    }

    // Hàm phụ trợ: Chuyển Uri của Android thành File vật lý để OkHttp (CloudinaryHelper) đọc được
    private File getFileFromUri(Uri uri) {
        try {
            java.io.InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File tempFile = new File(requireContext().getCacheDir(), "upload_img_" + System.currentTimeMillis() + ".jpg");
            java.io.OutputStream outputStream = new java.io.FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getSelectedTopic() {
        int checkedId = chipGroupTopics.getCheckedChipId();
        if (checkedId == View.NO_ID) {
            return "Ngân sách";
        }
        Chip chip = chipGroupTopics.findViewById(checkedId);
        return chip == null ? "Ngân sách" : chip.getText().toString();
    }

    private void resetComposer() {
        editPostContent.setText("");
        clearSelectedImage();
        if (milestoneMode) {
            setMilestoneMode(false);
        }
        chipGroupTopics.check(R.id.chipBudgeting);
        txtComposerHint.setText("Sẵn sàng cho bài chia sẻ tiếp theo.");
    }

    private void setPosting(boolean posting) {
        progressPosting.setVisibility(posting ? View.VISIBLE : View.GONE);
        btnSubmitPost.setEnabled(!posting);
        actionPhoto.setEnabled(!posting);
        actionMilestone.setEnabled(!posting);
        btnAudience.setEnabled(!posting);
        editPostContent.setEnabled(!posting);
        btnSubmitPost.setText(posting ? "Đang đăng..." : "Đăng");
    }

    private void updateSubmitState() {
        boolean hasText = editPostContent != null && editPostContent.getText().toString().trim().length() > 0;
        boolean hasImage = selectedImageUri != null;
        boolean canSubmit = hasText;
        btnSubmitPost.setEnabled(canSubmit);
        btnSubmitPost.setAlpha(canSubmit ? 1f : 0.55f);
        int count = editPostContent == null ? 0 : editPostContent.length();
        int color = count > 250 ? R.color.status_red : R.color.item_description;
        txtComposerCount.setTextColor(ContextCompat.getColor(requireContext(), color));
    }

    private void navigateBack() {
        NavController navController = NavHostFragment.findNavController(this);
        if (!navController.popBackStack()) {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        }
    }

    private void bindCurrentUserProfile(DocumentSnapshot doc, TextView txtComposerName) {
        if (doc == null || !doc.exists()) {
            return;
        }
        String displayName = cleanDisplayName(doc.getString("displayName"));
        String username = cleanDisplayName(doc.getString("username"));
        txtComposerName.setText(!displayName.equals("Người dùng Cashify") ? displayName : username);

        String avatarUrl = doc.getString("avatarUrl");
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            ImageHelper.loadAvatar(avatarUrl, imgComposerAvatar);
        }
    }

    private String cleanDisplayName(String value) {
        if (value == null || value.trim().isEmpty() || value.contains("@")) {
            return "Người dùng Cashify";
        }
        return value.trim();
    }
}
