package com.example.cashify.data.local;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.cashify.data.model.Budget;
import com.example.cashify.data.model.Category;

public class BudgetWithCategory {
    // Nhúng toàn bộ các trường của bảng Budget vào object này
    @Embedded
    public Budget budget;

    // Tự động map dữ liệu từ bảng Category dựa trên khóa ngoại categoryId
    @Relation(
            parentColumn = "categoryId",
            entityColumn = "id"
    )
    public Category category;
}