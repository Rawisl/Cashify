package com.example.cashify;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.splashscreen.SplashScreen;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.appcompat.app.AppCompatDelegate; // Nhớ import thư viện này

import com.example.cashify.AddTransaction.AddTransactionActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.cashify.database.DatabaseSeeder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {
    // Tạo một lá cờ hiệu
    boolean keepSplash = true;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        //Gọi thư viện Google trước super.onCreate cho splash screen
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        //Khóa cứng app ở chế độ Sáng
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        //Kích hoạt EdgeToEdge và GẮN GIAO DIỆN LÊN TRƯỚC
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        //khai báo biến lưu fab để lát dùng để chuyển qua màn hình thêm giao dịch
        FloatingActionButton fabAddTransaction = findViewById(R.id.fab_add_transaction);
        //tìm thanh điều hướng
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        //sự kiện click nút fab
        fabAddTransaction.setOnClickListener(v ->
        {
            Intent intent = new Intent(this, AddTransactionActivity.class);
            startActivity(intent);
            // Gọi hiệu ứng trượt LÊN ngay sau khi start
            overridePendingTransition(R.anim.slide_in_up, R.anim.stay);
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if(navHostFragment!=null)
        {
            NavController navController = (NavController) navHostFragment.getNavController();
            //ghép thanh điều hướng với bộ điều khiển để chuyển fragment
            NavigationUI.setupWithNavController(bottomNav, navController);

            // Lắng nghe sự kiện mỗi khi người dùng chuyển tab/fragment để fix lỗi chìm fab
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {

                // Ép tọa độ trục Y của nút về vị trí gốc (mặt đất) ngay lập tức
                fabAddTransaction.setTranslationY(0f);

                // Đảm bảo trạng thái hiển thị là True
                if (!fabAddTransaction.isShown()) {
                    fabAddTransaction.show();
                }

                //đoạn này để nút fab ko hiện trong settings
                int id = destination.getId();

                // Giả sử R.id.settingsFragment là màn hình không cần nút thêm giao dịch
                if (id == R.id.nav_settings) {
                    fabAddTransaction.hide(); // Giấu đi khi vào các tab không liên quan
                } else {
                    fabAddTransaction.show(); // Hiện lên ở các tab quản lý thu chi
                }
            });

        }

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


    }

}