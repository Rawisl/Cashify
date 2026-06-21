package com.example.cashify.ui.feed;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.cashify.R;
import com.example.cashify.ui.common.AvatarImageView;
import com.example.cashify.utils.ApiClient;
import com.example.cashify.utils.ApiService;
import com.example.cashify.utils.DialogHelper;
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
    private TextView btnAudiencePill;
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
    private AvatarImageView imgComposerAvatar;
    private ChipGroup chipGroupTopics;

    private Uri selectedImageUri;
    private boolean milestoneMode;
    private String selectedAudience = "Public";
    private String selectedCategory = "Thoughts";
    private String selectedCategoryKey = "thoughts";
    private final Set<String> selectedTopicHashtags = new LinkedHashSet<>();
    private boolean applyingHashtagStyle = false;
    private boolean syncingHashtags = false;

    private View milestonePreviewContainer;
    private TextView tvPreviewIcon, tvPreviewTitle, tvPreviewMonth, tvPreviewAmount;
    private ProgressBar pbPreviewProgress;
    private String generatedMilestoneJson = null;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null || !isAdded()) return;

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
                            () -> {
                                if (actionPhoto != null) actionPhoto.performClick();
                            },
                            null
                    );
                    return;
                }

                selectedImageUri = uri;
                if (imgPostPreview != null) imgPostPreview.setImageURI(uri);
                if (imagePreviewContainer != null) imagePreviewContainer.setVisibility(View.VISIBLE);
                if (txtComposerHint != null) txtComposerHint.setText("Image added. Ready to share.");
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
        btnAudiencePill = view.findViewById(R.id.btnAudiencePill);
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
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> navigateBack());
        }
    }

    private void setupComposer(View view) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        TextView txtComposerName = view.findViewById(R.id.txtComposerName);
        if (user != null) {
            String composerName = cleanDisplayName(user.getDisplayName());
            if (txtComposerName != null) {
                txtComposerName.setText(composerName);
            }
            ImageHelper.loadAvatar(user.getPhotoUrl(), imgComposerAvatar,
                    firstNonEmpty(composerName, user.getEmail(), user.getUid()));
            FirebaseFirestore.getInstance().collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (isAdded()) bindCurrentUserProfile(doc, txtComposerName);
                    });
        }

        if (editPostContent != null) {
            editPostContent.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (txtComposerCount != null) {
                        txtComposerCount.setText(String.format(Locale.US, "%d/280", s.length()));
                    }
                    if (!syncingHashtags) syncSelectedHashtagsFromEditor(s.toString());
                    updateSubmitState();
                }

                @Override
                public void afterTextChanged(Editable s) {
                    applyHashtagStyle(s);
                }
            });

            editPostContent.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus && txtComposerHint != null) {
                    txtComposerHint.setText(milestoneMode
                            ? "Milestone posts are better with a clear win."
                            : "Start a small financial story.");
                }
            });
        }

        if (getArguments() != null && getArguments().containsKey("milestone_limit")) {
            long limit = getArguments().getLong("milestone_limit");
            long spent = getArguments().getLong("milestone_spent");
            String periodType = getArguments().getString("milestone_period");
            String periodLabel = getArguments().getString("milestone_label");

            long remaining = limit - spent;
            int progress = limit > 0 ? (int) ((spent * 100) / limit) : 0;
            int uiProgress = Math.min(progress, 100);

            String amountLabel = remaining >= 0
                    ? "Left: " + com.example.cashify.utils.CurrencyFormatter.formatCompactVND(remaining)
                    : "Over: " + com.example.cashify.utils.CurrencyFormatter.formatCompactVND(Math.abs(remaining));
            String iconText = progress + "%";
            String title = "Summary " + ("MONTH".equals(periodType) ? "Monthly Budget" : "Weekly Budget");
            String defaultDescription = remaining >= 0
                    ? "You've managed your spending very well this period. Great job! 🚀"
                    : "You've gone over budget this period. Try to stay on track next time. 🥲";

            milestoneMode = true;
            if (milestonePreviewContainer != null) milestonePreviewContainer.setVisibility(View.VISIBLE);
            if (tvPreviewIcon != null) tvPreviewIcon.setText(iconText);
            if (tvPreviewTitle != null) tvPreviewTitle.setText(title);
            if (tvPreviewMonth != null) tvPreviewMonth.setText(periodLabel);
            if (tvPreviewAmount != null) tvPreviewAmount.setText(amountLabel);
            if (pbPreviewProgress != null) pbPreviewProgress.setProgress(uiProgress);

            if (actionPhoto != null) actionPhoto.setVisibility(View.GONE);
            if (actionMilestone != null) actionMilestone.setVisibility(View.GONE);

            if (txtComposerHint != null) {
                txtComposerHint.setText("Your milestone is ready! Post your thought above.");
            }

            try {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("iconText", iconText);
                obj.put("title", title);
                obj.put("description", defaultDescription);
                obj.put("month", periodLabel);
                obj.put("amount", amountLabel);
                obj.put("progress", uiProgress);
                generatedMilestoneJson = obj.toString();
            } catch (Exception ignored) {}
        }

        if (getArguments() != null && getArguments().containsKey("edit_post_id")) {
            String oldContent = getArguments().getString("edit_post_content");
            String oldMilestone = getArguments().getString("edit_milestone_data");

            if (editPostContent != null) editPostContent.setText(oldContent);
            if (btnSubmitPost != null) btnSubmitPost.setText("Save Update");
            if (txtComposerHint != null) txtComposerHint.setText("Editing your post.");

            if (oldMilestone != null && !oldMilestone.isEmpty()) {
                milestoneMode = true;
                generatedMilestoneJson = oldMilestone;
                try {
                    org.json.JSONObject json = new org.json.JSONObject(oldMilestone);
                    if (milestonePreviewContainer != null) milestonePreviewContainer.setVisibility(View.VISIBLE);
                    if (tvPreviewIcon != null) tvPreviewIcon.setText(json.optString("iconText", "🏆"));
                    if (tvPreviewTitle != null) tvPreviewTitle.setText(json.optString("title", "Milestone"));
                    if (tvPreviewMonth != null) tvPreviewMonth.setText(json.optString("month", ""));
                    if (tvPreviewAmount != null) tvPreviewAmount.setText(json.optString("amount", ""));
                    if (pbPreviewProgress != null) pbPreviewProgress.setProgress(json.optInt("progress", 0));

                    if (actionPhoto != null) actionPhoto.setVisibility(View.GONE);
                    if (actionMilestone != null) actionMilestone.setVisibility(View.GONE);
                } catch (Exception ignored) {}
            }
        }

        updateAudiencePill();
        if (btnAudiencePill != null) {
            btnAudiencePill.setOnClickListener(v -> showAudiencePopup());
        }
        if (actionPhoto != null) actionPhoto.setOnClickListener(v -> {
            if (isAdded()) pickImageLauncher.launch("image/*");
        });
        
        if (actionMilestone != null) actionMilestone.setOnClickListener(v -> selectCategory("Milestone", "milestone", true, R.id.chipSaving));
        if (actionThoughts != null) actionThoughts.setOnClickListener(v -> selectCategory("Thoughts", "thoughts", false, R.id.chipBudgeting));
        if (actionAnalysis != null) actionAnalysis.setOnClickListener(v -> selectCategory("Analysis", "analysis", false, R.id.chipInvesting));
        if (actionShare != null) actionShare.setOnClickListener(v -> selectCategory("Share", "share", false, R.id.chipDebt));
        
        setupTopicHashtags();
        View btnRemoveImage = view.findViewById(R.id.btnRemoveImage);
        if (btnRemoveImage != null) btnRemoveImage.setOnClickListener(v -> clearSelectedImage());
        if (btnSubmitPost != null) btnSubmitPost.setOnClickListener(v -> submitPost());
        
        applyInitialCategoryArgument();
        updateCategoryTiles();
        updateCategoryDesign();
        updateSubmitState();
    }

    private void applyInitialCategoryArgument() {
        Bundle args = getArguments();
        if (args == null) return;
        String categoryKey = args.getString("categoryKey", "");
        if ("milestone".equals(categoryKey)) {
            selectCategory("Milestone", "milestone", true, R.id.chipSaving);
        } else if ("analysis".equals(categoryKey)) {
            selectCategory("Analysis", "analysis", false, R.id.chipInvesting);
        } else if ("share".equals(categoryKey)) {
            selectCategory("Share", "share", false, R.id.chipDebt);
        } else if ("thoughts".equals(categoryKey)) {
            selectCategory("Thoughts", "thoughts", false, R.id.chipBudgeting);
        }
    }

    private void selectAudience(String audience) {
        selectedAudience = audience;
        updateAudiencePill();
    }

    private void updateAudiencePill() {
        if (btnAudiencePill == null || !isAdded()) return;
        String label = audienceLabel();
        btnAudiencePill.setText(label);
        btnAudiencePill.setContentDescription(label + " audience dropdown");
    }

    private void showAudiencePopup() {
        if (btnAudiencePill == null || !isAdded()) return;
        android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), btnAudiencePill);
        popup.getMenu().add(0, 1, 0, "Everyone");
        popup.getMenu().add(0, 2, 1, "Friends");
        popup.getMenu().add(0, 3, 2, "Only Me");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: selectAudience("Public"); break;
                case 2: selectAudience("Friends"); break;
                case 3: selectAudience("Private"); break;
            }
            return true;
        });
        popup.show();
    }

    private int dp(int value) {
        if (!isAdded()) return 0;
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void setMilestoneMode(boolean enabled) {
        milestoneMode = enabled;
        if (!enabled && milestonePreviewContainer != null) {
            milestonePreviewContainer.setVisibility(View.GONE);
            if (actionPhoto != null) actionPhoto.setVisibility(View.VISIBLE);
            if (actionMilestone != null) actionMilestone.setVisibility(View.VISIBLE);
        }
        updateCategoryTiles();
        updateCategoryDesign();
        updateSubmitState();
    }

    private void selectCategory(String category, String categoryKey, boolean milestone, int chipId) {
        selectedCategory = category;
        selectedCategoryKey = categoryKey;
        setMilestoneMode(milestone);
        if (chipGroupTopics != null && chipId != View.NO_ID) {
            chipGroupTopics.check(chipId);
        }
    }

    private void setupTopicHashtags() {
        if (chipGroupTopics == null) return;
        updateTopicChipStyle();
        chipGroupTopics.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (syncingHashtags) {
                updateTopicChipStyle();
                return;
            }
            selectedTopicHashtags.clear();
            for (Integer checkedId : checkedIds) {
                if (checkedId != null) {
                    selectedTopicHashtags.add(hashtagForTopic(checkedId));
                }
            }
            updateTopicChipStyle();
            syncHashtagsInEditor();
        });
    }

    private void updateTopicChipStyle() {
        if (chipGroupTopics == null || !isAdded()) return;
        styleTopicChip(R.id.chipBudgeting, "#FFF0C9", "#F2C15E", "#7A4D09");
        styleTopicChip(R.id.chipSaving, "#E2F5DA", "#A7D99B", "#31523B");
        styleTopicChip(R.id.chipDebt, "#FFE1E5", "#F2A9B4", "#7B3640");
        styleTopicChip(R.id.chipInvesting, "#E2ECFF", "#AFC5F7", "#294A88");
    }

    private void styleTopicChip(int chipId, String selectedBackgroundColor, String selectedStrokeColor, String selectedTextColor) {
        Chip chip = chipGroupTopics.findViewById(chipId);
        if (chip == null) return;
        boolean checked = chip.isChecked();
        int backgroundColor = checked
                ? Color.parseColor(selectedBackgroundColor)
                : ContextCompat.getColor(requireContext(), R.color.bg_main);
        int strokeColor = Color.parseColor(checked ? selectedStrokeColor : "#E8DCCB");
        int textColor = checked
                ? Color.parseColor(selectedTextColor)
                : ContextCompat.getColor(requireContext(), R.color.brand_primary);
        chip.setCheckedIconVisible(false);
        chip.setChipBackgroundColor(ColorStateList.valueOf(backgroundColor));
        chip.setChipStrokeColor(ColorStateList.valueOf(strokeColor));
        chip.setTextColor(textColor);
    }

    private String hashtagForTopic(int checkedId) {
        if (checkedId == R.id.chipSaving) return "#Saving";
        if (checkedId == R.id.chipDebt) return "#Debt";
        if (checkedId == R.id.chipInvesting) return "#Investing";
        return "#Budget";
    }

    private boolean isTopicHashtag(String tag) {
        return "#Budget".equals(tag)
                || "#Saving".equals(tag)
                || "#Debt".equals(tag)
                || "#Investing".equals(tag);
    }

    private void syncSelectedHashtagsFromEditor(String text) {
        if (chipGroupTopics == null || text == null) return;
        Matcher matcher = Pattern.compile("#[A-Za-z0-9_]+").matcher(text);
        boolean changed = false;
        while (matcher.find()) {
            String tag = matcher.group();
            if (isTopicHashtag(tag) && !selectedTopicHashtags.contains(tag)) {
                selectedTopicHashtags.add(tag);
                changed = true;
            }
        }
        if (changed) {
            syncingHashtags = true;
            try {
                setTopicChipChecked(R.id.chipBudgeting, selectedTopicHashtags.contains("#Budget"));
                setTopicChipChecked(R.id.chipSaving, selectedTopicHashtags.contains("#Saving"));
                setTopicChipChecked(R.id.chipDebt, selectedTopicHashtags.contains("#Debt"));
                setTopicChipChecked(R.id.chipInvesting, selectedTopicHashtags.contains("#Investing"));
            } finally {
                syncingHashtags = false;
            }
            updateTopicChipStyle();
        }
    }

    private void setTopicChipChecked(int chipId, boolean checked) {
        if (chipGroupTopics == null) return;
        Chip chip = chipGroupTopics.findViewById(chipId);
        if (chip != null && chip.isChecked() != checked) {
            chip.setChecked(checked);
        }
    }

    private void syncHashtagsInEditor() {
        if (editPostContent == null) return;
        syncingHashtags = true;
        try {
            String text = editPostContent.getText().toString();
            int cursor = editPostContent.getSelectionStart();

            Matcher matcher = Pattern.compile("(?<!\\S)#(?:Budget|Saving|Debt|Investing)(?![A-Za-z0-9_])\\s*").matcher(text);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String tag = matcher.group().trim();
                if (selectedTopicHashtags.contains(tag)) {
                    matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group()));
                } else {
                    matcher.appendReplacement(buffer, "");
                }
            }
            matcher.appendTail(buffer);
            text = buffer.toString().replaceAll("[ \\t]{2,}", " ").trim();

            for (String tag : selectedTopicHashtags) {
                if (!Pattern.compile("(?<!\\S)" + Pattern.quote(tag) + "(?![A-Za-z0-9_])").matcher(text).find()) {
                    if (text.length() > 0 && !text.endsWith(" ")) {
                        text += " ";
                    }
                    text += tag;
                }
            }

            editPostContent.setText(text);
            editPostContent.setSelection(Math.min(Math.max(cursor, 0), editPostContent.getText().length()));
        } finally {
            syncingHashtags = false;
        }
    }

    private String audienceLabel() {
        switch (selectedAudience) {
            case "Friends": return "Friends";
            case "Private": return "Only Me";
            default: return "Everyone";
        }
    }

    private void applyHashtagStyle(Editable editable) {
        if (applyingHashtagStyle || editable == null) return;
        applyingHashtagStyle = true;
        ForegroundColorSpan[] existing = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : existing) editable.removeSpan(span);

        Matcher matcher = Pattern.compile("#[A-Za-z0-9_]+").matcher(editable.toString());
        while (matcher.find()) {
            String hashtag = editable.toString().substring(matcher.start(), matcher.end());
            editable.setSpan(new ForegroundColorSpan(colorForHashtag(hashtag)),
                    matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        applyingHashtagStyle = false;
    }

    private int colorForHashtag(String hashtag) {
        String lower = hashtag == null ? "" : hashtag.toLowerCase(Locale.US);
        if (lower.equals("#budget") || lower.equals("#budgeting")) {
            return Color.parseColor("#D9A43A");
        }
        if (lower.equals("#saving") || lower.equals("#savings") || lower.equals("#save")) {
            return Color.parseColor("#6FBF73");
        }
        if (lower.equals("#debt") || lower.equals("#debtfree")) {
            return Color.parseColor("#E7808E");
        }
        if (lower.equals("#investing") || lower.equals("#investment") || lower.equals("#invest")) {
            return Color.parseColor("#7FA2F2");
        }
        return Color.parseColor("#1A237E");
    }

    private void clearSelectedImage() {
        selectedImageUri = null;
        if (imgPostPreview != null) imgPostPreview.setImageDrawable(null);
        if (imagePreviewContainer != null) imagePreviewContainer.setVisibility(View.GONE);
        updateSubmitState();
    }

    private void submitPost() {
        if (editPostContent == null || !isAdded()) return;
        String content = editPostContent.getText().toString().trim();
        
        if (!selectedTopicHashtags.isEmpty()) {
            StringBuilder sb = new StringBuilder(content);
            for (String tag : selectedTopicHashtags) {
                if (!content.contains(tag)) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(tag);
                }
            }
            content = sb.toString().trim();
        }

        String milestoneData = (milestoneMode && generatedMilestoneJson != null) ? generatedMilestoneJson : null;
        String type = milestoneData != null ? "MILESTONE_POST" : "USER_POST";
        String audienceParam = "FRIENDS";
        if ("Public".equals(selectedAudience)) audienceParam = "PUBLIC";
        else if ("Private".equals(selectedAudience) || "Only Me".equals(selectedAudience)) audienceParam = "PRIVATE";

        setPosting(true);
        final String finalContent = content;
        final String finalAudience = audienceParam;
        final String finalMilestoneData = milestoneData;
        final String finalType = type;

        if (selectedImageUri != null) {
            File imageFile = getFileFromUri(selectedImageUri);
            if (imageFile == null) { setPosting(false); return; }

            UploadNotificationHelper notif = new UploadNotificationHelper(requireContext());
            com.example.cashify.utils.CloudinaryHelper.uploadImage(imageFile, new com.example.cashify.utils.CloudinaryHelper.UploadCallback() {
                @Override public void onProgress(int percent) { notif.update(percent); }
                @Override public void onSuccess(String imageUrl) {
                    notif.done();
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> callBackendToCreatePost(finalContent, finalType, imageUrl, finalMilestoneData, finalAudience));
                    }
                }
                @Override public void onFailure(String error) {
                    notif.error();
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> setPosting(false));
                    }
                }
            });
        } else {
            callBackendToCreatePost(finalContent, finalType, "", finalMilestoneData, finalAudience);
        }
    }

    private void callBackendToCreatePost(String content, String type, String imageUrl, String milestoneData, String audienceParam) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { setPosting(false); return; }

        user.getIdToken(true).addOnSuccessListener(getTokenResult -> {
            String token = "Bearer " + getTokenResult.getToken();
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            ApiService.CreatePostRequest request = new ApiService.CreatePostRequest(content, type, imageUrl, milestoneData, audienceParam);

            apiService.createPost(token, request).enqueue(new retrofit2.Callback<Object>() {
                @Override
                public void onResponse(@NonNull retrofit2.Call<Object> call, @NonNull retrofit2.Response<Object> response) {
                    if (!isAdded() || getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        setPosting(false);
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Post created!", Toast.LENGTH_SHORT).show();
                            resetComposer();
                            navigateBack();
                        } else {
                            Toast.makeText(getContext(), "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                @Override public void onFailure(@NonNull retrofit2.Call<Object> call, @NonNull Throwable t) {
                    if (!isAdded() || getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        setPosting(false);
                        Toast.makeText(getContext(), "Network error.", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
    }

    private void setPosting(boolean posting) {
        if (progressPosting != null) progressPosting.setVisibility(posting ? View.VISIBLE : View.GONE);
        if (btnSubmitPost != null) btnSubmitPost.setEnabled(!posting);
        if (editPostContent != null) editPostContent.setEnabled(!posting);
    }

    private void updateSubmitState() {
        if (editPostContent == null || btnSubmitPost == null) return;
        boolean hasContent = editPostContent.getText().toString().trim().length() > 0;
        boolean hasMilestone = milestoneMode && generatedMilestoneJson != null;
        boolean canSubmit = hasContent || selectedImageUri != null || hasMilestone;
        btnSubmitPost.setEnabled(canSubmit);
        btnSubmitPost.setAlpha(canSubmit ? 1f : 0.55f);
    }

    private void navigateBack() {
        if (!isAdded()) return;
        NavController navController = NavHostFragment.findNavController(this);
        if (!navController.popBackStack()) {
            if (getActivity() != null) getActivity().getOnBackPressedDispatcher().onBackPressed();
        }
    }

    private void bindCurrentUserProfile(DocumentSnapshot doc, TextView txtComposerName) {
        if (doc == null || !doc.exists()) return;
        String displayName = cleanDisplayName(doc.getString("displayName"));
        String username = cleanDisplayName(doc.getString("username"));
        if (txtComposerName != null) {
            txtComposerName.setText(!"User".equals(displayName) ? displayName : username);
        }
        ImageHelper.loadAvatar(doc.getString("avatarUrl"), imgComposerAvatar, firstNonEmpty(displayName, username, doc.getId()));
    }

    private String cleanDisplayName(String value) {
        if (value == null || value.trim().isEmpty() || value.contains("@")) return "User";
        return value.trim();
    }

    private String firstNonEmpty(String... values) {
        for (String v : values) if (v != null && !v.trim().isEmpty()) return v.trim();
        return "";
    }

    private long getFileSizeFromUri(Uri uri) {
        if (!isAdded()) return 0;
        try (android.database.Cursor cursor = requireContext().getContentResolver().query(uri, new String[]{android.provider.OpenableColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getLong(0);
        } catch (Exception e) { return 0; }
        return 0;
    }

    private File getFileFromUri(Uri uri) {
        if (!isAdded()) return null;
        try {
            java.io.InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            File tempFile = new File(requireContext().getCacheDir(), "upload_" + System.currentTimeMillis() + ".jpg");
            java.io.OutputStream outputStream = new java.io.FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) outputStream.write(buffer, 0, length);
            outputStream.close(); inputStream.close();
            return tempFile;
        } catch (Exception e) { return null; }
    }

    private void resetComposer() {
        if (editPostContent != null) editPostContent.setText("");
        clearSelectedImage();
        generatedMilestoneJson = null;
        if (milestoneMode) setMilestoneMode(false);
        selectedTopicHashtags.clear();
        if (chipGroupTopics != null) chipGroupTopics.clearCheck();
    }

    private void updateCategoryTiles() {
        if (!isAdded()) return;
        updateCategoryTile(actionMilestone, "Milestone", R.drawable.bg_category_tile_milestone, "#4B2A11", "#8A6237");
        updateCategoryTile(actionThoughts, "Thoughts", R.drawable.bg_category_tile_thoughts, "#5A3422", "#B07D62");
        updateCategoryTile(actionAnalysis, "Analysis", R.drawable.bg_category_tile_analysis, "#3E260F", "#8A6237");
        updateCategoryTile(actionShare, "Share", R.drawable.bg_category_tile_share, "#5C3920", "#D4A373");
    }

    private void updateCategoryDesign() {
        if (panelCategoryMode == null || !isAdded()) return;

        int panelBg, iconRes;
        String kicker, title, description, prompt, composerHint, submitText;

        switch (selectedCategoryKey) {
            case "milestone":
                panelBg = R.drawable.bg_mode_panel_milestone;
                iconRes = R.drawable.ic_feed_trophy;
                kicker = "CELEBRATE A MILESTONE";
                title = "Grow your story";
                description = "Turn a small financial win into a memorable milestone for your journey.";
                prompt = "Prompt: What goal did you just reach, and why are you proud of it?";
                composerHint = "Which milestone did you reach?";
                submitText = "Post Milestone";
                break;
            case "analysis":
                panelBg = R.drawable.bg_mode_panel_analysis;
                iconRes = R.drawable.ic_cozy_chart;
                kicker = "FINANCIAL DEEP DIVE";
                title = "Analyze Progress";
                description = "For data-backed observations, spending lessons, or financial patterns you just noticed.";
                prompt = "Prompt: What changed most in your money habits this month?";
                composerHint = "What would you like to analyze in your finances?";
                submitText = "Post Analysis";
                break;
            case "share":
                panelBg = R.drawable.bg_mode_panel_share;
                iconRes = R.drawable.ic_share;
                kicker = "SHARE A FINANCIAL TIP";
                title = "Grow together";
                description = "A small tip, real experience, or story that can help the community.";
                prompt = "Prompt: What money tip do you wish you had learned earlier?";
                composerHint = "Which financial tip would you like to share?";
                submitText = "Post Share";
                break;
            case "thoughts":
            default:
                panelBg = R.drawable.bg_mode_panel_thoughts;
                iconRes = R.drawable.ic_cozy_notebook;
                kicker = "DIGITAL JOURNAL";
                title = "Share a Thought";
                description = "A gentle space to share what you learned on your financial journey today.";
                prompt = "Prompt: What did you realize today about how you spend money?";
                composerHint = "What are you thinking about money today?";
                submitText = "Post Thought";
                break;
        }

        panelCategoryMode.setBackgroundResource(panelBg);
        if (imgModeIcon != null) imgModeIcon.setImageResource(iconRes);
        if (txtModeKicker != null) txtModeKicker.setText(kicker);
        if (txtModeTitle != null) txtModeTitle.setText(title);
        if (txtModeDescription != null) txtModeDescription.setText(description);
        if (txtModePrompt != null) txtModePrompt.setText(prompt);
        if (editPostContent != null) editPostContent.setHint(composerHint);
        if (txtComposerHint != null) txtComposerHint.setText(prompt);
        if (btnSubmitPost != null) btnSubmitPost.setText(submitText);
    }

    private void updateCategoryTile(TextView tile, String category, int normalBg,
                                    String normalTextColor, String normalIconColor) {
        if (tile == null) return;
        boolean selected = category.equals(selectedCategory);
        tile.setBackgroundResource(selected ? R.drawable.bg_category_tile_selected : normalBg);
        tile.setText(selected ? "✓ " + category : category);
        tile.setTextColor(Color.parseColor(selected ? "#FFFFFFFF" : normalTextColor));
        TextViewCompat.setCompoundDrawableTintList(tile, ColorStateList.valueOf(
                Color.parseColor(selected ? "#FFFFFFFF" : normalIconColor)));
    }

}
