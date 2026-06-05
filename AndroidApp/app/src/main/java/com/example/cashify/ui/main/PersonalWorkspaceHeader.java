package com.example.cashify.ui.main;

import android.view.View;
import android.widget.TextView;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.cashify.R;
import com.example.cashify.data.remote.FirebaseManager;
import com.example.cashify.ui.notifications.NotificationBottomSheet;
import com.google.android.material.appbar.MaterialToolbar;

public final class PersonalWorkspaceHeader {

    private PersonalWorkspaceHeader() {
    }

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

        TextView badge = root.findViewById(R.id.tvNotificationBadge);
        if (badge != null) {
            FirebaseManager.getInstance()
                    .listenToUnreadNotifications(new FirebaseManager.DataCallback<Integer>() {
                        @Override
                        public void onSuccess(Integer count) {
                            if (count != null && count > 0) {
                                badge.setVisibility(View.VISIBLE);
                                badge.setText(count > 9 ? "9+" : String.valueOf(count));
                            } else {
                                badge.setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public void onError(String message) {
                            badge.setVisibility(View.GONE);
                        }
                    });
        }

        View notificationButton = root.findViewById(R.id.btnHomeNotifications);
        if (notificationButton != null) {
            notificationButton.setOnClickListener(v ->
                    new NotificationBottomSheet()
                            .show(fragment.getChildFragmentManager(), "NotificationBottomSheet"));
        }
    }
}
