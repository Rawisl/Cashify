package com.example.cashify.database;

import android.content.Context;
import android.util.Log;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

public class FakeDataSeeder {
    public static void seed(Context context, List<Category> categories) {
        TransactionDao dao = AppDatabase.getInstance(context).transactionDao();

        // Tìm ID theo tên category
        int idLuong = findId(categories, "Lương");
        int idGiaDinh = findId(categories, "Gia đình cho");
        int idThuong = findId(categories, "Thưởng");
        int idAnUong = findId(categories, "Ăn uống");
        int idDiChuyen = findId(categories, "Di chuyển");
        int idMuaSam = findId(categories, "Mua sắm");
        int idHoaDon = findId(categories, "Hóa đơn");
        int idCafe = findId(categories, "Cafe");
        int idGiaiTri = findId(categories, "Giải trí");
        int idXangXe = findId(categories, "Xăng xe");
        int idTienTro = findId(categories, "Tiền trọ");
        int idKhac = findId(categories, "Khác");

        // THÁNG 2/2026
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.FEBRUARY, 1, 9, 0, 0);
        // Lương nhận qua thẻ, chi tiêu linh hoạt các ví
        dao.insert(makeTransaction(15000000L, 1, idLuong, "Lương tháng 02", getMillis(cal, 5), "Bank"));
        dao.insert(makeTransaction(2000000L, 1, idGiaDinh, "Ba mẹ gửi tiền phòng", getMillis(cal, 1), "Card"));
        dao.insert(makeTransaction(3500000L, 0, idTienTro, "Tiền thuê nhà tháng 2", getMillis(cal, 2), "Card"));
        dao.insert(makeTransaction(450000L, 0, idHoaDon, "Hóa đơn Điện & Nước", getMillis(cal, 10), "Card"));
        dao.insert(makeTransaction(35000L, 0, idAnUong, "Cơm tấm Quận 4", getMillis(cal, 3), "Cash"));
        dao.insert(makeTransaction(45000L, 0, idCafe, "Phê La", getMillis(cal, 5), "Bank"));
        dao.insert(makeTransaction(500000L, 0, idMuaSam, "Quần áo Shopee", getMillis(cal, 14), "Bank"));

        // THÁNG 3/2026
        cal.set(2026, Calendar.MARCH, 1, 9, 0, 0);
        dao.insert(makeTransaction(15000000L, 1, idLuong, "Lương tháng 03", getMillis(cal, 5), "Bank"));
        dao.insert(makeTransaction(55000L, 0, idAnUong, "Bún đậu mắm tôm", getMillis(cal, 3), "Cash"));
        dao.insert(makeTransaction(200000L, 0, idXangXe, "Bảo trì xe máy", getMillis(cal, 12), "Cash"));
        dao.insert(makeTransaction(2000000L, 0, idMuaSam, "Tai nghe Bluetooth", getMillis(cal, 20), "Bank"));
        dao.insert(makeTransaction(120000L, 0, idAnUong, "Lẩu Kichi Kichi", getMillis(cal, 25), "Card"));

        // THÁNG 4/2026
        cal.set(2026, Calendar.APRIL, 1, 9, 0, 0);
        dao.insert(makeTransaction(15000000L, 1, idLuong, "Lương tháng 04", getMillis(cal, 5), "Card"));
        dao.insert(makeTransaction(3500000L, 0, idTienTro, "Tiền thuê nhà 4", getMillis(cal, 2), "Bank"));
        dao.insert(makeTransaction(35000L, 0, idAnUong, "Cơm tấm sáng", getMillis(cal, 4), "Cash"));

        Log.d("BACKEND_TEST", "Seed xong " + dao.countTransactions() + " giao dịch kèm Phương thức thanh toán!");
    }

    // Hàm tìm ID theo tên
    private static int findId(List<Category> categories, String name) {
        for (Category c : categories) {
            if (c.name.equals(name)) return c.id;
        }
        Log.e("BACKEND_TEST", "Không tìm thấy category: " + name);
        return -1; // Trả về -1 nếu không tìm thấy
    }

        // Hàm phụ trợ để lấy Millis cho một ngày cụ thể trong tháng 3
        private static long getMillis (Calendar cal,int day){
            cal.set(Calendar.DAY_OF_MONTH, day);
            return cal.getTimeInMillis();
        }

        private static Transaction makeTransaction ( long amount, int type, int categoryId, String
        note,long timestamp, String method){
            Transaction t = new Transaction();
            t.amount = amount;
            t.type = type;
            t.categoryId = categoryId;
            t.note = note;
            t.timestamp = timestamp;
            t.paymentMethod=method;
            return t;
        }
    }

