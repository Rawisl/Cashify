package com.example.cashify.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.cashify.R;
import com.google.android.material.button.MaterialButton;

/**
 * Utility class to display unified custom dialogs across the app.
 * * Usage Examples:
 * * 1. Error/Alert Dialog (Single RED button):
 * DialogHelper.showAlert(this, "Input Error", "Amount must be greater than 0", () -> etAmount.requestFocus());
 * * 2. Success Dialog (Single PRIMARY button):
 * DialogHelper.showSuccess(this, "Success", "Transaction saved successfully!", () -> finish());
 * * 3. Standard Confirmation (Two buttons, default text):
 * DialogHelper.showCustomDialog(this, "Save", "Save this transaction?", DialogHelper.DialogType.NORMAL, () -> saveToDB());
 * * 4. Dangerous Action Confirmation (Custom text, RED confirm button):
 * DialogHelper.showCustomDialog(this, "Delete Category", "Are you sure you want to delete this?",
 * "Delete", "Cancel", DialogHelper.DialogType.DANGER, true, () -> deleteCategory(), null);
 */
public class DialogHelper {

    public enum DialogType {
        NORMAL, // Primary color confirm button (e.g., Save, OK)
        DANGER  // Red color confirm button (e.g., Delete, Error)
    }

    /**
     * Fully customizable dialog constructor.
     */
    public static void showCustomDialog(
            Context context,
            String title,
            String message,
            String confirmText,
            String cancelText,
            DialogType type,
            boolean showCancelButton,
            Runnable onConfirm,
            Runnable onCancel
    ) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.layout_custom_dialog);

        // Apply transparent background for custom rounded corners and set width to 85% of screen
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Force user interaction with buttons
        dialog.setCancelable(false);

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);

        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setText(title);
        tvMessage.setText(message);

        // Fallback to default strings if custom text is not provided
        btnConfirm.setText(confirmText != null ? confirmText : context.getString(R.string.action_confirm));
        btnCancel.setText(cancelText != null ? cancelText : context.getString(R.string.action_cancel));

        // Apply dynamic tint based on DialogType
        if (type == DialogType.DANGER) {
            btnConfirm.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_red)));
        } else {
            btnConfirm.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.brand_primary)));
        }

        // Adjust layout dynamically if the Cancel button is hidden
        if (!showCancelButton) {
            btnCancel.setVisibility(View.GONE);

            // Center the Confirm button
            LinearLayout btnContainer = dialog.findViewById(R.id.btnContainer);
            btnContainer.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.4), // ~40% screen width
                    (int) (56 * context.getResources().getDisplayMetrics().density)        // 56dp to px
            );
            params.setMargins(0, 0, 0, 0);
            btnConfirm.setLayoutParams(params);
        } else {
            btnCancel.setVisibility(View.VISIBLE);
        }

        btnConfirm.setOnClickListener(v -> {
            if (onConfirm != null) {
                onConfirm.run();
            }
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            if (onCancel != null) {
                onCancel.run();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    // =========================================================================
    // OVERLOADED CONVENIENCE METHODS
    // =========================================================================

    /**
     * Standard confirmation dialog (Two buttons, default text).
     */
    public static void showCustomDialog(
            Context context,
            String title,
            String message,
            DialogType type,
            Runnable onConfirm
    ) {
        showCustomDialog(context, title, message, null, null, type, true, onConfirm, null);
    }

    /**
     * Alert/Error dialog (Single RED button).
     */
    public static void showAlert(
            Context context,
            String title,
            String message,
            Runnable onConfirm
    ) {
        showCustomDialog(context, title, message, "OK", null, DialogType.DANGER, false, onConfirm, null);
    }

    /**
     * Success/Info dialog (Single PRIMARY button).
     */
    public static void showSuccess(
            Context context,
            String title,
            String message,
            Runnable onConfirm
    ) {
        showCustomDialog(context, title, message, "OK", null, DialogType.NORMAL, false, onConfirm, null);
    }
}