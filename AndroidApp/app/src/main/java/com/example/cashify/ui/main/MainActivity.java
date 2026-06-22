package com.example.cashify.ui.main;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
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
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.cashify.R;
import com.example.cashify.data.repository.AuthRepositoryImpl;
import com.example.cashify.data.repository.IAuthRepository;
import com.example.cashify.ui.auth.LoginActivity;
import com.example.cashify.ui.transactions.AddTransactionActivity;
import com.example.cashify.ui.transactions.TransactionViewModel;
import com.example.cashify.ui.workspace.AddWorkspaceBottomSheet;
import com.example.cashify.utils.ToastHelper;
import com.example.cashify.utils.WorkScheduler;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends BaseActivity {
    private boolean keepSplash = true;
    private TransactionViewModel transactionViewModel;
    private String currentWorkspaceId = "PERSONAL";

    // Tách Auth logic ra Repository cho chuẩn Clean Architecture
    private final IAuthRepository authRepository = new AuthRepositoryImpl();

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
        super.onCreate(savedInstanceState);

        // Cấp phát ViewModels
        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        setupPersonalWorkspaceSystemBars();

        // Kiểm tra Auth qua Repository
        if (!authRepository.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        setupBaseSidebar();

        String currentUserId = authRepository.getCurrentUserId();
        Log.d("AUTH_FLOW", "Signed in successfully! UID: " + currentUserId);

        setupDeferredNavigationFromIntent();

        //Đẩy trách nhiệm dọn dẹp Database về cho ViewModel
        mainViewModel.checkAndSeedLocalData(currentUserId, () -> {
            Log.d("AUTH_FLOW", "Data ready. Fetching history for UI.");
            transactionViewModel.fetchHistoryData(currentWorkspaceId, "", true);
        });

        // Tự động tắt Splash Screen
        new Handler(Looper.getMainLooper()).postDelayed(() -> keepSplash = false, 1400);
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

    private void setupPersonalWorkspaceSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.parseColor("#FBFCFF"));
        getWindow().setNavigationBarColor(Color.parseColor("#F7F9FF"));
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = getWindow().getDecorView().getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            getWindow().getDecorView().setSystemUiVisibility(flags);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        setupDeferredNavigationFromIntent();
    }

    private void setupDeferredNavigationFromIntent() {
        if (getIntent().hasExtra("OPEN_WORKSPACE_ID")) {
            currentWorkspaceId = getIntent().getStringExtra("OPEN_WORKSPACE_ID");
            navigateWhenHome(controller -> {
                Bundle bundle = new Bundle();
                bundle.putString("WORKSPACE_ID", currentWorkspaceId);
                controller.navigate(R.id.nav_workspace_container, bundle);
                getIntent().removeExtra("OPEN_WORKSPACE_ID");
            });
        }

        if (getIntent().getBooleanExtra("OPEN_SOCIAL", false)) {
            navigateWhenHome(controller -> {
                controller.navigate(R.id.nav_social_container);
                getIntent().removeExtra("OPEN_SOCIAL");
            });
        }

        if (getIntent().getBooleanExtra("ACTION_EDIT_POST", false)) {
            navigateWhenHome(controller -> {
                Bundle bundle = new Bundle();
                bundle.putString("edit_post_id", getIntent().getStringExtra("edit_post_id"));
                bundle.putString("edit_post_content", getIntent().getStringExtra("edit_post_content"));
                bundle.putString("edit_post_title", getIntent().getStringExtra("edit_post_title"));
                bundle.putString("edit_post_image", getIntent().getStringExtra("edit_post_image"));

                if (getIntent().hasExtra("edit_milestone_data")) {
                    bundle.putString("edit_milestone_data", getIntent().getStringExtra("edit_milestone_data"));
                }

                controller.navigate(R.id.nav_social_container);
                controller.navigate(R.id.nav_post_feed, bundle);
                getIntent().removeExtra("ACTION_EDIT_POST");
            });
        }

        if (getIntent().getBooleanExtra("OPEN_POST_FEED", false)) {
            navigateWhenHome(controller -> {
                controller.navigate(R.id.nav_social_container);
                controller.navigate(R.id.nav_post_feed);
                getIntent().removeExtra("OPEN_POST_FEED");
            });
        }

        //Xử lý khi mở màn tạo bài kèm data Milestone (Từ Budget/Goals)
        if (getIntent().getBooleanExtra("ACTION_CREATE_MILESTONE", false)) {
            navigateWhenHome(controller -> {
                Bundle bundle = new Bundle();
                if (getIntent().hasExtra("milestone_limit")) {
                    bundle.putLong("milestone_limit", getIntent().getLongExtra("milestone_limit", 0));
                    bundle.putLong("milestone_spent", getIntent().getLongExtra("milestone_spent", 0));
                    bundle.putString("milestone_period", getIntent().getStringExtra("milestone_period"));
                    bundle.putString("milestone_label", getIntent().getStringExtra("milestone_label"));
                }

                controller.navigate(R.id.nav_social_container);
                controller.navigate(R.id.nav_post_feed, bundle);

                getIntent().removeExtra("ACTION_CREATE_MILESTONE");
            });
        }

        if (getIntent().hasExtra("OPEN_USER_PROFILE")) {
            String profileId = getIntent().getStringExtra("OPEN_USER_PROFILE");
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment);
            if (navHostFragment != null) {
                NavController controller = navHostFragment.getNavController();
                Bundle args = new Bundle();
                args.putString("USER_ID", profileId);
                args.putBoolean("OPEN_OTHER_PROFILE", true);
                args.putBoolean("FINISH_ON_BACK", getIntent().getBooleanExtra("FINISH_ON_BACK", false));
                
                // Navigate to social container, pass arguments so it can navigate internally
                controller.navigate(R.id.nav_social_container, args);
            }
            getIntent().removeExtra("OPEN_USER_PROFILE");
        }
    }

    private void navigateWhenHome(NavAction action) {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) return;

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
            transactionViewModel.fetchHistoryData("PERSONAL", "", true);
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
        View addTransactionButton = findViewById(R.id.fab_add_transaction);
        setupAddTransactionPressEffect(addTransactionButton);
        addTransactionButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddTransactionActivity.class);
            intent.putExtra("WORKSPACE_ID", currentWorkspaceId);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_up, R.anim.stay);
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        View bottomNavContainer = findViewById(R.id.bottom_nav_container);

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomNavContainer, (v, windowInsets) -> {
            androidx.core.graphics.Insets navBarInsets = windowInsets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars()
                            | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            int systemMargin = Math.round(getResources().getDimension(R.dimen.bottom_nav_system_margin));

            android.view.ViewGroup.MarginLayoutParams mlp =
                    (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.bottomMargin = navBarInsets.bottom + systemMargin;
            v.setLayoutParams(mlp);

            return androidx.core.view.WindowInsetsCompat.CONSUMED;
        });

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> insets);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) return;

        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(bottomNav, navController);
        bottomNav.getMenu().findItem(R.id.nav_center_cta_space).setEnabled(false);

        int currentDestinationId = navController.getCurrentDestination() == null
                ? bottomNav.getSelectedItemId()
                : navController.getCurrentDestination().getId();
        animateBottomNavigationSelection(bottomNav, currentDestinationId, false);

        View navHostView = findViewById(R.id.nav_host_fragment);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            syncSidebarSelection(id);
            animateBottomNavigationSelection(bottomNav, id, true);

            if (id == R.id.nav_workspace_container
                    || id == R.id.nav_social_container
                    || id == R.id.nav_post_feed) {
                bottomNavContainer.setVisibility(View.GONE);
                navHostView.setPadding(0, 0, 0, 0);
            } else {
                bottomNavContainer.setVisibility(View.VISIBLE);
                navHostView.setPadding(0, 0, 0, 0);
            }
        });
    }

    private void setupAddTransactionPressEffect(View button) {
        float normalElevation = getResources().getDimension(R.dimen.bottom_nav_center_button_elevation);
        float normalTranslationZ = getResources().getDimension(R.dimen.bottom_nav_center_button_translation_z);
        float scaleUp = getResources().getFraction(R.fraction.bottom_nav_center_button_press_scale_up, 1, 1);
        float scaleDown = getResources().getFraction(R.fraction.bottom_nav_center_button_press_scale_down, 1, 1);
        int scaleUpDuration = getResources().getInteger(R.integer.bottom_nav_center_button_press_up_duration);
        int scaleDownDuration = getResources().getInteger(R.integer.bottom_nav_center_button_press_down_duration);
        int settleDuration = getResources().getInteger(R.integer.bottom_nav_center_button_press_settle_duration);

        button.setElevation(normalElevation);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            button.setTranslationZ(normalTranslationZ);
        }

        button.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().cancel();
                    v.animate().scaleX(scaleUp).scaleY(scaleUp).setDuration(scaleUpDuration)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
                    break;
                case MotionEvent.ACTION_UP:
                    v.setElevation(normalElevation);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) v.setTranslationZ(normalTranslationZ);
                    playAddButtonSpring(v, scaleDown, scaleDownDuration, settleDuration);
                    break;
                case MotionEvent.ACTION_CANCEL:
                    v.setElevation(normalElevation);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) v.setTranslationZ(normalTranslationZ);
                    v.animate().cancel();
                    v.animate().scaleX(1f).scaleY(1f).setDuration(settleDuration)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
                    break;
            }
            return false;
        });
    }

    private void playAddButtonSpring(View button, float scaleDown, int scaleDownDuration, int settleDuration) {
        button.animate().cancel();
        AnimatorSet shrink = new AnimatorSet();
        shrink.playTogether(
                ObjectAnimator.ofFloat(button, View.SCALE_X, scaleDown),
                ObjectAnimator.ofFloat(button, View.SCALE_Y, scaleDown));
        shrink.setDuration(scaleDownDuration);
        shrink.setInterpolator(new android.view.animation.DecelerateInterpolator());

        AnimatorSet settle = new AnimatorSet();
        settle.playTogether(
                ObjectAnimator.ofFloat(button, View.SCALE_X, 1f),
                ObjectAnimator.ofFloat(button, View.SCALE_Y, 1f));
        settle.setDuration(settleDuration);
        settle.setInterpolator(new android.view.animation.OvershootInterpolator(1.4f));

        AnimatorSet spring = new AnimatorSet();
        spring.playSequentially(shrink, settle);
        spring.start();
    }

    private void animateBottomNavigationSelection(BottomNavigationView bottomNav, int destinationId, boolean animate) {
        bottomNav.post(() -> {
            float itemVerticalOffset = getResources().getDimension(R.dimen.bottom_nav_item_vertical_offset);
            float selectedFloatOffset = getResources().getDimension(R.dimen.bottom_nav_selected_icon_float_offset);
            int floatDuration = getResources().getInteger(R.integer.bottom_nav_icon_float_duration);
            for (int i = 0; i < bottomNav.getMenu().size(); i++) {
                int itemId = bottomNav.getMenu().getItem(i).getItemId();
                if (itemId == R.id.nav_center_cta_space) continue;

                View itemView = bottomNav.findViewById(itemId);
                if (itemView != null) {
                    float targetOffset = itemId == destinationId
                            ? itemVerticalOffset - selectedFloatOffset : itemVerticalOffset;
                    if (animate) {
                        itemView.animate().translationY(targetOffset).setDuration(floatDuration)
                                .setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
                    } else {
                        itemView.setTranslationY(targetOffset);
                    }
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
                && navController.getCurrentDestination().getId() == R.id.nav_post_feed) return;

        if (navController.getGraph().findNode(R.id.nav_post_feed) == null) {
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
            ToastHelper.show(this, "Could not open Post screen.");
        }
    }

    private void syncSidebarSelection(int destinationId) {
        if (navigationView == null) return;

        android.view.Menu menu = navigationView.getMenu();
        if (destinationId == R.id.nav_social_container) {
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
        if (item != null) item.setChecked(true);
    }

    private void maybeOpenCreateWorkspaceSheet() {
        if (getIntent().getBooleanExtra("OPEN_CREATE_WORKSPACE", false)) {
            new AddWorkspaceBottomSheet()
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
        mainViewModel.registerFcmToken();
    }

    private interface NavAction {
        void run(NavController controller);
    }
}