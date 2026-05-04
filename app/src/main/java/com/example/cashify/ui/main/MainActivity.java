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

public class MainActivity extends AppCompatActivity {
    // Tạo một lá cờ hiệu
    boolean keepSplash = true;
    private MainViewModel mainViewModel;
    private TransactionViewModel transactionViewModel; // Tách riêng ViewModel để dễ gọi

    private String currentWorkspaceId = "PERSONAL";

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

            // ĐÁNH DẤU HIGHLIGHT: Chỉ tô màu nếu bấm vào Ví Cá Nhân hoặc Quỹ Nhóm
            if (id == R.id.nav_workspace_personal || menuIdToWorkspaceIdMap.containsKey(id)) {
                item.setChecked(true);
            }

            // --- KIỂM TRA XEM CÓ BẤM VÀO QUỸ ĐỘNG KO ---
            if (menuIdToWorkspaceIdMap.containsKey(id)) {
                String clickedWorkspaceId = menuIdToWorkspaceIdMap.get(id);

                Bundle bundle = new Bundle();
                bundle.putString("WORKSPACE_ID", clickedWorkspaceId);

                NavController navController = androidx.navigation.Navigation.findNavController(this, R.id.nav_host_fragment);
                navController.navigate(R.id.nav_workspace_container, bundle);

            }
            // --- CÁC MENU TĨNH ---
            else if (id == R.id.nav_workspace_personal) {
                currentWorkspaceId = "PERSONAL";
                NavController navController = androidx.navigation.Navigation.findNavController(this, R.id.nav_host_fragment);
                navController.popBackStack(R.id.nav_home, false);
                transactionViewModel.fetchHistoryData("PERSONAL");
            }
            else if (id == R.id.nav_add_workspace) {
                drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START);
                com.example.cashify.ui.workspace.AddWorkspaceBottomSheet bottomSheet = new com.example.cashify.ui.workspace.AddWorkspaceBottomSheet();
                bottomSheet.show(getSupportFragmentManager(), "AddWorkspaceBottomSheet");
                // Mở BottomSheet thì KHÔNG return true ở đây để tránh bị lỗi hiển thị UI
                return false;
            }
            else if (id == R.id.nav_friends) {
                Intent intent = new Intent(MainActivity.this, com.example.cashify.ui.FriendsActivity.FriendsActivity.class);
                startActivity(intent);
            }

            drawerLayout.closeDrawer(GravityCompat.START);

            // TRẢ VỀ TRUE ĐỂ LƯU MÀU HIGHLIGHT
            return true;
        });
    }

    // ============================================================
    // [ĐÃ THÊM MỚI] BƯỚC 3: HÀM VẼ MENU QUỸ ĐỘNG LÊN SIDEBAR
    // ============================================================
    private void updateSidebarMenu(List<Workspace> workspaces) {
        android.view.Menu menu = navigationView.getMenu();

        // 1. Xóa các Quỹ động cũ (Dùng Map để xóa chính xác, KHÔNG dùng removeGroup vì sẽ xóa nhầm Personal và Add Group)
        for (Integer itemId : menuIdToWorkspaceIdMap.keySet()) {
            menu.removeItem(itemId);
        }
        menuIdToWorkspaceIdMap.clear();

        // 2. Bơm danh sách Quỹ mới vào chung nhóm R.id.group_workspaces
        for (Workspace w : workspaces) {
            int itemId = w.getId().hashCode();
            menuIdToWorkspaceIdMap.put(itemId, w.getId());

            // Vẽ lên menu: add(groupId, itemId, order, title)
            // Để order = 1 để nó chèn vào giữa (nằm dưới Ví cá nhân, nằm trên nút Add group)
            android.view.MenuItem item = menu.add(R.id.group_workspaces, itemId, 1, w.getName());

            String iconName = w.getIconName();
            if (iconName == null || iconName.isEmpty()) iconName = "ic_other";

            int iconResId = getResources().getIdentifier(iconName, "drawable", getPackageName());
            if (iconResId != 0) {
                item.setIcon(iconResId);
            } else {
                item.setIcon(R.drawable.ic_other);
            }

            // Kích hoạt checkable để lưu trạng thái highlight
            item.setCheckable(true);
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
        View navHostView = findViewById(R.id.nav_host_fragment);
        // Fix lỗi chìm FAB ở màn Setting + Ẩn/hiện bottom nav
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            //
            if (id == R.id.nav_workspace_container) {
                // Giấu nhẹm thanh Nav và nút FAB của Ví Cá nhân đi
                bottomNav.setVisibility(View.GONE);
                fabAddTransaction.hide();
                navHostView.setPadding(0, 0, 0, 0); // Xóa padding để thanh nav bên quỹ không dôi lên
            } else {
                // Đang ở ngoài Ví Cá nhân -> Hiện lại thanh Nav
                bottomNav.setVisibility(View.VISIBLE);
                int padding56dp = (int) (56 * getResources().getDisplayMetrics().density);
                navHostView.setPadding(0, 0, 0, padding56dp);
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