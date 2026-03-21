package com.example.cashify.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public int type; // 0=Chi, 1=Thu
    public String icon_name; // Tên của icon để lấy từ resources
    public String color_code; // Mã màu Hex (VD: #FF5733)
    public int is_default; //1=mặc định (ko cho xóa), 0=user tự tạo (xóa cx dc)
    public int is_deleted;//1=đã xóa mêm, 0=bình thường

}
