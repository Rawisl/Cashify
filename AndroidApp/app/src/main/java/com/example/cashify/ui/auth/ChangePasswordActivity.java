package com.example.cashify.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.example.cashify.R;
import com.example.cashify.data.remote.FirebaseManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;

public class ChangePasswordActivity extends AppCompatActivity {

    private CardView cvGoogleBanner;
    private TextView tvGoogleBanner;
    private LinearLayout layoutPasswordForm;
    private LinearLayout layoutStrength;
    private TextInputLayout tilCurrentPassword;
    private TextInputLayout tilNewPassword;
    private TextInputLayout tilConfirmPassword;
    private ProgressBar pbStrength;
    private TextView tvStrengthLabel;
    private MaterialButton btnChangePassword;
    private MaterialButton btnSetPassword;

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        auth = FirebaseManager.getInstance().getAuth();
        bindViews();
        setupUI();
        setupPasswordStrength();
        setupClickListeners();
    }

    private void bindViews() {
        cvGoogleBanner     = findViewById(R.id.cv_google_banner);
        tvGoogleBanner     = findViewById(R.id.tv_google_banner);
        layoutPasswordForm = findViewById(R.id.layout_password_form);
        layoutStrength     = findViewById(R.id.layout_strength);
        tilCurrentPassword = findViewById(R.id.til_current_password);
        tilNewPassword     = findViewById(R.id.til_new_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        pbStrength         = findViewById(R.id.pb_strength);
        tvStrengthLabel    = findViewById(R.id.tv_strength_label);
        btnChangePassword  = findViewById(R.id.btn_change_password);
        btnSetPassword     = findViewById(R.id.btn_set_password);
    }

    private void setupUI() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        boolean isGoogleUser = false;
        boolean hasEmailProvider = false;

        for (UserInfo info : user.getProviderData()) {
            if (GoogleAuthProvider.PROVIDER_ID.equals(info.getProviderId())) isGoogleUser = true;
            if (EmailAuthProvider.PROVIDER_ID.equals(info.getProviderId()))  hasEmailProvider = true;
        }

        if (isGoogleUser && hasEmailProvider) {
            showGoogleBanner(true);
            showPasswordForm();
        } else if (isGoogleUser) {
            showGoogleBanner(false);
            layoutPasswordForm.setVisibility(android.view.View.VISIBLE);
            tilCurrentPassword.setVisibility(android.view.View.GONE);
            btnChangePassword.setVisibility(android.view.View.GONE);
            btnSetPassword.setVisibility(android.view.View.VISIBLE);
        } else {
            showPasswordForm();
        }
    }

    private void showPasswordForm() {
        layoutPasswordForm.setVisibility(android.view.View.VISIBLE);
        tilCurrentPassword.setVisibility(android.view.View.VISIBLE);
        btnChangePassword.setVisibility(android.view.View.VISIBLE);
        btnSetPassword.setVisibility(android.view.View.GONE);
    }

    private void showGoogleBanner(boolean linkedMode) {
        cvGoogleBanner.setVisibility(android.view.View.VISIBLE);
        tvGoogleBanner.setText(linkedMode
                ? getString(R.string.change_password_google_linked_banner)
                : getString(R.string.change_password_google_banner));
    }

    private void setupPasswordStrength() {
        if (tilNewPassword.getEditText() == null) return;

        tilNewPassword.getEditText().addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String pw = (s != null) ? s.toString() : "";
                if (pw.isEmpty()) {
                    layoutStrength.setVisibility(android.view.View.GONE);
                    return;
                }
                layoutStrength.setVisibility(android.view.View.VISIBLE);
                applyStrength(pw);
            }
        });
    }

    private void applyStrength(String password) {
        int score = 0;
        if (password.length() >= 8)               score++;
        if (password.matches(".*\\d.*"))          score++;
        if (password.matches(".*[^a-zA-Z0-9].*")) score++;

        int colorRes, labelRes, progress;
        if (score <= 1) {
            colorRes = R.color.status_red;
            labelRes = R.string.password_strength_weak;
            progress = 1;
        } else if (score == 2) {
            colorRes = R.color.cat_pastel_orange;
            labelRes = R.string.password_strength_medium;
            progress = 2;
        } else {
            colorRes = R.color.status_green;
            labelRes = R.string.password_strength_strong;
            progress = 3;
        }

        pbStrength.setProgress(progress);
        pbStrength.setProgressTintList(ContextCompat.getColorStateList(this, colorRes));
        tvStrengthLabel.setText(labelRes);
        tvStrengthLabel.setTextColor(ContextCompat.getColor(this, colorRes));
    }

    private void setupClickListeners() {
        btnChangePassword.setOnClickListener(v -> {
            String currentPw = getInputText(tilCurrentPassword);
            String newPw     = getInputText(tilNewPassword);
            String confirmPw = getInputText(tilConfirmPassword);
            if (!validateChangeFields(currentPw, newPw, confirmPw)) return;
            setLoading(true);
            reauthenticateAndChange(currentPw, newPw);
        });

        btnSetPassword.setOnClickListener(v -> {
            String newPw     = getInputText(tilNewPassword);
            String confirmPw = getInputText(tilConfirmPassword);
            if (!validateNewOnly(newPw, confirmPw)) return;
            setLoading(true);
            linkEmailPassword(newPw);
        });
    }

    private void reauthenticateAndChange(String currentPw, String newPw) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPw);

        user.reauthenticate(credential)
                .addOnSuccessListener(unused ->
                        user.updatePassword(newPw)
                                .addOnSuccessListener(v2 -> {
                                    setLoading(false);
                                    showSnackbar(getString(R.string.change_password_success), true);
                                    clearFields();
                                })
                                .addOnFailureListener(e -> {
                                    setLoading(false);
                                    showSnackbar(e.getLocalizedMessage());
                                })
                )
                .addOnFailureListener(e -> {
                    setLoading(false);
                    tilCurrentPassword.setError(getString(R.string.change_password_wrong_current));
                });
    }

    private void linkEmailPassword(String newPw) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), newPw);

        user.linkWithCredential(credential)
                .addOnSuccessListener(result -> {
                    setLoading(false);
                    showSnackbar(getString(R.string.change_password_set_success), true);
                    setupUI();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showSnackbar(e.getLocalizedMessage());
                });
    }

    private boolean validateChangeFields(String currentPw, String newPw, String confirmPw) {
        clearErrors();
        boolean valid = true;
        if (currentPw.isEmpty()) {
            tilCurrentPassword.setError(getString(R.string.error_field_required));
            valid = false;
        }
        if (newPw.length() < 6) {
            tilNewPassword.setError(getString(R.string.change_password_too_short));
            valid = false;
        } else if (newPw.equals(currentPw)) {
            tilNewPassword.setError(getString(R.string.change_password_same_as_old));
            valid = false;
        }
        if (!confirmPw.equals(newPw)) {
            tilConfirmPassword.setError(getString(R.string.change_password_mismatch));
            valid = false;
        }
        return valid;
    }

    private boolean validateNewOnly(String newPw, String confirmPw) {
        clearErrors();
        boolean valid = true;
        if (newPw.length() < 6) {
            tilNewPassword.setError(getString(R.string.change_password_too_short));
            valid = false;
        }
        if (!confirmPw.equals(newPw)) {
            tilConfirmPassword.setError(getString(R.string.change_password_mismatch));
            valid = false;
        }
        return valid;
    }

    private void clearErrors() {
        tilCurrentPassword.setError(null);
        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private String getInputText(TextInputLayout til) {
        if (til.getEditText() == null) return "";
        return til.getEditText().getText().toString().trim();
    }

    private void setLoading(boolean loading) {
        btnChangePassword.setEnabled(!loading);
        btnSetPassword.setEnabled(!loading);
        btnChangePassword.setText(loading
                ? getString(R.string.loading)
                : getString(R.string.change_password_btn));
    }

    private void clearFields() {
        if (tilCurrentPassword.getEditText() != null) tilCurrentPassword.getEditText().setText("");
        if (tilNewPassword.getEditText() != null)     tilNewPassword.getEditText().setText("");
        if (tilConfirmPassword.getEditText() != null) tilConfirmPassword.getEditText().setText("");
    }

    private void showSnackbar(String message) {
        showSnackbar(message, false);
    }

    private void showSnackbar(String message, boolean success) {
        Snackbar sb = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
        if (success) {
            sb.setBackgroundTint(ContextCompat.getColor(this, R.color.status_green));
            sb.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
        sb.show();
    }
}