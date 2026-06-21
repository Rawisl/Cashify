package com.example.cashify.ui.common;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.cashify.R;
import com.example.cashify.ui.main.MainActivity;
import com.example.cashify.ui.main.MainViewModel;
import com.example.cashify.ui.notifications.NotificationBottomSheet;
import com.google.android.material.appbar.MaterialToolbar;

public abstract class BaseFragment extends Fragment {

    protected MainViewModel mainViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Cấp phát ViewModel chung scope với MainActivity để Share Data (Badge)
        if (getActivity() != null) {
            mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        }
    }

    /**
     * Hàm dùng chung để cài đặt Header cho mọi màn hình
     * @param toolbar: Toolbar chứa nút Navigation (Menu)
     * @param btnBell: Icon chuông/Layout bao quanh chuông
     * @param tvBadge: TextView hiển thị số đỏ
     */
    protected void setupCommonHeader(MaterialToolbar toolbar, View btnBell, TextView tvBadge) {

        //Setup nút Menu mở Sidebar
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    DrawerLayout drawer = getActivity().findViewById(R.id.drawerLayout);
                    if (drawer != null) {
                        drawer.openDrawer(GravityCompat.START);
                    }
                }
            });
        }

        if (btnBell != null) {
            btnBell.setOnClickListener(v -> {
                com.example.cashify.ui.notifications.NotificationBottomSheet bottomSheet = new com.example.cashify.ui.notifications.NotificationBottomSheet();
                bottomSheet.show(getParentFragmentManager(), "NotificationBottomSheet");
            });
        }

        //Setup Notification Badge ẩn theo MainViewModel
        if (tvBadge != null && mainViewModel != null) {
            mainViewModel.getUnreadNotificationCount().observe(getViewLifecycleOwner(), count -> {
                if (count != null && count > 0) {
                    tvBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                    tvBadge.setVisibility(View.VISIBLE);
                } else {
                    tvBadge.setVisibility(View.GONE);
                }
            });
        }

        if (btnBell != null) {
            btnBell.setOnClickListener(v -> {
                if (getParentFragmentManager().findFragmentByTag("NotificationBottomSheet") == null) {
                    NotificationBottomSheet bottomSheet = new NotificationBottomSheet();
                    bottomSheet.show(getParentFragmentManager(), "NotificationBottomSheet");
                }
            });
        }
    }
}