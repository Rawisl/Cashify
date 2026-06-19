package com.example.cashify.ui.social;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.cashify.R;

import com.example.cashify.ui.main.MainActivity;
import com.example.cashify.utils.ImageHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class SocialNewsfeedFragment extends Fragment {

    private SocialNewsfeedViewModel viewModel;

    private RecyclerView rvFeed;
    private SocialNewsfeedAdapter feedAdapter; // Dùng đúng tên Adapter sếp đã đổi
    private View layoutFeedEmpty, layoutFeedEnd, layoutFeedSkeleton;
    private TextView layoutFeedError;
    private ProgressBar progressFeedMore;
    private SwipeRefreshLayout swipeRefreshNewsfeed;
    private NestedScrollView scrollNewsfeed;

    public static boolean needRefreshFeed = false;
    public static String syncedPostId = null; // CHỐT CHẶN: Lưu ID bài viết cần update Like/Comment

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_social_newsfeed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(SocialNewsfeedViewModel.class);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) viewModel.loadProfile(user.getUid());

        initViews(view);
        setupObservers();

        viewModel.refreshFeed();
    }

    @Override
    public void onResume() {
        super.onResume();
        // NẾU VỪA TỪ MÀN DETAIL RA -> CHỈ ĐỒNG BỘ 1 BÀI VÀ GIỮ NGUYÊN VỊ TRÍ CUỘN
        if (syncedPostId != null) {
            viewModel.syncSinglePost(syncedPostId);
            syncedPostId = null;
        }

        if (needRefreshFeed) {
            viewModel.refreshFeed();
            needRefreshFeed = false;
        }
    }

    private void initViews(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbarSocialNewsfeed);
        View createPostPrompt = view.findViewById(R.id.cardCreatePostPrompt);
        View createPostPromptButton = view.findViewById(R.id.btnCreatePostPrompt);

        rvFeed = view.findViewById(R.id.rvNewsfeed);
        layoutFeedEmpty = view.findViewById(R.id.layoutNewsfeedEmpty);
        layoutFeedEnd = view.findViewById(R.id.layoutNewsfeedEnd);
        layoutFeedSkeleton = view.findViewById(R.id.layoutNewsfeedSkeleton);
        layoutFeedError = view.findViewById(R.id.layoutNewsfeedError);
        progressFeedMore = view.findViewById(R.id.progressNewsfeedMore);
        swipeRefreshNewsfeed = view.findViewById(R.id.swipeRefreshNewsfeed);
        scrollNewsfeed = view.findViewById(R.id.scrollNewsfeed);

        if (rvFeed != null) {
            feedAdapter = new SocialNewsfeedAdapter(
                    item -> {
                        Intent intent = new Intent(requireContext(), PostDetailActivity.class);
                        intent.putExtra(PostDetailActivity.EXTRA_POST_ID, item.getId());
                        startActivity(intent);
                        // Đánh dấu ID khi bấm vào xem chi tiết
                        syncedPostId = item.getId();
                    },
                    this::showPostBottomSheet
            );

            feedAdapter.setOnLikeClickListener((postId, isLiked, callback) -> {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    user.getIdToken(true).addOnSuccessListener(result -> {
                        viewModel.toggleLike(postId, "Bearer " + result.getToken(), isLiked);
                        callback.onResult(true);
                    }).addOnFailureListener(e -> callback.onResult(false));
                } else callback.onResult(false);
            });

            rvFeed.setLayoutManager(new LinearLayoutManager(requireContext()));
            rvFeed.setAdapter(feedAdapter);
            rvFeed.setHasFixedSize(false);
        }

        if (swipeRefreshNewsfeed != null) {
            swipeRefreshNewsfeed.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.brand_primary));
            swipeRefreshNewsfeed.setOnRefreshListener(() -> viewModel.refreshFeed());
        }

        if (scrollNewsfeed != null) {
            scrollNewsfeed.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                    (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                        View child = v.getChildAt(0);
                        if (child == null) return;
                        int distanceToBottom = child.getMeasuredHeight() - (scrollY + v.getHeight());
                        if (distanceToBottom <= Math.round(320 * getResources().getDisplayMetrics().density)) {
                            viewModel.loadNextPage();
                        }
                    });
        }

        if (layoutFeedError != null) layoutFeedError.setOnClickListener(v -> viewModel.refreshFeed());

        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                DrawerLayout drawer = getActivity().findViewById(R.id.drawerLayout);
                if (drawer != null) drawer.openDrawer(GravityCompat.START);
            }
        });

        createPostPrompt.setOnClickListener(v -> runPressAnimation(createPostPromptButton != null ? createPostPromptButton : v, this::openCreatePost));
        if (createPostPromptButton != null) createPostPromptButton.setOnClickListener(v -> runPressAnimation(v, this::openCreatePost));

        setupProfileSurfaces(view);
    }

    private void setupObservers() {
        viewModel.getFeedItems().observe(getViewLifecycleOwner(), items -> {
            feedAdapter.submitList(new ArrayList<>(items));
        });

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (swipeRefreshNewsfeed != null) swipeRefreshNewsfeed.setRefreshing(isLoading);
            if (isLoading && feedAdapter.getCurrentList().isEmpty()) {
                layoutFeedSkeleton.setVisibility(View.VISIBLE);
                rvFeed.setVisibility(View.GONE);
                layoutFeedEmpty.setVisibility(View.GONE);
                layoutFeedError.setVisibility(View.GONE);
            } else {
                layoutFeedSkeleton.setVisibility(View.GONE);
                rvFeed.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getIsLoadingMore().observe(getViewLifecycleOwner(), isMore -> {
            if (progressFeedMore != null) progressFeedMore.setVisibility(isMore ? View.VISIBLE : View.GONE);
        });

        viewModel.getIsFeedEmpty().observe(getViewLifecycleOwner(), isEmpty -> {
            layoutFeedEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            if (isEmpty) {
                rvFeed.setVisibility(View.GONE);
                layoutFeedSkeleton.setVisibility(View.GONE);
            }
        });

        viewModel.getIsLastPage().observe(getViewLifecycleOwner(), isLast -> {
            boolean showEnd = isLast && !feedAdapter.getCurrentList().isEmpty() && !Boolean.TRUE.equals(viewModel.isLoading.getValue());
            if (layoutFeedEnd != null) layoutFeedEnd.setVisibility(showEnd ? View.VISIBLE : View.GONE);
        });

        viewModel.getIsDeleteSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show();
                ((MutableLiveData<Boolean>) viewModel.getIsDeleteSuccess()).setValue(false);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void openCreatePost() {
        if (getActivity() == null) return;
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).openCreatePostScreen();
            return;
        }
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        if (navController.getGraph().findNode(R.id.nav_post_feed) != null) {
            navController.navigate(R.id.nav_post_feed);
        }
    }

    private void runPressAnimation(View view, Runnable action) {
        view.animate().scaleX(0.98f).scaleY(0.98f).setDuration(70)
                .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(90).withEndAction(action).start()).start();
    }

    private void showPostBottomSheet(FeedItem item) {
        if (getActivity() == null) return;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_option, null);

        sheetView.findViewById(R.id.btnEditComment).setVisibility(View.GONE);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = user != null ? user.getUid() : "";
        DocumentSnapshot userProfile = viewModel.getProfile().getValue();
        boolean isAdmin = userProfile != null && "ADMIN".equals(userProfile.getString("role"));
        boolean canEditOrDelete = (!currentUserId.isEmpty() && currentUserId.equals(item.getUserId())) || isAdmin;

        View btnEditPost = sheetView.findViewById(R.id.btnEditPost);
        View btnDeletePost = sheetView.findViewById(R.id.btnDeleteComment);

        if (canEditOrDelete) {
            if (btnEditPost != null) btnEditPost.setVisibility(View.VISIBLE);
            btnDeletePost.setVisibility(View.VISIBLE);
            sheetView.findViewById(R.id.btnHideComment).setVisibility(View.GONE);
            sheetView.findViewById(R.id.btnReportPost).setVisibility(View.GONE);

            btnDeletePost.setOnClickListener(v -> {
                dialog.dismiss();
                if (user == null) return;
                user.getIdToken(true).addOnSuccessListener(result -> {
                    viewModel.deletePost(item.getId(), "Bearer " + result.getToken());
                });
            });

            if (btnEditPost != null) {
                btnEditPost.setOnClickListener(v -> {
                    dialog.dismiss();
                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    intent.putExtra("ACTION_EDIT_POST", true);
                    intent.putExtra("edit_post_id", item.getId());
                    if (item instanceof FeedItem.NormalPost) {
                        intent.putExtra("edit_post_title", ((FeedItem.NormalPost) item).title);
                        intent.putExtra("edit_post_content", ((FeedItem.NormalPost) item).description);
                        intent.putExtra("edit_post_image", ((FeedItem.NormalPost) item).imageUrl);
                    } else if (item instanceof FeedItem.MilestonePost) {
                        intent.putExtra("edit_post_content", ((FeedItem.MilestonePost) item).description);
                        intent.putExtra("edit_milestone_data", ((FeedItem.MilestonePost) item).milestoneJson);
                    }
                    startActivity(intent);
                });
            }
        } else {
            if (btnEditPost != null) btnEditPost.setVisibility(View.GONE);
            btnDeletePost.setVisibility(View.GONE);
            sheetView.findViewById(R.id.btnHideComment).setVisibility(View.VISIBLE);
            sheetView.findViewById(R.id.btnReportPost).setVisibility(View.VISIBLE);

            sheetView.findViewById(R.id.btnHideComment).setOnClickListener(v -> {
                dialog.dismiss();
                Toast.makeText(requireContext(), "Post hidden", Toast.LENGTH_SHORT).show();
            });
            sheetView.findViewById(R.id.btnReportPost).setOnClickListener(v -> {
                dialog.dismiss();
                Toast.makeText(requireContext(), "Post reported", Toast.LENGTH_SHORT).show();
            });
        }

        sheetView.findViewById(R.id.btnCancelComment).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(sheetView);
        dialog.show();
    }

    // Decorate Header
    private void setupProfileSurfaces(View root) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        ImageView createAvatar = root.findViewById(R.id.imgCreatePromptAvatar);
        if (user != null) ImageHelper.loadAvatar(user.getPhotoUrl(), createAvatar, user.getDisplayName());

        FirebaseFirestore.getInstance().collection("users").limit(5).get()
                .addOnSuccessListener(snapshot -> {
                    if (!isAdded() || snapshot == null || snapshot.isEmpty()) return;
                    int index = 1;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        bindPostProfile(root, index, doc);
                        index++;
                        if (index > 5) break;
                    }
                });
    }

    private void bindPostProfile(View root, int index, DocumentSnapshot doc) {
        TextView nameView = root.findViewById(getResources().getIdentifier("txtPostName" + index, "id", requireContext().getPackageName()));
        ImageView avatarView = root.findViewById(getResources().getIdentifier("imgPostAvatar" + index, "id", requireContext().getPackageName()));
        String name = doc.getString("displayName");
        if (name == null || name.isEmpty()) name = doc.getString("username");
        if (name == null) name = "Cashify User";
        if (nameView != null) nameView.setText(name);
        if (avatarView != null) ImageHelper.loadAvatar(doc.getString("avatarUrl"), avatarView, name);
    }
}