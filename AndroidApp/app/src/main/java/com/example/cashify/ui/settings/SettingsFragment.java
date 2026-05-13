package com.example.cashify.ui.settings;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.cashify.data.remote.FirebaseManager;
import com.example.cashify.ui.auth.ChangePasswordActivity;
import com.example.cashify.ui.category.CategoryManagement;
import com.example.cashify.R;
import com.example.cashify.data.local.AppDatabase;
import com.example.cashify.ui.auth.LoginActivity;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.concurrent.Executors;

public class SettingsFragment extends Fragment {

    private SettingsViewModel settingsViewModel;
    private SwitchMaterial toggleNotification; // THÊM MỚI

    public SettingsFragment() {
        // Required empty public constructor
    }
    // THÊM MỚI: Launcher xin permission Android 13+
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            // User cho phép -> Bật toggle
                            syncToggleWithSystem();
                        } else {
                            // User từ chối, hoặc hệ thống chặn popup (Permanently Denied)
                            // -> Bật Settings hệ thống để họ tự gạt công tắc
                            openAppNotificationSettings();
                        }
                    }
            );

    // THÊM MỚI: Launcher chờ quay về từ System Settings
    private final ActivityResultLauncher<Intent> systemSettingsLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> syncToggleWithSystem()
            );

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        // THÊM MỚI: Notification toggle
        toggleNotification = view.findViewById(R.id.toggle_notification);
        LinearLayout btnNotification = view.findViewById(R.id.btn_notification);
        syncToggleWithSystem();
        btnNotification.setOnClickListener(v -> {
            if (isNotificationEnabled()) {
                openAppNotificationSettings(); // đang BẬT → mở Settings để tắt
            } else {
                requestNotificationPermission(); // đang TẮT → xin bật
            }
        });

        // Security -> ChangePasswordActivity
        LinearLayout btnSecurity = view.findViewById(R.id.btn_security);
        btnSecurity.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ChangePasswordActivity.class);
            startActivity(intent);
        });

        // GỮ NGUYÊN: Categories
        LinearLayout btnCategories = view.findViewById(R.id.btn_categories);
        btnCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CategoryManagement.class);
                startActivity(intent);
            }
        });

        // GỮ NGUYÊN: Reset transaction
        LinearLayout btnResetTransaction = view.findViewById(R.id.btn_reset_transaction);
        btnResetTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = getString(R.string.confirm_reset) + " all " + getString(R.string.nav_transaction_history) + "?";
                AppDatabase db = AppDatabase.getInstance(requireContext());

                new AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.action_reset_transactions))
                        .setMessage(msg)
                        .setPositiveButton(getString(R.string.action_reset), (d, w) -> {
                            // Dọn dẹp Firebase trước
                            FirebaseManager.getInstance().deleteAllTransactionsFromCloud("PERSONAL", new FirebaseManager.DataCallback<Void>() {
                                @Override
                                public void onSuccess(Void data) {
                                    // Nếu xóa Cloud thành công, mới dọn tiếp Room Database
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        AppDatabase.getInstance(requireContext()).transactionDao().deleteAllTransactions("PERSONAL");

                                        // Hiện thông báo thành công
                                        requireActivity().runOnUiThread(() ->
                                                ToastHelper.show(requireContext(), "All transactions have been deleted!")
                                        );
                                    });
                                }
                                @Override
                                public void onError(String message) {
                                    ToastHelper.show(requireContext(), "Cloud deleting error: " + message);
                                }
                            });
                        })
                        .setNegativeButton(getString(R.string.action_cancel), null)
                        .show();
            }
        });

        // GỮ NGUYÊN: Logout
        LinearLayout btnLogout = view.findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            // Dọn sạch rác trong Room Database trước (viết luồng phụ cho khỏi đơ máy)
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                db.clearAllTables();

                // Báo cho ViewModel biết để đi xử lý vụ Đăng xuất Google & Firebase
                requireActivity().runOnUiThread(() -> {
                    // Gọi hàm logout trong ViewModel, truyền context vào!
                    settingsViewModel.logout(requireContext());
                });
            }).start();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Chỉ việc "nằm vùng" chờ tín hiệu báo XONG từ ViewModel
        settingsViewModel.isLoggedOut.observe(getViewLifecycleOwner(), isLoggedOut -> {
            if (isLoggedOut != null && isLoggedOut) {
                // Đã dọn dẹp xong mọi thứ -> Tự tin đóng gói văng ra Login
                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                // Trả lại trạng thái cho lần sau nếu Fragment vô tình tái sinh
                // settingsViewModel.isLoggedOut.setValue(false); (Tùy chọn)
            }
        });
    }

    // THÊM MỚI: Đồng bộ khi quay về từ System Settings
    @Override
    public void onResume() {
        super.onResume();
        syncToggleWithSystem();
    }

    // ── THÊM MỚI: Helpers notification ────────────────────────

    private boolean isNotificationEnabled() {
        return NotificationManagerCompat.from(requireContext()).areNotificationsEnabled();
    }

    private void syncToggleWithSystem() {
        if (toggleNotification == null) return;
        toggleNotification.setChecked(isNotificationEnabled());
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        } else {
            openAppNotificationSettings();
        }
    }

    private void openAppNotificationSettings() {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
        }
        systemSettingsLauncher.launch(intent);
    }
}