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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.cashify.R;
import com.example.cashify.ui.common.BaseFragment;
import com.example.cashify.ui.main.MainActivity;
import com.example.cashify.utils.ImageHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;

public class SocialNewsfeedFragment extends BaseFragment {

    private SocialNewsfeedViewModel viewModel;

    private RecyclerView rvFeed;
    private SocialNewsfeedAdapter feedAdapter;
    private View layoutFeedEmpty, layoutFeedEnd, layoutFeedSkeleton;
    private TextView layoutFeedError;
    private ProgressBar progressFeedMore;
    private SwipeRefreshLayout swipeRefreshNewsfeed;
    private NestedScrollView scrollNewsfeed;
    private ImageView createAvatar;

    public static boolean needRefreshFeed = false;
    public static String syncedPostId = null;

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
        if (user != null) {
            viewModel.loadProfile(user.getUid());
            viewModel.loadTopUsersForDecoration();
        }

        initViews(view);
        setupObservers();

        viewModel.refreshFeed();
    }

    @Override
    public void onResume() {
        super.onResume();

        
        if (syncedPostId != null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                user.getIdToken(true).addOnSuccessListener(result -> {
                    String token = "Bearer " + result.getToken();
                    viewModel.syncSinglePost(syncedPostId, token);
                    syncedPostId = null;
                });
            } else {
                syncedPostId = null;
            }
        }

        if (needRefreshFeed) {
            viewModel.refreshFeed();
            needRefreshFeed = false;
        }
    }

    private void initViews(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbarSocialNewsfeed);
        View bellIcon = view.findViewById(R.id.imgBellIcon);
        TextView bellBadge = view.findViewById(R.id.tvBellBadge);
        View createPostPrompt = view.findViewById(R.id.cardCreatePostPrompt);
        View createPostPromptButton = view.findViewById(R.id.btnCreatePostPrompt);
        createAvatar = view.findViewById(R.id.imgCreatePromptAvatar);

        rvFeed = view.findViewById(R.id.rvNewsfeed);
        layoutFeedEmpty = view.findViewById(R.id.layoutNewsfeedEmpty);
        layoutFeedEnd = view.findViewById(R.id.layoutNewsfeedEnd);
        layoutFeedSkeleton = view.findViewById(R.id.layoutNewsfeedSkeleton);
        layoutFeedError = view.findViewById(R.id.layoutNewsfeedError);
        progressFeedMore = view.findViewById(R.id.progressNewsfeedMore);
        swipeRefreshNewsfeed = view.findViewById(R.id.swipeRefreshNewsfeed);
        scrollNewsfeed = view.findViewById(R.id.scrollNewsfeed);

        if (rvFeed != null) {
            // master: SocialNewsfeedAdapter với listener tách biệt
            feedAdapter = new SocialNewsfeedAdapter(
                    item -> {
                        Intent intent = new Intent(requireContext(), PostDetailActivity.class);
                        intent.putExtra(PostDetailActivity.EXTRA_POST_ID, item.getId());
                        startActivity(intent);
                        syncedPostId = item.getId(); // master: đánh dấu để sync khi quay lại
                    },
                    this::showPostBottomSheet  // ui-consistency: BottomSheet thay vì PopupMenu
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

        
            feedAdapter.setOnAvatarClickListener(this::openMemberProfile);

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

        setupCommonHeader(toolbar, bellIcon, bellBadge);

        createPostPrompt.setOnClickListener(v -> runPressAnimation(createPostPromptButton != null ? createPostPromptButton : v, this::openCreatePost));
        if (createPostPromptButton != null) createPostPromptButton.setOnClickListener(v -> runPressAnimation(v, this::openCreatePost));
    }

    private void setupObservers() {
        // master: MVVM observers
        viewModel.getFeedItems().observe(getViewLifecycleOwner(), items -> {
            feedAdapter.submitList(new ArrayList<>(items));
        });

        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (swipeRefreshNewsfeed != null) swipeRefreshNewsfeed.setRefreshing(isLoading);
            if (isLoading && feedAdapter.getCurrentList().isEmpty()) {
                showFeedSkeleton(true);
            } else {
                showFeedSkeleton(false);
            }
        });

        viewModel.getIsLoadingMore().observe(getViewLifecycleOwner(), isMore -> {
            if (progressFeedMore != null) progressFeedMore.setVisibility(isMore ? View.VISIBLE : View.GONE);
        });

        viewModel.getIsFeedEmpty().observe(getViewLifecycleOwner(), isEmpty -> {
            showFeedEmpty(isEmpty);
        });

        viewModel.getIsLastPage().observe(getViewLifecycleOwner(), isLast -> {
            boolean showEnd = isLast
                    && !feedAdapter.getCurrentList().isEmpty()
                    && !Boolean.TRUE.equals(viewModel.isLoading.getValue());
            showFeedEnd(showEnd);
        });

        viewModel.getIsDeleteSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(requireContext(), "Post deleted", Toast.LENGTH_SHORT).show();
                viewModel.resetDeleteStatus();
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getProfile().observe(getViewLifecycleOwner(), doc -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                if (createAvatar != null) {
                    ImageHelper.loadAvatar(user.getPhotoUrl(), createAvatar, user.getDisplayName());
                }
            }
        });

        // ui-consistency: trang trí top users
        viewModel.getTopUsers().observe(getViewLifecycleOwner(), snapshots -> {
            if (snapshots == null || snapshots.isEmpty()) return;
            int index = 1;
            for (DocumentSnapshot doc : snapshots) {
                bindPostProfile(getView(), index, doc);
                index++;
                if (index > 5) break;
            }
        });
    }

    
    private void showFeedEmpty(boolean show) {
        if (show) showFeedSkeleton(false);
        if (layoutFeedEmpty != null)
            layoutFeedEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        if (rvFeed != null)
            rvFeed.setVisibility(show ? View.GONE : View.VISIBLE);
        if (show) showFeedEnd(false);
    }

    private void showFeedError(boolean show) {
        if (layoutFeedError != null) {
            layoutFeedError.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show) {
            showFeedSkeleton(false);
            showFeedEmpty(false);
            showFeedEnd(false);
            if (rvFeed != null) rvFeed.setVisibility(View.GONE);
        }
    }

    private void showFeedEnd(boolean show) {
        if (layoutFeedEnd != null) {
            layoutFeedEnd.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showFeedSkeleton(boolean show) {
        if (layoutFeedSkeleton != null) {
            layoutFeedSkeleton.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show) {
            if (rvFeed != null) rvFeed.setVisibility(View.GONE);
            if (layoutFeedEmpty != null) layoutFeedEmpty.setVisibility(View.GONE);
            if (layoutFeedError != null) layoutFeedError.setVisibility(View.GONE);
            showFeedEnd(false);
        }
    }

   
    private void openMemberProfile(String userId) {
        if (!isAdded() || userId == null || userId.trim().isEmpty()) return;
        Bundle args = new Bundle();
        args.putString("USER_ID", userId);
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.action_newsfeed_to_other_profile, args);
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
        view.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100)
                .withEndAction(() -> {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(250)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(2f))
                            .withEndAction(action).start();
                }).start();
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
        View btnHidePost = sheetView.findViewById(R.id.btnHideComment);
        View btnReportPost = sheetView.findViewById(R.id.btnReportPost);

        if (canEditOrDelete) {
            if (btnEditPost != null) btnEditPost.setVisibility(View.VISIBLE);
            if (btnDeletePost != null) btnDeletePost.setVisibility(View.VISIBLE);
            if (btnHidePost != null) btnHidePost.setVisibility(View.GONE);
            if (btnReportPost != null) btnReportPost.setVisibility(View.GONE);

            if (btnDeletePost != null) {
                btnDeletePost.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (user == null) return;
                    user.getIdToken(true).addOnSuccessListener(result -> {
                        viewModel.deletePost(item.getId(), "Bearer " + result.getToken());
                    });
                });
            }

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
            if (btnDeletePost != null) btnDeletePost.setVisibility(View.GONE);
            if (btnHidePost != null) btnHidePost.setVisibility(View.VISIBLE);
            if (btnReportPost != null) btnReportPost.setVisibility(View.VISIBLE);

            if (btnHidePost != null) {
                btnHidePost.setOnClickListener(v -> {
                    dialog.dismiss();
                    //Gọi ViewModel để ẩn Post trên máy khách và báo lên server
                    if (user == null) return;
                    user.getIdToken(true).addOnSuccessListener(result -> {
                        viewModel.hidePost(item.getId(), "Bearer " + result.getToken());
                        Toast.makeText(requireContext(), "Post hidden", Toast.LENGTH_SHORT).show();
                    });
                });
            }

            if (btnReportPost != null) {
                btnReportPost.setOnClickListener(v -> {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "Post reported", Toast.LENGTH_SHORT).show();
                });
            }
        }

        sheetView.findViewById(R.id.btnCancelComment).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void bindPostProfile(View root, int index, DocumentSnapshot doc) {
        if (root == null) return;
        TextView nameView = root.findViewById(getResources().getIdentifier("txtPostName" + index, "id", requireContext().getPackageName()));
        ImageView avatarView = root.findViewById(getResources().getIdentifier("imgPostAvatar" + index, "id", requireContext().getPackageName()));
        String name = doc.getString("displayName");
        if (name == null || name.isEmpty()) name = doc.getString("username");
        if (name == null) name = "Cashify User";
        if (nameView != null) nameView.setText(name);
        if (avatarView != null) ImageHelper.loadAvatar(doc.getString("avatarUrl"), avatarView, name);
    }
}