package com.example.cashify.ui.main;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;


import com.example.cashify.data.model.Workspace;
import com.example.cashify.ui.auth.EditProfileActivity;
import com.example.cashify.ui.auth.LoginActivity;
//import com.example.cashify.ui.workspace.WorkspaceDetailActivity;
import com.example.cashify.utils.ImageHelper;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.example.cashify.ui.transactions.AddTransactionActivity;
import com.example.cashify.R;

import com.example.cashify.ui.transactions.TransactionViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.cashify.data.local.AppDatabase;
import com.example.cashify.data.local.DatabaseSeeder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.cashify.data.remote.FirebaseManager;
import com.example.cashify.utils.WorkScheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.android.material.badge.BadgeDrawable;

public class MainActivity extends BaseActivity {

    // Tạo một lá cờ hiệu
    boolean keepSplash = true;

    // mainViewModel đã được khai báo ở BaseActivity nên không cần khai báo lại
    private TransactionViewModel transactionViewModel; // Tách riêng ViewModel để dễ gọi

    private String currentWorkspaceId = "PERSONAL";

    // Các biến drawerLayout, navigationView, menuIdToWorkspaceIdMap đã chuyển sang BaseActivity

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    setupNotifications();
                } else {
                    Log.w("NOTIF", "User rejected notification access.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Chế độ sáng
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        //Gọi thư viện Google trước super.onCreate cho splash screen
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        // Khởi tạo TransactionViewModel chuẩn gọn
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        super.onCreate(savedInstanceState);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // --- GỌI HÀM CỦA CHA ĐỂ SETUP GIAO DIỆN VÀ SIDEBAR ---
        setContentView(R.layout.activity_main);
        setupBaseSidebar();

        String currentUserId = currentUser.getUid();
        android.util.Log.d("AUTH_FLOW", "Signed in successfully! UID: " + currentUserId);

        // --- BẮT SỰ KIỆN: NẾU ĐƯỢC CHUYỂN TỚI TỪ MÀN HÌNH FRIENDS / INVITATIONS ---
        if (getIntent().hasExtra("OPEN_WORKSPACE_ID")) {
            currentWorkspaceId = getIntent().getStringExtra("OPEN_WORKSPACE_ID");

            NavHostFragment nhf = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            if (nhf != null) {
                NavController nav = nhf.getNavController();

                // Chờ NavController load xong destination đầu tiên (nav_home) rồi mới navigate
                nav.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
                    @Override
                    public void onDestinationChanged(@NonNull NavController controller,
                                                     @NonNull NavDestination destination,
                                                     @Nullable Bundle arguments) {
                        if (destination.getId() == R.id.nav_home) {
                            Bundle bundle = new Bundle();
                            bundle.putString("WORKSPACE_ID", currentWorkspaceId);
                            nav.navigate(R.id.nav_workspace_container, bundle);

                            nav.removeOnDestinationChangedListener(this);
                        }
                    }
                });
            }
        }

        if (getIntent().getBooleanExtra("OPEN_SOCIAL", false)) {
            NavHostFragment nhf = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            if (nhf != null) {
                NavController nav = nhf.getNavController();
                nav.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
                    @Override
                    public void onDestinationChanged(@NonNull NavController controller,
                                                     @NonNull NavDestination destination,
                                                     @Nullable Bundle arguments) {
                        if (destination.getId() == R.id.nav_home) {
                            nav.navigate(R.id.nav_social_container);
                            nav.removeOnDestinationChangedListener(this);
                        }
                    }
                });
            }
        }

        // ============================================================
        // [ĐÃ CẬP NHẬT] BƯỚC 1: KÍCH HOẠT TẢI DANH SÁCH QUỸ
        // ============================================================
        // Lưu ý: BaseActivity đã tự động khởi tạo mainViewModel và gọi loadWorkspaces()
        // ở bên trong hàm setupBaseSidebar() rồi, nên ta không cần gọi lại ở đây nữa!

        //reset và seed data - chạy ngầm
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(MainActivity.this);

                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                String lastUserId = prefs.getString("last_uid", "");

                if (!lastUserId.equals(currentUserId)) {
                    db.clearAllTables();
                    prefs.edit().putString("last_uid", currentUserId).apply();
                    android.util.Log.d("AUTH_FLOW", "New user detected! Destroying old data");
                }

                DatabaseSeeder.seedIfEmpty(MainActivity.this);

                int count = db.transactionDao().countTransactions("PERSONAL");
                if (count == 0) {
                    Log.d("AUTH_FLOW", "Database is empty. Fetching data from server...");
                    // mainViewModel đã được khởi tạo sẵn bên BaseActivity
                    mainViewModel.syncAllDataFromServer(MainActivity.this);
                } else {
                    Log.d("AUTH_FLOW", "Data already exists.");
                    // Dùng transactionViewModel đã khai báo ở trên
                    transactionViewModel.fetchHistoryData(currentWorkspaceId);
                }

                mainViewModel.startRealTimeSync(MainActivity.this);

            } catch (Exception e) {
                Log.e("AUTH_FLOW", "Database error: " + e.getMessage());
            }
        }).start();

        //Cài đồng hồ đếm ngược cho splash screen 2000 ms
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                keepSplash = false;
            }
        }, 2000);

        splashScreen.setKeepOnScreenCondition(new SplashScreen.KeepOnScreenCondition() {
            @Override
            public boolean shouldKeepOnScreen() {
                return keepSplash;
            }
        });
        //EdgeToEdge.enable(this);

        // Khởi tạo các view của MainActivity
        setupNavigationAndFab();

        // ============================================================
        // [ĐÃ CẬP NHẬT] BƯỚC 2: LẮNG NGHE ĐỂ VẼ SIDEBAR MENU (REAL-TIME)
        // ============================================================
        // Việc observe workspaces và vẽ sidebar menu đã được BaseActivity lo trọn gói!


        // ============================================================
        // TODO 2: LẮNG NGHE SỰ KIỆN ĐỔI WORKSPACE
        // - observe(mainViewModel.getCurrentWorkspace()):
        //   Khi Workspace thay đổi, hãy đóng Side Bar (drawerLayout.closeDrawers())
        //   và ra lệnh cho các Fragment con tải lại dữ liệu theo ID mới.
        // ============================================================

        checkNotificationPermission();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // drawerLayout lấy từ BaseActivity
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
    }

    // ============================================================
    // BẮT BUỘC PHẢI CÓ: ĐIỀU HƯỚNG QUỸ DÀNH RIÊNG CHO MAIN ACTIVITY
    // ============================================================
    @Override
    protected void onNavigationItemSelected(int itemId) {
        if (menuIdToWorkspaceIdMap.containsKey(itemId)) {
            currentWorkspaceId = menuIdToWorkspaceIdMap.get(itemId);

            NavController nav = androidx.navigation.Navigation
                    .findNavController(this, R.id.nav_host_fragment);

            // Pop về home trước để tránh same destination bị skip
            nav.popBackStack(R.id.nav_home, false);

            Bundle bundle = new Bundle();
            bundle.putString("WORKSPACE_ID", currentWorkspaceId);
            nav.navigate(R.id.nav_workspace_container, bundle);

        } else if (itemId == R.id.nav_workspace_personal) {
            currentWorkspaceId = "PERSONAL";
            androidx.navigation.Navigation.findNavController(this, R.id.nav_host_fragment)
                    .popBackStack(R.id.nav_home, false);
            transactionViewModel.fetchHistoryData("PERSONAL");
        } else if (itemId == R.id.nav_social) {
            NavController nav = androidx.navigation.Navigation
                    .findNavController(this, R.id.nav_host_fragment);
            nav.popBackStack(R.id.nav_home, false);
            nav.navigate(R.id.nav_social_container);
        }

    }

    private void setupNavigationAndFab() {
        // Nút FAB
        FloatingActionButton fabAddTransaction = findViewById(R.id.fab_add_transaction);
        fabAddTransaction.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddTransactionActivity.class);
            intent.putExtra("WORKSPACE_ID", currentWorkspaceId);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_up, R.anim.stay);
        });

        // Thanh điều hướng Bottom
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        //Tách bottom khỏi thanh hệ thống
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, windowInsets) -> {
            // Lấy độ cao của thanh tác vụ hệ thống (navigation bar)
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());

            // Đổi 35dp (theo layout_marginBottom của XML) sang Pixel
            int margin20dp = (int) (20 * getResources().getDisplayMetrics().density);

            // Ép Insets thành Margin
            android.view.ViewGroup.MarginLayoutParams mlp = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.bottomMargin = insets.bottom + margin20dp;
            v.setLayoutParams(mlp);

            // Khóa mõm, cấm hệ thống tự đẻ thêm padding
            //return androidx.core.view.WindowInsetsCompat.CONSUMED;
            return windowInsets;
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        NavigationUI.setupWithNavController(bottomNav, navController);
        View navHostView = findViewById(R.id.nav_host_fragment);

        // Fix lỗi chìm FAB ở màn Setting + Ẩn/hiện bottom nav
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            if (id == R.id.nav_workspace_container || id == R.id.nav_social_container) {
                // Giấu nhẹm thanh Nav và nút FAB của Ví Cá nhân đi
                bottomNav.setVisibility(View.GONE);
                fabAddTransaction.hide();
                navHostView.setPadding(0, 0, 0, 0); // Xóa padding để thanh nav bên quỹ không dôi lên
            } else {
                // Đang ở ngoài Ví Cá nhân -> Hiện lại thanh Nav
                bottomNav.setVisibility(View.VISIBLE);
                navHostView.setPadding(0, 0, 0, 0);

                if (id == R.id.nav_settings) {
                    fabAddTransaction.hide();
                } else {
                    fabAddTransaction.show();
                }
            }
        });
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                setupNotifications();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            setupNotifications();
        }
    }

    private void setupNotifications() {
        WorkScheduler.scheduleDailyReminder(this);
        FirebaseManager.getInstance().getFcmToken(new FirebaseManager.DataCallback<String>() {
            @Override
            public void onSuccess(String token) {
            }

            @Override
            public void onError(String message) {
            }
        });
    }
}