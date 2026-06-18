package com.example.cashify.data.local;

import android.content.Context;
import android.util.Log;
import com.example.cashify.data.model.Category;
import java.util.concurrent.Executors;

public class DatabaseSeeder {

    public static void seedIfEmpty(Context context) {
        // Chạy trên luồng phụ để không làm đơ giao diện lúc khởi tạo app
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                CategoryDao dao = AppDatabase.getInstance(context).categoryDao();

                // Chỉ nạp dữ liệu mẫu nếu DB đang trống
                if (dao.getCategoriesByType(0).isEmpty() && dao.getCategoriesByType(1).isEmpty()) {

                    // Khởi tạo danh mục Mặc định.
                    // Truyền ID cứng (1 -> 12) để tránh lỗi trùng lặp khi Sync với Firebase

                    // Income (Thu)
                    dao.insert(makeCategory(1, "Salary", "ic_salary", "#AD78B4", 1));
                    dao.insert(makeCategory(2, "Allowance", "ic_family", "#81949D", 1));
                    dao.insert(makeCategory(3, "Bonus", "ic_bonus", "#9B8077", 1));

                    // Expense (Chi)
                    dao.insert(makeCategory(4, "Food & Dining", "ic_food", "#E96565", 0));
                    dao.insert(makeCategory(5, "Transport", "ic_transport", "#FDA664", 0));
                    dao.insert(makeCategory(6, "Shopping", "ic_shopping", "#F675A1", 0));
                    dao.insert(makeCategory(7, "Bills", "ic_bill", "#559DE4", 0));
                    dao.insert(makeCategory(8, "Coffee", "ic_cafe", "#6CD0D0", 0));
                    dao.insert(makeCategory(9, "Entertainment", "ic_entertain", "#847FF0", 0));
                    dao.insert(makeCategory(10, "Fuel", "ic_fuel", "#87D18C", 0));
                    dao.insert(makeCategory(11, "Rent", "ic_house", "#93CE9D", 0));
                    dao.insert(makeCategory(12, "Other", "ic_other", "#000000", 0));

                    Log.d("DatabaseSeeder", "Seed categories successfully!");
                }
            } catch (Exception e) {
                Log.e("DatabaseSeeder", "Seed categories error: ", e);
            }
        });
    }

    private static Category makeCategory(int id, String name, String icon, String color, int type) {
        Category c = new Category();
        c.id = id;
        c.name = name;
        c.iconName = icon;
        c.colorCode = color;
        c.type = type;
        c.isDefault = 1;
        c.isDeleted = 0;
        // Bắt buộc giữ nhãn này để phân biệt danh mục cá nhân với danh mục của quỹ nhóm
        c.workspaceId = "PERSONAL";
        return c;
    }
}