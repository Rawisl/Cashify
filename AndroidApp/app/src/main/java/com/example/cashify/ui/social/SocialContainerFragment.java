package com.example.cashify.ui.social;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.cashify.R;

public class SocialContainerFragment extends Fragment {

    // =========================================================================
    // CONSTANTS: Navigation Bar Colors
    // =========================================================================
    private static final int ACTIVE_COLOR = Color.rgb(26, 35, 126);
    private static final int INACTIVE_COLOR = Color.rgb(96, 125, 139);

    // =========================================================================
    // UI COMPONENTS
    // =========================================================================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_social_container, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupInnerNavigation(view);
    }

    private void setupInnerNavigation(View view) {
        NavHostFragment navHostFragment = (NavHostFragment) getChildFragmentManager()
                .findFragmentById(R.id.social_inner_nav_host);
        if (navHostFragment == null) return;

        NavController navController = navHostFragment.getNavController();
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation_social);
        if (bottomNav != null) {
            androidx.navigation.ui.NavigationUI.setupWithNavController(bottomNav, navController);
            
            int currentDestinationId = navController.getCurrentDestination() == null
                    ? bottomNav.getSelectedItemId()
                    : navController.getCurrentDestination().getId();
            animateBottomNavigationSelection(bottomNav, currentDestinationId, false);

        }

        View container = view.findViewById(R.id.bottom_navigation_social_container);
        if (container != null) {
            ViewCompat.setOnApplyWindowInsetsListener(container, (v, windowInsets) -> {
                Insets navInsets = windowInsets.getInsets(
                        WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                int systemMargin = Math.round(getResources().getDimension(R.dimen.bottom_nav_system_margin));

                android.view.ViewGroup.MarginLayoutParams mlp =
                        (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
                mlp.bottomMargin = navInsets.bottom + systemMargin;
                v.setLayoutParams(mlp);

                return WindowInsetsCompat.CONSUMED;
            });
        }
        
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (bottomNav != null) {
                animateBottomNavigationSelection(bottomNav, destination.getId(), true);
            }
            if (destination.getId() == R.id.nav_other_profile) {
                if (container != null) container.setVisibility(View.GONE);
            } else {
                if (container != null) container.setVisibility(View.VISIBLE);
            }
        });

        if (getArguments() != null && getArguments().getBoolean("OPEN_OTHER_PROFILE", false)) {
            String userId = getArguments().getString("USER_ID");
            Bundle args = new Bundle();
            args.putString("USER_ID", userId);
            args.putBoolean("FINISH_ON_BACK", getArguments().getBoolean("FINISH_ON_BACK", false));
            navController.navigate(R.id.nav_other_profile, args);
            getArguments().remove("OPEN_OTHER_PROFILE");
        }
    }

    private void animateBottomNavigationSelection(com.google.android.material.bottomnavigation.BottomNavigationView bottomNav, int destinationId, boolean animate) {
        bottomNav.post(() -> {
            float selectedFloatOffset = getResources().getDimension(R.dimen.bottom_nav_selected_icon_float_offset);
            int floatDuration = getResources().getInteger(R.integer.bottom_nav_icon_float_duration);

            for (int i = 0; i < bottomNav.getMenu().size(); i++) {
                int itemId = bottomNav.getMenu().getItem(i).getItemId();
                android.view.View itemView = bottomNav.findViewById(itemId);
                if (itemView != null) {
                    float targetOffset = itemId == destinationId ? -selectedFloatOffset : 0f;
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
}