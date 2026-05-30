package com.example.cashify.ui.FriendsActivity;

import android.os.Bundle;
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
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class SuggestedFriendsActivity extends AppCompatActivity {

    private SocialViewModel socialViewModel;
    private FriendAdapter adapter;
    private final List<User> suggestions = new ArrayList<>();
    private RecyclerView rvSuggestedFriends;
    private TextView tvSuggestedEmpty;

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
        toolbar.setNavigationOnClickListener(v -> finish());
        rvSuggestedFriends = findViewById(R.id.rvSuggestedFriends);
        tvSuggestedEmpty = findViewById(R.id.tvSuggestedEmpty);
    }

    private void setupList() {
        adapter = new FriendAdapter(suggestions, new FriendAdapter.ActionListener() {
            @Override public void onAddFriend(User user) { socialViewModel.sendFriendRequest(user); }
            @Override public void onCancelRequest(User user) { socialViewModel.cancelFriendRequest(user); }
            @Override public void onAccept(User user) { socialViewModel.acceptFriendRequest(user); }
            @Override public void onDecline(User user) { socialViewModel.declineFriendRequest(user); }
            @Override public void onUnfriend(User user) { socialViewModel.unfriend(user); }
            @Override public void onMessage(User user) {}
        });
        rvSuggestedFriends.setLayoutManager(new LinearLayoutManager(this));
        rvSuggestedFriends.setAdapter(adapter);
    }

    private void setupSearch() {
        EditText edtSearch = findViewById(R.id.edtSearchSuggested);
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                socialViewModel.filterSuggestionsLocal(s == null ? "" : s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        socialViewModel.suggestionList.observe(this, users -> {
            suggestions.clear();
            if (users != null) suggestions.addAll(users);
            adapter.updateList(suggestions);
            boolean empty = suggestions.isEmpty();
            tvSuggestedEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            rvSuggestedFriends.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        socialViewModel.error.observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        socialViewModel.toast.observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }
}
