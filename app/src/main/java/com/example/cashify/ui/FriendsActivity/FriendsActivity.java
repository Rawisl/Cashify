package com.example.cashify.ui.FriendsActivity; // Vẫn nhắc lại là check lại cái tên thư mục nha

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cashify.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class FriendsActivity extends AppCompatActivity {

    // Khai báo biến truyền thống
    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabAddFriend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Dùng setContentView nạp trực tiếp file giao diện XML
        // Nhớ check lại tên file layout có đúng là activity_friends không nha
        setContentView(R.layout.activity_friends);

        // 2. Ánh xạ các thành phần giao diện (findViewById)
        bottomNavigation = findViewById(R.id.bottomNavigation); // Đổi ID này cho khớp với file XML của ghệ
        fabAddFriend = findViewById(R.id.fabAddFriend);          // Đổi ID này cho khớp với file XML của ghệ

        // 3. Setup Bottom Navigation
        if (bottomNavigation != null) {
            setupBottomNav();
        }

        // 4. Mở form thêm bạn khi nhấn nút (+)
        if (fabAddFriend != null) {
            fabAddFriend.setOnClickListener(v -> {
                // Tạm thời Toast để check, mốt ráp cái BottomSheet vào đây
                Toast.makeText(this, "Mở form thêm bạn!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupBottomNav() {
        // Tắt cái Indicator mặc định của Material 3 để hiện đúng cái background team design
        bottomNavigation.setItemActiveIndicatorEnabled(false);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_messages) {
                // Chỗ này để load Fragment Tin nhắn
                return true;
            } else if (id == R.id.nav_friends) {
                // Chỗ này load Fragment Bạn bè
                return true;
            } else if (id == R.id.nav_requests) {
                // Chỗ này load Fragment Lời mời
                return true;
            }
            return false;
        });

        // Mặc định chọn tab Friends cho giống hình mẫu
        bottomNavigation.setSelectedItemId(R.id.nav_friends);
    }
}