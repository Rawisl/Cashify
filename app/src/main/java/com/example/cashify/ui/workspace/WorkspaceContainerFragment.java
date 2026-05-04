package com.example.cashify.ui.workspace;

import android.content.Intent;
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

        // Nhận ID Quỹ từ Sidebar của MainActivity truyền sang bằng Bundle
        if (getArguments() != null) {
            workspaceId = getArguments().getString("WORKSPACE_ID");
        }

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation_workspace);
        FloatingActionButton fabAddTransaction = view.findViewById(R.id.fabAddWorkspaceTransaction);

        // Lấy NavController của cái ruột BÊN TRONG vỏ hộp
        NavHostFragment navHostFragment = (NavHostFragment) getChildFragmentManager().findFragmentById(R.id.workspace_inner_nav_host);

        if (navHostFragment != null) {
            NavController innerNavController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNav, innerNavController);

            // Logic ẩn hiện nút FAB
            innerNavController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                if (destination.getId() == R.id.workspace_nav_home) {
                    fabAddTransaction.show();
                } else {
                    fabAddTransaction.hide();
                }
            });
        }

        // Bắt sự kiện bấm FAB
        fabAddTransaction.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddTransactionActivity.class);
            intent.putExtra("WORKSPACE_ID", workspaceId);
            startActivity(intent);
        });
    }
}