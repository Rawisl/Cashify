package com.example.cashify.database;

import androidx.room.Embedded;
import androidx.room.Relation;

public class TransactionWithCategory {
    @Embedded
    public Transaction transaction; // Chứa: id, amount, note, timestamp...

    @Relation(
            parentColumn = "categoryId", // Cột bên bảng Transaction
            entityColumn = "id"          // Cột bên bảng Category
    )
    public Category category; // Chứa nguyên cục: name, iconName, colorCode...
}