package com.example.cashify.database;

import android.content.Context;
import android.util.Log;
import java.util.Calendar;
import java.util.concurrent.Executors;

public class FakeDataSeeder {
    public static void seed(Context context) {
        Executors.newSingleThreadExecutor().execute(() -> {
            TransactionDao dao = AppDatabase.getInstance(context).transactionDao();

            // auto chạy
            if (true) {
                Log.d("BACKEND_TEST", "--- GIẢ BỘ CÓ 50 LẦN NẠP TIỀN, CHI TIỀN ---");

                // Thời gian trong khoảng: Tháng 3/2026
                Calendar cal = Calendar.getInstance();
                cal.set(2026, Calendar.MARCH, 1, 9, 0, 0); // từ 9h sáng ngày 01/03

                // THU VÔ
                dao.insert(makeTransaction(15000000L, 1, 1, "Lương tháng 03", getMillis(cal, 5))); //  05/03
                dao.insert(makeTransaction(2000000L, 1, 2, "Ba mẹ gửi tiền phòng", getMillis(cal, 1))); // 01/03
                dao.insert(makeTransaction(500000L, 1, 3, "Thưởng dự án", getMillis(cal, 15)));

                // CHI RA (BỊ ĐỘNG)
                dao.insert(makeTransaction(3500000L, 0, 11, "Tiền thuê nhà tháng 3", getMillis(cal, 2)));
                dao.insert(makeTransaction(450000L, 0, 7, "Hóa đơn Điện & Nước", getMillis(cal, 10)));
                dao.insert(makeTransaction(200000L, 0, 12, "Đóng tiền quỹ lớp", getMillis(cal, 12)));

                // ĂN TOÀN LÀ ĂN
                dao.insert(makeTransaction(35000, 0, 4, "Cơm tấm bãi rác Quận 4", getMillis(cal, 1)));
                dao.insert(makeTransaction(45000, 0, 8, "Phê La - Ô Long Nhài", getMillis(cal, 1)));
                dao.insert(makeTransaction(20000, 0, 4, "Bánh mì dân tổ", getMillis(cal, 2)));
                dao.insert(makeTransaction(55000, 0, 4, "Bún đậu mắm tôm", getMillis(cal, 3)));
                dao.insert(makeTransaction(30000, 0, 8, "Cafe muối chú Long", getMillis(cal, 3)));
                dao.insert(makeTransaction(45000, 0, 4, "Phở bò tái lăn", getMillis(cal, 4)));
                dao.insert(makeTransaction(60000, 0, 4, "Pizza Hut khuyến mãi", getMillis(cal, 5)));
                dao.insert(makeTransaction(25000, 0, 10, "Gửi xe tháng UIT", getMillis(cal, 5)));
                dao.insert(makeTransaction(40000, 0, 4, "Cơm gà xối mỡ", getMillis(cal, 6)));
                dao.insert(makeTransaction(35000, 0, 8, "Trà đào Highland", getMillis(cal, 7)));

                // PHÁT SINH CÁC KIỂU
                dao.insert(makeTransaction(150000, 0, 12, "Mua quà sinh nhật bạn", getMillis(cal, 8)));
                dao.insert(makeTransaction(500000, 0, 6, "Mua quần áo mới (Shopee)", getMillis(cal, 10)));
                dao.insert(makeTransaction(200000, 0, 10, "Bảo trì xe máy", getMillis(cal, 12)));
                dao.insert(makeTransaction(75000, 0, 9, "Vé xem phim Lotte", getMillis(cal, 14)));
                dao.insert(makeTransaction(120000, 0, 4, "Lẩu Kichi Kichi với nhóm", getMillis(cal, 15)));

                // NUÔI GHỆ
                dao.insert(makeTransaction(1200000L, 0, 6, "Mua mỹ phẩm", getMillis(cal, 20)));
                dao.insert(makeTransaction(150000L, 0, 9, "Xem phim CGV", getMillis(cal, 14)));
                dao.insert(makeTransaction(85000L, 0, 8, "Trà sữa", getMillis(cal, 25)));
                dao.insert(makeTransaction(300000L, 0, 6, "Mua sách học Kotlin", getMillis(cal, 18)));

                //LẶP CHO ĐỦ 5 CHỤC
                dao.insert(makeTransaction(35000, 0, 4, "Bún bò Huế", getMillis(cal, 16)));
                dao.insert(makeTransaction(25000, 0, 8, "Trà đá vỉa hè", getMillis(cal, 17)));
                dao.insert(makeTransaction(50000, 0, 10, "Đổ xăng đầy bình", getMillis(cal, 18)));
                dao.insert(makeTransaction(2000000, 0, 6, "Mua tai nghe Bluetooth", getMillis(cal, 20)));
                dao.insert(makeTransaction(35000, 0, 4, "Bún bò Huế", getMillis(cal, 16)));
                dao.insert(makeTransaction(25000, 0, 8, "Trà đá vỉa hè", getMillis(cal, 17)));
                dao.insert(makeTransaction(50000, 0, 10, "Đổ xăng đầy bình", getMillis(cal, 18)));
                dao.insert(makeTransaction(2000000, 0, 6, "Mua tai nghe Bluetooth", getMillis(cal, 20)));
                dao.insert(makeTransaction(35000, 0, 4, "Bún bò Huế", getMillis(cal, 16)));
                dao.insert(makeTransaction(25000, 0, 8, "Trà đá vỉa hè", getMillis(cal, 17)));
                dao.insert(makeTransaction(50000, 0, 10, "Đổ xăng đầy bình", getMillis(cal, 18)));
                dao.insert(makeTransaction(2000000, 0, 6, "Mua tai nghe Bluetooth", getMillis(cal, 20)));
                dao.insert(makeTransaction(35000, 0, 4, "Bún bò Huế", getMillis(cal, 16)));
                dao.insert(makeTransaction(25000, 0, 8, "Trà đá vỉa hè", getMillis(cal, 17)));
                dao.insert(makeTransaction(50000, 0, 10, "Đổ xăng đầy bình", getMillis(cal, 18)));
                dao.insert(makeTransaction(2000000, 0, 6, "Mua tai nghe Bluetooth", getMillis(cal, 20)));
                dao.insert(makeTransaction(35000, 0, 4, "Bún bò Huế", getMillis(cal, 16)));
                dao.insert(makeTransaction(25000, 0, 8, "Trà đá vỉa hè", getMillis(cal, 17)));
                dao.insert(makeTransaction(50000, 0, 10, "Đổ xăng đầy bình", getMillis(cal, 18)));
                dao.insert(makeTransaction(2000000, 0, 6, "Mua tai nghe Bluetooth", getMillis(cal, 20)));
                dao.insert(makeTransaction(35000, 0, 4, "Bún bò Huế", getMillis(cal, 16)));
                dao.insert(makeTransaction(25000, 0, 8, "Trà đá vỉa hè", getMillis(cal, 17)));
                dao.insert(makeTransaction(50000, 0, 10, "Đổ xăng đầy bình", getMillis(cal, 18)));
                dao.insert(makeTransaction(2000000, 0, 6, "Mua tai nghe Bluetooth", getMillis(cal, 20)));

                // in logcat
                cal.set(2026, Calendar.MARCH, 1, 0, 0, 0);
                long start = cal.getTimeInMillis();
                cal.set(2026, Calendar.MARCH, 31, 23, 59, 59);
                long end = cal.getTimeInMillis();

                long expense = dao.getTotalExpense(start, end);
                long income = dao.getTotalIncome(start, end);

                Log.d("BACKEND_TEST", "========================================");
                Log.d("BACKEND_TEST", "TEST HÀNG");
                Log.d("BACKEND_TEST", "Tổng Thu tháng 3: " + income + " VND");
                Log.d("BACKEND_TEST", "Tổng Chi tháng 3: " + expense + " VND");
                Log.d("BACKEND_TEST", "Số dư còn lại: " + (income - expense) + " VND");
                Log.d("BACKEND_TEST", "========================================");
            }
        });
    }

    // Hàm phụ trợ để lấy Millis cho một ngày cụ thể trong tháng 3
    private static long getMillis(Calendar cal, int day) {
        cal.set(Calendar.DAY_OF_MONTH, day);
        return cal.getTimeInMillis();
    }

    private static Transaction makeTransaction(long amount, int type, int categoryId, String note, long timestamp) {
        Transaction t = new Transaction();
        t.amount = amount;
        t.type = type;
        t.categoryId = categoryId;
        t.note = note;
        t.timestamp = timestamp;
        return t;
    }
}