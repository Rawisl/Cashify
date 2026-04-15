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

import java.text.DecimalFormat;

public class NumpadBottomSheet extends BottomSheetDialogFragment
{

    //Have to distinguish raw data and display text (formatted)
    private String rawAmount = "0";
    private TextView tvAmount;

    //Danh sách hàm xử lý nhập, xóa, format số
    @SuppressLint("SetTextI18n")
    private void updateAmountDisplay() {
        try {
            // 1. Ép chuỗi thô thành số nguyên (Long để chứa được nghìn tỷ)
            long amount = Long.parseLong(rawAmount);

            // 2. Dùng DecimalFormat để phẩy tiền (VD: 1000000 -> 1,000,000)
            DecimalFormat formatter = new DecimalFormat("#,###");
            String formattedString = formatter.format(amount);

            // 3. Chuẩn VNĐ: Đổi dấu phẩy thành dấu chấm (1.000.000)
            formattedString = formattedString.replace(",", ".");

            // 4. Gắn lên màn hình + Chữ "đ"
            tvAmount.setText(formattedString + "đ");

        } catch (NumberFormatException e) {
            // Chống crash nếu người dùng gõ số quá dài (vượt quá giới hạn của Long)
            rawAmount = rawAmount.substring(0, rawAmount.length() - 1);
        }
    }

    private void appendNumber(String num) {
        // Chặn người dùng gõ quá 15 số (Tầm trăm nghìn tỷ là đủ xài rồi!)
        if (rawAmount.length() >= 15) return;

        // Nếu đang là "0", thì đè số mới lên (Tránh vụ hiển thị "01", "02")
        if (rawAmount.equals("0")) {
            rawAmount = num;
        } else {
            // Nếu không thì cứ nối vào đuôi
            rawAmount += num;
        }

        // Gọi cỗ máy phép thuật cập nhật UI
        updateAmountDisplay();
    }

    private void removeLastNumber() {
        // Nếu chuỗi dài hơn 1 ký tự -> Cắt ký tự cuối cùng
        if (rawAmount.length() > 1) {
            rawAmount = rawAmount.substring(0, rawAmount.length() - 1);
        } else {
            // Nếu chỉ còn 1 ký tự mà bấm xóa -> Trả về "0"
            rawAmount = "0";
        }

        // Gọi cỗ máy phép thuật cập nhật UI
        updateAmountDisplay();
    }

    //hiện số cũ của object mới bấm vô (pre-fill)
    public void setInitialAmount(String amount)
    {
        if (amount != null && !amount.isEmpty()) {
            this.rawAmount = amount;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saveInstanceState)
    {
        //load .xml layout
        return inflater.inflate(R.layout.layout_bottom_sheet_numpad, container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view,savedInstanceState);

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

        btnBackspace.setOnClickListener(v->removeLastNumber());

        btnContinue.setOnClickListener(v ->
            {
                // Chặn không cho lưu nếu tiền bằng 0
                if (rawAmount.equals("0") || rawAmount.isEmpty()) {
                    // Có thể hiện Toast báo lỗi ở đây: Toast.makeText(getContext(), "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
                    return; // Dừng lại, không làm gì cả
                }

                if (listener != null) {
                    listener.onAmountConfirmed(rawAmount, tvAmount.getText().toString());
                }
                dismiss();
            }
        );

        updateAmountDisplay();
    }

    // 1. Định nghĩa bộ đàm cho nút continue
    public interface OnNumpadListener
    {
        // Hàm này sẽ chở 2 món hàng: Số thô (để lưu Database) và Số đẹp (để hiện lên màn hình)
        void onAmountConfirmed(String rawAmount, String formattedAmount);
    }

    // 2. Tạo một biến để cầm cái bộ đàm này
    private OnNumpadListener listener;

    // 3. Hàm để màn hình chính kết nối vào bộ đàm
    public void setListener(OnNumpadListener listener)
    {
        this.listener = listener;
    }
}


