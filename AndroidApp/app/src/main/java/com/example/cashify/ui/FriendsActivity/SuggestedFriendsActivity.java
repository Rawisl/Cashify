package com.example.cashify.ui.FriendsActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.User;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.appbar.MaterialToolbar;

public class SuggestedFriendsActivity extends AppCompatActivity {

    private FriendsViewModel friendsViewModel;
    private FriendAdapter adapter;
    private RecyclerView rvSuggestedFriends;
    private TextView tvSuggestedEmpty;

    // Search optimization
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggested_friends);

        friendsViewModel = new ViewModelProvider(this).get(FriendsViewModel.class);

        bindViews();
        setupList();
        setupSearch();
        observeViewModel();

        friendsViewModel.fetchOnlyFriends();
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarSuggestedFriends);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        rvSuggestedFriends = findViewById(R.id.rvSuggestedFriends);
        tvSuggestedEmpty = findViewById(R.id.tvSuggestedEmpty);
    }

    private void setupList() {
        // Delegate all friend actions to the ViewModel
        adapter = new FriendAdapter(new java.util.ArrayList<>(), new FriendAdapter.ActionListener() {
            @Override public void onAddFriend(User user) { friendsViewModel.sendFriendRequest(user); }
            @Override public void onCancelRequest(User user) { friendsViewModel.cancelFriendRequest(user); }
            @Override public void onAccept(User user) { friendsViewModel.acceptFriendRequest(user); }
            @Override public void onDecline(User user) { friendsViewModel.declineFriendRequest(user); }
            @Override public void onUnfriend(User user) { friendsViewModel.unfriend(user); }
            @Override public void onMessage(User user) { /* Chat navigation if needed */ }
            @Override public void onAvatarClick(User user) {
                android.content.Intent intent = new android.content.Intent(SuggestedFriendsActivity.this, com.example.cashify.ui.main.MainActivity.class);
                intent.putExtra("OPEN_USER_PROFILE", user.getUid());
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });

        rvSuggestedFriends.setLayoutManager(new LinearLayoutManager(this));
        rvSuggestedFriends.setAdapter(adapter);
    }

    private void setupSearch() {
        EditText edtSearch = findViewById(R.id.edtSearchSuggested);
        if (edtSearch == null) return;

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Debounce search input to prevent rapid, unnecessary filtering
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);

                String query = s == null ? "" : s.toString().trim();
                searchRunnable = () -> friendsViewModel.filterSuggestionsLocal(query);

                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        friendsViewModel.suggestionList.observe(this, users -> {
            boolean isEmpty = (users == null || users.isEmpty());
            adapter.updateList(users);

            tvSuggestedEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            rvSuggestedFriends.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });

        friendsViewModel.error.observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                ToastHelper.show(this, msg);
            }
        });

        friendsViewModel.toast.observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                ToastHelper.show(this, msg);
            }
        });
    }
}