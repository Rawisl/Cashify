package com.example.cashify.data.model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.util.List;

/**
 * Model đại diện cho một không gian làm việc (Cá nhân hoặc Quỹ nhóm).
 */
public class Workspace {
    //cái ý tưởng là làm side bar bên trái ấy, xong mỗi section trong sidebar đó sẽ có đại loại như "Personal", "Quỹ A", "Quỹ B", nên là cần cái class này lưu id, tên, type (personal/group), owner ID (ID chủ quỹ,..), member (danh sách thành viên trong quỹ)

    @DocumentId
    private String id;           // Khóa chính (Firestore Document ID)
    private String name;         // Tên quỹ (VD: Quỹ ăn chơi, Tiền nhà...)
    private String type;         // "PERSONAL" hoặc "GROUP"
    private String ownerId;      // UID của người tạo (Chủ quỹ)
    private List<String> members; // Danh sách UID các thành viên được tham gia
    private double balance;      // Số dư hiện tại của quỹ này
    private double totalIncome;  // Tổng tiền đã thu vào quỹ
    private double totalExpense; // Tổng tiền quỹ đã chi ra
    private String iconName; // Thêm dòng này

    public Workspace() {
    }

    public Workspace(String name, String type, String ownerId) {
        this.name = name;
        this.type = type;
        this.ownerId = ownerId;
        this.balance = 0.0;
        this.totalIncome = 0.0;
        this.totalExpense = 0.0;
    }

    @Exclude
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
    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }
}
