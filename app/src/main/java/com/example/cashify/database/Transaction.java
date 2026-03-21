package com.example.cashify.database;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions",
        foreignKeys = @ForeignKey(entity = Category.class,
                parentColumns = "id",
                childColumns = "categoryId",
                onDelete = ForeignKey.CASCADE),// Xóa danh mục thì xóa luôn giao dịch liên quan
                indices = {@Index("categoryId")}
)
public class Transaction {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public long amount;
    public int categoryId; // Liên kết với bảng Danh mục
    public String note;
    public long timestamp; // Lưu ngày tháng bằng số Long để tính toán/lọc cho nhanh

    public int type; // 0=chi, 1=thu (lưu lại để truy vấn nhanh, khỏi join)
}

