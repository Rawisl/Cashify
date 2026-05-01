package com.example.cashify.data.model;

import java.util.List;

/**
 * Model đại diện cho một không gian làm việc (Cá nhân hoặc Quỹ nhóm).
 */
public class Workspace {
    //cái ý tưởng là làm side bar bên trái ấy, xong mỗi section trong sidebar đó sẽ có đại loại như "Personal", "Quỹ A", "Quỹ B", nên là cần cái class này lưu id, tên, type (personal/group), owner ID (ID chủ quỹ,..), member (danh sách thành viên trong quỹ)

    private String id;           // Khóa chính (Firestore Document ID)
    private String name;         // Tên quỹ (VD: Quỹ ăn chơi, Tiền nhà...)
    private String type;         // "PERSONAL" hoặc "GROUP"
    private String ownerId;      // UID của người tạo (Chủ quỹ)
    private List<String> members; // Danh sách UID các thành viên được tham gia
    private double balance;      // Số dư hiện tại của quỹ này
    private double totalIncome;  // Tổng tiền đã thu vào quỹ
    private double totalExpense; // Tổng tiền quỹ đã chi ra

    // ============================================================
    // TODO 1: CONSTRUCTOR KHÔNG THAM SỐ
    // - Bắt buộc để Firestore map dữ liệu tự động.
    // ============================================================
    public Workspace() {
    }

    // ============================================================
    // TODO 2: CONSTRUCTOR KHI TẠO QUỸ MỚI
    // - Khi tạo quỹ GROUP: ownerId là người tạo, members chứa ownerId ban đầu.
    // - Khi tạo quỹ PERSONAL: name có thể là "Cá nhân", type là "PERSONAL".
    // ============================================================
    public Workspace(String id, String name, String type, String ownerId) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.ownerId = ownerId;
        this.balance = 0.0;
        this.totalIncome = 0.0;
        this.totalExpense = 0.0;
    }

    // ============================================================
    // TODO 3: GETTERS & SETTERS
    // - Generate đầy đủ để các ViewModel có thể truy xuất.
    // ============================================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(double totalIncome) {
        this.totalIncome = totalIncome;
    }

    public double getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(double totalExpense) {
        this.totalExpense = totalExpense;
    }
}
