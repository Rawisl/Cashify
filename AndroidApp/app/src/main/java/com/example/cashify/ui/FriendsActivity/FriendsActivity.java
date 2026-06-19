package com.example.cashify.ui.FriendsActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.User;
import com.example.cashify.ui.main.BaseActivity;
import com.example.cashify.ui.main.MainActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class FriendsActivity extends BaseActivity {

    private FriendsViewModel socialViewModel;

    // UI Components
    private FloatingActionButton fabAddFriend;
    private RecyclerView rvFriends;
    private RecyclerView rvSuggestions;
    private TextView tvFriendsEmpty;
    private TextView tvSuggestionsEmpty;
    private TextView tvSuggestionCount;
    private TextView tvRequestsSummary;

    // Adapters
    private FriendAdapter friendAdapter;
    private SuggestionAdapter suggestionAdapter;

    // Search optimization
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);
        setupBaseSidebar();

        socialViewModel = new ViewModelProvider(this).get(FriendsViewModel.class);

        bindViews();
        setupAdapters();
        setupActions();
        setupSearch();
        observeViewModel();

        socialViewModel.fetchOnlyFriends();
    }

    private void bindViews() {
        fabAddFriend = findViewById(R.id.fabAddFriend);
        rvFriends = findViewById(R.id.rvFriends);
        rvSuggestions = findViewById(R.id.rvSuggestions);
        tvFriendsEmpty = findViewById(R.id.tvFriendsEmpty);
        tvSuggestionsEmpty = findViewById(R.id.tvSuggestionsEmpty);
        tvSuggestionCount = findViewById(R.id.tvSuggestionCount);
        tvRequestsSummary = findViewById(R.id.tvRequestsSummary);
    }

    private void setupAdapters() {
        // Friend List Adapter Setup
        FriendAdapter.ActionListener friendListener = new FriendAdapter.ActionListener() {
            @Override
            public void onAddFriend(User user) {
                socialViewModel.sendFriendRequest(user);
            }

            @Override
            public void onCancelRequest(User user) {
                socialViewModel.cancelFriendRequest(user);
            }

            @Override
            public void onAccept(User user) {
                socialViewModel.acceptFriendRequest(user);
            }

            @Override
            public void onDecline(User user) {
                socialViewModel.declineFriendRequest(user);
            }

            @Override
            public void onUnfriend(User user) {
                socialViewModel.unfriend(user);
            }

            @Override
            public void onMessage(User user) {
                openChat(user);
            }
        };

        // Initialize with empty list, delegate data management to the Adapter
        friendAdapter = new FriendAdapter(new ArrayList<>(), friendListener);
        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        rvFriends.setNestedScrollingEnabled(false);
        rvFriends.setAdapter(friendAdapter);

        // Suggestions List Adapter Setup
        SuggestionAdapter.ActionListener suggestionListener = new SuggestionAdapter.ActionListener() {
            @Override
            public void onAddFriend(User user) {
                socialViewModel.sendFriendRequest(user);
            }

            @Override
            public void onCancelRequest(User user) {
                socialViewModel.cancelFriendRequest(user);
            }

            @Override
            public void onSeeAll() {
                openSuggestedFriends();
            }
        };

        suggestionAdapter = new SuggestionAdapter(new ArrayList<>(), suggestionListener);
        rvSuggestions.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSuggestions.setAdapter(suggestionAdapter);
    }

    private void setupActions() {
        MaterialToolbar toolbarFriends = findViewById(R.id.toolbarFriends);
        View bellIcon = findViewById(R.id.imgBellIcon);
        TextView bellBadge = findViewById(R.id.tvBellBadge);

        setupCommonHeader(toolbarFriends, bellIcon, bellBadge);

        if (toolbarFriends != null) {
            toolbarFriends.setNavigationOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
            });
        }

        View btnMessages = findViewById(R.id.btnMessages);
        if (btnMessages != null) {
            btnMessages.setOnClickListener(v -> startActivity(new Intent(this, MessagesActivity.class)));
        }

        View cardRequests = findViewById(R.id.cardRequests);
        if (cardRequests != null) {
            cardRequests.setOnClickListener(v -> startActivity(new Intent(this, RequestsActivity.class)));
        }

        if (fabAddFriend != null) {
            fabAddFriend.setOnClickListener(v -> showAddFriendDialog());
        }
    }

    private void setupSearch() {
        EditText edtSearch = findViewById(R.id.edtSearch);
        if (edtSearch == null) return;

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Debounce search input (waits 300ms after user stops typing to trigger search)
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);

                String query = s == null ? "" : s.toString().trim();
                searchRunnable = () -> {
                    socialViewModel.filterFriendsLocal(query);
                    socialViewModel.filterSuggestionsLocal(query);
                };

                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void observeViewModel() {
        socialViewModel.friendList.observe(this, users -> {
            boolean isEmpty = (users == null || users.isEmpty());
            friendAdapter.updateList(users);

            tvFriendsEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            rvFriends.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });

        socialViewModel.suggestionList.observe(this, users -> {
            int fullCount = users == null ? 0 : users.size();

            // Limit preview to 5 items safely
            suggestionAdapter.updateList(users != null ? users.subList(0, Math.min(5, fullCount)) : new ArrayList<>());
            suggestionAdapter.setShowSeeAllCard(fullCount > 5);

            tvSuggestionCount.setText(getString(R.string.friend_count_format, fullCount));
            tvSuggestionsEmpty.setVisibility(fullCount == 0 ? View.VISIBLE : View.GONE);
            rvSuggestions.setVisibility(fullCount == 0 ? View.GONE : View.VISIBLE);
        });

        socialViewModel.incomingList.observe(this, users -> {
            int count = users == null ? 0 : users.size();
            tvRequestsSummary.setText(count > 0
                    ? getString(R.string.friend_requests_waiting, count)
                    : getString(R.string.friend_requests_empty));
        });

        socialViewModel.error.observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        socialViewModel.toast.observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onNavigationItemSelected(int itemId) {
        if (menuIdToWorkspaceIdMap.containsKey(itemId)) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("OPEN_WORKSPACE_ID", menuIdToWorkspaceIdMap.get(itemId));
            startActivity(intent);
            finish();
        } else if (itemId == R.id.nav_workspace_personal) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void showAddFriendDialog() {
        AddFriendBottomSheet bottomSheet = new AddFriendBottomSheet();
        bottomSheet.show(getSupportFragmentManager(), "AddFriendBottomSheet");
    }

    private void openChat(User user) {
        Intent intent = new Intent(FriendsActivity.this, FriendChatActivity.class);
        intent.putExtra(FriendChatActivity.EXTRA_FRIEND_UID, user.getUid());
        intent.putExtra(FriendChatActivity.EXTRA_FRIEND_NAME, user.getNameToShow());
        intent.putExtra(FriendChatActivity.EXTRA_FRIEND_AVATAR, user.getAvatarUrl());
        startActivity(intent);
    }

    private void openSuggestedFriends() {
        startActivity(new Intent(this, SuggestedFriendsActivity.class));
    }
}