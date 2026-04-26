package com.example.cashify.data.local;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.cashify.data.model.Budget;
import com.example.cashify.data.model.Category;

public class BudgetWithCategory {
    @Embedded
    public Budget budget; // Chứa: id, limitAmount, categoryId, periodType...

    @Relation(
            parentColumn = "categoryId",
            entityColumn = "id"
    )
    public Category category; // Chứa: name, iconName, colorCode (ĐÂY LÀ CHỖ LẤY MÀU!)
}