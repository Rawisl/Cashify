package com.example.cashify.data.local;

import android.content.Context;
import android.util.Log;

import com.example.cashify.data.model.Category;
import com.example.cashify.data.model.Transaction;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FakeDataSeeder {

    public static void seed(Context context, List<Category> categories, String uid, boolean pushToFirebase) {
        TransactionDao dao = AppDatabase.getInstance(context).transactionDao();

        // Bỏ qua nếu máy này đã có dữ liệu
        if (dao.countTransactions("PERSONAL") > 0) return;

        List<Transaction> generatedTrans = new ArrayList<>();

        // Thu nhập
        insertIncome(dao, categories, generatedTrans, 2026, 3, 9, "Allowance", 1000, "Allowance tuần");
        insertIncome(dao, categories, generatedTrans, 2026, 3, 16, "Allowance", 700, "Allowance tuần");
        insertIncome(dao, categories, generatedTrans, 2026, 3, 23, "Allowance", 1100, "Allowance tuần");

        // Dữ liệu chi tiêu
        String rawCsv =
                "2026-03-09|Food & Dining|30|Bún chả;" +
                        "2026-03-09|Fuel|80|27k/lít đcmcs?;" +
                        "2026-03-10|Food & Dining|30|Cơm gà xối mỡ;" +
                        "2026-03-11|Food & Dining|40|BĐMT đặc biệt;" +
                        "2026-03-11|Food & Dining|40|Lẩu chay;" +
                        "2026-06-20|Entertainment|33|Spotify;";

        //muốn seed thêm thì tự thêm giống định dạng trên vào cái rawCsv ấy
        String[] rows = rawCsv.split(";");
        for (String row : rows) {
            String[] cols = row.split("\\|", -1);
            if (cols.length < 4) continue;

            String[] dateParts = cols[0].split("-");
            int year = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]) - 1;
            int day = Integer.parseInt(dateParts[2]);

            String catName = cols[1];
            long amount = Long.parseLong(cols[2]) * 1000L;
            String note = cols[3];

            int catId = findId(categories, catName);
            if (catId == -1) continue;

            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day, 12, 0, 0);

            Transaction t = new Transaction();
            t.id = UUID.randomUUID().toString();
            t.amount = amount;
            t.type = 0; // Expense
            t.categoryId = catId;
            t.note = note;
            t.timestamp = cal.getTimeInMillis();
            t.paymentMethod = "Cash";
            t.workspaceId = "PERSONAL";

            dao.insert(t);
            generatedTrans.add(t);
        }

        //ĐẨY LÊN FIREBASE BẰNG 1 REQUEST DUY NHẤT
        if (pushToFirebase && uid != null && !uid.isEmpty()) {
            FirebaseFirestore dbCloud = FirebaseFirestore.getInstance();
            WriteBatch batch = dbCloud.batch();

            for (Transaction trans : generatedTrans) {
                Map<String, Object> data = new HashMap<>();
                data.put("amount", trans.amount);
                data.put("type", trans.type);
                data.put("categoryId", trans.categoryId);
                data.put("note", trans.note);
                data.put("timestamp", trans.timestamp);
                data.put("paymentMethod", trans.paymentMethod);
                data.put("workspaceId", trans.workspaceId);

                // Gói vào WriteBatch
                batch.set(dbCloud.collection("users")
                        .document(uid)
                        .collection("transactions")
                        .document(trans.id), data);
            }

            // Gửi cục hàng lên Cloud
            batch.commit()
                    .addOnSuccessListener(v -> Log.d("FakeDataSeeder", "Bắn " + generatedTrans.size() + " records lên Firebase thành công!"))
                    .addOnFailureListener(e -> Log.e("FakeDataSeeder", "Lỗi bắn Firebase: " + e.getMessage()));
        }

        Log.d("FakeDataSeeder", "Seed completed with " + dao.countTransactions("PERSONAL") + " local transactions.");
    }

    private static void insertIncome(TransactionDao dao, List<Category> categories, List<Transaction> generatedTrans, int year, int realMonth, int day, String catName, long amountK, String note) {
        int catId = findId(categories, catName);
        if (catId == -1) return;

        Calendar cal = Calendar.getInstance();
        cal.set(year, realMonth - 1, day, 9, 0, 0);

        Transaction t = new Transaction();
        t.id = UUID.randomUUID().toString();
        t.amount = amountK * 1000L;
        t.type = 1; // Income
        t.categoryId = catId;
        t.note = note;
        t.timestamp = cal.getTimeInMillis();
        t.paymentMethod = "Bank";
        t.workspaceId = "PERSONAL";

        dao.insert(t);
        generatedTrans.add(t);
    }

    private static int findId(List<Category> categories, String name) {
        for (Category c : categories) {
            if (c.name.equalsIgnoreCase(name)) return c.id;
        }
        return -1;
    }
}