package com.example.cashify.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyFormatter
{

    // Setup bộ ký hiệu chuẩn Việt Nam (Dấu chấm chia nghìn, phẩy chia thập phân)
    private static final DecimalFormat dfNormal;
    private static final DecimalFormat dfShort;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');

        dfNormal = new DecimalFormat("#,###", symbols); // Dành cho 15.000.000
        dfShort = new DecimalFormat("#.##", symbols);   // Dành cho 15,5
    }

    /**
     * KHUÔN 1: DÙNG CHO MASTER CARD (Full HD, chỉ rút gọn khi lên Tỷ)
     * VD: 15.000.000 -> 15.000.000 VNĐ
     * VD: 1.500.000.000 -> 1,5 Tỷ VNĐ
     */
    public static String formatFullVND(double amount) {
        double absAmount = Math.abs(amount);
        String sign = amount < 0 ? "-" : "";

        if (absAmount >= 1_000_000_000) {
            return sign + dfShort.format(absAmount / 1_000_000_000) + " Tỷ VND";
        } else {
            // Dưới 1 tỷ thì cứ full số 0 cho sướng mắt
            return sign + (absAmount == 0 ? "0" : dfNormal.format(absAmount)) + " VND";
        }
    }

    /**
     * KHUÔN 2: DÙNG CHO THẺ CATEGORY TRONG LIST (Rút gọn cả Triệu và Tỷ cho nhỏ gọn)
     * VD: 850.000 -> 850.000 đ
     * VD: 15.500.000 -> 15,5 Tr
     * VD: 1.500.000.000 -> 1,5 Tỷ
     */
    public static String formatCompactVND(double amount) {
        double absAmount = Math.abs(amount);
        String sign = amount < 0 ? "-" : "";

        if (absAmount >= 1_000_000_000) {
            return sign + dfShort.format(absAmount / 1_000_000_000) + " Tỷ";
        } else if (absAmount >= 1_000_000) {
            return sign + dfShort.format(absAmount / 1_000_000) + " Tr";
        } else {
            // Dưới 1 triệu thì hiện bình thường, thay chữ VNĐ bằng 'đ' cho ngắn nhất có thể
            return sign + (absAmount == 0 ? "0" : dfNormal.format(absAmount)) + " đ";
        }
    }

    // Bổ sung hàm Parse: Gom toàn bộ logic bóc tách chuỗi dơ bẩn vào đây
    public static double parseVNDToDouble(String formattedAmount) {
        if (formattedAmount == null || formattedAmount.trim().isEmpty()) {
            return 0;
        }
        try {
            // Giữ lại số nguyên, loại bỏ dấu phẩy, chấm, chữ cái...
            String cleanString = formattedAmount.replaceAll("[^\\d]", "");
            if (cleanString.isEmpty()) return 0;
            return Double.parseDouble(cleanString);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    public static String formatDoubleToVND(double amount) {
        // Sử dụng khuôn format có dấu phân cách hàng ngàn (VD: 50.000)
        return dfNormal.format(amount);
    }
}