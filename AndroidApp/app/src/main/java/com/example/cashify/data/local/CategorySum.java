package com.example.cashify.data.local;

// Class này không phải Entity, chỉ để Room ánh xạ kết quả query Top 5
public class CategorySum {
    public int categoryId;
    public String categoryName;
    public long total;
}