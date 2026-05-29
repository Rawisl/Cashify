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

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.cashify.R;
import com.example.cashify.data.local.AppDatabase;
import com.example.cashify.ui.auth.LoginActivity;
import com.example.cashify.ui.transactions.AddTransactionActivity;
import com.example.cashify.ui.transactions.TransactionViewModel;
import com.example.cashify.utils.ToastHelper;
import com.example.cashify.utils.WorkScheduler;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends BaseActivity {
    private boolean keepSplash = true;
    private TransactionViewModel transactionViewModel;
    private String currentWorkspaceId = "PERSONAL";

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
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        super.onCreate(savedInstanceState);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        setupBaseSidebar();

        String currentUserId = currentUser.getUid();
        Log.d("AUTH_FLOW", "Signed in successfully! UID: " + currentUserId);

        setupDeferredNavigationFromIntent();
        seedAndSyncLocalData(currentUserId);

        new Handler(Looper.getMainLooper()).postDelayed(() -> keepSplash = false, 2000);
        splashScreen.setKeepOnScreenCondition(() -> keepSplash);

        setupNavigationAndFab();
        maybeOpenCreateWorkspaceSheet();
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Cập nhật lại Intent mới nhất
        setupDeferredNavigationFromIntent(); // Chạy lại logic kiểm tra bưu kiện
    }

    private void setupDeferredNavigationFromIntent() {
        if (getIntent().hasExtra("OPEN_WORKSPACE_ID")) {
            currentWorkspaceId = getIntent().getStringExtra("OPEN_WORKSPACE_ID");
            navigateWhenHome(controller -> {
                Bundle bundle = new Bundle();
                bundle.putString("WORKSPACE_ID", currentWorkspaceId);
                controller.navigate(R.id.nav_workspace_container, bundle);
            });
        }

        if (getIntent().getBooleanExtra("OPEN_SOCIAL", false)) {
            navigateWhenHome(controller -> controller.navigate(R.id.nav_social_container));
        }

        if (getIntent().getBooleanExtra("ACTION_EDIT_POST", false)) {
            navigateWhenHome(controller -> {
                Bundle bundle = new Bundle();
                bundle.putString("edit_post_id", getIntent().getStringExtra("edit_post_id"));
                bundle.putString("edit_post_content", getIntent().getStringExtra("edit_post_content"));

                // Hứng luôn cục Milestone (nếu là bài Cột mốc)
                if (getIntent().hasExtra("edit_milestone_data")) {
                    bundle.putString("edit_milestone_data", getIntent().getStringExtra("edit_milestone_data"));
                }

                // Ném Bundle sang màn hình Đăng bài
                controller.navigate(R.id.nav_post_feed, bundle);
            });
        }

        if (getIntent().getBooleanExtra("OPEN_POST_FEED", false)) {
            navigateWhenHome(controller -> controller.navigate(R.id.nav_post_feed));
        }
    }

    private void navigateWhenHome(NavAction action) {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            return;
        }

        NavController navController = navHostFragment.getNavController();
        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController controller,
                                             @NonNull NavDestination destination,
                                             @Nullable Bundle arguments) {
                if (destination.getId() == R.id.nav_home) {
                    action.run(controller);
                    controller.removeOnDestinationChangedListener(this);
                }
            }
        });
    }

    private void seedAndSyncLocalData(String currentUserId) {
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(MainActivity.this);

                SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                String lastUserId = prefs.getString("last_uid", "");

                if (!lastUserId.equals(currentUserId)) {
                    db.clearAllTables();
                    prefs.edit().putString("last_uid", currentUserId).apply();
                    Log.d("AUTH_FLOW", "New user detected! Destroying old data");
                }

                com.example.cashify.data.local.DatabaseSeeder.seedIfEmpty(MainActivity.this);

                int count = db.transactionDao().countTransactions("PERSONAL");
                if (count == 0) {
                    Log.d("AUTH_FLOW", "Database is empty. Fetching data from server...");
                    mainViewModel.syncAllDataFromServer(MainActivity.this);
                } else {
                    Log.d("AUTH_FLOW", "Data already exists.");
                    transactionViewModel.fetchHistoryData(currentWorkspaceId);
                }

                mainViewModel.startRealTimeSync(MainActivity.this);
            } catch (Exception e) {
                Log.e("AUTH_FLOW", "Database error: " + e.getMessage());
            }
        }).start();
    }

    @Override
    protected void onNavigationItemSelected(int itemId) {
        if (menuIdToWorkspaceIdMap.containsKey(itemId)) {
            currentWorkspaceId = menuIdToWorkspaceIdMap.get(itemId);
            NavController nav = androidx.navigation.Navigation.findNavController(this, R.id.nav_host_fragment);
            nav.popBackStack(R.id.nav_home, false);

            Bundle bundle = new Bundle();
            bundle.putString("WORKSPACE_ID", currentWorkspaceId);
            nav.navigate(R.id.nav_workspace_container, bundle);
        } else if (itemId == R.id.nav_workspace_personal) {
            currentWorkspaceId = "PERSONAL";
            androidx.navigation.Navigation.findNavController(this, R.id.nav_host_fragment)
                    .popBackStack(R.id.nav_home, false);
            transactionViewModel.fetchHistoryData("PERSONAL");
        } else if (itemId == R.id.nav_post_feed) {
            openPostFeedFromDrawer(null);
        } else if (itemId == R.id.nav_social) {
            NavController nav = androidx.navigation.Navigation.findNavController(this, R.id.nav_host_fragment);
            if (nav.getCurrentDestination() == null
                    || nav.getCurrentDestination().getId() != R.id.nav_social_container) {
                nav.popBackStack(R.id.nav_home, false);
                nav.navigate(R.id.nav_social_container);
            }
        }
    }

    private void setupNavigationAndFab() {
        FloatingActionButton fabAddTransaction = findViewById(R.id.fab_add_transaction);
        fabAddTransaction.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddTransactionActivity.class);
            intent.putExtra("WORKSPACE_ID", currentWorkspaceId);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_up, R.anim.stay);
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars());
            int margin20dp = (int) (20 * getResources().getDisplayMetrics().density);

            android.view.ViewGroup.MarginLayoutParams mlp =
                    (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.bottomMargin = insets.bottom + margin20dp;
            v.setLayoutParams(mlp);

            return windowInsets;
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            return;
        }

        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(bottomNav, navController);

        View navHostView = findViewById(R.id.nav_host_fragment);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            syncSidebarSelection(id);

            if (id == R.id.nav_workspace_container
                    || id == R.id.nav_social_container
                    || id == R.id.nav_post_feed) {
                bottomNav.setVisibility(View.GONE);
                fabAddTransaction.hide();
                navHostView.setPadding(0, 0, 0, 0);
            } else {
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

    public void openCreatePostScreen() {
        openPostFeedFromDrawer(null);
    }

    public void openCreatePostScreen(@Nullable String categoryKey) {
        openPostFeedFromDrawer(categoryKey);
    }

    private void openPostFeedFromDrawer(@Nullable String categoryKey) {
        NavController navController = androidx.navigation.Navigation.findNavController(this, R.id.nav_host_fragment);

        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.nav_post_feed) {
            return;
        }

        if (navController.getGraph().findNode(R.id.nav_post_feed) == null) {
            Log.e("NAV_DRAWER", "Post feed destination is missing from nav_graph.");
            ToastHelper.show(this, "Post screen is unavailable.");
            return;
        }

        try {
            Bundle args = null;
            if (categoryKey != null && !categoryKey.trim().isEmpty()) {
                args = new Bundle();
                args.putString("categoryKey", categoryKey);
            }
            navController.navigate(R.id.nav_post_feed, args);
        } catch (IllegalArgumentException e) {
            Log.e("NAV_DRAWER", "Failed to navigate to Post feed from drawer.", e);
            ToastHelper.show(this, "Could not open Post screen.");
        }
    }

    private void syncSidebarSelection(int destinationId) {
        if (navigationView == null) {
            return;
        }

        android.view.Menu menu = navigationView.getMenu();
        if (destinationId == R.id.nav_post_feed) {
            setSidebarItemChecked(menu, R.id.nav_post_feed);
        } else if (destinationId == R.id.nav_social_container) {
            setSidebarItemChecked(menu, R.id.nav_social);
        } else if (destinationId == R.id.nav_home
                || destinationId == R.id.nav_budget
                || destinationId == R.id.nav_transaction
                || destinationId == R.id.nav_settings) {
            setSidebarItemChecked(menu, R.id.nav_workspace_personal);
        }
    }

    private void setSidebarItemChecked(android.view.Menu menu, int itemId) {
        android.view.MenuItem item = menu.findItem(itemId);
        if (item != null) {
            item.setChecked(true);
        }
    }

    private void maybeOpenCreateWorkspaceSheet() {
        if (getIntent().getBooleanExtra("OPEN_CREATE_WORKSPACE", false)) {
            new com.example.cashify.ui.workspace.AddWorkspaceBottomSheet()
                    .show(getSupportFragmentManager(), "AddWorkspaceBottomSheet");
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
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
        com.example.cashify.data.remote.FirebaseManager.getInstance().getFcmToken(
                new com.example.cashify.data.remote.FirebaseManager.DataCallback<String>() {
                    @Override
                    public void onSuccess(String token) {
                    }

                    @Override
                    public void onError(String message) {
                    }
                });
    }

    private interface NavAction {
        void run(NavController controller);
    }
}
