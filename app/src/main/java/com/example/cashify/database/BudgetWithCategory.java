package com.example.cashify.database;

import androidx.room.Embedded;
import androidx.room.Relation;

public class BudgetWithCategory {
    @Embedded
    public Budget budget; // Chứa: id, limitAmount, categoryId, periodType...

    @Relation(
            parentColumn = "categoryId",
            entityColumn = "id"
    )
    public Category category; // Chứa: name, iconName, colorCode (ĐÂY LÀ CHỖ LẤY MÀU!)
}