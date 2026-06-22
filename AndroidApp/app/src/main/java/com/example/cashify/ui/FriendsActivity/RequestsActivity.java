package com.example.cashify.ui.FriendsActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.User;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class RequestsActivity extends AppCompatActivity {

    private FriendsViewModel friendsViewModel;
    private TabLayout tabLayout;
    private RecyclerView rvRequests;
    private LinearLayout layoutEmpty;

    private RequestAdapter adapter;
    private boolean isShowingIncoming = true; // Tracks the currently active tab

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requests);

        initViews();
        friendsViewModel = new ViewModelProvider(this).get(FriendsViewModel.class);

        setupTabs();
        setupRecyclerView();
        observeViewModel();

        // Trigger initial data fetch
        friendsViewModel.fetchRequests();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        rvRequests = findViewById(R.id.rvRequests);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        MaterialToolbar toolbar = findViewById(R.id.toolbarRequests);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Received"));
        tabLayout.addTab(tabLayout.newTab().setText("Sent"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isShowingIncoming = (tab.getPosition() == 0);
                // Recreate adapter ONLY when switching tabs (to toggle action buttons)
                recreateAdapterForCurrentTab();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        recreateAdapterForCurrentTab();
    }

    /**
     * Initializes a new adapter instance. This is required when switching tabs
     * because the 'isIncoming' flag fundamentally changes the ViewHolder layout structure.
     */
    private void recreateAdapterForCurrentTab() {
        adapter = new RequestAdapter(new ArrayList<>(), isShowingIncoming, new RequestAdapter.RequestActionListener() {
            @Override
            public void onAccept(User user) {
                friendsViewModel.acceptFriendRequest(user);
            }

            @Override
            public void onDecline(User user) {
                friendsViewModel.declineFriendRequest(user);
            }

            @Override
            public void onCancel(User user) {
                friendsViewModel.cancelFriendRequest(user);
            }

            @Override
            public void onAvatarClick(User user) {
                android.content.Intent intent = new android.content.Intent(RequestsActivity.this, com.example.cashify.ui.main.MainActivity.class);
                intent.putExtra("OPEN_USER_PROFILE", user.getUid());
                intent.putExtra("FINISH_ON_BACK", true);
                startActivity(intent);
            }
        });
        rvRequests.setAdapter(adapter);

        // Immediately populate the new adapter with existing cached data
        refreshCurrentData();
    }

    private void observeViewModel() {
        friendsViewModel.incomingList.observe(this, users -> {
            if (isShowingIncoming) {
                updateAdapterData(users);
            }
        });

        friendsViewModel.sentList.observe(this, users -> {
            if (!isShowingIncoming) {
                updateAdapterData(users);
            }
        });

        friendsViewModel.toast.observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                ToastHelper.show(this, msg);
            }
        });
    }

    /**
     * Pulls the correct list from the ViewModel based on the active tab and updates the UI.
     */
    private void refreshCurrentData() {
        List<User> displayList = new ArrayList<>();
        if (isShowingIncoming && friendsViewModel.incomingList.getValue() != null) {
            displayList = friendsViewModel.incomingList.getValue();
        } else if (!isShowingIncoming && friendsViewModel.sentList.getValue() != null) {
            displayList = friendsViewModel.sentList.getValue();
        }
        updateAdapterData(displayList);
    }

    /**
     * Performs a lightweight data update using the existing adapter to prevent UI flickering.
     */
    private void updateAdapterData(List<User> users) {
        List<User> safeList = users != null ? users : new ArrayList<>();

        if (adapter != null) {
            adapter.updateData(safeList);
        }

        // Toggle Empty State UI
        if (safeList.isEmpty()) {
            rvRequests.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvRequests.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }
}