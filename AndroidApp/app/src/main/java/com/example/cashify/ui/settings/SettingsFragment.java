package com.example.cashify.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.cashify.R;
import com.example.cashify.ui.auth.ChangePasswordActivity;
import com.example.cashify.ui.auth.LoginActivity;
import com.example.cashify.ui.category.CategoryManagement;
import com.example.cashify.ui.main.PersonalWorkspaceHeader;
import com.example.cashify.utils.DialogHelper;
import com.example.cashify.utils.CurrencyManager;
import com.example.cashify.utils.ToastHelper;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    private static final String SETTINGS_PREFS = "SettingsPrefs";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";

    private SettingsViewModel settingsViewModel;
    private SwitchMaterial toggleNotification;
    private LinearLayout languageOptions;
    private ImageView languageArrow;
    private LinearLayout currencyOptions;
    private ImageView currencyArrow;
    private TextView currencySummary;
    private TextView optionCurrencyVnd;
    private TextView optionCurrencyUsd;

    private boolean isUpdatingNotificationToggle = false;
    private boolean isLanguageExpanded = false;
    private boolean isCurrencyExpanded = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            setNotificationPreference(true);
                        } else {
                            setNotificationPreference(false);
                            ToastHelper.show(requireContext(), "Notifications permission was not enabled.");
                        }
                        syncToggleWithPreference();
                    }
            );

    public SettingsFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        // --- Notifications ---
        toggleNotification = view.findViewById(R.id.toggle_notification);
        LinearLayout btnNotification = view.findViewById(R.id.btn_notification);
        setupNotificationToggle(btnNotification);

        // --- Language ---
        languageOptions = view.findViewById(R.id.layout_language_options);
        languageArrow = view.findViewById(R.id.iv_language_arrow);
        LinearLayout btnLanguage = view.findViewById(R.id.btn_language);
        btnLanguage.setOnClickListener(v -> toggleLanguageOptions());
        View englishOption = view.findViewById(R.id.option_language_english);
        englishOption.setOnClickListener(v -> setLanguageExpanded(false));

        // --- Currency ---
        currencyOptions = view.findViewById(R.id.layout_currency_options);
        currencyArrow = view.findViewById(R.id.iv_currency_arrow);
        currencySummary = view.findViewById(R.id.tv_currency_summary);
        optionCurrencyVnd = view.findViewById(R.id.option_currency_vnd);
        optionCurrencyUsd = view.findViewById(R.id.option_currency_usd);
        LinearLayout btnCurrency = view.findViewById(R.id.btn_currency);
        btnCurrency.setOnClickListener(v -> toggleCurrencyOptions());
        optionCurrencyVnd.setOnClickListener(v -> setCurrency(CurrencyManager.CURRENCY_VND));
        optionCurrencyUsd.setOnClickListener(v -> setCurrency(CurrencyManager.CURRENCY_USD));
        syncCurrencySelection();

        // --- Navigation Buttons ---
        LinearLayout btnSecurity = view.findViewById(R.id.btn_security);
        btnSecurity.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ChangePasswordActivity.class);
            startActivity(intent);
        });

        LinearLayout btnCategories = view.findViewById(R.id.btn_categories);
        btnCategories.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CategoryManagement.class);
            startActivity(intent);
        });

        //Đẩy logic xóa data về cho ViewModel lo
        LinearLayout btnResetTransaction = view.findViewById(R.id.btn_reset_transaction);
        btnResetTransaction.setOnClickListener(v -> {
            String msg = getString(R.string.confirm_reset) + " all " + getString(R.string.nav_transaction_history) + "?";
            DialogHelper.showCustomDialog(
                    requireContext(),
                    getString(R.string.action_reset_transactions),
                    msg,
                    getString(R.string.action_reset),
                    getString(R.string.action_cancel),
                    DialogHelper.DialogType.DANGER,
                    true,
                    () -> settingsViewModel.resetAllTransactions(requireContext()), // Chỉ việc gọi hàm
                    null
            );
        });

        //Đẩy logic clear DB nội bộ về ViewModel
        LinearLayout btnLogout = view.findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> settingsViewModel.clearDataAndLogout(requireContext()));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        PersonalWorkspaceHeader.bind(this, view);
        observeViewModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        syncToggleWithPreference();
        syncCurrencySelection();
    }

    private void observeViewModel() {
        // Hóng lệnh đăng xuất
        settingsViewModel.isLoggedOut.observe(getViewLifecycleOwner(), isLoggedOut -> {
            if (isLoggedOut != null && isLoggedOut) {
                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

        // Hóng kết quả Reset Transactions (Thành công hay Thất bại)
        settingsViewModel.getResetStatus().observe(getViewLifecycleOwner(), status -> {
            if (status == null) return;
            if (status.isSuccess) {
                DialogHelper.showSuccess(requireContext(), "Done", "All transactions have been deleted!", null);
            } else {
                DialogHelper.showAlert(requireContext(), "Error", "Failed to reset transactions: " + status.message, null);
            }
            // Reset lại state để không bị nổ Dialog lại khi xoay màn hình
            settingsViewModel.clearResetStatus();
        });
    }

    // --- CÁC HÀM XỬ LÝ SETTINGS UI  ---

    private void setupNotificationToggle(LinearLayout btnNotification) {
        syncToggleWithPreference();
        toggleNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingNotificationToggle) return;
            if (isChecked) {
                requestNotificationPermission();
            } else {
                setNotificationPreference(false);
                syncToggleWithPreference();
            }
        });
        btnNotification.setOnClickListener(v -> toggleNotification.setChecked(!toggleNotification.isChecked()));
    }

    private boolean isNotificationEnabled() {
        return NotificationManagerCompat.from(requireContext()).areNotificationsEnabled();
    }

    private boolean isNotificationPreferenceEnabled() {
        return requireContext().getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE).getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    private void setNotificationPreference(boolean enabled) {
        SharedPreferences prefs = requireContext().getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    private void syncToggleWithPreference() {
        if (toggleNotification == null) return;
        isUpdatingNotificationToggle = true;
        toggleNotification.setChecked(isNotificationPreferenceEnabled() && isNotificationEnabled());
        isUpdatingNotificationToggle = false;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                setNotificationPreference(true);
                syncToggleWithPreference();
            } else {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
            return;
        }

        if (isNotificationEnabled()) {
            setNotificationPreference(true);
        } else {
            setNotificationPreference(false);
            ToastHelper.show(requireContext(), "Notifications are disabled in system settings.");
        }
        syncToggleWithPreference();
    }

    private void toggleLanguageOptions() { setLanguageExpanded(!isLanguageExpanded); }

    private void setLanguageExpanded(boolean expanded) {
        isLanguageExpanded = expanded;
        if (languageOptions != null) languageOptions.setVisibility(expanded ? View.VISIBLE : View.GONE);
        if (languageArrow != null) languageArrow.animate().rotation(expanded ? 90f : 0f).setDuration(180).start();
    }

    private void toggleCurrencyOptions() { setCurrencyExpanded(!isCurrencyExpanded); }

    private void setCurrencyExpanded(boolean expanded) {
        isCurrencyExpanded = expanded;
        if (currencyOptions != null) currencyOptions.setVisibility(expanded ? View.VISIBLE : View.GONE);
        if (currencyArrow != null) currencyArrow.animate().rotation(expanded ? 90f : 0f).setDuration(180).start();
    }

    private void setCurrency(String currencyCode) {
        String current = CurrencyManager.getSelectedCurrency(requireContext());
        if (current.equals(currencyCode)) {
            setCurrencyExpanded(false);
            return;
        }
        CurrencyManager.setSelectedCurrency(requireContext(), currencyCode);
        syncCurrencySelection();
        setCurrencyExpanded(false);
        ToastHelper.show(requireContext(), getString(R.string.settings_currency_updated));
    }

    private void syncCurrencySelection() {
        if (currencySummary == null || optionCurrencyVnd == null || optionCurrencyUsd == null || getContext() == null) return;
        String selectedCurrency = CurrencyManager.getSelectedCurrency(requireContext());
        boolean isVnd = CurrencyManager.CURRENCY_VND.equals(selectedCurrency);
        currencySummary.setText(isVnd ? getString(R.string.settings_item_currency_vnd_desc) : getString(R.string.settings_item_currency_usd_desc));
        optionCurrencyVnd.setTextColor(ContextCompat.getColor(requireContext(), isVnd ? R.color.brand_primary : R.color.item_description));
        optionCurrencyUsd.setTextColor(ContextCompat.getColor(requireContext(), isVnd ? R.color.item_description : R.color.brand_primary));
    }
}