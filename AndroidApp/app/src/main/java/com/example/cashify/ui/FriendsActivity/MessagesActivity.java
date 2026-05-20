package com.example.cashify.ui.FriendsActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.DirectConversation;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class MessagesActivity extends AppCompatActivity {
    private ConversationAdapter adapter;
    private RecyclerView rvConversations;
    private ProgressBar progressLoading;
    private TextView tvEmptyState;
    private TextView tvErrorState;
    private MessagesViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        rvConversations = findViewById(R.id.rvConversations);
        progressLoading = findViewById(R.id.progressLoading);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvErrorState = findViewById(R.id.tvErrorState);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        adapter = new ConversationAdapter(new ArrayList<>(), this::openConversation);
        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        rvConversations.setAdapter(adapter);

        setupBottomNav(bottomNavigation);

        viewModel = new ViewModelProvider(this).get(MessagesViewModel.class);
        viewModel.getConversations().observe(this, conversations -> {
            adapter.updateList(conversations);
            boolean isEmpty = conversations == null || conversations.isEmpty();
            boolean isLoading = Boolean.TRUE.equals(viewModel.getLoading().getValue());
            tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            rvConversations.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            if (isLoading) tvEmptyState.setVisibility(View.GONE);
            if (!isEmpty) tvErrorState.setVisibility(View.GONE);
        });
        viewModel.getLoading().observe(this, isLoading -> progressLoading.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE));
        viewModel.getError().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                tvErrorState.setVisibility(View.VISIBLE);
                ToastHelper.show(this, "T\u1ea3i danh s\u00e1ch tr\u00f2 chuy\u1ec7n th\u1ea5t b\u1ea1i");
            }
        });
        viewModel.loadConversations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.loadConversations();
    }

    private void openConversation(DirectConversation conversation) {
        Intent intent = new Intent(this, FriendChatActivity.class);
        intent.putExtra(FriendChatActivity.EXTRA_FRIEND_UID, conversation.getFriendUid());
        intent.putExtra(FriendChatActivity.EXTRA_FRIEND_NAME, conversation.getNameToShow());
        intent.putExtra(FriendChatActivity.EXTRA_FRIEND_AVATAR, conversation.getFriendAvatarUrl());
        startActivity(intent);
    }

    private void setupBottomNav(BottomNavigationView bottomNavigation) {
        bottomNavigation.setItemActiveIndicatorEnabled(false);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_messages) return true;
            if (id == R.id.nav_friends) {
                startActivity(new Intent(this, FriendsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            if (id == R.id.nav_requests) {
                startActivity(new Intent(this, RequestsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
        bottomNavigation.setSelectedItemId(R.id.nav_messages);
    }
}
