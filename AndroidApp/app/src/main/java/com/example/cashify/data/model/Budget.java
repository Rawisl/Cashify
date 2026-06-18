package com.example.cashify.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budgets")
public class Budget {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int categoryId; // Danh mục áp dụng ngân sách; -1 = Ngân sách tổng (Master Budget)
    public long limitAmount; // Hạn mức tối đa
    public String periodType; // Chu kỳ: "week", "month", "year"
    public long startDate; // Mốc thời gian bắt đầu (Timestamp)
    public long endDate; // Mốc thời gian kết thúc (Timestamp)
    public String workspaceId; // ID của quỹ (Cá nhân hoặc Nhóm)

    public Budget() {
    }
}