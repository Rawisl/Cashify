package com.example.cashify.data.local;

import androidx.room.Embedded;
import androidx.room.Relation;

import com.example.cashify.data.model.Category;
import com.example.cashify.data.model.Transaction;

public class TransactionWithCategory {
    @Embedded
    public Transaction transaction; // Chứa: id, amount, note, timestamp...

    @Relation(
            parentColumn = "categoryId", // Cột bên bảng Transaction
            entityColumn = "id"          // Cột bên bảng Category
    )
    public Category category; // Chứa nguyên cục: name, iconName, colorCode...
}