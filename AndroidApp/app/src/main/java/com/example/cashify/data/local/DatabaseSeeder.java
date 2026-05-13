package com.example.cashify.data.local;

import android.content.Context;
import android.util.Log;
import com.example.cashify.data.model.Category;
import java.util.concurrent.Executors;

public class DatabaseSeeder {

    public static void seedIfEmpty(Context context) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                CategoryDao dao = AppDatabase.getInstance(context).categoryDao();

                // Check isEmpty để chèn chỉ khi trống dữ liệu
                if (dao.getCategoriesByType(0).isEmpty() && dao.getCategoriesByType(1).isEmpty()) {

                    // Truyền ID cứng để Room không bị crash (1 -> 12)
                    dao.insert(makeCategory(1, "Lương", "ic_salary", "#AD78B4", 1));
                    dao.insert(makeCategory(2, "Gia đình cho", "ic_family", "#81949D", 1));
                    dao.insert(makeCategory(3, "Thưởng", "ic_bonus", "#9B8077", 1));

                    dao.insert(makeCategory(4, "Ăn uống", "ic_food", "#E96565", 0));
                    dao.insert(makeCategory(5, "Di chuyển", "ic_transport", "#FDA664", 0));
                    dao.insert(makeCategory(6, "Mua sắm", "ic_shopping", "#F675A1", 0));
                    dao.insert(makeCategory(7, "Hóa đơn", "ic_bill", "#559DE4", 0));
                    dao.insert(makeCategory(8, "Cafe", "ic_cafe", "#6CD0D0", 0));
                    dao.insert(makeCategory(9, "Giải trí", "ic_entertain", "#847FF0", 0));
                    dao.insert(makeCategory(10, "Xăng xe", "ic_fuel", "#87D18C", 0));
                    dao.insert(makeCategory(11, "Tiền trọ", "ic_house", "#93CE9D", 0));
                    dao.insert(makeCategory(12, "Khác", "ic_other", "#000000", 0));

                    Log.d("DatabaseSeeder", "Tải dữ liệu thành công!");
                }
            } catch (Exception e) {
                Log.e("DatabaseSeeder", "Lỗi tải dữ liệu: ", e);
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
        c.workspaceId = "PERSONAL"; // VẪN PHẢI GIỮ CÁI NHÃN NÀY
        return c;
    }
}