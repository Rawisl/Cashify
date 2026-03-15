package com.example.cashify.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budgets")
public class Budget {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int categoryId; // Ngân sách cho mục nào?
    public double amountLimit; // Hạn mức (Ví dụ: 2.000.000đ)
    public int month; // Tháng (1-12)
    public int year; // Năm
}