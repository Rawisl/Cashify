package com.example.cashify.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budgets")
public class Budget {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int categoryId; // Ngân sách cho mục nào?; -1=ngân sách tổng toàn app
    public long limitAmount; // Hạn mức (Ví dụ: 2.000.000đ)
    public String periodType; //week, month, year
    public long startDate; //timestamp
    public long endDate;//timestamp : mấy má coi cách sử dụng cái mốc tgian này nah
    public String workspaceId;

    public Budget() {
    }
}