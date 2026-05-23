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
import androidx.navigation.ui.NavigationUI;

import com.example.cashify.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * SocialContainerFragment — Shell chứa NavHost + BottomNav của màn Social.
 * Giống hệt WorkspaceContainerFragment: không chứa business logic,
 * chỉ setup navigation và mở sidebar.
 */
public class SocialContainerFragment extends Fragment {

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
        // Lấy NavController của inner NavHost (social_inner_nav_host)
        NavHostFragment navHostFragment = (NavHostFragment) getChildFragmentManager()
                .findFragmentById(R.id.social_inner_nav_host);
        if (navHostFragment == null) return;

        NavController navController = navHostFragment.getNavController();
        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation_social);

        // Nối BottomNav với NavController — tự xử lý việc chuyển fragment
        NavigationUI.setupWithNavController(bottomNav, navController);
    }
}