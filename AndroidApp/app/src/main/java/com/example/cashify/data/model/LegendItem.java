package com.example.cashify.data.model;

// UI Model đại diện cho một mục chú giải trên biểu đồ (PieChart)
public class LegendItem {
    private int color;
    private String name;

    // Lưu ở dạng String vì tầng ViewModel đã chịu trách nhiệm format dữ liệu thô thành định dạng tiền tệ
    private String formattedAmount;

    public LegendItem(int color, String name, String formattedAmount) {
        this.color = color;
        this.name = name;
        this.formattedAmount = formattedAmount;
    }

    public int getColor() { return color; }
    public String getName() { return name; }
    public String getFormattedAmount() { return formattedAmount; }
}