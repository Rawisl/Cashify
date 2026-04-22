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

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.Executors;

public class SettingsFragment extends Fragment {

    private SettingsViewModel settingsViewModel;

    public SettingsFragment() {
        // Required empty public constructor
    }
    //Hàm nạp giao diện .Xml lên màn hình
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        //Tìm cái nút categories bằng id đã đặt cho n
        LinearLayout btnCategories = view.findViewById(R.id.btn_categories);
        btnCategories.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //Intent(màn hình Activity hiện tại, màn hình Activity  muốn đến)
                //Do đây là 1 fragment, không có quyền, nên phải gọi hàm getActivity() để lấy Activity chứa nó

                Intent intent = new Intent(getActivity(), CategoryManagement.class);
                startActivity(intent);
            }
        });

        LinearLayout btnResetTransaction = view.findViewById(R.id.btn_reset_transaction);
        btnResetTransaction.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String msg = getString(R.string.confirm_reset) + " all " + getString(R.string.nav_transaction_history) + "?";                AppDatabase db = AppDatabase.getInstance(requireContext());

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
            // Chạy một luồng phụ để xóa Database
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                // Lệnh này sẽ xóa sạch dữ liệu trong tất cả các bảng (Transactions, Categories, Budget...)
                db.clearAllTables();

                // Sau khi xóa xong, thực hiện đăng xuất Firebase và chuyển màn hình
                requireActivity().runOnUiThread(() -> {
                    FirebaseAuth.getInstance().signOut();

                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    // Xóa sạch lịch sử các màn hình trước đó để bảo mật
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                });
            }).start();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        settingsViewModel.isLoggedOut.observe(getViewLifecycleOwner(), isLoggedOut -> {
            if (isLoggedOut) {
                // Xóa luôn phiên của Google để lần sau phải chọn lại mail
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
                GoogleSignIn.getClient(requireContext(), gso).signOut().addOnCompleteListener(task -> {

                    // Chuyển hướng về Login và xóa sạch lịch sử
                    Intent intent = new Intent(requireActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                });
            }
        });
    }
}