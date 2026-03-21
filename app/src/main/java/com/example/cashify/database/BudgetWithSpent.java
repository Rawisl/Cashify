package com.example.cashify.database;

public class BudgetWithSpent {
    public int id;
    public int categoryId;
    public long limitAmount;
    public String periodType;
    public long startDate;
    public long endDate;
    public long spentAmount; // Tổng đã tiêu thực tế
}