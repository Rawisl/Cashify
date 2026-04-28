package com.example.cashify.ui.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.cashify.R;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditProfileActivity extends AppCompatActivity {

    private UpdateUserViewModel viewModel;
    private CircleImageView imgEditAvatar;
    private TextInputEditText edtEditName, edtEditEmail;
    private MaterialButton btnSaveProfile;

    // Biến lưu trữ đường dẫn tấm ảnh user chọn từ thư viện
    private Uri selectedImageUri = null;

    // Trình kích hoạt (Launcher) để mở thư viện ảnh của điện thoại
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    // Dùng Glide load ngay tấm ảnh vừa chọn lên hình tròn cho user xem trước
                    Glide.with(this).load(uri).into(imgEditAvatar);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // 1. Ánh xạ View
        imgEditAvatar = findViewById(R.id.imgEditAvatar);
        edtEditName = findViewById(R.id.edtEditName);
        edtEditEmail = findViewById(R.id.edtEditEmail);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        MaterialToolbar toolbar = findViewById(R.id.toolbarEditProfile);

        // Bấm nút mũi tên trên cùng bên trái để thoát
        toolbar.setNavigationOnClickListener(v -> finish());

        // 2. Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(UpdateUserViewModel.class);

        // 3. Đổ data hiện tại của User lên form lúc mới mở
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            edtEditEmail.setText(currentUser.getEmail()); // Email bị khóa không cho sửa

            if (currentUser.getDisplayName() != null) {
                edtEditName.setText(currentUser.getDisplayName());
            }
            if (currentUser.getPhotoUrl() != null) {
                // Glide sẽ tự động tải ảnh từ Firebase và bo tròn vào imgEditAvatar
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .placeholder(R.mipmap.ic_launcher) // Hiển thị ảnh nháp trong lúc chờ mạng tải
                        .into(imgEditAvatar);
            }
        }

        // 4. Quan sát tín hiệu từ ViewModel để cập nhật UI
        viewModel.getIsLoading().observe(this, isLoading -> {
            // Khi đang tải: Khóa nút lại và đổi chữ
            btnSaveProfile.setEnabled(!isLoading);
            btnSaveProfile.setText(isLoading ? "ĐANG LƯU..." : "LƯU THAY ĐỔI");
        });

        viewModel.getMessage().observe(this, msg -> {
            if (msg != null) {
                ToastHelper.show(this, msg);
                // Reset thông báo về null để tránh lỗi spam Toast khi xoay màn hình
                viewModel.clearMessage();


                // Cập nhật thành công thì đóng màn hình lại (quay về Home)
                if (msg.equals("Update profile successfully!")) {
                    finish();
                }
            }
        });

        // 5. Bắt sự kiện bấm nút
        // Bấm vào cục Avatar hoặc nút máy ảnh mini
        findViewById(R.id.fabChangePhoto).setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        imgEditAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // Bấm nút Lưu
        btnSaveProfile.setOnClickListener(v -> {
            String newName = edtEditName.getText() != null ? edtEditName.getText().toString() : "";
            // Truyền tên và đường dẫn ảnh sang ViewModel xử lý
            viewModel.updateProfile(newName, selectedImageUri);
        });
    }
}