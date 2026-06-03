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
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
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
import com.example.cashify.utils.UploadNotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommunityFeedFragment extends Fragment {
    private final Handler handler = new Handler(Looper.getMainLooper());

    private EditText editPostContent;
    private TextView txtComposerCount;
    private TextView txtComposerHint;
    private TextView btnAudience;
    private TextView btnAudienceFriends;
    private TextView btnAudiencePrivate;
    private TextView actionMilestone;
    private TextView actionThoughts;
    private TextView actionAnalysis;
    private TextView actionShare;
    private TextView actionPhoto;
    private LinearLayout panelCategoryMode;
    private ImageView imgModeIcon;
    private TextView txtModeKicker;
    private TextView txtModeTitle;
    private TextView txtModeDescription;
    private TextView txtModePrompt;
    private MaterialButton btnSubmitPost;
    private ProgressBar progressPosting;
    private FrameLayout imagePreviewContainer;
    private ImageView imgPostPreview;
    private ImageView imgComposerAvatar;
    private ChipGroup chipGroupTopics;

    private Uri selectedImageUri;
    private boolean milestoneMode;
    private String selectedAudience = "Công khai";
    private String selectedCategory = "Suy nghĩ";
    private String selectedCategoryKey = "thoughts";
    private final Set<String> selectedTopicHashtags = new LinkedHashSet<>();
    private boolean applyingHashtagStyle = false;
    private PopupWindow audiencePopup;

    private View milestonePreviewContainer;
    private TextView tvPreviewIcon, tvPreviewTitle, tvPreviewMonth, tvPreviewAmount;
    private ProgressBar pbPreviewProgress;
    private String generatedMilestoneJson = null; // Cục JSON để dành lúc bấm Đăng

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
        btnAudienceFriends = view.findViewById(R.id.btnAudienceFriends);
        btnAudiencePrivate = view.findViewById(R.id.btnAudiencePrivate);
        actionMilestone = view.findViewById(R.id.actionMilestone);
        actionThoughts = view.findViewById(R.id.actionThoughts);
        actionAnalysis = view.findViewById(R.id.actionAnalysis);
        actionShare = view.findViewById(R.id.actionShare);
        actionPhoto = view.findViewById(R.id.actionPhoto);
        panelCategoryMode = view.findViewById(R.id.panelCategoryMode);
        imgModeIcon = view.findViewById(R.id.imgModeIcon);
        txtModeKicker = view.findViewById(R.id.txtModeKicker);
        txtModeTitle = view.findViewById(R.id.txtModeTitle);
        txtModeDescription = view.findViewById(R.id.txtModeDescription);
        txtModePrompt = view.findViewById(R.id.txtModePrompt);
        btnSubmitPost = view.findViewById(R.id.btnSubmitPost);
        progressPosting = view.findViewById(R.id.progressPosting);
        imagePreviewContainer = view.findViewById(R.id.imagePreviewContainer);
        imgPostPreview = view.findViewById(R.id.imgPostPreview);
        imgComposerAvatar = view.findViewById(R.id.imgComposerAvatar);
        chipGroupTopics = view.findViewById(R.id.chipGroupTopics);
        milestonePreviewContainer = view.findViewById(R.id.milestonePreviewContainer);
        tvPreviewIcon = view.findViewById(R.id.tvPreviewIcon);
        tvPreviewTitle = view.findViewById(R.id.tvPreviewTitle);
        tvPreviewMonth = view.findViewById(R.id.tvPreviewMonth);
        tvPreviewAmount = view.findViewById(R.id.tvPreviewAmount);
        pbPreviewProgress = view.findViewById(R.id.pbPreviewProgress);
    }

    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbarCreatePost);
        toolbar.setNavigationOnClickListener(v -> navigateBack());
    }

    private void setupComposer(View view) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        TextView txtComposerName = view.findViewById(R.id.txtComposerName);
        if (user != null) {
            String composerName = cleanDisplayName(user.getDisplayName());
            txtComposerName.setText(composerName);
            ImageHelper.loadAvatar(user.getPhotoUrl(), imgComposerAvatar,
                    firstNonEmpty(composerName, user.getEmail(), user.getUid()));
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

            @Override
            public void afterTextChanged(Editable s) {
                applyHashtagStyle(s);
            }
        });

        editPostContent.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                txtComposerHint.setText(milestoneMode
                        ? "Bài cột mốc sẽ hay hơn khi có một chiến thắng thật rõ."
                        : "Bắt đầu một câu chuyện tài chính nhỏ.");
            }
        });

        // HỨNG "BƯU KIỆN" TỪ BUDGET FRAGMENT TRUYỀN SANG
        if (getArguments() != null && getArguments().containsKey("milestone_limit")) {
            long limit = getArguments().getLong("milestone_limit");
            long spent = getArguments().getLong("milestone_spent");
            String periodType = getArguments().getString("milestone_period");
            String periodLabel = getArguments().getString("milestone_label");

            // Server không làm thì Client làm: Tự tính toán
            long remaining = limit - spent;
            int progress = (int) ((spent * 100) / limit);
            int uiProgress = progress > 100 ? 100 : progress;

            String amountLabel = remaining >= 0
                    ? "Còn dư: " + com.example.cashify.utils.CurrencyFormatter.formatCompactVND(remaining)
                    : "Vượt mức: " + com.example.cashify.utils.CurrencyFormatter.formatCompactVND(Math.abs(remaining));
            String iconText = progress + "%";
            String title = "Tổng kết " + ("MONTH".equals(periodType) ? "Ngân sách tháng" : "Ngân sách tuần");
            String defaultDescription = remaining >= 0
                    ? "Mình đã quản lý chi tiêu rất tốt trong kỳ này. Rất đáng tự hào! 🚀"
                    : "Kỳ này đã chi tiêu vượt ngân sách, cần kỷ luật hơn vào kỳ sau. 🥲";

            // Hiển thị lên giao diện thẻ Bo góc
            milestoneMode = true;
            milestonePreviewContainer.setVisibility(View.VISIBLE);
            tvPreviewIcon.setText(iconText);
            tvPreviewTitle.setText(title);
            tvPreviewMonth.setText(periodLabel);
            tvPreviewAmount.setText(amountLabel);
            pbPreviewProgress.setProgress(uiProgress);

            // Tắt nút Thêm Ảnh đi vì đã có cột mốc
            actionPhoto.setVisibility(View.GONE);
            actionMilestone.setVisibility(View.GONE);

            // Gợi ý cho người dùng viết caption
            txtComposerHint.setText("Cột mốc của bạn đã sẵn sàng! Gõ thêm cảm nghĩ phía trên.");

            // Đóng gói JSON sẵn, đợi bấm Đăng là phi lên C#
            try {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("iconText", iconText);
                obj.put("title", title);
                obj.put("description", defaultDescription); // Đề phòng user lười k gõ gì
                obj.put("month", periodLabel);
                obj.put("amount", amountLabel);
                obj.put("progress", uiProgress);
                generatedMilestoneJson = obj.toString();
            } catch (Exception ignored) {}
        }

        // HỨNG DỮ LIỆU ĐỂ MỞ CHẾ ĐỘ CHỈNH SỬA
        if (getArguments() != null && getArguments().containsKey("edit_post_id")) {
            String editId = getArguments().getString("edit_post_id");
            String oldContent = getArguments().getString("edit_post_content");
            String oldMilestone = getArguments().getString("edit_milestone_data");

            // Fill data cũ vào
            editPostContent.setText(oldContent);
            btnSubmitPost.setText("Lưu cập nhật");
            txtComposerHint.setText("Chỉnh sửa nội dung bài viết của bạn.");

            // Nếu là bài Cột mốc thì dựng lại thẻ Preview
            if (oldMilestone != null && !oldMilestone.isEmpty()) {
                milestoneMode = true;
                generatedMilestoneJson = oldMilestone;
                try {
                    org.json.JSONObject json = new org.json.JSONObject(oldMilestone);
                    milestonePreviewContainer.setVisibility(View.VISIBLE);
                    tvPreviewIcon.setText(json.optString("iconText", "🏆"));
                    tvPreviewTitle.setText(json.optString("title", "Cột mốc"));
                    tvPreviewMonth.setText(json.optString("month", ""));
                    tvPreviewAmount.setText(json.optString("amount", ""));
                    pbPreviewProgress.setProgress(json.optInt("progress", 0));

                    actionPhoto.setVisibility(View.GONE);
                    actionMilestone.setVisibility(View.GONE);
                } catch (Exception ignored) {}
            }
        }

        updateAudienceButton();
        updateAudienceDock();
        btnAudience.setOnClickListener(v -> selectAudience("Công khai"));
        btnAudienceFriends.setOnClickListener(v -> selectAudience("Bạn bè"));
        btnAudiencePrivate.setOnClickListener(v -> selectAudience("Chỉ mình tôi"));
        actionPhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        actionMilestone.setOnClickListener(v -> selectCategory("Cột mốc", "milestone", true, R.id.chipSaving));
        actionThoughts.setOnClickListener(v -> selectCategory("Suy nghĩ", "thoughts", false, R.id.chipBudgeting));
        actionAnalysis.setOnClickListener(v -> selectCategory("Phân tích", "analysis", false, R.id.chipInvesting));
        actionShare.setOnClickListener(v -> selectCategory("Chia sẻ", "share", false, R.id.chipDebt));
        setupTopicHashtags();
        view.findViewById(R.id.btnRemoveImage).setOnClickListener(v -> clearSelectedImage());
        btnSubmitPost.setOnClickListener(v -> submitPost());
        applyInitialCategoryArgument();
        updateCategoryTiles();
        updateCategoryDesign();
        updateSubmitState();
    }

    private void applyInitialCategoryArgument() {
        Bundle args = getArguments();
        if (args == null) {
            return;
        }
        String categoryKey = args.getString("categoryKey", "");
        if ("milestone".equals(categoryKey)) {
            selectCategory("Cá»™t má»‘c", "milestone", true, R.id.chipSaving);
        } else if ("analysis".equals(categoryKey)) {
            selectCategory("PhÃ¢n tÃ­ch", "analysis", false, R.id.chipInvesting);
        } else if ("share".equals(categoryKey)) {
            selectCategory("Chia sáº»", "share", false, R.id.chipDebt);
        } else if ("thoughts".equals(categoryKey)) {
            selectCategory("Suy nghÄ©", "thoughts", false, R.id.chipBudgeting);
        }
    }

    private void selectAudience(String audience) {
        selectedAudience = audience;
        updateAudienceButton();
        updateAudienceDock();
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
            updateAudienceDock();
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

    private void updateAudienceDock() {
        updateAudienceOption(btnAudience, "Công khai", R.drawable.ic_privacy_public);
        updateAudienceOption(btnAudienceFriends, "Bạn bè", R.drawable.ic_friends);
        updateAudienceOption(btnAudiencePrivate, "Chỉ mình tôi", R.drawable.ic_privacy_lock);
    }

    private void updateAudienceOption(TextView view, String label, int iconRes) {
        boolean selected = label.equals(selectedAudience);
        int textColor = ContextCompat.getColor(requireContext(), selected ? R.color.white : R.color.item_title);
        int iconColor = ContextCompat.getColor(requireContext(), selected ? R.color.white : R.color.item_title);

        view.setText("Chỉ mình tôi".equals(label) ? "Riêng tư" : label);
        view.setTextColor(textColor);
        view.setBackgroundResource(selected
                ? R.drawable.bg_publish_editorial
                : R.drawable.bg_privacy_option_inactive);
        view.setCompoundDrawablePadding(dp(6));
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(
                tintedDrawable(iconRes, iconColor),
                null,
                null,
                null
        );
        view.setCompoundDrawableTintList(ColorStateList.valueOf(iconColor));
    }

    private Drawable tintedDrawable(int iconRes, int color) {
        Drawable drawable = ContextCompat.getDrawable(requireContext(), iconRes);
        if (drawable == null) {
            return null;
        }
        Drawable wrapped = DrawableCompat.wrap(drawable.mutate());
        DrawableCompat.setTint(wrapped, color);
        return wrapped;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void setMilestoneMode(boolean enabled) {
        milestoneMode = enabled;
        txtComposerHint.setText(enabled
                ? "Chế độ cột mốc: chia sẻ mục tiêu, chuỗi ngày tốt hoặc một chiến thắng nhỏ."
                : "Bắt đầu một câu chuyện tài chính nhỏ.");
        editPostContent.setHint(enabled
                ? "Bạn vừa đạt cột mốc nào?"
                : "Bạn muốn chia sẻ chuyện tiền bạc gì hôm nay?");
        updateCategoryTiles();
        updateCategoryDesign();
        updateSubmitState();
    }

    private void selectCategory(String category, boolean milestone, int chipId) {
        selectCategory(category, categoryKeyFromLabel(category), milestone, chipId);
    }

    private void selectCategory(String category, String categoryKey, boolean milestone, int chipId) {
        selectedCategory = category;
        selectedCategoryKey = categoryKey;
        setMilestoneMode(milestone);
    }

    private String categoryKeyFromLabel(String category) {
        if (category == null) {
            return "thoughts";
        }
        if (category.contains("Cột")) {
            return "milestone";
        }
        if (category.contains("Phân")) {
            return "analysis";
        }
        if (category.contains("Chia")) {
            return "share";
        }
        return "thoughts";
    }

    private void setupTopicHashtags() {
        if (chipGroupTopics == null) {
            return;
        }
        setupTopicChipStyle();
        chipGroupTopics.setOnCheckedStateChangeListener((group, checkedIds) -> {
            Set<String> nextHashtags = hashtagsForCheckedIds(checkedIds);
            syncTopicHashtags(nextHashtags);
            updateTopicChipStyle();
        });
    }

    private void setupTopicChipStyle() {
        styleTopicChip(R.id.chipBudgeting, "#FFF0C9", "#F2C15E", "#7A4D09");
        styleTopicChip(R.id.chipSaving, "#E2F5DA", "#A7D99B", "#31523B");
        styleTopicChip(R.id.chipDebt, "#FFE1E5", "#F2A9B4", "#7B3640");
        styleTopicChip(R.id.chipInvesting, "#E2ECFF", "#AFC5F7", "#294A88");
    }

    private void updateTopicChipStyle() {
        styleTopicChip(R.id.chipBudgeting, "#FFF0C9", "#F2C15E", "#7A4D09");
        styleTopicChip(R.id.chipSaving, "#E2F5DA", "#A7D99B", "#31523B");
        styleTopicChip(R.id.chipDebt, "#FFE1E5", "#F2A9B4", "#7B3640");
        styleTopicChip(R.id.chipInvesting, "#E2ECFF", "#AFC5F7", "#294A88");
    }

    private void styleTopicChip(int chipId, String selectedBackgroundColor, String selectedStrokeColor, String selectedTextColor) {
        Chip chip = chipGroupTopics.findViewById(chipId);
        if (chip == null) {
            return;
        }
        boolean checked = chip.isChecked();
        int backgroundColor = checked
                ? android.graphics.Color.parseColor(selectedBackgroundColor)
                : ContextCompat.getColor(requireContext(), R.color.bg_main);
        int strokeColor = android.graphics.Color.parseColor(checked ? selectedStrokeColor : "#E8DCCB");
        int textColor = checked
                ? android.graphics.Color.parseColor(selectedTextColor)
                : ContextCompat.getColor(requireContext(), R.color.brand_primary);
        chip.setCheckedIconVisible(false);
        chip.setChipBackgroundColor(ColorStateList.valueOf(backgroundColor));
        chip.setChipStrokeColor(ColorStateList.valueOf(strokeColor));
        chip.setTextColor(textColor);
    }

    private Set<String> hashtagsForCheckedIds(List<Integer> checkedIds) {
        Set<String> hashtags = new LinkedHashSet<>();
        if (checkedIds == null) {
            return hashtags;
        }
        for (Integer checkedId : checkedIds) {
            if (checkedId != null) {
                hashtags.add(hashtagForTopic(checkedId));
            }
        }
        return hashtags;
    }

    private String hashtagForTopic(int checkedId) {
        if (checkedId == R.id.chipSaving) {
            return "#TietKiem";
        } else if (checkedId == R.id.chipDebt) {
            return "#No";
        } else if (checkedId == R.id.chipInvesting) {
            return "#DauTu";
        }
        return "#NganSach";
    }

    private void syncTopicHashtags(Set<String> nextHashtags) {
        String current = editPostContent.getText().toString();
        for (String hashtag : selectedTopicHashtags) {
            current = removeHashtagToken(current, hashtag);
        }

        String next = current.replaceAll("\\s+$", "");
        if (!nextHashtags.isEmpty()) {
            String hashtagLine = String.join(" ", nextHashtags);
            next = next.isEmpty() ? hashtagLine : next + "\n" + hashtagLine;
        }

        editPostContent.setText(next);
        editPostContent.setSelection(editPostContent.length());
        selectedTopicHashtags.clear();
        selectedTopicHashtags.addAll(nextHashtags);
        updateSubmitState();
    }

    private String removeHashtagToken(String text, String hashtag) {
        String next = text
                .replace(hashtag + " ", "")
                .replace(" " + hashtag, "")
                .replace("\n" + hashtag, "\n")
                .replace(hashtag, "");
        return next
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n");
    }

    private void applyHashtagStyle(Editable editable) {
        if (applyingHashtagStyle || editable == null) {
            return;
        }
        applyingHashtagStyle = true;
        ForegroundColorSpan[] existing = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : existing) {
            editable.removeSpan(span);
        }

        Matcher matcher = Pattern.compile("#[A-Za-z0-9_]+").matcher(editable.toString());
        while (matcher.find()) {
            String hashtag = editable.subSequence(matcher.start(), matcher.end()).toString();
            editable.setSpan(
                    new ForegroundColorSpan(colorForHashtag(hashtag)),
                    matcher.start(),
                    matcher.end(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        applyingHashtagStyle = false;
    }

    private int colorForHashtag(String hashtag) {
        if ("#TietKiem".equals(hashtag)) {
            return android.graphics.Color.parseColor("#74A982");
        }
        if ("#No".equals(hashtag)) {
            return android.graphics.Color.parseColor("#D98782");
        }
        if ("#DauTu".equals(hashtag)) {
            return android.graphics.Color.parseColor("#8794DB");
        }
        return android.graphics.Color.parseColor("#D29A6F");
    }

    private void updateCategoryTiles() {
        updateCategoryTile(actionMilestone, "Cột mốc", R.drawable.bg_category_tile_milestone,
                "#4B2A11", "#8A6237");
        updateCategoryTile(actionThoughts, "Suy nghĩ", R.drawable.bg_category_tile_thoughts,
                "#5A3422", "#B07D62");
        updateCategoryTile(actionAnalysis, "Phân tích", R.drawable.bg_category_tile_analysis,
                "#3E260F", "#8A6237");
        updateCategoryTile(actionShare, "Chia sẻ", R.drawable.bg_category_tile_share,
                "#5C3920", "#D4A373");
    }

    private void updateCategoryDesign() {
        if (panelCategoryMode == null) {
            return;
        }

        int panelBg;
        int iconRes;
        String kicker;
        String title;
        String description;
        String prompt;
        String composerHint;
        String submitText;

        switch (selectedCategoryKey) {
            case "milestone":
                panelBg = R.drawable.bg_mode_panel_milestone;
                iconRes = R.drawable.ic_feed_trophy;
                kicker = "CELEBRATE A MILESTONE";
                title = "Grow your story";
                description = "Biến một chiến thắng tài chính nhỏ thành cột mốc đáng nhớ cho hành trình của bạn.";
                prompt = "Gợi ý: Bạn vừa đạt được mục tiêu nào, và điều đó khiến bạn tự hào ra sao?";
                composerHint = "Bạn vừa đạt cột mốc nào?";
                submitText = "Đăng cột mốc";
                break;
            case "analysis":
                panelBg = R.drawable.bg_mode_panel_analysis;
                iconRes = R.drawable.ic_cozy_chart;
                kicker = "FINANCIAL DEEP DIVE";
                title = "Phân tích tiến độ";
                description = "Dành cho những quan sát có dữ liệu, bài học chi tiêu hoặc pattern tài chính bạn vừa nhận ra.";
                prompt = "Gợi ý: Tháng này điều gì tăng/giảm rõ nhất trong thói quen tiền bạc của bạn?";
                composerHint = "Bạn muốn phân tích điều gì trong tài chính của mình?";
                submitText = "Đăng phân tích";
                break;
            case "share":
                panelBg = R.drawable.bg_mode_panel_share;
                iconRes = R.drawable.ic_share;
                kicker = "SHARE A FINANCIAL TIP";
                title = "Grow together";
                description = "Một mẹo nhỏ, một kinh nghiệm thật hoặc một câu chuyện có thể giúp cộng đồng tốt hơn.";
                prompt = "Gợi ý: Mẹo nào bạn ước mình biết sớm hơn khi quản lý tiền?";
                composerHint = "Bạn muốn chia sẻ mẹo tài chính nào?";
                submitText = "Đăng chia sẻ";
                break;
            case "thoughts":
            default:
                panelBg = R.drawable.bg_mode_panel_thoughts;
                iconRes = R.drawable.ic_cozy_notebook;
                kicker = "DIGITAL JOURNAL";
                title = "Chia sẻ suy nghĩ";
                description = "Một không gian nhẹ để kể lại điều bạn học được trong hành trình tài chính hôm nay.";
                prompt = "Gợi ý: Hôm nay bạn nhận ra điều gì về cách mình tiêu tiền?";
                composerHint = "Hôm nay bạn đang nghĩ gì về tiền bạc?";
                submitText = "Đăng suy nghĩ";
                break;
        }

        panelCategoryMode.setBackgroundResource(panelBg);
        imgModeIcon.setImageResource(iconRes);
        txtModeKicker.setText(kicker);
        txtModeTitle.setText(title);
        txtModeDescription.setText(description);
        txtModePrompt.setText(prompt);
        editPostContent.setHint(composerHint);
        txtComposerHint.setText(prompt);
        btnSubmitPost.setText(submitText);
    }

    private void updateCategoryTile(TextView tile, String category, int normalBg,
                                    String normalTextColor, String normalIconColor) {
        boolean selected = category.equals(selectedCategory);
        tile.setBackgroundResource(selected ? R.drawable.bg_category_tile_selected : normalBg);
        tile.setText(selected ? "✓ " + category : category);
        tile.setTextColor(android.graphics.Color.parseColor(selected ? "#FFFFFFFF" : normalTextColor));
        tile.setCompoundDrawableTintList(ColorStateList.valueOf(
                android.graphics.Color.parseColor(selected ? "#FFFFFFFF" : normalIconColor)));
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
        String type = milestoneMode ? "MILESTONE_POST" : "USER_POST";
        String milestoneData = generatedMilestoneJson;
        String editPostId = getArguments() != null ? getArguments().getString("edit_post_id") : null;

        // 1. VALIDATE LÊN ĐẦU: Chặn ngay nếu không có chữ/ảnh/cột mốc (Dù là tạo mới hay sửa bài)
        if (content.isEmpty() && selectedImageUri == null && milestoneData == null) {
            Toast.makeText(requireContext(), "Hãy viết nội dung, thêm ảnh hoặc cột mốc trước nhé.", Toast.LENGTH_SHORT).show();
            editPostContent.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editPostContent, InputMethodManager.SHOW_IMPLICIT);
            }
            return;
        }

        String audienceParam = "FRIENDS"; // Mặc định
        if ("Công khai".equals(selectedAudience)) {
            audienceParam = "PUBLIC";
        } else if ("Chỉ mình tôi".equals(selectedAudience)) {
            audienceParam = "PRIVATE";
        }

        setPosting(true);

        final String finalContentToSubmit = content;
        final String finalAudienceToSubmit = audienceParam;

        // ==========================================
        // LUỒNG 1: NẾU LÀ CHẾ ĐỘ SỬA BÀI
        // ==========================================
        if (editPostId != null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) { setPosting(false); return; }

            user.getIdToken(true).addOnSuccessListener(tokenResult -> {
                String token = "Bearer " + tokenResult.getToken();
                com.example.cashify.utils.ApiService.EditPostRequest req = new com.example.cashify.utils.ApiService.EditPostRequest();
                req.PostId = editPostId;

                // Dùng biến final ở đây
                req.NewContent = finalContentToSubmit;
                req.Audience = finalAudienceToSubmit;

                com.example.cashify.utils.ApiClient.getClient().create(com.example.cashify.utils.ApiService.class)
                        .editPost(token, req).enqueue(new retrofit2.Callback<Object>() {
                            @Override public void onResponse(@NonNull retrofit2.Call<Object> call, @NonNull retrofit2.Response<Object> response) {
                                setPosting(false);
                                if (response.isSuccessful()) {
                                    Toast.makeText(requireContext(), "Sửa bài thành công!", Toast.LENGTH_SHORT).show();
                                    navigateBack();
                                } else Toast.makeText(requireContext(), "Lỗi server!", Toast.LENGTH_SHORT).show();
                            }
                            @Override public void onFailure(@NonNull retrofit2.Call<Object> call, @NonNull Throwable t) {
                                setPosting(false);
                            }
                        });
            });
            return;
        }

        // ==========================================
        // LUỒNG 2: NẾU LÀ TẠO BÀI ĐĂNG MỚI
        // ==========================================
        if (selectedImageUri != null) {
            txtComposerHint.setText("Đang tải ảnh lên máy chủ...");
            File imageFile = getFileFromUri(selectedImageUri);

            if (imageFile == null) {
                setPosting(false);
                Toast.makeText(requireContext(), "Lỗi đọc file ảnh!", Toast.LENGTH_SHORT).show();
                return;
            }

            UploadNotificationHelper notif = new UploadNotificationHelper(requireContext());

            com.example.cashify.utils.CloudinaryHelper.uploadImage(imageFile, new com.example.cashify.utils.CloudinaryHelper.UploadCallback() {
                @Override
                public void onProgress(int percent) {
                    notif.update(percent); // hiện trên status bar, không đụng UI fragment
                }

                @Override
                public void onSuccess(String imageUrl) {
                    notif.done();
                    requireActivity().runOnUiThread(() ->
                            callBackendToCreatePost(finalContentToSubmit, type, imageUrl, milestoneData, finalAudienceToSubmit)
                    );
                }

                @Override
                public void onFailure(String error) {
                    notif.error();
                    requireActivity().runOnUiThread(() -> {
                        setPosting(false);
                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Không thể tải ảnh")
                                .setMessage(error)
                                .setPositiveButton("Thử lại", (d, w) -> submitPost())
                                .setNegativeButton("Huỷ", null)
                                .show();
                    });
                }
            });
        } else {
            // Dùng biến final ở đây
            callBackendToCreatePost(finalContentToSubmit, type, "", milestoneData, finalAudienceToSubmit);
        }
    }

    // Nhớ update lại hàm callBackendToCreatePost để nó nhận tham số Audience nha sếp
    private void callBackendToCreatePost(String content, String type, String imageUrl, String milestoneData, String audienceParam) {
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

            com.example.cashify.utils.ApiService.CreatePostRequest request =
                    new com.example.cashify.utils.ApiService.CreatePostRequest(content, type, imageUrl, milestoneData, audienceParam);

            apiService.createPost(token, request).enqueue(new retrofit2.Callback<Object>() {
                @Override
                public void onResponse(@NonNull retrofit2.Call<Object> call, @NonNull retrofit2.Response<Object> response) {
                    requireActivity().runOnUiThread(() -> {
                        setPosting(false);
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                            resetComposer();
                            navigateBack();
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

            String audienceParam = "FRIENDS"; // Mặc định là Bạn bè
            if ("Công khai".equals(selectedAudience)) {
                audienceParam = "PUBLIC";
            } else if ("Chỉ mình tôi".equals(selectedAudience)) {
                audienceParam = "PRIVATE";
            }

            // Khởi tạo Request Model (Nhớ đảm bảo ApiService đã có class này)
            com.example.cashify.utils.ApiService.CreatePostRequest request =
                    new com.example.cashify.utils.ApiService.CreatePostRequest(content, type, imageUrl, milestoneData,audienceParam);

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
        selectedTopicHashtags.clear();
        chipGroupTopics.clearCheck();
        txtComposerHint.setText("Sẵn sàng cho bài chia sẻ tiếp theo.");
    }

    private void setPosting(boolean posting) {
        progressPosting.setVisibility(posting ? View.VISIBLE : View.GONE);
        btnSubmitPost.setEnabled(!posting);
        actionPhoto.setEnabled(!posting);
        actionMilestone.setEnabled(!posting);
        btnAudience.setEnabled(!posting);
        editPostContent.setEnabled(!posting);
        btnSubmitPost.setText(posting ? "Đang đăng..." : publishTextForCategory());
    }

    private String publishTextForCategory() {
        switch (selectedCategoryKey) {
            case "milestone":
                return "Đăng cột mốc";
            case "analysis":
                return "Đăng phân tích";
            case "share":
                return "Đăng chia sẻ";
            case "thoughts":
            default:
                return "Đăng suy nghĩ";
        }
    }

    private void updateSubmitState() {
        boolean hasText = editPostContent != null && editPostContent.getText().toString().trim().length() > 0;
        boolean hasImage = selectedImageUri != null;
        boolean hasMilestone = generatedMilestoneJson != null; // Kiểm tra có Milestone không

        // SỬA: Chỉ cần 1 trong 3 cái có dữ liệu là nút Đăng sẽ sáng lên
        boolean canSubmit = hasText || hasImage || hasMilestone;

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
        ImageHelper.loadAvatar(avatarUrl, imgComposerAvatar,
                firstNonEmpty(displayName, username, doc.getId()));
    }

    private String cleanDisplayName(String value) {
        if (value == null || value.trim().isEmpty() || value.contains("@")) {
            return "Người dùng Cashify";
        }
        return value.trim();
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }
}
