package com.example.cashify.database;

import android.content.Context;
import java.util.concurrent.Executors;

public class DatabaseSeeder {
    public static void seedIfEmpty(Context context){
        Executors.newSingleThreadExecutor().execute(() ->{
            CategoryDao dao=AppDatabase.getInstance(context).categoryDao();

            //seed neu chua co du lieu
            if(dao.getCategoriesByType(0).isEmpty()&&dao.getCategoriesByType(1).isEmpty()){
                //--thu vao--//
                dao.insert(makeCategory("Lương", "ic_salary", "#FFFFFF", 1, 1));
                dao.insert(makeCategory("Gia đình cho", "ic_family", "#FFFFFF", 1, 1));
                dao.insert(makeCategory("Thưởng", "ic_bonus", "#FFFFFF", 1, 1));

                //--chi ra--//
                dao.insert(makeCategory("Ăn uống", "ic_food", "#FFFFFF", 0, 1));
                dao.insert(makeCategory("Di chuyển", "ic_transport", "#FFFFFF", 0, 1));
                dao.insert(makeCategory("Mua sắm", "ic_shopping", "#FFFFFF", 0, 1));
                dao.insert(makeCategory("Hóa đơn", "ic_bill", "#FFFFFF", 0, 1));
                dao.insert(makeCategory("Cafe", "ic_cafe", "#FFFFFF", 0, 1));
                dao.insert(makeCategory("Giải trí", "ic_entertain", "#FFFFFF", 0, 1));
                dao.insert(makeCategory("Xăng xe", "ic_fuel", "#FFFFFF", 0, 1));
                dao.insert(makeCategory("Tiền trọ", "ic_house", "#FFFFFF", 0, 1));
                dao.insert(makeCategory("Khác", "ic_other", "#FFFFFF", 0, 1));

            }
        });
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
