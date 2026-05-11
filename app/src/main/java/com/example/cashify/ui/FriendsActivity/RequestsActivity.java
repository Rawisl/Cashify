package com.example.cashify.ui.FriendsActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class RequestsActivity extends AppCompatActivity {

    private SocialViewModel socialViewModel;
    private TabLayout tabLayout;
    private RecyclerView rvRequests;
    private LinearLayout layoutEmpty;
    private BottomNavigationView bottomNavigation;

    private RequestAdapter adapter;
    private List<User> incomingList = new ArrayList<>();
    private List<User> sentList = new ArrayList<>();
    private boolean isShowingIncoming = true; // Theo dõi đang ở tab nào

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requests);

        tabLayout = findViewById(R.id.tabLayout);
        rvRequests = findViewById(R.id.rvRequests);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        socialViewModel = new ViewModelProvider(this).get(SocialViewModel.class);

        setupTabs();
        setupRecyclerView();
        setupBottomNav();
        setupObservers();

        socialViewModel.fetchRequests(); // Gọi load data
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Received"));
        tabLayout.addTab(tabLayout.newTab().setText("Sent"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isShowingIncoming = (tab.getPosition() == 0);
                updateUI(); // Đổi tab thì vẽ lại list
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupRecyclerView() {
        rvRequests.setLayoutManager(new LinearLayoutManager(this));

        adapter = new RequestAdapter(new ArrayList<>(), isShowingIncoming, new RequestAdapter.RequestActionListener() {
            @Override
            public void onAccept(User user) {
                socialViewModel.acceptFriendRequest(user);
            }

            @Override
            public void onDecline(User user) {
                socialViewModel.declineFriendRequest(user);
            }

            @Override
            public void onCancel(User user) {
                socialViewModel.cancelFriendRequest(user);
            }
        });
        rvRequests.setAdapter(adapter);
    }

    private void setupObservers() {
        socialViewModel.incomingList.observe(this, users -> {
            incomingList = users != null ? users : new ArrayList<>();
            if (isShowingIncoming) updateUI();
        });

        socialViewModel.sentList.observe(this, users -> {
            sentList = users != null ? users : new ArrayList<>();
            if (!isShowingIncoming) updateUI();
        });

        socialViewModel.toast.observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    // Logic vẽ List hoặc vẽ Empty State
    private void updateUI() {
        // Lấy đúng danh sách theo tab hiện tại từ ViewModel ra, thay vì dùng cái biến toàn cục bị out-date
        List<User> displayList = new ArrayList<>();
        if (isShowingIncoming && socialViewModel.incomingList.getValue() != null) {
            displayList = socialViewModel.incomingList.getValue();
        } else if (!isShowingIncoming && socialViewModel.sentList.getValue() != null) {
            displayList = socialViewModel.sentList.getValue();
        }

        // Cấp lại Adapter để đổi loại Nút (Accept/Decline <-> Cancel)
        adapter = new RequestAdapter(displayList, isShowingIncoming, new RequestAdapter.RequestActionListener() {
            @Override
            public void onAccept(User user) {
                socialViewModel.acceptFriendRequest(user);
            }

            @Override
            public void onDecline(User user) {
                socialViewModel.declineFriendRequest(user);
            }

            @Override
            public void onCancel(User user) {
                socialViewModel.cancelFriendRequest(user);
            }
        });
        rvRequests.setAdapter(adapter);

        if (displayList.isEmpty()) {
            rvRequests.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            rvRequests.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void setupBottomNav() {
        bottomNavigation.setItemActiveIndicatorEnabled(false);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_friends) {
                // CHUYỂN VỀ MÀN HÌNH BẠN BÈ VÀ TẮT MÀN NÀY ĐI
                startActivity(new Intent(RequestsActivity.this, FriendsActivity.class));
                overridePendingTransition(0, 0); // Tắt hiệu ứng trượt màn hình cho nó mượt như đổi Tab
                finish();
                return true;
            } else if (id == R.id.nav_requests) {
                return true; // Đang ở đây rồi thì đứng im
            }
            // TODO: Sếp thêm luồng cho nav_messages ở đây nếu sau này làm
            // TODO: Sếp thêm luồng cho nav_messages ở đây nếu sau này làm
            return false;
        });
        bottomNavigation.setSelectedItemId(R.id.nav_requests); // Sáng đèn tab Requests
    }
}