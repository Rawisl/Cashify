package com.example.cashify.ui.FriendsActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.User;
import com.example.cashify.ui.main.BaseActivity;
import com.example.cashify.ui.main.MainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class FriendsActivity extends BaseActivity { // Đã kế thừa BaseActivity

    private static final String TAG = "CASHIFY";

    private SocialViewModel socialViewModel;
    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabAddFriend;
    private RecyclerView rvFriends;
    private FriendAdapter friendAdapter;
    private final List<User> friendList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        // 1. GỌI HÀM CỦA CHA ĐỂ SETUP SIDEBAR CỰC MƯỢT
        setupBaseSidebar();

        // Ánh xạ view
        bottomNavigation = findViewById(R.id.bottomNavigation);
        fabAddFriend = findViewById(R.id.fabAddFriend);
        rvFriends = findViewById(R.id.rvFriends);
        com.google.android.material.appbar.MaterialToolbar toolbarFriends = findViewById(R.id.toolbarFriends);

        socialViewModel = new ViewModelProvider(this).get(SocialViewModel.class);

        // Adapter giờ chỉ cần xử lý 2 việc: Nhắn tin và Hủy kết bạn
        friendAdapter = new FriendAdapter(friendList, new FriendAdapter.ActionListener() {
            @Override public void onAddFriend(User user) {} // Bỏ qua
            @Override public void onCancelRequest(User user) {} // Bỏ qua
            @Override public void onAccept(User user) {} // Bỏ qua
            @Override public void onDecline(User user) {} // Bỏ qua

            @Override
            public void onUnfriend(User user) {
                socialViewModel.unfriend(user);
            }

            @Override
            public void onMessage(User user) {
                Toast.makeText(FriendsActivity.this, "Mở chat với " + user.getNameToShow(), Toast.LENGTH_SHORT).show();
                // TODO: mở ChatActivity
            }
        });

        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        rvFriends.setAdapter(friendAdapter);

        if (bottomNavigation != null) setupBottomNav();

        // HIỆN POPUP TÌM BẠN QUA EMAIL CHUẨN CONCEPT
        if (fabAddFriend != null) {
            fabAddFriend.setOnClickListener(v -> showAddFriendDialog());
        }

        // Lắng nghe danh sách BẠN BÈ
        socialViewModel.friendList.observe(this, users -> {
            if (users != null) {
                friendList.clear();
                friendList.addAll(users);
                friendAdapter.notifyDataSetChanged();
            }
        });

        socialViewModel.error.observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        socialViewModel.toast.observe(this, msg -> {
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // Search Bar (Chỉ search những người đã là bạn)
        EditText edtSearch = findViewById(R.id.edtSearch);
        if (edtSearch != null) {
            edtSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    socialViewModel.filterFriendsLocal(s.toString().trim());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // 2. MỞ CỬA SIDEBAR KHI BẤM NÚT 3 GẠCH
        if (toolbarFriends != null) {
            toolbarFriends.setNavigationOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
            });
        }

        // Khởi động!
        socialViewModel.fetchOnlyFriends();
    }

    // 3. XỬ LÝ LOGIC KHI BẤM VÀO ITEM QUỸ TRÊN SIDEBAR (BẮT BUỘC VÌ IMPLEMENTS TỪ BASEACTIVITY)
    @Override
    protected void onNavigationItemSelected(int itemId) {
        if (menuIdToWorkspaceIdMap.containsKey(itemId)) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("OPEN_WORKSPACE_ID", menuIdToWorkspaceIdMap.get(itemId));
            startActivity(intent);
            finish();
        } else if (itemId == R.id.nav_workspace_personal) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void showAddFriendDialog() {
        AddFriendBottomSheet bottomSheet = new AddFriendBottomSheet();
        bottomSheet.show(getSupportFragmentManager(), "AddFriendBottomSheet");
    }

    private void setupBottomNav() {
        bottomNavigation.setItemActiveIndicatorEnabled(false);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_friends) {
                return true; // Đang ở màn Friends rồi thì đứng im
            }
            else if (id == R.id.nav_requests) {
                // Phóng xe sang màn hình Requests (Lời mời)
                startActivity(new Intent(FriendsActivity.this, RequestsActivity.class));
                overridePendingTransition(0, 0); // Tắt hiệu ứng trượt màn hình cho nó mượt như đổi Tab
                finish(); // Đóng màn cũ lại cho đỡ nặng máy
                return true;
            }
            else if (id == R.id.nav_messages) {
                // TODO: Sếp thả intent mở ChatActivity ở đây nếu sau này làm
                return true;
            }
            return false;
        });

        // Sáng đèn tab Friends khi đang ở màn này
        bottomNavigation.setSelectedItemId(R.id.nav_friends);
    }
}