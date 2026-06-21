package com.example.cashify.ui.transactions;

public class FilterChip {

    public enum FilterType {
        DATE, TYPE, METHOD, CATEGORY
    }

    private String label;        // Default label (e.g., "Time")
    private String activeLabel;  // Label when active (e.g., "Today")
    private FilterType type;     // Type of filter
    private boolean isActive;    // Active state of the chip
    private int iconRes;         // Drawable resource ID (0 if not used)

    /**
     * Constructor for text-only chips (no custom icon).
     */
    public FilterChip(String label, FilterType type) {
        this.label = label;
        this.activeLabel = label;
        this.type = type;
        this.isActive = false;
        this.iconRes = 0; // 0 indicates no specific drawable
    }

    /**
     * Constructor for chips with a specific icon.
     */
    public FilterChip(String label, FilterType type, int iconRes) {
        this.label = label;
        this.activeLabel = label;
        this.type = type;
        this.isActive = false;
        this.iconRes = iconRes;
    }

    // =========================================================================
    // GETTERS & SETTERS
    // =========================================================================

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