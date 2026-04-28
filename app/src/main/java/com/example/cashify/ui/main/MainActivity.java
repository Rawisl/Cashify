package com.example.cashify.ui.main;

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
import com.example.cashify.ui.auth.EditProfileActivity;
import com.example.cashify.ui.auth.LoginActivity;
import com.example.cashify.utils.ImageHelper;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.navigation.NavigationView; // Thêm thư viện này
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

import java.util.List;

public class MainActivity extends AppCompatActivity {
    // Tạo một lá cờ hiệu
    boolean keepSplash = true;
    private MainViewModel mainViewModel;

    // KHAI BÁO BIẾN CHO SIDEBAR
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
        TransactionViewModel viewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        super.onCreate(savedInstanceState);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // NẾU CHƯA ĐĂNG NHẬP:
            //  Chuyển hướng sang màn hình Login
            startActivity(new Intent(this, LoginActivity.class));
            //  Đóng MainActivity lại để user không bấm Back về đây được
            finish();
            //  Quan trọng nhất: Gọi return để DỪNG NGAY mọi code phía dưới (không chạy DB, không load UI)
            return;
        }

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Nếu đã lọt qua if (tức là đã đăng nhập), lấy UID ra xài
        String currentUserId = currentUser.getUid();
        android.util.Log.d("AUTH_FLOW", "Signed in successfully! UID: " + currentUserId);

        //reset và seed data - chạy ngầm
        new Thread(() -> {
//            try {
//                AppDatabase db = AppDatabase.getInstance(MainActivity.this);
//                CategoryDao categoryDao = db.categoryDao();
//
//                // nếu muốn reset database, bật 2 dòng này chạy 1 lần
//                // db.transactionDao().deleteAllTransactions();
//                //categoryDao.deleteAllCategories();
//
//                // Nạp Category trước - chờ xong mới đi tiếp
//                DatabaseSeeder.seedIfEmpty(MainActivity.this);
//
//                // Lấy danh sách category để kiểm tra + lấy ID thực tế
//                List<Category> checkList = categoryDao.getAllActive();
//
//                // Log ID thực tế để debug
//                if (checkList != null) {
//                    for (Category c : checkList) {
//                        android.util.Log.d("BACKEND_TEST", "Category ID: " + c.id + " | Name: " + c.name + " | Type: " + c.type);
//                    }
//                }
//
//                if (checkList != null && !checkList.isEmpty()) {
//                    int count = db.transactionDao().countTransactions();
//                    if (count == 0) {
//                        // Truyền checkList vào FakeDataSeeder để dùng ID thực tế
//                        FakeDataSeeder.seed(MainActivity.this, checkList);
//                        android.util.Log.d("BACKEND_TEST", "Đã nạp giao dịch mẫu!");
//                        viewModel.fetchHistoryData();
//                    } else {
//                        android.util.Log.d("BACKEND_TEST", "Đã có " + count + " giao dịch. Bỏ qua seed!");
//                        viewModel.fetchHistoryData();
//                    }
//                } else {
//                    android.util.Log.e("BACKEND_TEST", "LỖI: Danh mục vẫn trống rỗng!");
//                }
//            } catch (Exception e) {
//                android.util.Log.e("BACKEND_TEST", "Có biến rồi An ơi: " + e.getMessage());
//            }

            try {
                AppDatabase db = AppDatabase.getInstance(MainActivity.this);

                // --- KIỂM TRA XEM CÓ ĐỔI NGƯỜI DÙNG KHÔNG ---
                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                String lastUserId = prefs.getString("last_uid", "");

                if (!lastUserId.equals(currentUserId)) {
                    // Nếu UID khác UID lần trước đăng nhập -> Đây là người mới hoặc đổi acc
                    // Dọn sạch bàn làm việc để đón khách mới
                    db.clearAllTables();

                    // Lưu lại UID mới này để lần sau không xóa nữa
                    prefs.edit().putString("last_uid", currentUserId).apply();
                    android.util.Log.d("AUTH_FLOW", "New user detected! Destroying old data");
                }

                // --- NẠP CATEGORY (DANH MỤC) ---
                // Phải nạp cái này trước thì các giao dịch tải về mới có ID để liên kết (Foreign Key)
                DatabaseSeeder.seedIfEmpty(MainActivity.this);

                // --- ĐỒNG BỘ DỮ LIỆU ---
                int count = db.transactionDao().countTransactions();
                if (count == 0) {
                    // Nếu máy trống rỗng -> Lên Firebase lấy về
                    Log.d("AUTH_FLOW", "Database is empty. Fetching data from server...");
                    mainViewModel.syncAllDataFromServer(MainActivity.this);
                } else {
                    // Nếu đã có data (người cũ quay lại) -> Chỉ cần làm mới UI
                    Log.d("AUTH_FLOW", "Data already exists.");
                    viewModel.fetchHistoryData();
                }

                mainViewModel.startRealTimeSync(MainActivity.this);

            } catch (Exception e) {
                Log.e("AUTH_FLOW", "Database error: " + e.getMessage());
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

        // ============================================================
        // TODO 1: SETUP MAIN VIEWMODEL & WORKSPACE
        // - Khởi tạo mainViewModel bằng ViewModelProvider.
        // - Gọi mainViewModel.loadWorkspaces(currentUserId) để lấy danh sách Quỹ.
        // ============================================================

        initSidebar(currentUser); // Truyền currentUser vào để lát gán Tên/Email lên Header
        setupNavigationAndFab();
        //        setupFab(); // Giữ nguyên logic FAB

        // ============================================================
        // TODO 2: LẮNG NGHE SỰ KIỆN ĐỔI WORKSPACE
        // - observe(mainViewModel.getCurrentWorkspace()):
        //   Khi Workspace thay đổi, hãy đóng Side Bar (drawerLayout.closeDrawers())
        //   và ra lệnh cho các Fragment con tải lại dữ liệu theo ID mới.
        // ============================================================
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

        // ============================================================
        // TODO 5: SETUP NOTIFICATIONS (Kiểm tra quyền & Lập lịch)
        // Gọi ở cuối onCreate, chỉ khi user đã đăng nhập hợp lệ
        // ============================================================
        checkNotificationPermission();
        // ============================================================
// XỬ LÝ NÚT BACK (CHUẨN ANDROID 13+)
// ============================================================
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    // Nếu Sidebar đang lòi ra -> Thụt nó vào
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    // Nếu Sidebar đang đóng -> Tạm tắt cái bắt sự kiện này đi để hệ thống tự xử lý (Thoát app hoặc lùi Fragment)
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    // Bật lại để sẵn sàng cho lần bấm Back tiếp theo
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
    private void initSidebar(FirebaseUser currentUser) {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        updateSidebarProfileUI();
        // 1. XỬ LÝ CLICK HEADER (MỞ PROFILE)
        View headerView = navigationView.getHeaderView(0);
        LinearLayout headerProfileLayout = headerView.findViewById(R.id.headerProfileLayout);

        headerProfileLayout.setOnClickListener(v -> {
            // Thụt cái Sidebar vào trước cho mượt
            drawerLayout.closeDrawer(GravityCompat.START);

            // Khởi tạo Intent để chuyển sang màn hình EditProfileActivity vừa code
            Intent intent = new Intent(MainActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        // 2. XỬ LÝ BẤM CÁC NÚT TRONG SIDEBAR
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_workspace_personal) {
                ToastHelper.show(this, "Đã chọn Ví Cá Nhân");
                // TODO: Gọi mainViewModel đổi ID về Cá nhân
            } else if (id == R.id.nav_add_workspace) {
                ToastHelper.show(this, "Mở form Tạo Quỹ Mới");
            } else if (id == R.id.nav_friends) {
                ToastHelper.show(this, "Mở danh sách Bạn bè");
            }

            drawerLayout.closeDrawer(GravityCompat.START); // Bấm xong phải thụt cái Sidebar vào
            return true;
        });
    }

    private void setupNavigationAndFab() {
        // Nút FAB
        FloatingActionButton fabAddTransaction = findViewById(R.id.fab_add_transaction);
        fabAddTransaction.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddTransactionActivity.class);
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
            if (id == R.id.nav_settings) { // Giả sử ID fragment settings của ghệ là nav_settings
                fabAddTransaction.hide();
            } else {
                fabAddTransaction.show();
            }
        });
    }
    // ============================================================
    // CÁC HÀM XỬ LÝ NOTIFICATION ĐƯỢC THÊM MỚI
    // ============================================================
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                // Đã có quyền -> Bật thông báo
                setupNotifications();
            } else {
                // Chưa có quyền -> Xin quyền từ user
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // Android 12 trở xuống mặc định có quyền
            setupNotifications();
        }
    }

    private void setupNotifications() {
        // 1. Kích hoạt WorkManager nhắc nhở mỗi ngày (Offline)
        WorkScheduler.scheduleDailyReminder(this);
        Log.d("NOTIF", "Đã lên lịch nhắc nhở nhập chi tiêu.");

        // 2. Lấy FCM Token mới nhất lưu lên Firestore để nhận Push Notification (Online)
        FirebaseManager.getInstance().getFcmToken(new FirebaseManager.DataCallback<String>() {
            @Override
            public void onSuccess(String token) {
                Log.d("NOTIF", "Cập nhật FCM Token thành công lên Firestore.");
            }

            @Override
            public void onError(String message) {
                Log.e("NOTIF", "Lỗi khi lấy FCM Token: " + message);
            }
        });
    }
    //Xử lý gán Tên, Email và Avatar lên Header
    private void updateSidebarProfileUI() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Bọc check null kỹ để chống crash app
        if (currentUser != null && navigationView != null && navigationView.getHeaderCount() > 0) {
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                TextView tvName = headerView.findViewById(R.id.tvNameHeader);
                TextView tvEmail = headerView.findViewById(R.id.tvEmailHeader);
                de.hdodenhof.circleimageview.CircleImageView imgAvatarHeader = headerView.findViewById(R.id.imgAvatarHeader);

                // Cập nhật Tên & Email
                if (tvName != null && currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                    tvName.setText(currentUser.getDisplayName());
                }
                if (tvEmail != null && currentUser.getEmail() != null) {
                    tvEmail.setText(currentUser.getEmail());
                }

                // Load ảnh trực tiếp qua ImageHelper chuẩn gọn
                if (imgAvatarHeader != null && currentUser.getPhotoUrl() != null) {
                    ImageHelper.loadAvatar(currentUser.getPhotoUrl(), imgAvatarHeader);
                }
            }
        }
    }
}