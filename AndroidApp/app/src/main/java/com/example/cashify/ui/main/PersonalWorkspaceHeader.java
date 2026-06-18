package com.example.cashify.ui.main;

import android.view.View;
import android.widget.TextView;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.cashify.R;
import com.example.cashify.ui.notifications.NotificationBottomSheet;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * Utility class to bind the common Personal Workspace Toolbar.
 * Strictly handles UI logic only. Data must be provided by observing ViewModels.
 */
public final class PersonalWorkspaceHeader {

    private PersonalWorkspaceHeader() {}

    public static void bind(Fragment fragment, View root) {
        MaterialToolbar toolbar = root.findViewById(R.id.toolbarPersonal);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                DrawerLayout drawer = fragment.requireActivity().findViewById(R.id.drawerLayout);
                if (drawer != null) {
                    drawer.openDrawer(GravityCompat.START);
                }
            });
        }

        TextView title = root.findViewById(R.id.tvToolbarTitle);
        if (title != null) {
            title.setText(resolveTitle(fragment));
        }

        View notificationButton = root.findViewById(R.id.btnHomeNotifications);
        if (notificationButton != null) {
            notificationButton.setOnClickListener(v ->
                    new NotificationBottomSheet()
                            .show(fragment.getChildFragmentManager(), "NotificationBottomSheet"));
        }
    }

    /**
     * Updates the notification badge count on the toolbar.
     * Fragments should call this from their ViewModel observer blocks.
     */
    public static void updateBadge(View root, int count) {
        TextView badge = root.findViewById(R.id.tvNotificationBadge);
        if (badge == null) return;

        if (count > 0) {
            badge.setVisibility(View.VISIBLE);
            badge.setText(count > 9 ? "9+" : String.valueOf(count));
        } else {
            badge.setVisibility(View.GONE);
        }
    }

    private static String resolveTitle(Fragment fragment) {
        String className = fragment.getClass().getSimpleName();
        switch (className) {
            case "HomeFragment": return "Dashboard";
            case "TransactionFragment": return "Transactions";
            case "BudgetFragment": return "Budget";
            case "SettingsFragment": return "Settings";
            default: return "Personal Workspace";
        }
    }
}