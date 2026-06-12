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

public class NumpadBottomSheet extends BottomSheetDialogFragment {

    private String rawAmount = "0";
    private TextView tvAmount;
    private OnNumpadListener listener;

    @SuppressLint("SetTextI18n")
    private void updateAmountDisplay() {
        try {
            tvAmount.setText(CurrencyManager.formatNumpadDigits(rawAmount));
        } catch (NumberFormatException e) {
            rawAmount = rawAmount.length() > 1 ? rawAmount.substring(0, rawAmount.length() - 1) : "0";
            tvAmount.setText(CurrencyManager.formatNumpadDigits(rawAmount));
        }
    }

    private void appendNumber(String num) {
        if (rawAmount.length() >= 15) return;
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
        TextView btn1 = view.findViewById(R.id.btn_num_1);
        TextView btn2 = view.findViewById(R.id.btn_num_2);
        TextView btn3 = view.findViewById(R.id.btn_num_3);
        TextView btn4 = view.findViewById(R.id.btn_num_4);
        TextView btn5 = view.findViewById(R.id.btn_num_5);
        TextView btn6 = view.findViewById(R.id.btn_num_6);
        TextView btn7 = view.findViewById(R.id.btn_num_7);
        TextView btn8 = view.findViewById(R.id.btn_num_8);
        TextView btn9 = view.findViewById(R.id.btn_num_9);
        TextView btn0 = view.findViewById(R.id.btn_num_0);
        ImageView btnBackspace = view.findViewById(R.id.btn_backspace);
        Button btnContinue = view.findViewById(R.id.btn_continue);

        btn1.setOnClickListener(v -> appendNumber("1"));
        btn2.setOnClickListener(v -> appendNumber("2"));
        btn3.setOnClickListener(v -> appendNumber("3"));
        btn4.setOnClickListener(v -> appendNumber("4"));
        btn5.setOnClickListener(v -> appendNumber("5"));
        btn6.setOnClickListener(v -> appendNumber("6"));
        btn7.setOnClickListener(v -> appendNumber("7"));
        btn8.setOnClickListener(v -> appendNumber("8"));
        btn9.setOnClickListener(v -> appendNumber("9"));
        btn0.setOnClickListener(v -> appendNumber("0"));
        btnBackspace.setOnClickListener(v -> removeLastNumber());

        btnContinue.setOnClickListener(v -> {
            if (rawAmount.equals("0") || rawAmount.isEmpty()) return;
            if (listener != null) {
                listener.onAmountConfirmed(
                        CurrencyManager.numpadRawToDisplayInput(rawAmount),
                        tvAmount.getText().toString()
                );
            }
            dismiss();
        });

        updateAmountDisplay();
    }

    public interface OnNumpadListener {
        void onAmountConfirmed(String rawAmount, String formattedAmount);
    }

    public void setListener(OnNumpadListener listener) {
        this.listener = listener;
    }
}
