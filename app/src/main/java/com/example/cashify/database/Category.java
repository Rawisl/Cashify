package com.example.cashify.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class Category {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String iconName; // Tên của icon để lấy từ resources
    public String colorCode; // Mã màu Hex (VD: #FF5733)

    public int type; // 0: Chi ra (Expense), 1: Thu vào (Income)
}
