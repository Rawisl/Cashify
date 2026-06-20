package com.example.cashify.utils;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.cashify.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

// UI Component dùng chung (Reusable View) cho việc nhập số tiền
public class NumpadBottomSheet extends BottomSheetDialogFragment {

    private String rawAmount = "0";
    private TextView tvAmount;
    private OnNumpadListener listener;

    @SuppressLint("SetTextI18n")
    private void updateAmountDisplay() {
        try {
            tvAmount.setText(CurrencyManager.formatNumpadDigits(rawAmount));
        } catch (NumberFormatException e) {
            // Fallback an toàn: Tránh crash nếu dữ liệu đầu vào vượt quá giới hạn parse của CurrencyManager
            rawAmount = rawAmount.length() > 1 ? rawAmount.substring(0, rawAmount.length() - 1) : "0";
            tvAmount.setText(CurrencyManager.formatNumpadDigits(rawAmount));
        }
    }

    private void appendNumber(String num) {
        if (rawAmount.length() >= 15) return; // Chặn độ dài tối đa để chống tràn kiểu dữ liệu (Long/Double)
        rawAmount = rawAmount.equals("0") ? num : rawAmount + num;
        updateAmountDisplay();
    }

    private void removeLastNumber() {
        rawAmount = rawAmount.length() > 1 ? rawAmount.substring(0, rawAmount.length() - 1) : "0";
        updateAmountDisplay();
    }

    public void setInitialAmount(String amount) {
        if (amount != null && !amount.isEmpty()) {
            rawAmount = amount.replaceAll("[^\\d]", "");
            if (rawAmount.isEmpty()) rawAmount = "0";
        }
    }

    public void setInitialBaseAmount(double amount) {
        rawAmount = CurrencyManager.numpadDigitsFromBase(amount);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saveInstanceState) {
        return inflater.inflate(R.layout.layout_bottom_sheet_numpad, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvAmount = view.findViewById(R.id.tv_amount);

        // Gán sự kiện click cho các phím số (UI Logic)
        view.findViewById(R.id.btn_num_1).setOnClickListener(v -> appendNumber("1"));
        view.findViewById(R.id.btn_num_2).setOnClickListener(v -> appendNumber("2"));
        view.findViewById(R.id.btn_num_3).setOnClickListener(v -> appendNumber("3"));
        view.findViewById(R.id.btn_num_4).setOnClickListener(v -> appendNumber("4"));
        view.findViewById(R.id.btn_num_5).setOnClickListener(v -> appendNumber("5"));
        view.findViewById(R.id.btn_num_6).setOnClickListener(v -> appendNumber("6"));
        view.findViewById(R.id.btn_num_7).setOnClickListener(v -> appendNumber("7"));
        view.findViewById(R.id.btn_num_8).setOnClickListener(v -> appendNumber("8"));
        view.findViewById(R.id.btn_num_9).setOnClickListener(v -> appendNumber("9"));
        view.findViewById(R.id.btn_num_0).setOnClickListener(v -> appendNumber("0"));

        view.findViewById(R.id.btn_backspace).setOnClickListener(v -> removeLastNumber());

        Button btnContinue = view.findViewById(R.id.btn_continue);
        btnContinue.setOnClickListener(v -> {
            if (rawAmount.equals("0") || rawAmount.isEmpty()) return;

            if (listener != null) {
                // Đẩy dữ liệu ngược về màn hình gọi nó
                listener.onAmountConfirmed(
                        CurrencyManager.numpadRawToDisplayInput(rawAmount),
                        tvAmount.getText().toString()
                );
            }
            dismiss();
        });

        // Khởi tạo hiển thị ban đầu
        updateAmountDisplay();
    }

    // Giao thức (Contract) để giao tiếp với các Fragment/Activity khác
    public interface OnNumpadListener {
        void onAmountConfirmed(String rawAmount, String formattedAmount);
    }

    public void setListener(OnNumpadListener listener) {
        this.listener = listener;
    }
}