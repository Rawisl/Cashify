package com.example.cashify;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

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

        fabAddTransaction.setOnClickListener(v ->
        {
            //Dùng activity option bundle để n phóng màn hình mới từ cái nút fab nhìn cho n pro vip
            Intent intent = new Intent(this, AddTransactionActivity.class);
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, fabAddTransaction, "transition_fab");
            startActivity(intent, options.toBundle());
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if(navHostFragment!=null)
        {
            NavController navController = (NavController) navHostFragment.getNavController();
            //ghép thanh điều hướng với bộ điều khiển để chuyển fragment
            NavigationUI.setupWithNavController(bottomNav, navController);
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