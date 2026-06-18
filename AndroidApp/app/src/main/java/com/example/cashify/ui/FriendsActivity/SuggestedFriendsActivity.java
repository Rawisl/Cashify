package com.example.cashify.ui.FriendsActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.User;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.appbar.MaterialToolbar;

public class SuggestedFriendsActivity extends AppCompatActivity {

    private SocialViewModel socialViewModel;
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

        socialViewModel = new ViewModelProvider(this).get(SocialViewModel.class);

        bindViews();
        setupList();
        setupSearch();
        observeViewModel();

        socialViewModel.fetchOnlyFriends();
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
            @Override public void onAddFriend(User user) { socialViewModel.sendFriendRequest(user); }
            @Override public void onCancelRequest(User user) { socialViewModel.cancelFriendRequest(user); }
            @Override public void onAccept(User user) { socialViewModel.acceptFriendRequest(user); }
            @Override public void onDecline(User user) { socialViewModel.declineFriendRequest(user); }
            @Override public void onUnfriend(User user) { socialViewModel.unfriend(user); }
            @Override public void onMessage(User user) { /* Chat navigation if needed */ }
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
                searchRunnable = () -> socialViewModel.filterSuggestionsLocal(query);

                searchHandler.postDelayed(searchRunnable, 300);
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        socialViewModel.suggestionList.observe(this, users -> {
            boolean isEmpty = (users == null || users.isEmpty());
            adapter.updateList(users);

            tvSuggestedEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            rvSuggestedFriends.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });

        socialViewModel.error.observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                ToastHelper.show(this, msg);
            }
        });

        socialViewModel.toast.observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                ToastHelper.show(this, msg);
            }
        });
    }
}