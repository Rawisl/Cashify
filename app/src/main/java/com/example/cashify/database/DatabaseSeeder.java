package com.example.cashify.database;



import android.content.Context;

import java.util.concurrent.Executors;



public class    DatabaseSeeder {

    public static void seedIfEmpty(Context context) {

//Gọi hàm execute này tránh làm đơ màn hình khi chèn dữ liệu

            CategoryDao dao = AppDatabase.getInstance(context).categoryDao();


//Check isEmpty để chèn chỉ khi trống dữ liệu, tránh bị trùng lặp, hoan hô Thu An

            if (dao.getCategoriesByType(0).isEmpty() && dao.getCategoriesByType(1).isEmpty()) {

//Danh sách này chứa các danh mục phổ biến

//--thu vao--//

                dao.insert(makeCategory("Lương", "ic_salary", "#AD78B4", 1, 1));

                dao.insert(makeCategory("Gia đình cho", "ic_family", "#81949D", 1, 1));

                dao.insert(makeCategory("Thưởng", "ic_bonus", "#9B8077", 1, 1));


//--chi ra--//

                dao.insert(makeCategory("Ăn uống", "ic_food", "#E96565", 0, 1));

                dao.insert(makeCategory("Di chuyển", "ic_transport", "#FDA664", 0, 1));

                dao.insert(makeCategory("Mua sắm", "ic_shopping", "#F675A1", 0, 1));

                dao.insert(makeCategory("Hóa đơn", "ic_bill", "#559DE4", 0, 1));

                dao.insert(makeCategory("Cafe", "ic_cafe", "#6CD0D0", 0, 1));

                dao.insert(makeCategory("Giải trí", "ic_entertain", "#847FF0", 0, 1));

                dao.insert(makeCategory("Xăng xe", "ic_fuel", "#87D18C", 0, 1));

                dao.insert(makeCategory("Tiền trọ", "ic_house", "#93CE9D", 0, 1));

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