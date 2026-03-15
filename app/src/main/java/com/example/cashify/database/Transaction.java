package com.example.cashify.database;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions",
        foreignKeys = @ForeignKey(entity = Category.class,
                parentColumns = "id",
                childColumns = "categoryId",
                onDelete = ForeignKey.CASCADE)) // Xóa danh mục thì xóa luôn giao dịch liên quan
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public double amount;
    public String note;
    public long timestamp; // Lưu ngày tháng bằng số Long để tính toán/lọc cho nhanh
    public int categoryId; // Liên kết với bảng Danh mục
    public int type; // Lưu lại type để Query biểu đồ không cần JOIN bảng cho nặng
}

