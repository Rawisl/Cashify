    package com.example.cashify.ui.auth;

    import android.content.Intent;
    import android.os.Bundle;
    import android.os.Handler;
    import android.os.Looper;

    import androidx.appcompat.app.AppCompatActivity;

    import com.example.cashify.ui.main.MainActivity;
    import com.example.cashify.R;
    import com.example.cashify.data.remote.FirebaseManager;

    public class SplashActivity extends AppCompatActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // TODO 1: setContentView(R.layout.activity_splash);
            // (Layout này chỉ cần 1 cái Logo và Tên App như ghệ đã làm)
            setContentView(R.layout.activity_splash);

            checkUserStatus();
        }

        private void checkUserStatus() {
            // ============================================================
            // TODO 2: LOGIC ĐIỀU HƯỚNG THÔNG MINH
            // - Delay khoảng 1.5s - 2s để user kịp nhìn thấy Logo (Branding).
            // - Dùng FirebaseAuth.getInstance().getCurrentUser() để check.
            // - IF (user != null) -> Mở MainActivity.
            // - ELSE -> Mở LoginActivity.
            // - QUAN TRỌNG: Gọi finish() sau khi startActivity để user bấm Back không quay lại Splash.
            // ============================================================

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Check Firebase User
                if (FirebaseManager.getInstance().getAuth().getCurrentUser() != null) {
                    // Đã đăng nhập -> Vào Main
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                } else {
                    // Chưa đăng nhập -> Bắt Login
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                }
                finish(); // Quan trọng: Đóng Splash để user bấm Back không thấy lại logo
            }, 1500); // 1.5 giây là vừa đẹp cho một sự khởi đầu
        }

        //TODO 3: Vào file AndroidManifest.xml:
        //Tìm cái đoạn <intent-filter> có chứa android.intent.action.MAIN và android.intent.category.LAUNCHER.
        //Cắt nguyên cái cụm đó từ trong thẻ <activity android:name=".MainActivity"> và Dán nó vào trong thẻ <activity android:name=".ui.auth.SplashActivity">.
        //Dọn dẹp MainActivity: Xóa sạch mấy cái code liên quan đến Timer hay hiện Logo lúc đầu ở đó đi. Giờ MainActivity chỉ lo đúng việc hiển thị Dashboard thôi.
    }
