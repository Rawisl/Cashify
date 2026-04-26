package com.example.cashify.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "categories")
// implements Serializable để "đóng gói" toàn bộ Object này lại,
// giúp truyền nguyên 1 cục dữ liệu qua Bundle (từ Activity sang BottomSheet) 1 cách tự động mà không bị crash.
public class Category implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public int type; // 0=Chi, 1=Thu
    public String iconName; // Tên của icon để lấy từ resources
    public String colorCode; // Mã màu Hex (VD: #FF5733)
    public int isDefault; //1=mặc định (ko cho xóa), 0=user tự tạo (xóa cx dc)
    public int isDeleted;//1=đã xóa mêm, 0=bình thường
    public Category() {
    }
}
