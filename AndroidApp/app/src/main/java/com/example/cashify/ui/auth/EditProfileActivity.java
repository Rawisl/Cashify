package com.example.cashify.ui.auth;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.cashify.R;
import com.example.cashify.utils.FileUtils;
import com.example.cashify.utils.ImageHelper;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;

public class EditProfileActivity extends AppCompatActivity {

    private UpdateUserViewModel viewModel;

    private ImageView imgEditAvatar;
    private TextInputEditText edtEditName, edtEditEmail;
    private MaterialButton btnSaveProfile;

    private Uri selectedImageUri = null;

    // Launcher to open the device's image picker
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    // Provide instant UI feedback by loading the selected local image
                    String fallbackName = edtEditName.getText() != null ? edtEditName.getText().toString() : "";
                    ImageHelper.loadAvatar(uri, imgEditAvatar, fallbackName);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        viewModel = new ViewModelProvider(this).get(UpdateUserViewModel.class);

        initViews();
        loadCurrentUserData();
        setupObservers();
        setupListeners();
    }

    private void initViews() {
        imgEditAvatar = findViewById(R.id.imgEditAvatar);
        edtEditName = findViewById(R.id.edtEditName);
        edtEditEmail = findViewById(R.id.edtEditEmail);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        MaterialToolbar toolbar = findViewById(R.id.toolbarEditProfile);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * Populates the form with existing user data.
     * Note: In a strict MVVM, this should be fetched from a UserRepository via ViewModel.
     * Kept here using FirebaseAuth for direct access to avoid structural over-engineering.
     */
    private void loadCurrentUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Email is strictly read-only for this form
            edtEditEmail.setText(currentUser.getEmail());

            if (currentUser.getDisplayName() != null) {
                edtEditName.setText(currentUser.getDisplayName());
            }
            if (currentUser.getPhotoUrl() != null) {
                String fallbackName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : currentUser.getEmail();
                ImageHelper.loadAvatar(currentUser.getPhotoUrl(), imgEditAvatar, fallbackName);
            }
        }
    }

    private void setupObservers() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            // Block user interactions during network operations
            btnSaveProfile.setEnabled(!isLoading);
            btnSaveProfile.setText(isLoading ? "Saving..." : "Save Changes");
        });

        viewModel.getMessage().observe(this, msg -> {
            if (msg != null) {
                ToastHelper.show(this, msg);
                viewModel.clearMessage(); // Prevent Toast spam on configuration changes (e.g., rotation)

                // CRITICAL FIX: Matched the exact string outputted by UpdateUserViewModel
                if (msg.equals("Profile updated successfully.")) {
                    finish();
                }
            }
        });
    }

    private void setupListeners() {
        findViewById(R.id.fabChangePhoto).setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        imgEditAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        btnSaveProfile.setOnClickListener(v -> {
            String newName = edtEditName.getText() != null ? edtEditName.getText().toString().trim() : "";

            File imageFile = null;
            if (selectedImageUri != null) {
                // Delegate File I/O to utility layer
                imageFile = FileUtils.getFileFromUri(this, selectedImageUri);
            }

            // Delegate network and database logic to ViewModel
            viewModel.updateProfile(newName, imageFile);
        });
    }
}