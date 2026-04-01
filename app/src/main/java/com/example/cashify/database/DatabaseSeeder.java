package com.example.cashify.database;



import android.content.Context;

import java.util.concurrent.Executors;



public class DatabaseSeeder {

    public static void seedIfEmpty(Context context) {

//Gọi hàm execute này tránh làm đơ màn hình khi chèn dữ liệu

            CategoryDao dao = AppDatabase.getInstance(context).categoryDao();


//Check isEmpty để chèn chỉ khi trống dữ liệu, tránh bị trùng lặp, hoan hô Thu An

            if (dao.getCategoriesByType(0).isEmpty() && dao.getCategoriesByType(1).isEmpty()) {

//Danh sách này chứa các danh mục phổ biến

//--thu vao--//

                dao.insert(makeCategory("Lương", "ic_salary", "#000000", 1, 1));

                dao.insert(makeCategory("Gia đình cho", "ic_family", "#000000", 1, 1));

                dao.insert(makeCategory("Thưởng", "ic_bonus", "#000000", 1, 1));


//--chi ra--//

                dao.insert(makeCategory("Ăn uống", "ic_food", "#FF5722", 0, 1));

                dao.insert(makeCategory("Di chuyển", "ic_transport", "#000000", 0, 1));

                dao.insert(makeCategory("Mua sắm", "ic_shopping", "#000000", 0, 1));

                dao.insert(makeCategory("Hóa đơn", "ic_bill", "#000000", 0, 1));

                dao.insert(makeCategory("Cafe", "ic_cafe", "#000000", 0, 1));

                dao.insert(makeCategory("Giải trí", "ic_entertain", "#000000", 0, 1));

                dao.insert(makeCategory("Xăng xe", "ic_fuel", "#000000", 0, 1));

                dao.insert(makeCategory("Tiền trọ", "ic_house", "#000000", 0, 1));

                dao.insert(makeCategory("Khác", "ic_other", "#000000", 0, 1));


            }

    }

    private static Category makeCategory(String name, String icon, String color, int type, int isDefault){

        Category c=new Category();

        c.name=name;

        c.iconName=icon;

        c.colorCode=color;

        c.type=type;

        c.isDefault=isDefault;

        c.isDeleted=0;

        return c;

    }

}