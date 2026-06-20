package com.example.cashify.data.local;

// DTO (Data Transfer Object) dùng để hứng dữ liệu gộp (Aggregation) từ Room Database.
// Lớp này không phải là một Table (Entity), nó chỉ giúp ánh xạ kết quả truy vấn tính tổng (SUM) theo danh mục.
public class CategorySum {
    public int categoryId;
    public String categoryName;
    public long total; // Tổng số tiền (thu hoặc chi) của danh mục này
}