package com.example.cashify.data.local;

public class BudgetWithSpent {
    public int id;
    public int categoryId;

    // Dữ liệu được JOIN sang từ bảng Category
    public String categoryName;
    public String categoryIcon;
    public String categoryColor;

    // Dữ liệu gốc của bảng Budget
    public long limitAmount;
    public String periodType;
    public long startDate;
    public long endDate;

    // Dữ liệu được tính toán (SUM) trực tiếp từ bảng Transaction
    public long spentAmount;
}