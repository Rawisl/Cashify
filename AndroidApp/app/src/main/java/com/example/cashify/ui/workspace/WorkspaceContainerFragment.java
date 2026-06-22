package com.example.cashify.ui.workspace;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.cashify.R;
import com.example.cashify.ui.transactions.AddTransactionActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class WorkspaceContainerFragment extends Fragment {

    private String workspaceId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_workspace_container, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Retrieve Workspace ID passed from MainActivity's Sidebar via Bundle
        if (getArguments() != null) {
            workspaceId = getArguments().getString("WORKSPACE_ID");
        }

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation_workspace);
        FloatingActionButton fabAddTransaction = view.findViewById(R.id.fabAddWorkspaceTransaction);


        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, windowInsets) -> {
            Insets navInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            int systemMargin = Math.round(getResources().getDimension(R.dimen.bottom_nav_system_margin));

            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.bottomMargin = navInsets.bottom + systemMargin;
            v.setLayoutParams(mlp);

            return WindowInsetsCompat.CONSUMED;
        });

        // =========================================================================
        // =========================================================================
        // NAVIGATION SETUP
        // =========================================================================

        // Retrieve the NavController for the internal navigation host
        NavHostFragment navHostFragment = (NavHostFragment) getChildFragmentManager()
                .findFragmentById(R.id.workspace_inner_nav_host);

        if (navHostFragment != null) {
            NavController innerNavController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNav, innerNavController);

            // Dynamic FAB visibility based on the current destination
            innerNavController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                if (destination.getId() == R.id.workspace_nav_home) {
                    fabAddTransaction.show();
                } else {
                    fabAddTransaction.hide();
                }
            });

            // Handle direct deep-linking to the Chat tab
            if (getArguments() != null && getArguments().getBoolean("OPEN_CHAT_TAB", false)) {
                bottomNav.setSelectedItemId(R.id.workspace_nav_chat);
                // Consume the flag to prevent re-triggering on configuration changes
                getArguments().putBoolean("OPEN_CHAT_TAB", false);
            }
        }

        // =========================================================================
        // ACTIONS
        // =========================================================================
        fabAddTransaction.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddTransactionActivity.class);
            intent.putExtra("WORKSPACE_ID", workspaceId);
            startActivity(intent);
        });
    }
}