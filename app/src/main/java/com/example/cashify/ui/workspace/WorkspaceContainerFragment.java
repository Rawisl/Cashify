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

        // Nhận ID Quỹ từ Sidebar của MainActivity truyền sang bằng Bundle
        if (getArguments() != null) {
            workspaceId = getArguments().getString("WORKSPACE_ID");
        }

        BottomNavigationView bottomNav = view.findViewById(R.id.bottom_navigation_workspace);
        FloatingActionButton fabAddTransaction = view.findViewById(R.id.fabAddWorkspaceTransaction);

        //Tách bottom navigation khỏi thanh hệ thống điện thoại (Tại cái thuộc tính fit system của layout)

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, windowInsets) -> {
            // Lấy độ cao của thanh 3 nút / vạch vuốt của hệ thống
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            // Đổi 20dp margin bồ muốn sang dạng Pixel để tính toán
            int margin20dp = (int) (20 * getResources().getDisplayMetrics().density);

            // Set lại Margin Bottom = Chiều cao thanh hệ thống + 20dp lơ lửng
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.bottomMargin = insets.bottom + margin20dp;
            v.setLayoutParams(mlp);

            // Báo cho Android: "Tao tính toán xong rồi, đừng tự động nhét padding vào nữa!"
            return WindowInsetsCompat.CONSUMED;
        });

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