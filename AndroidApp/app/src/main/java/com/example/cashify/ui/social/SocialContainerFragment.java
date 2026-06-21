package com.example.cashify.ui.social;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.cashify.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SocialContainerFragment extends Fragment {

    private boolean syncingBottomNavSelection = false;

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
        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation_social);
        if (bottomNav == null) return;

        bottomNav.setOnItemSelectedListener(item -> {
            if (syncingBottomNavSelection) {
                return true;
            }

            int destinationId = item.getItemId();
            if (navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() == destinationId) {
                return true;
            }
            navController.popBackStack(destinationId, false);
            if (navController.getCurrentDestination() == null
                    || navController.getCurrentDestination().getId() != destinationId) {
                navController.navigate(destinationId);
            }
            return true;
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destinationId = destination.getId() == R.id.nav_other_profile
                    ? R.id.nav_social_profile
                    : destination.getId();
            if (bottomNav.getSelectedItemId() != destinationId) {
                syncingBottomNavSelection = true;
                bottomNav.setSelectedItemId(destinationId);
                syncingBottomNavSelection = false;
            }
        });
    }
}
