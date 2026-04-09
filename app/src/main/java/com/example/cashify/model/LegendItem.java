package com.example.cashify.model;

public class LegendItem {
    private int color;
    private String name;
    private String formattedAmount; // Để chuỗi String luôn vì ViewModel đã format sẵn tiền

    public LegendItem(int color, String name, String formattedAmount) {
        this.color = color;
        this.name = name;
        this.formattedAmount = formattedAmount;
    }

    public int getColor() { return color; }
    public String getName() { return name; }
    public String getFormattedAmount() { return formattedAmount; }
}
