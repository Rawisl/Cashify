package com.example.cashify.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.cashify.R;
import com.google.android.material.button.MaterialButton;

public class DialogHelper {

    public enum DialogType {
        NORMAL, // Nút confirm màu xanh
        DANGER  // Nút confirm màu đỏ (dùng cho delete/error)
    }

    // Hàm đầy đủ (Full parameters)
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

        // Bo góc trong suốt
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.85);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.setCancelable(false); // Bắt buộc bấm nút

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.tvDialogMessage);

        // Đã cập nhật thành MaterialButton để khớp với file XML mới
        MaterialButton btnConfirm = dialog.findViewById(R.id.btnConfirm);
        MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);

        // Gán text
        tvTitle.setText(title);
        tvMessage.setText(message);

        // Nếu truyền null thì lấy string mặc định trong app
        btnConfirm.setText(confirmText != null ? confirmText : context.getString(R.string.action_confirm));
        btnCancel.setText(cancelText != null ? cancelText : context.getString(R.string.action_cancel));

        // Đổi màu nút Confirm trực tiếp qua TintList thay vì dùng file drawable
        if (type == DialogType.DANGER) {
            btnConfirm.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.status_red)));
        } else {
            btnConfirm.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.brand_primary)));
        }

        // Ẩn/Hiện nút Cancel
        if (!showCancelButton) {
            btnCancel.setVisibility(View.GONE);
        } else {
            btnCancel.setVisibility(View.VISIBLE);
        }

        // Bắt sự kiện Click
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

    // --- CÁC HÀM OVERLOAD RÚT GỌN ĐỂ GỌI CHO NHANH ---

    // 1. Dùng cho Xác nhận / Xóa bình thường (Có 2 nút, xài text mặc định)
    public static void showCustomDialog(
            Context context,
            String title,
            String message,
            DialogType type,
            Runnable onConfirm
    ) {
        showCustomDialog(context, title, message, null, null, type, true, onConfirm, null);
    }

    // 2. Dùng cho Báo lỗi / Cảnh báo (Chỉ có 1 nút OK màu đỏ DANGER)
    public static void showAlert(
            Context context,
            String title,
            String message,
            Runnable onConfirm
    ) {
        showCustomDialog(context, title, message, "OK", null, DialogType.DANGER, false, onConfirm, null);
    }

    // 3. Dùng cho Thông báo thành công / Info (Chỉ có 1 nút OK màu xanh NORMAL)
    public static void showSuccess(
            Context context,
            String title,
            String message,
            Runnable onConfirm
    ) {
        showCustomDialog(context, title, message, "OK", null, DialogType.NORMAL, false, onConfirm, null);
    }
}

//Cách dùng:
//// Báo lỗi: Số tiền phải lớn hơn 0
//        DialogHelper.showAlert(
//        this,
//                "Lỗi nhập liệu",
//        getString(R.string.error_invalid_money_amount),
//        () -> {
//                // Code thực thi sau khi người dùng bấm "OK" (Dialog tự động đóng)
//                // Ví dụ: Focus lại vào ô nhập tiền
//                etAmount.requestFocus();
//        }
//                );
//
//// Xác nhận lưu giao dịch
//                DialogHelper.showCustomDialog(
//        this,
//                "Lưu giao dịch",
//                "Bạn có chắc chắn muốn lưu giao dịch này vào hệ thống?",
//        DialogHelper.DialogType.NORMAL, // NORMAL = Nút xanh
//        () -> {
//// Code lưu transaction vào Database ở đây
//saveTransactionToDB();
//        }
//                );
//
//// Lấy tên category từ list, ví dụ "Ăn uống"
//String categoryName = "Ăn uống";
//
//DialogHelper.showCustomDialog(
//        this,
//        getString(R.string.action_delete_category), // "Delete category"
//getString(R.string.confirm_delete, categoryName), // "Are you sure you want to delete 'Ăn uống'?"
//getString(R.string.action_delete), // Đổi chữ Confirm -> "Delete"
//getString(R.string.action_cancel), // Giữ nguyên chữ "Cancel"
//DialogHelper.DialogType.DANGER,    // DANGER = Nút đỏ
//        true,                              // Cho phép hiện nút Cancel
//        () -> {
//// Code thực thi khi bấm "Delete"
//deleteCategoryFromDB(categoryName);
//        },
//                () -> {
//                // Code thực thi khi bấm "Cancel" (Ví dụ: log ra màn hình, hoặc bỏ trống)
//                }
//                );
//
//                DialogHelper.showCustomDialog(
//        this,
//                "Cảnh báo nguy hiểm",
//        getString(R.string.settings_item_reset_transaction_desc), // "Delete all the transactions"
//        "Đồng ý Reset", // Đổi tên nút cho rõ ràng
//                "Thôi",
//DialogHelper.DialogType.DANGER,
//        true,
//        () -> {
//// Xóa toàn bộ DB
//resetAllTransactions();
//        },
//                null // Nếu bấm Cancel không cần làm gì thêm thì truyền null cho lẹ
//                );
// Thông báo xóa thành công, bấm OK là tự tắt dialog, không cần code thêm
//DialogHelper.showSuccess(
//        this,
//                "Thành công",
//                "Đã xóa danh mục Ăn uống thành công!",
//                null
//);
//
//// Hoặc nếu muốn làm gì đó sau khi người dùng bấm OK:
//DialogHelper.showSuccess(
//        this,
//                "Thành công",
//                "Thêm giao dịch mới thành công!",
//                () -> {
//// Ví dụ: Load lại danh sách hoặc chuyển về màn hình Home
//finish();
//        }
//                );