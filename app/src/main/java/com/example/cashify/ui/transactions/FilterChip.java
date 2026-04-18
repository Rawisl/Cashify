package com.example.cashify.ui.transactions;

public class FilterChip {

    public enum FilterType {
        DATE, TYPE, METHOD, CATEGORY
    }

    private String label;        // Nhãn mặc định (vd: "Thời gian")
    private String activeLabel;  // Nhãn khi đã chọn (vd: "Hôm nay")
    private FilterType type;     // Loại filter
    private boolean isActive;    // Trạng thái chip có đang được filter hay không
    private int iconRes;         // ID ảnh Icon từ drawable (0 nếu không dùng)

    // 1. Constructor 2 tham số (Dùng cho initDefaultChips không truyền ảnh)
    public FilterChip(String label, FilterType type) {
        this.label = label;
        this.activeLabel = label;
        this.type = type;
        this.isActive = false;
        this.iconRes = 0; // 0 nghĩa là mặc định không dùng ảnh drawable
    }

    // 2. Constructor 3 tham số (Dùng khi bạn muốn gán sẵn 1 icon cụ thể)
    public FilterChip(String label, FilterType type, int iconRes) {
        this.label = label;
        this.activeLabel = label;
        this.type = type;
        this.isActive = false;
        this.iconRes = iconRes;
    }

    // --- GETTERS VÀ SETTERS ---

    public String getFilLabel() {
        return label;
    }

    public void setFilLabel(String label) {
        this.label = label;
    }

    public String getActiveLabel() {
        return activeLabel;
    }

    public void setActiveLabel(String activeLabel) {
        this.activeLabel = activeLabel;
    }

    public FilterType getType() {
        return type;
    }

    public void setType(FilterType type) {
        this.type = type;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public int getIconRes() {
        return iconRes;
    }

    public void setIconRes(int iconRes) {
        this.iconRes = iconRes;
    }
}