package com.example.cashify.ui.settings;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.cashify.ui.category.CategoryManagement;
import com.example.cashify.R;
import com.example.cashify.data.local.AppDatabase;
import com.example.cashify.ui.auth.LoginActivity;

import java.util.concurrent.Executors;

public class SettingsFragment extends Fragment {

    private SettingsViewModel settingsViewModel;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        LinearLayout btnCategories = view.findViewById(R.id.btn_categories);
        btnCategories.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CategoryManagement.class);
                startActivity(intent);
            }
        });

        LinearLayout btnResetTransaction = view.findViewById(R.id.btn_reset_transaction);
        btnResetTransaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = getString(R.string.confirm_reset) + " all " + getString(R.string.nav_transaction_history) + "?";
                AppDatabase db = AppDatabase.getInstance(requireContext());

                new AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.action_reset_transactions))
                        .setMessage(msg)
                        .setPositiveButton(getString(R.string.action_reset), (d, w) -> Executors.newSingleThreadExecutor().execute(() -> {
                            db.transactionDao().deleteAllTransactions();
                        }))
                        .setNegativeButton(getString(R.string.action_cancel), null)
                        .show();
            }
        });

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
}