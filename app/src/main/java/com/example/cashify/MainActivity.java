package com.example.cashify;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.core.splashscreen.SplashScreen;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import androidx.appcompat.app.AppCompatDelegate; // Nhớ import thư viện này

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.cashify.database.DatabaseSeeder;

public class MainActivity extends AppCompatActivity {
    // Tạo một lá cờ hiệu
    boolean keepSplash = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Khóa cứng app ở chế độ Sáng
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        //Gọi thư viện Google trước super.onCreate cho splash screen
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        //Gọi hàm khởi tạo các danh mục lần đầu mở app
        DatabaseSeeder.seedIfEmpty(this);

        //Cài đồng hồ đếm ngược cho splash screen 2000 ms
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                keepSplash = false;
            }
        }, 2000);

        splashScreen.setKeepOnScreenCondition(new SplashScreen.KeepOnScreenCondition() {
            @Override
            public boolean shouldKeepOnScreen()
            {
                return keepSplash;
            }
        });
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        //tìm thanh điều hướng
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = (NavController) navHostFragment.getNavController();

        //ghép thanh điều hướng với bộ điều khiển để chuyển fragment
        NavigationUI.setupWithNavController(bottomNav,navController);


        //ghép thanh điều hướng với bộ điều khiển để chuyển fragment
        NavigationUI.setupWithNavController(bottomNav,navController);



        // ==========================================================
        // KHU VỰC TEST NUMPAD (CHẠY THỬ XONG THÌ XÓA ĐOẠN NÀY ĐI)
        // ==========================================================
        android.widget.Button btnTestNumpad = findViewById(R.id.btn_test_numpad);
        if (btnTestNumpad != null) {
            btnTestNumpad.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View v) {
                    // 1. Khởi tạo Bàn phím
                    com.example.cashify.ui.NumpadBottomSheet numpad = new com.example.cashify.ui.NumpadBottomSheet();

                    // 2. Lắng nghe kết quả khi bấm TIẾP TỤC
                    numpad.setListener(new com.example.cashify.ui.NumpadBottomSheet.OnNumpadListener() {
                        @Override
                        public void onAmountConfirmed(String rawAmount, String formattedAmount) {
                            // Cập nhật text của cái nút test để xem kết quả luôn
                            btnTestNumpad.setText("Kết quả: " + formattedAmount);

                            // Bạn có thể in log ra xem số thô (rawAmount) có đúng không
                            android.util.Log.d("TEST_NUMPAD", "Số thô lưu Database: " + rawAmount);
                        }
                    });

                    // 3. Hiển thị thang máy
                    numpad.show(getSupportFragmentManager(), "TestNumpad");
                }
            });
        }
        // ==========================================================

    }
}