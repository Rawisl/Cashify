package com.example.cashify.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budgets")
public class Budget {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int category_id; // Ngân sách cho mục nào?; -1=ngân sách tổng toàn app
    public long limit_amount; // Hạn mức (Ví dụ: 2.000.000đ)
    public String period_type; //week, month, year
    public long start_date; //timestamp
    public long end_date;//timestamp : mấy má coi cách sử dụng cái mốc tgian này nah
}