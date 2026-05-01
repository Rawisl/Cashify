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
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.view.GravityCompat; // Thêm thư viện này cho Sidebar
import androidx.drawerlayout.widget.DrawerLayout; // Thêm thư viện này

import com.bumptech.glide.Glide;
import com.example.cashify.data.model.Workspace; // Thêm import này
import com.example.cashify.ui.auth.EditProfileActivity;
import com.example.cashify.ui.auth.LoginActivity;
import com.example.cashify.ui.workspace.WorkspaceDetailActivity;
import com.example.cashify.utils.ImageHelper;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.example.cashify.ui.transactions.AddTransactionActivity;
import com.example.cashify.R;
import com.example.cashify.data.model.Category;
import com.example.cashify.data.local.CategoryDao;
import com.example.cashify.ui.transactions.TransactionViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.cashify.data.local.AppDatabase;
import com.example.cashify.data.local.DatabaseSeeder;
import com.example.cashify.data.local.FakeDataSeeder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.cashify.data.remote.FirebaseManager;
import com.example.cashify.utils.WorkScheduler;

import java.util.HashMap; // Thêm import này
import java.util.List;
import java.util.Map; // Thêm import này

public class MainActivity extends AppCompatActivity {
    // Tạo một lá cờ hiệu
    boolean keepSplash = true;
    private MainViewModel mainViewModel;
    private TransactionViewModel transactionViewModel; // Tách riêng ViewModel để dễ gọi

    private String currentWorkspaceId = "PERSONAL";

    // ĐỊNH NGHĨA ID ĐỂ QUẢN LÝ MENU ĐỘNG
    private final int WORKSPACE_GROUP_ID = 999;
    // Dùng Map để lưu ID của MenuItem và ID của Workspace tương ứng (để biết click vào đâu)
    private final Map<Integer, String> menuIdToWorkspaceIdMap = new HashMap<>();

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
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

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        String currentUserId = currentUser.getUid();
        android.util.Log.d("AUTH_FLOW", "Signed in successfully! UID: " + currentUserId);

        // ============================================================
        // [ĐÃ CẬP NHẬT] BƯỚC 1: KÍCH HOẠT TẢI DANH SÁCH QUỸ
        // ============================================================
        mainViewModel.loadWorkspaces(currentUserId);


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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initSidebar(); // currentUser đã lấy qua FirebaseAuth rồi, ko cần truyền vào nữa
        setupNavigationAndFab();

        // ============================================================
        // [ĐÃ CẬP NHẬT] BƯỚC 2: LẮNG NGHE ĐỂ VẼ SIDEBAR MENU (REAL-TIME)
        // ============================================================
        mainViewModel.getWorkspaces().observe(this, workspaces -> {
            if (workspaces != null) {
                // Gọi hàm vẽ menu động
                updateSidebarMenu(workspaces);
            }
        });


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

    @Override
    protected void onResume() {
        super.onResume();
        updateSidebarProfileUI();
    }

    private void initSidebar() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        updateSidebarProfileUI();

        // 1. XỬ LÝ CLICK HEADER (MỞ PROFILE)
        View headerView = navigationView.getHeaderView(0);
        LinearLayout headerProfileLayout = headerView.findViewById(R.id.headerProfileLayout);

        headerProfileLayout.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            Intent intent = new Intent(MainActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        // 2. XỬ LÝ BẤM CÁC NÚT TRONG SIDEBAR
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            // --- LÀM MỚI CHỖ NÀY: KIỂM TRA XEM CÓ BẤM VÀO QUỸ ĐỘNG KO ---
            if (menuIdToWorkspaceIdMap.containsKey(id)) {
                // Lấy ID Quỹ thật từ Map ra
                String clickedWorkspaceId = menuIdToWorkspaceIdMap.get(id);

                // Mở màn hình Chi tiết Quỹ
                Intent intent = new Intent(this, WorkspaceDetailActivity.class);
                intent.putExtra("WORKSPACE_ID", clickedWorkspaceId);
                startActivity(intent);

                // TODO: Gọi ViewModel đổi Workspace hiện tại nếu màn hình Home/Dashboard của ghệ có dùng
            }
            // --- CÁC MENU TĨNH CŨ ---
            else if (id == R.id.nav_workspace_personal) {
                currentWorkspaceId = "PERSONAL";
                ToastHelper.show(this, "Đã chọn Ví Cá Nhân");
                // TODO: Update UI Fragment theo ví cá nhân
            }
            else if (id == R.id.nav_add_workspace) {
                drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
                com.example.cashify.ui.workspace.AddWorkspaceBottomSheet bottomSheet = new com.example.cashify.ui.workspace.AddWorkspaceBottomSheet();
                bottomSheet.show(getSupportFragmentManager(), "AddWorkspaceBottomSheet");
            }
            else if (id == R.id.nav_friends) {
                ToastHelper.show(this, "Mở danh sách Bạn bè");
            }
            // XÓA cái nav_workspace_dummy cũ đi vì giờ mình xài menu động rồi

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    // ============================================================
    // [ĐÃ THÊM MỚI] BƯỚC 3: HÀM VẼ MENU QUỸ ĐỘNG LÊN SIDEBAR
    // ============================================================
    private void updateSidebarMenu(List<Workspace> workspaces) {
        android.view.Menu menu = navigationView.getMenu();

        // 1. Xóa các Quỹ cũ đã vẽ lần trước (tránh bị đúp)
        menu.removeGroup(WORKSPACE_GROUP_ID);
        menuIdToWorkspaceIdMap.clear();

        // 2. Bơm danh sách Quỹ mới vào menu
        for (Workspace w : workspaces) {
            // Tạo 1 ID duy nhất cho MenuItem bằng cách lấy hashCode của Quỹ ID
            int itemId = w.getId().hashCode();

            // Lưu lại mối quan hệ: "Bấm vào itemId này thì tức là chọn Quỹ ID này"
            menuIdToWorkspaceIdMap.put(itemId, w.getId());

            // Vẽ lên menu: add(groupId, itemId, order, title)
            // Nhớ đổi R.drawable.ic_other thành icon Quỹ Nhóm của ghệ cho đẹp
            android.view.MenuItem item = menu.add(WORKSPACE_GROUP_ID, itemId, android.view.Menu.NONE, w.getName());
            item.setIcon(R.drawable.ic_other); // <--- Đổi icon ở đây
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
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        NavigationUI.setupWithNavController(bottomNav, navController);

        // Fix lỗi chìm FAB ở màn Setting
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            // Đổi R.id.nav_settings thành đúng ID fragment settings của ghệ
            if (id == R.id.nav_settings) {
                fabAddTransaction.hide();
            } else {
                fabAddTransaction.show();
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

    private void updateSidebarProfileUI() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null && navigationView != null && navigationView.getHeaderCount() > 0) {
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                TextView tvName = headerView.findViewById(R.id.tvNameHeader);
                TextView tvEmail = headerView.findViewById(R.id.tvEmailHeader);
                de.hdodenhof.circleimageview.CircleImageView imgAvatarHeader = headerView.findViewById(R.id.imgAvatarHeader);

                if (tvName != null && currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                    tvName.setText(currentUser.getDisplayName());
                }
                if (tvEmail != null && currentUser.getEmail() != null) {
                    tvEmail.setText(currentUser.getEmail());
                }

                if (imgAvatarHeader != null && currentUser.getPhotoUrl() != null) {
                    ImageHelper.loadAvatar(currentUser.getPhotoUrl().toString(), imgAvatarHeader);
                }
            }
        }
    }
}