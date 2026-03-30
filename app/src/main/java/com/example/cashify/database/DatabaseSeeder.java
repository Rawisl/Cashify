package com.example.cashify.database;

import android.content.Context;

import com.example.cashify.R;

import java.util.concurrent.Executors;

public class DatabaseSeeder {
    public static void seedIfEmpty(Context context){
        //Gọi hàm execute này tránh làm đơ màn hình khi chèn dữ liệu
        Executors.newSingleThreadExecutor().execute(() ->{
            CategoryDao dao=AppDatabase.getInstance(context).categoryDao();

            //Check isEmpty để chèn chỉ khi trống dữ liệu, tránh bị trùng lặp, hoan hô Thu An
            if(dao.getCategoriesByType(0).isEmpty()&&dao.getCategoriesByType(1).isEmpty()){
                //Danh sách này chứa các danh mục phổ biến

                //--thu vao--//
                dao.insert(makeCategory("Lương", "ic_salary", R.color.black, 1, 1));
                dao.insert(makeCategory("Gia đình cho", "ic_family", R.color.black, 1, 1));
                dao.insert(makeCategory("Thưởng", "ic_bonus", R.color.black, 1, 1));

                //--chi ra--//
                dao.insert(makeCategory("Ăn uống", "ic_food", R.color.black, 0, 1));
                dao.insert(makeCategory("Di chuyển", "ic_transport", R.color.black, 0, 1));
                dao.insert(makeCategory("Mua sắm", "ic_shopping", R.color.black, 0, 1));
                dao.insert(makeCategory("Hóa đơn", "ic_bill", R.color.black, 0, 1));
                dao.insert(makeCategory("Cafe", "ic_cafe", R.color.black, 0, 1));
                dao.insert(makeCategory("Giải trí", "ic_entertain", R.color.black, 0, 1));
                dao.insert(makeCategory("Xăng xe", "ic_fuel", R.color.black, 0, 1));
                dao.insert(makeCategory("Tiền trọ", "ic_house", R.color.black, 0, 1));
                dao.insert(makeCategory("Khác", "ic_other", R.color.black, 0, 1));

            }
        });
    }
    private static Category makeCategory(String name, String icon, int colorResId, int type, int isDefault){
        Category c=new Category();
        c.name=name;
        c.iconName=icon;
        c.colorCode = String.valueOf(colorResId);
        c.type=type;
        c.isDefault=isDefault;
        c.isDeleted=0;
        return c;
    }
}
