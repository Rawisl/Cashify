package com.example.cashify.viewmodel;

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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;

import com.example.cashify.AddTransaction.AddTransactionActivity;
import com.example.cashify.R;
import com.example.cashify.database.Category;
import com.example.cashify.database.CategoryDao;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.cashify.database.AppDatabase;
import com.example.cashify.database.DatabaseSeeder;
import com.example.cashify.database.FakeDataSeeder;
import com.example.cashify.viewmodel.TransactionViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    // Tạo một lá cờ hiệu
    boolean keepSplash = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Chế độ sáng
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        //Gọi thư viện Google trước super.onCreate cho splash screen
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        TransactionViewModel viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        super.onCreate(savedInstanceState);
        //reset và seed data - chạy ngầm
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(MainActivity.this);
                CategoryDao categoryDao = db.categoryDao();

                // nếu muốn reset database, bật 2 dòng này chạy 1 lần
                // db.transactionDao().deleteAllTransactions();
                // categoryDao.deleteAllCategories();

                // Nạp Category trước - chờ xong mới đi tiếp
                DatabaseSeeder.seedIfEmpty(MainActivity.this);

                // Lấy danh sách category để kiểm tra + lấy ID thực tế
                List<Category> checkList = categoryDao.getAllActive();

                // Log ID thực tế để debug
                if (checkList != null) {
                    for (Category c : checkList) {
                        android.util.Log.d("BACKEND_TEST", "Category ID: " + c.id + " | Name: " + c.name + " | Type: " + c.type);
                    }
                }

                if (checkList != null && !checkList.isEmpty()) {
                    int count = db.transactionDao().countTransactions();
                    if (count == 0) {
                        // Truyền checkList vào FakeDataSeeder để dùng ID thực tế
                        FakeDataSeeder.seed(MainActivity.this, checkList);
                        android.util.Log.d("BACKEND_TEST", "Đã nạp giao dịch mẫu!");
                        viewModel.fetchHistoryData();
                    } else {
                        android.util.Log.d("BACKEND_TEST", "Đã có " + count + " giao dịch. Bỏ qua seed!");
                        viewModel.fetchHistoryData();
                    }
                } else {
                    android.util.Log.e("BACKEND_TEST", "LỖI: Danh mục vẫn trống rỗng!");
                }
            } catch (Exception e) {
                android.util.Log.e("BACKEND_TEST", "Có biến rồi An ơi: " + e.getMessage());
            }
        }).start();
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

        //khai báo biến lưu fab để lát dùng để chuyển qua màn hình thêm giao dịch
        FloatingActionButton fabAddTransaction = findViewById(R.id.fab_add_transaction);

        //sự kiện click nút fab
        fabAddTransaction.setOnClickListener(v ->
        {
            Intent intent = new Intent(this, AddTransactionActivity.class);
            startActivity(intent);
            // Gọi hiệu ứng trượt LÊN ngay sau khi start
            overridePendingTransition(R.anim.slide_in_up, R.anim.stay);
        });

        //tìm thanh điều hướng
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = (NavController) navHostFragment.getNavController();

        //ghép thanh điều hướng với bộ điều khiển để chuyển fragment
        NavigationUI.setupWithNavController(bottomNav,navController);

        // Lắng nghe sự kiện mỗi khi người dùng chuyển tab/fragment để fix lỗi chìm fab
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {

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
}