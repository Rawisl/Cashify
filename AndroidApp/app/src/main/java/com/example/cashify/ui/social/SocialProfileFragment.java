package com.example.cashify.ui.social;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.imageview.ShapeableImageView;

/**
 * SocialProfileFragment — Tab "Hồ sơ cá nhân" (MyWall).
 *
 * Nửa trên : Avatar, tên hiển thị, số bạn bè, số cúp chiến tích.
 * Nửa dưới : RecyclerView chỉ load bài của chính user hoặc
 *             bài do hệ thống auto-sinh cho user đó.
 */
public class SocialProfileFragment extends Fragment {

    // ── Views ─────────────────────────────────────────────────────
    private ShapeableImageView imgAvatar;
    private TextView tvDisplayName;
    private TextView tvFriendCount;
    private TextView tvTrophyCount;
    private RecyclerView rvMyPosts;
    private View layoutEmptyState;

    // ── ViewModel ─────────────────────────────────────────────────
    private SocialViewModel socialViewModel;

    // ── Adapter (khai báo trước, implement sau) ───────────────────
    // private MyPostsAdapter myPostsAdapter;

    // ─────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_social_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        initViewModel();
        initToolbar(view);
        setupRecyclerView();
        observeViewModel();
    }

    // ── Bind Views ────────────────────────────────────────────────

    private void bindViews(View view) {
        imgAvatar       = view.findViewById(R.id.imgAvatar);
        tvDisplayName   = view.findViewById(R.id.tvDisplayName);
        tvFriendCount   = view.findViewById(R.id.tvFriendCount);
        tvTrophyCount   = view.findViewById(R.id.tvTrophyCount);
        rvMyPosts       = view.findViewById(R.id.rvMyPosts);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
    }

    // ── ViewModel ─────────────────────────────────────────────────

    private void initViewModel() {
        socialViewModel = new ViewModelProvider(requireActivity())
                .get(SocialViewModel.class);

        com.google.firebase.auth.FirebaseUser currentUser =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            socialViewModel.loadProfile(currentUser.getUid());
        }
    }

    // ── Toolbar / Sidebar ─────────────────────────────────────────

    private void initToolbar(View view) {
        MaterialToolbar toolbar = view.findViewById(R.id.toolbarSocialProfile);
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                androidx.drawerlayout.widget.DrawerLayout drawer =
                        getActivity().findViewById(R.id.drawerLayout);
                if (drawer != null) {
                    drawer.openDrawer(androidx.core.view.GravityCompat.START);
                }
            }
        });
    }

    // ── RecyclerView ──────────────────────────────────────────────

    private void setupRecyclerView() {
        rvMyPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMyPosts.setHasFixedSize(false);   // wrap_content bên trong NestedScrollView

        // TODO: Khởi tạo adapter và gán vào RecyclerView
        // myPostsAdapter = new MyPostsAdapter(post -> { /* onClick */ });
        // rvMyPosts.setAdapter(myPostsAdapter);
    }

    // ── Observe ───────────────────────────────────────────────────

    private void observeViewModel() {

        // Tên + username + avatar
        socialViewModel.getProfile().observe(getViewLifecycleOwner(), doc -> {
            if (doc == null) return;

            String displayName = doc.getString("displayName");
            String username    = doc.getString("username");
            String avatarUrl   = doc.getString("avatarUrl");

            tvDisplayName.setText(displayName != null ? displayName : "Người dùng");

            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                com.example.cashify.utils.ImageHelper.loadAvatar(avatarUrl, imgAvatar);
            }
        });

        // Số bạn bè
        socialViewModel.getFriendCount().observe(getViewLifecycleOwner(), count -> {
            tvFriendCount.setText(String.valueOf(count));
        });
    }

    // ── Helper ────────────────────────────────────────────────────

    /**
     * Ẩn/hiện empty state, đảo ngược RecyclerView.
     */
    private void showEmptyState(boolean show) {
        layoutEmptyState.setVisibility(show ? View.VISIBLE : View.GONE);
        rvMyPosts.setVisibility(show ? View.GONE : View.VISIBLE);
    }
}