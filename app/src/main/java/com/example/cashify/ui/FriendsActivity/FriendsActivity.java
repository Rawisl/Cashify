package com.example.cashify.ui.FriendsActivity; // Nhớ check lại package name nha An

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cashify.R;
import com.example.cashify.databinding.ActivityFriendsBinding;

public class FriendsActivity extends AppCompatActivity {
    private ActivityFriendsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Khởi tạo ViewBinding
        binding = ActivityFriendsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. Setup Bottom Navigation (Phong cách viên thuốc của An)
        setupBottomNav();

        // 3. Mở BottomSheet thêm bạn khi nhấn nút (+)
        binding.fabAddFriend.setOnClickListener(v -> {
            // Tạm thời Toast để check, sau đó mình làm class AddFriendBottomSheet nha
            android.widget.Toast.makeText(this, "Mở form thêm bạn!", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    private void setupBottomNav() {
        // Tắt cái Indicator mặc định của Material 3 để hiện đúng cái background An des
        binding.bottomNavigation.setItemActiveIndicatorEnabled(false);

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
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
        binding.bottomNavigation.setSelectedItemId(R.id.nav_friends);
    }
}