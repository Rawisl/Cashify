package com.example.cashify.ui.social;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.utils.CurrencyFormatter;
import com.example.cashify.utils.DialogHelper;
import com.example.cashify.utils.ImageHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONObject;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SocialComposerFragment extends Fragment {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SocialComposerViewModel viewModel;
    private EditText editPostContent;
    private EditText editPostTitle;
    private TextView txtComposerCount, txtComposerHint, btnAudienceSelector;
    private TextView actionMilestone, actionThoughts, actionAnalysis, actionShare, actionPhoto;
    private LinearLayout panelCategoryMode;
    private ImageView imgModeIcon, imgPostPreview, imgComposerAvatar;
    private TextView txtModeKicker, txtModeTitle, txtModeDescription, txtModePrompt;
    private android.view.MenuItem postMenuItem;
    private ProgressBar progressPosting;
    private FrameLayout imagePreviewContainer;
    private ChipGroup chipGroupTopics;
    private PopupWindow audiencePopup;

    private View milestonePreviewContainer;
    private TextView tvPreviewIcon, tvPreviewTitle, tvPreviewMonth, tvPreviewAmount;
    private ProgressBar pbPreviewProgress;

    private Uri selectedImageUri;
    private String existingImageUrl = null;
    private boolean milestoneMode;
    private String selectedAudience = "Public";
    private String selectedCategory = "Thoughts";
    private String selectedCategoryKey = "thoughts";
    private final Set<String> selectedTopicHashtags = new LinkedHashSet<>();
    private boolean applyingHashtagStyle = false;
    private String generatedMilestoneJson = null;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;

                long fileSize = getFileSizeFromUri(uri);
                if (fileSize > 10 * 1024 * 1024) {
                    DialogHelper.showCustomDialog(
                            requireContext(),
                            "Image too large",
                            "The image you selected exceeds 10MB. Please select a smaller image.",
                            "Choose again",
                            "Cancel",
                            DialogHelper.DialogType.DANGER,
                            true,
                            () -> actionPhoto.performClick(),
                            null
                    );
                    return;
                }

                selectedImageUri = uri;
                imgPostPreview.setImageURI(uri);
                imagePreviewContainer.setVisibility(View.VISIBLE);
                txtComposerHint.setText("Image added. You can add a description or post now.");
                updateSubmitState();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post_create, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SocialComposerViewModel.class);
        bindViews(view);
        setupToolbar(view);
        setupComposer(view);
        setupObservers();

        ImageView btnRemoveMilestonePreview = view.findViewById(R.id.btnRemoveMilestonePreview);
        if (btnRemoveMilestonePreview != null) {
            btnRemoveMilestonePreview.setOnClickListener(v -> {
                milestonePreviewContainer.setVisibility(View.GONE);
                milestoneMode = false;
                generatedMilestoneJson = null;
                actionPhoto.setVisibility(View.VISIBLE);

                if (txtComposerHint != null) txtComposerHint.setText("What financial story would you like to share today?");
                updateSubmitState();
            });
        }
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    private void bindViews(View view) {
        editPostContent = view.findViewById(R.id.editPostContent);
        editPostTitle = view.findViewById(R.id.editPostTitle);
        txtComposerCount = view.findViewById(R.id.txtComposerCount);
        txtComposerHint = null; // Removed from UI
        btnAudienceSelector = view.findViewById(R.id.btnAudienceSelector);
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
        progressPosting = null; // Removed from UI
        imagePreviewContainer = view.findViewById(R.id.imagePreviewContainer);
        imgPostPreview = view.findViewById(R.id.imgPostPreview);
        imgComposerAvatar = view.findViewById(R.id.imgComposerAvatar);
        chipGroupTopics = view.findViewById(R.id.chipGroupTopics);

        milestonePreviewContainer = view.findViewById(R.id.milestonePreviewContainer);
        if (milestonePreviewContainer != null) {
            tvPreviewIcon = milestonePreviewContainer.findViewById(R.id.tvPreviewIcon);
            tvPreviewTitle = milestonePreviewContainer.findViewById(R.id.tvPreviewTitle);
            tvPreviewMonth = milestonePreviewContainer.findViewById(R.id.tvPreviewMonth);
            tvPreviewAmount = milestonePreviewContainer.findViewById(R.id.tvPreviewAmount);
            pbPreviewProgress = milestonePreviewContainer.findViewById(R.id.pbPreviewProgress);
        }
    }

    private void setupToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbarCreatePost);
        toolbar.setNavigationOnClickListener(v -> navigateBack());
        postMenuItem = toolbar.getMenu().add(android.view.Menu.NONE, 1, android.view.Menu.NONE, "POST");
        postMenuItem.setIcon(R.drawable.ic_send);
        postMenuItem.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS | android.view.MenuItem.SHOW_AS_ACTION_WITH_TEXT);

        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                if (editPostContent != null && editPostContent.getText().toString().trim().length() > 0 ||
                    editPostTitle != null && editPostTitle.getVisibility() == View.VISIBLE && editPostTitle.getText().toString().trim().length() > 0 ||
                    selectedImageUri != null || (existingImageUrl != null && !existingImageUrl.isEmpty()) ||
                    generatedMilestoneJson != null) {
                    submitPost();
                } else {
                    Toast.makeText(requireContext(), "Post cannot be empty", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });
    }

    private void setupComposer(View view) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        TextView txtComposerName = view.findViewById(R.id.txtComposerName);

        if (user != null) {
            String composerName = cleanDisplayName(user.getDisplayName());
            txtComposerName.setText(composerName);
            ImageHelper.loadAvatar(user.getPhotoUrl(), imgComposerAvatar, firstNonEmpty(composerName, user.getEmail(), user.getUid()));
            viewModel.loadUserProfile();
        }

        editPostContent.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                txtComposerCount.setText(String.format(Locale.US, "%d/280", s.length()));
                updateSubmitState();
            }
            @Override public void afterTextChanged(Editable s) {
                applyHashtagStyle(s);
            }
        });

        editPostContent.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && txtComposerHint != null) {
                txtComposerHint.setText(milestoneMode
                        ? "Milestones are better when celebrating a clear win."
                        : "Start a small financial story.");
            }
        });

        handleIncomingArguments();

        updateAudienceButton();
        if (btnAudienceSelector != null) {
            btnAudienceSelector.setOnClickListener(v -> showAudienceMenu(v));
        }

        actionPhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        actionMilestone.setOnClickListener(v -> {
            selectCategory("Milestone", "milestone", true, R.id.chipSaving);
            showAchievementBottomSheet();
        });

        actionThoughts.setOnClickListener(v -> selectCategory("Thoughts", "thoughts", false, R.id.chipBudgeting));
        actionAnalysis.setOnClickListener(v -> selectCategory("Analysis", "analysis", false, R.id.chipInvesting));
        actionShare.setOnClickListener(v -> selectCategory("Share", "share", false, R.id.chipDebt));

        setupTopicHashtags();
        view.findViewById(R.id.btnRemoveImage).setOnClickListener(v -> clearSelectedImage());

        applyInitialCategoryArgument();
        updateCategoryTiles();
        updateCategoryDesign();
        updateSubmitState();
    }

    private void handleIncomingArguments() {
        if (getArguments() == null) return;

        // Xử lý khi tạo Milestone từ Limit/Budget
        if (getArguments().containsKey("milestone_limit")) {
            long limit = getArguments().getLong("milestone_limit");
            long spent = getArguments().getLong("milestone_spent");
            String periodType = getArguments().getString("milestone_period");
            String periodLabel = getArguments().getString("milestone_label");

            long remaining = limit - spent;
            int progress = (int) ((spent * 100) / limit);
            int uiProgress = Math.min(progress, 100);

            String amountLabel = remaining >= 0
                    ? "Remaining: " + CurrencyFormatter.formatCompactVND(remaining)
                    : "Overspent: " + CurrencyFormatter.formatCompactVND(Math.abs(remaining));
            String title = "MONTH".equals(periodType) ? "Monthly Budget Summary" : "Weekly Budget Summary";
            String defaultDescription = remaining >= 0
                    ? "You've managed your spending very well this period. Great job! 🚀"
                    : "You've gone over budget this period. Try to stay on track next time. 🥲";

            milestoneMode = true;
            milestonePreviewContainer.setVisibility(View.VISIBLE);
            tvPreviewIcon.setText(progress + "%");
            tvPreviewTitle.setText(title);
            tvPreviewMonth.setText(periodLabel);
            tvPreviewAmount.setText(amountLabel);
            pbPreviewProgress.setProgress(uiProgress);

            if (actionPhoto != null) actionPhoto.setVisibility(View.GONE);

            txtComposerHint.setText("Your milestone is ready! Post your thoughts above.");

            if (editPostTitle != null) {
                editPostTitle.setVisibility(View.GONE);
                View divider = getView() != null ? getView().findViewById(R.id.dividerTitle) : null;
                if (divider != null) divider.setVisibility(View.GONE);
            }

            try {
                JSONObject obj = new JSONObject();
                obj.put("iconText", progress + "%");
                obj.put("title", title);
                obj.put("description", defaultDescription);
                obj.put("month", periodLabel);
                obj.put("amount", amountLabel);
                obj.put("progress", uiProgress);
                generatedMilestoneJson = obj.toString();
            } catch (Exception ignored) {}
        }

        // Xử lý khi Edit bài viết cũ
        if (getArguments().containsKey("edit_post_id")) {
            String oldTitle = getArguments().getString("edit_post_title");
            String oldContent = getArguments().getString("edit_post_content");
            String oldMilestone = getArguments().getString("edit_milestone_data");
            String oldImage = getArguments().getString("edit_post_image");

            if (oldMilestone != null && !oldMilestone.trim().isEmpty()) {
                // Có achievement -> Cứng ngắc là Milestone (Tự ẩn Title, tự ẩn Image)
                selectCategory("Milestone", "milestone", true, R.id.chipSaving);
            } else {
                // Không có achievement -> Quy hết về Thoughts (Được hiện Title, được up Image)
                selectCategory("Thoughts", "thoughts", false, R.id.chipBudgeting);
            }

            if (oldTitle != null && editPostTitle != null) editPostTitle.setText(oldTitle);
            if (editPostContent != null) editPostContent.setText(oldContent);

            if (oldImage != null && !oldImage.trim().isEmpty()) {
                existingImageUrl = oldImage;
                com.bumptech.glide.Glide.with(requireContext()).load(existingImageUrl).into(imgPostPreview);
                imagePreviewContainer.setVisibility(View.VISIBLE);
                if (actionPhoto != null) actionPhoto.setVisibility(View.GONE);
            }

            if (oldMilestone != null && !oldMilestone.trim().isEmpty()) {
                generatedMilestoneJson = oldMilestone;
                try {
                    JSONObject json = new JSONObject(oldMilestone);
                    milestonePreviewContainer.setVisibility(View.VISIBLE);
                    tvPreviewIcon.setText(json.optString("iconText", "🏆"));
                    tvPreviewTitle.setText(json.optString("title", "Milestone"));
                    tvPreviewMonth.setText(json.optString("month", ""));
                    tvPreviewAmount.setText(json.optString("amount", ""));
                    pbPreviewProgress.setProgress(json.optInt("progress", 0));

                    if (actionPhoto != null) actionPhoto.setVisibility(View.GONE);
                } catch (Exception ignored) {}
            }
            if (postMenuItem != null) postMenuItem.setTitle("SAVE");
            txtComposerHint.setText("What's on your mind? Edit your post here...");
        }
    }

    private void setupObservers() {
        TextView txtComposerName = getView().findViewById(R.id.txtComposerName);

        viewModel.getUserProfile().observe(getViewLifecycleOwner(), doc -> bindCurrentUserProfile(doc, txtComposerName));
        viewModel.loadUserProfile();

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), this::setPosting);

        viewModel.getPostEvent().observe(getViewLifecycleOwner(), successMessage -> {
            if (successMessage != null) {
                Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show();
                com.example.cashify.ui.social.SocialNewsfeedFragment.needRefreshFeed = true;
                navigateBack();
                viewModel.clearPostEvent();
            }
        });

        viewModel.getErrorEvent().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                viewModel.getErrorEvent();
            }
        });
    }

    private void applyInitialCategoryArgument() {
        Bundle args = getArguments();
        if (args == null) return;


        String categoryKey = args.getString("categoryKey", "");

        //Nếu Intent không truyền categoryKey nhưng có data Milestone, tự hiểu là Milestone
        if (categoryKey.isEmpty()) {
            if (args.containsKey("edit_milestone_data") || args.containsKey("milestone_limit")) {
                categoryKey = "milestone";
            }
        }

        switch (categoryKey) {
            case "milestone": selectCategory("Milestone", "milestone", true, R.id.chipSaving); break;
            case "analysis": selectCategory("Analysis", "analysis", false, R.id.chipInvesting); break;
            case "share": selectCategory("Share", "share", false, R.id.chipDebt); break;
            case "thoughts": selectCategory("Thoughts", "thoughts", false, R.id.chipBudgeting); break;
        }
    }

    private void selectAudience(String audience) {
        selectedAudience = audience;
        updateAudienceButton();
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

        addAudienceMenuItem(menu, "Public", R.drawable.ic_privacy_public);
        addAudienceMenuItem(menu, "Friends", R.drawable.ic_friends);
        addAudienceMenuItem(menu, "Only Me", R.drawable.ic_privacy_lock);

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
            if (audiencePopup != null) audiencePopup.dismiss();
        });
        menu.addView(item, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void updateAudienceButton() {
        if (btnAudienceSelector != null) {
            btnAudienceSelector.setText(selectedAudience);
        }
    }

    private int iconForAudience(String audience) {
        if ("Public".equals(audience)) return R.drawable.ic_privacy_public;
        if ("Only Me".equals(audience)) return R.drawable.ic_privacy_lock;
        return R.drawable.ic_friends;
    }

    private Drawable tintedDrawable(int iconRes) {
        Drawable drawable = ContextCompat.getDrawable(requireContext(), iconRes);
        if (drawable == null) return null;
        Drawable wrapped = DrawableCompat.wrap(drawable.mutate());
        DrawableCompat.setTint(wrapped, ContextCompat.getColor(requireContext(), R.color.brand_primary));
        return wrapped;
    }

    private Drawable tintedDrawable(int iconRes, int color) {
        Drawable drawable = ContextCompat.getDrawable(requireContext(), iconRes);
        if (drawable == null) return null;
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
                ? "Milestone mode: share your goal, streak, or a small win."
                : "Start a small financial story.");
        editPostContent.setHint(enabled
                ? "What milestone did you reach?"
                : "What financial story would you like to share today?");
        if (editPostTitle != null) {
            editPostTitle.setVisibility(enabled ? View.GONE : View.VISIBLE);
            View divider = getView() != null ? getView().findViewById(R.id.dividerTitle) : null;
            if (divider != null) divider.setVisibility(enabled ? View.GONE : View.VISIBLE);
        }

        if (actionPhoto != null) {
            actionPhoto.setVisibility(enabled ? View.GONE : View.VISIBLE);
        }
        // Nếu đang ở chế độ Milestone mà lỡ chọn ảnh trước đó rồi thì xóa luôn ảnh đó đi
        if (enabled && selectedImageUri != null) {
            clearSelectedImage();
        }
        updateCategoryTiles();
        updateCategoryDesign();
        updateSubmitState();
    }

    private void selectCategory(String category, String categoryKey, boolean milestone, int chipId) {
        selectedCategory = category;
        selectedCategoryKey = categoryKey;
        setMilestoneMode(milestone);
    }

    private void setupTopicHashtags() {
        if (chipGroupTopics == null) return;
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
        setupTopicChipStyle();
    }

    private void styleTopicChip(int chipId, String bg, String stroke, String textStr) {
        Chip chip = chipGroupTopics.findViewById(chipId);
        if (chip == null) return;
        boolean checked = chip.isChecked();
        int backgroundColor = checked ? android.graphics.Color.parseColor(bg) : ContextCompat.getColor(requireContext(), R.color.bg_main);
        int strokeColor = android.graphics.Color.parseColor(checked ? stroke : "#E8DCCB");
        int textColor = checked ? android.graphics.Color.parseColor(textStr) : ContextCompat.getColor(requireContext(), R.color.brand_primary);
        chip.setCheckedIconVisible(false);
        chip.setChipBackgroundColor(ColorStateList.valueOf(backgroundColor));
        chip.setChipStrokeColor(ColorStateList.valueOf(strokeColor));
        chip.setTextColor(textColor);
    }

    private Set<String> hashtagsForCheckedIds(List<Integer> checkedIds) {
        Set<String> hashtags = new LinkedHashSet<>();
        if (checkedIds == null) return hashtags;
        for (Integer id : checkedIds) if (id != null) hashtags.add(hashtagForTopic(id));
        return hashtags;
    }

    private String hashtagForTopic(int checkedId) {
        if (checkedId == R.id.chipSaving) return "#Saving";
        if (checkedId == R.id.chipDebt) return "#Debt";
        if (checkedId == R.id.chipInvesting) return "#Investing";
        return "#Budgeting";
    }

    private void syncTopicHashtags(Set<String> nextHashtags) {
        String current = editPostContent.getText().toString();
        for (String hashtag : selectedTopicHashtags) current = removeHashtagToken(current, hashtag);
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
        return text.replace(hashtag + " ", "").replace(" " + hashtag, "")
                .replace("\n" + hashtag, "\n").replace(hashtag, "")
                .replaceAll("[ \\t]+\\n", "\n").replaceAll("\\n{3,}", "\n\n");
    }

    private void applyHashtagStyle(Editable editable) {
        if (applyingHashtagStyle || editable == null) return;
        applyingHashtagStyle = true;
        ForegroundColorSpan[] existing = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : existing) editable.removeSpan(span);

        Matcher matcher = Pattern.compile("#[A-Za-z0-9_]+").matcher(editable.toString());
        while (matcher.find()) {
            String hashtag = editable.subSequence(matcher.start(), matcher.end()).toString();
            editable.setSpan(new ForegroundColorSpan(colorForHashtag(hashtag)),
                    matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        applyingHashtagStyle = false;
    }

    private int colorForHashtag(String hashtag) {
        if ("#Saving".equals(hashtag)) return android.graphics.Color.parseColor("#74A982");
        if ("#Debt".equals(hashtag)) return android.graphics.Color.parseColor("#D98782");
        if ("#Investing".equals(hashtag)) return android.graphics.Color.parseColor("#8794DB");
        return android.graphics.Color.parseColor("#D29A6F");
    }

    private void updateCategoryTiles() {
        updateCategoryTile(actionMilestone, "Milestone", R.drawable.bg_category_tile_milestone, "#4B2A11", "#8A6237");
        updateCategoryTile(actionThoughts, "Thoughts", R.drawable.bg_category_tile_thoughts, "#5A3422", "#B07D62");
        updateCategoryTile(actionAnalysis, "Analysis", R.drawable.bg_category_tile_analysis, "#3E260F", "#8A6237");
        updateCategoryTile(actionShare, "Share", R.drawable.bg_category_tile_share, "#5C3920", "#D4A373");
    }

    private void updateCategoryDesign() {
        if (panelCategoryMode == null) return;
        int panelBg, iconRes;
        String kicker, title, description, prompt, composerHint, submitText;

        switch (selectedCategoryKey) {
            case "milestone":
                panelBg = R.drawable.bg_mode_panel_milestone; iconRes = R.drawable.ic_feed_trophy;
                kicker = "CELEBRATE A MILESTONE"; title = "Grow your story";
                description = "Turn a small financial win into a memorable milestone for your journey.";
                prompt = "Prompt: What goal did you just reach, and why are you proud of it?";
                composerHint = "Which milestone did you reach?"; submitText = "Post Milestone";
                break;
            case "analysis":
                panelBg = R.drawable.bg_mode_panel_analysis; iconRes = R.drawable.ic_cozy_chart;
                kicker = "FINANCIAL DEEP DIVE"; title = "Analyze Progress";
                description = "For data-backed observations, spending lessons, or financial patterns you just noticed.";
                prompt = "Prompt: What changed most in your money habits this month?";
                composerHint = "What would you like to analyze in your finances?"; submitText = "Post Analysis";
                break;
            case "share":
                panelBg = R.drawable.bg_mode_panel_share; iconRes = R.drawable.ic_share;
                kicker = "SHARE A FINANCIAL TIP"; title = "Grow together";
                description = "A small tip, real experience, or story that can help the community.";
                prompt = "Prompt: What money tip do you wish you had learned earlier?";
                composerHint = "Which financial tip would you like to share?"; submitText = "Post Share";
                break;
            default:
                panelBg = R.drawable.bg_mode_panel_thoughts; iconRes = R.drawable.ic_cozy_notebook;
                kicker = "DIGITAL JOURNAL"; title = "Share a Thought";
                description = "A gentle space to share what you learned on your financial journey today.";
                prompt = "Prompt: What did you realize today about how you spend money?";
                composerHint = "What are you thinking about money today?"; submitText = "Post Thought";
                break;
        }
        panelCategoryMode.setBackgroundResource(panelBg);
        imgModeIcon.setImageResource(iconRes);
        txtModeKicker.setText(kicker);
        txtModeTitle.setText(title);
        txtModeDescription.setText(description);
        txtModePrompt.setText(prompt);
        editPostContent.setHint(composerHint);
        if (txtComposerHint != null) txtComposerHint.setText(prompt);
        if (postMenuItem != null) postMenuItem.setTitle(submitText.toUpperCase());
    }

    private void updateCategoryTile(TextView tile, String category, int normalBg, String normalTextColor, String normalIconColor) {
        boolean selected = category.equals(selectedCategory);
        tile.setBackgroundResource(selected ? R.drawable.bg_category_tile_selected : normalBg);
        tile.setText(selected ? "✓ " + category : category);
        tile.setTextColor(android.graphics.Color.parseColor(selected ? "#FFFFFFFF" : normalTextColor));
        tile.setCompoundDrawableTintList(ColorStateList.valueOf(android.graphics.Color.parseColor(selected ? "#FFFFFFFF" : normalIconColor)));
    }

    private void clearSelectedImage() {
        selectedImageUri = null;
        existingImageUrl = null;
        imgPostPreview.setImageDrawable(null);
        imagePreviewContainer.setVisibility(View.GONE);
        if (!milestoneMode) actionPhoto.setVisibility(View.VISIBLE);
        if (txtComposerHint != null) txtComposerHint.setText("Image removed.");
        updateSubmitState();
    }

    private void submitPost() {
        String content = editPostContent.getText().toString().trim();
        String title = editPostTitle != null && editPostTitle.getVisibility() == View.VISIBLE
                ? editPostTitle.getText().toString().trim() : "";

        String type = milestoneMode ? "MILESTONE_POST" : "USER_POST";
        String editPostId = getArguments() != null ? getArguments().getString("edit_post_id") : null;

        if (content.isEmpty() && selectedImageUri == null && generatedMilestoneJson == null && title.isEmpty()) {
            Toast.makeText(requireContext(), "Please write something, add an image, or add a milestone first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String audienceParam = "Public".equals(selectedAudience) ? "PUBLIC" : "Only Me".equals(selectedAudience) ? "PRIVATE" : "FRIENDS";

        viewModel.submitPost(editPostId, title, content, type, selectedImageUri, existingImageUrl, generatedMilestoneJson, audienceParam);
    }

    private long getFileSizeFromUri(Uri uri) {
        try (android.database.Cursor cursor = requireContext().getContentResolver().query(
                uri, new String[]{android.provider.OpenableColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                    return cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception e) {
            return 0;
        }
        return 0;
    }

    private void showAchievementBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View bottomSheetView = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_achievement, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        ProgressBar progressLoading = bottomSheetView.findViewById(R.id.progressLoadingAchievements);
        TextView tvEmpty = bottomSheetView.findViewById(R.id.tvEmptyAchievements);
        RecyclerView rvAchievements = bottomSheetView.findViewById(R.id.rvAchievements);

        rvAchievements.setLayoutManager(new LinearLayoutManager(requireContext()));
        bottomSheetDialog.show();

        viewModel.fetchAvailableAchievements();
        viewModel.getAchievements().removeObservers(getViewLifecycleOwner());
        viewModel.getAchievements().observe(getViewLifecycleOwner(), list -> {
            progressLoading.setVisibility(View.GONE);
            if (list == null || list.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                rvAchievements.setVisibility(View.GONE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                rvAchievements.setVisibility(View.VISIBLE);

                AchievementAdapter achievementAdapter = new AchievementAdapter(list, achievement -> {
                    if (bottomSheetDialog != null) bottomSheetDialog.dismiss();

                    milestoneMode = true;
                    if (milestonePreviewContainer != null) milestonePreviewContainer.setVisibility(View.VISIBLE);
                    if (tvPreviewIcon != null) tvPreviewIcon.setText(achievement.iconText);
                    if (tvPreviewTitle != null) tvPreviewTitle.setText(achievement.title);
                    if (tvPreviewMonth != null) tvPreviewMonth.setText(achievement.monthLabel);
                    if (tvPreviewAmount != null) tvPreviewAmount.setText(achievement.amountLabel);
                    if (pbPreviewProgress != null) pbPreviewProgress.setProgress(achievement.progress);

                    if (actionPhoto != null) actionPhoto.setVisibility(View.GONE);
                    if (txtComposerHint != null) txtComposerHint.setText("Your milestone is ready! Add some thoughts to share.");

                    try {
                        JSONObject json = new JSONObject();
                        json.put("achievementId", achievement.id);
                        json.put("iconText", achievement.iconText);
                        json.put("title", achievement.title);
                        json.put("description", achievement.description);
                        json.put("month", achievement.monthLabel);
                        json.put("amount", achievement.amountLabel);
                        json.put("progress", achievement.progress);
                        generatedMilestoneJson = json.toString();
                        updateSubmitState();
                    } catch (Exception ignored) {}
                });
                rvAchievements.setAdapter(achievementAdapter);
            }
        });
    }

    private void setPosting(boolean posting) {
        if (progressPosting != null) progressPosting.setVisibility(posting ? View.VISIBLE : View.GONE);
        if (postMenuItem != null) {
            postMenuItem.setEnabled(!posting);
            postMenuItem.setTitle(posting ? "POSTING..." : publishTextForCategory().toUpperCase());
        }
        if (actionPhoto != null) actionPhoto.setEnabled(!posting);
        if (actionMilestone != null) actionMilestone.setEnabled(!posting);
        if (btnAudienceSelector != null) btnAudienceSelector.setEnabled(!posting);
        if (editPostContent != null) editPostContent.setEnabled(!posting);
    }

    private String publishTextForCategory() {
        switch (selectedCategoryKey) {
            case "milestone": return "Post Milestone";
            case "analysis": return "Post Analysis";
            case "share": return "Post Share";
            default: return "Post Thought";
        }
    }

    private void updateSubmitState() {
        boolean hasText = editPostContent != null && editPostContent.getText().toString().trim().length() > 0;

        boolean hasTitle = editPostTitle != null && editPostTitle.getVisibility() == View.VISIBLE && editPostTitle.getText().toString().trim().length() > 0;
        boolean hasImage = selectedImageUri != null || (existingImageUrl != null && !existingImageUrl.isEmpty());

        boolean hasMilestone = generatedMilestoneJson != null;

        boolean canSubmit = hasText || hasTitle || hasImage || hasMilestone;

        if (postMenuItem != null) {
            postMenuItem.setEnabled(canSubmit);
            if (postMenuItem.getIcon() != null) {
                postMenuItem.getIcon().setAlpha(canSubmit ? 255 : 128);
            }
        }
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
        if (doc == null || !doc.exists()) return;
        String displayName = cleanDisplayName(doc.getString("displayName"));
        String username = cleanDisplayName(doc.getString("username"));
        txtComposerName.setText(!displayName.equals("Cashify User") ? displayName : username);
        String avatarUrl = doc.getString("avatarUrl");
        ImageHelper.loadAvatar(avatarUrl, imgComposerAvatar, firstNonEmpty(displayName, username, doc.getId()));
    }

    private String cleanDisplayName(String value) {
        if (value == null || value.trim().isEmpty() || value.contains("@")) return "Cashify User";
        return value.trim();
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) if (value != null && !value.trim().isEmpty()) return value.trim();
        return "";
    }
}