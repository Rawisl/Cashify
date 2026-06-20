package com.example.cashify.data.local;

import android.content.Context;
import android.util.Log;

import com.example.cashify.data.model.Category;
import com.example.cashify.data.model.Transaction;

import java.util.Calendar;
import java.util.List;

public class FakeDataSeeder {

    public static void seed(Context context, List<Category> categories) {
        TransactionDao dao = AppDatabase.getInstance(context).transactionDao();

        // Skip seeding if data already exists
        if (dao.countTransactions("PERSONAL") > 0) return;

        // Insert monthly allowance to balance the massive expenses
        insertIncome(dao, categories, 2026, 3, 9, "Allowance", 1000, "Allowance tuần");
        insertIncome(dao, categories, 2026, 3, 16, "Allowance", 700, "Allowance tuần");
        insertIncome(dao, categories, 2026, 3, 23, "Allowance", 1100, "Allowance tuần");


        // Raw CSV extracted from user's Excel file, mapped strictly to English categories
        String rawCsv =
                "2026-03-09|Food & Dining|30|Bún chả;" +
                        "2026-03-09|Fuel|80|27k/lít đcmcs?;" +
                        "2026-03-10|Food & Dining|30|Cơm gà xối mỡ;" +
                        "2026-03-11|Food & Dining|40|BĐMT đặc biệt;" +
                        "2026-03-11|Food & Dining|40|Lẩu chay;";
        //muốn seed thêm thì thêm đúng định dạng "năm-tháng-ngày|category|số tiền bỏ 3 số 0|description" vào cái rawCsv ấy

        String[] rows = rawCsv.split(";");
        for (String row : rows) {
            String[] cols = row.split("\\|", -1);
            if (cols.length < 4) continue;

            String[] dateParts = cols[0].split("-");
            int year = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]) - 1; // Lùi 1 cho chuẩn Calendar
            int day = Integer.parseInt(dateParts[2]);

            String catName = cols[1];
            long amount = Long.parseLong(cols[2]) * 1000L;
            String note = cols[3];

            int catId = findId(categories, catName);
            if (catId == -1) continue;

            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day, 12, 0, 0);

            Transaction t = new Transaction();
            t.amount = amount;
            t.type = 0; // Chi phí
            t.categoryId = catId;
            t.note = note;
            t.timestamp = cal.getTimeInMillis();
            t.paymentMethod = "Cash";
            t.workspaceId = "PERSONAL";

            dao.insert(t);
        }

        Log.d("FakeDataSeeder", "Seed completed with " + dao.countTransactions("PERSONAL") + " transactions.");
    }

    private static void insertIncome(TransactionDao dao, List<Category> categories, int year, int realMonth, int day, String catName, long amountK, String note) {
        int catId = findId(categories, catName);
        if (catId == -1) return;

        Calendar cal = Calendar.getInstance();
        // Lùi đi 1 để khớp với cách Calendar đếm tháng (0-11)
        cal.set(year, realMonth - 1, day, 9, 0, 0);

        Transaction t = new Transaction();
        t.amount = amountK * 1000L;
        t.type = 1; // Thu nhập
        t.categoryId = catId;
        t.note = note;
        t.timestamp = cal.getTimeInMillis();
        t.paymentMethod = "Bank";
        t.workspaceId = "PERSONAL";
        dao.insert(t);
    }

    private static int findId(List<Category> categories, String name) {
        for (Category c : categories) {
            // Dùng equalsIgnoreCase cho an toàn
            if (c.name.equalsIgnoreCase(name)) return c.id;
        }
        Log.e("FakeDataSeeder", "Category not found: " + name);
        return -1;
    }
}