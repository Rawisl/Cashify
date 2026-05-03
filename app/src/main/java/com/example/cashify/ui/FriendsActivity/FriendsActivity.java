package com.example.cashify.ui.FriendsActivity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class FriendsActivity extends AppCompatActivity {

    private static final String TAG = "CASHIFY";

    private SocialViewModel socialViewModel;
    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabAddFriend;
    private RecyclerView rvFriends;
    private FriendAdapter friendAdapter;
    private final List<User> userList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "=== FriendsActivity onCreate ===");

        setContentView(R.layout.activity_friends);

        // Ánh xạ view
        bottomNavigation = findViewById(R.id.bottomNavigation);
        fabAddFriend = findViewById(R.id.fabAddFriend);
        rvFriends = findViewById(R.id.rvFriends);

        // Setup ViewModel
        socialViewModel = new ViewModelProvider(this).get(SocialViewModel.class);

        // Setup Adapter — truyền listener vào, xử lý action qua ViewModel
        friendAdapter = new FriendAdapter(userList, new FriendAdapter.ActionListener() {
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
                Toast.makeText(FriendsActivity.this,
                        "Mở chat với " + user.getNameToShow(), Toast.LENGTH_SHORT).show();
                // TODO: mở ChatActivity
            }
        });

        // Setup RecyclerView
        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        rvFriends.setAdapter(friendAdapter);

        // Setup BottomNav
        if (bottomNavigation != null) setupBottomNav();

        // FAB
        if (fabAddFriend != null) {
            fabAddFriend.setOnClickListener(v ->
                    Toast.makeText(this, "Mở form thêm bạn!", Toast.LENGTH_SHORT).show()
            );
        }

        // Observe danh sách user
        socialViewModel.userList.observe(this, users -> {
            Log.e(TAG, "Observer: " + (users != null ? users.size() : "null") + " người");
            if (users != null) {
                userList.clear();
                userList.addAll(users);
                friendAdapter.notifyDataSetChanged();
            }
        });

        // Observe lỗi
        socialViewModel.error.observe(this, msg -> {
            if (msg != null) {
                Log.e(TAG, "Lỗi: " + msg);
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        // Observe toast thành công
        socialViewModel.toast.observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // Setup Search
        EditText edtSearch = findViewById(R.id.edtSearch);
        if (edtSearch != null) {
            edtSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    socialViewModel.filterUsers(s.toString().trim());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // Gọi fetch DUY NHẤT 1 LẦN
        socialViewModel.fetchUsers();

        Log.e(TAG, "=== FriendsActivity onCreate XONG ===");
    }

    private void setupBottomNav() {
        bottomNavigation.setItemActiveIndicatorEnabled(false);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_messages) return true;
            else if (id == R.id.nav_friends) return true;
            else if (id == R.id.nav_requests) return true;
            return false;
        });
        bottomNavigation.setSelectedItemId(R.id.nav_friends);
    }
}