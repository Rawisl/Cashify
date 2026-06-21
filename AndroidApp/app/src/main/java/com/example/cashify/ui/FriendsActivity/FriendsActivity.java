package com.example.cashify.ui.FriendsActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.User;
import com.example.cashify.ui.main.BaseActivity;
import com.example.cashify.ui.main.MainActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class FriendsActivity extends BaseActivity {

    private SocialViewModel socialViewModel;
    private FloatingActionButton fabAddFriend;
    private RecyclerView rvFriends;
    private RecyclerView rvSuggestions;
    private FriendAdapter friendAdapter;
    private SuggestionAdapter suggestionAdapter;
    private TextView tvFriendsEmpty;
    private TextView tvSuggestionsEmpty;
    private TextView tvSuggestionCount;
    private TextView tvRequestsSummary;
    private final List<User> friendList = new ArrayList<>();
    private final List<User> suggestionList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyLightSystemBars();
        setContentView(R.layout.activity_friends);
        setupBaseSidebar();
        if (drawerLayout != null) {
            drawerLayout.setStatusBarBackgroundColor(ContextCompat.getColor(this, R.color.bg_budget_screen));
        }

        bindViews();
        socialViewModel = new ViewModelProvider(this).get(SocialViewModel.class);
        setupAdapters();
        setupActions();
        observeViewModel();
        setupSearch();

        socialViewModel.fetchOnlyFriends();
    }

    private void applyLightSystemBars() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.bg_budget_screen));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
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
        FriendAdapter.ActionListener listener = new FriendAdapter.ActionListener() {
            @Override public void onAddFriend(User user) { socialViewModel.sendFriendRequest(user); }
            @Override public void onCancelRequest(User user) { socialViewModel.cancelFriendRequest(user); }
            @Override public void onAccept(User user) { socialViewModel.acceptFriendRequest(user); }
            @Override public void onDecline(User user) { socialViewModel.declineFriendRequest(user); }
            @Override public void onUnfriend(User user) { socialViewModel.unfriend(user); }

            @Override
            public void onMessage(User user) {
                openChat(user);
            }
        };

        friendAdapter = new FriendAdapter(friendList, listener);
        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        rvFriends.setNestedScrollingEnabled(false);
        rvFriends.setAdapter(friendAdapter);

        suggestionAdapter = new SuggestionAdapter(suggestionList, new SuggestionAdapter.ActionListener() {
            @Override public void onAddFriend(User user) { socialViewModel.sendFriendRequest(user); }
            @Override public void onCancelRequest(User user) { socialViewModel.cancelFriendRequest(user); }
            @Override public void onSeeAll() { openSuggestedFriends(); }
        });
        rvSuggestions.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSuggestions.setAdapter(suggestionAdapter);
    }

    private void setupActions() {
        com.google.android.material.appbar.MaterialToolbar toolbarFriends = findViewById(R.id.toolbarFriends);
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

    private void observeViewModel() {
        socialViewModel.friendList.observe(this, users -> {
            friendList.clear();
            if (users != null) friendList.addAll(users);
            friendAdapter.updateList(friendList);
            tvFriendsEmpty.setVisibility(friendList.isEmpty() ? View.VISIBLE : View.GONE);
            rvFriends.setVisibility(friendList.isEmpty() ? View.GONE : View.VISIBLE);
        });

        socialViewModel.suggestionList.observe(this, users -> {
            int fullCount = users == null ? 0 : users.size();
            suggestionList.clear();
            if (users != null) suggestionList.addAll(users.subList(0, Math.min(5, users.size())));
            suggestionAdapter.setShowSeeAllCard(fullCount > 5);
            suggestionAdapter.updateList(suggestionList);
            tvSuggestionCount.setText(fullCount + " people");
            tvSuggestionsEmpty.setVisibility(fullCount == 0 ? View.VISIBLE : View.GONE);
            rvSuggestions.setVisibility(fullCount == 0 ? View.GONE : View.VISIBLE);
        });

        socialViewModel.incomingList.observe(this, users -> {
            int count = users == null ? 0 : users.size();
            tvRequestsSummary.setText(count > 0
                    ? count + " requests waiting for your response"
                    : "View pending friend requests");
        });

        socialViewModel.error.observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        socialViewModel.toast.observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupSearch() {
        EditText edtSearch = findViewById(R.id.edtSearch);
        if (edtSearch == null) return;
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s == null ? "" : s.toString().trim();
                socialViewModel.filterFriendsLocal(query);
                socialViewModel.filterSuggestionsLocal(query);
            }

            @Override public void afterTextChanged(Editable s) {}
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
