package com.example.cashify.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

// implements Serializable để cho phép đóng gói toàn bộ Object và truyền qua Intent/Bundle (từ Activity sang BottomSheet)
@Entity(tableName = "categories")
public class Category implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String firestoreId; // ID Document trên Firestore (Dùng riêng cho quá trình đồng bộ Quỹ nhóm)

    public String name;
    public int type; // 0 = Expense (Chi), 1 = Income (Thu)
    public String iconName; // Tên resource icon
    public String colorCode; // Mã màu Hex hiển thị (VD: #FF5733)
    public int isDefault; // 1 = Mặc định hệ thống (Không cho phép xóa cứng), 0 = User tự tạo
    public int isDeleted; // 1 = Đã xóa mềm (Ẩn khỏi UI nhưng giữ lịch sử giao dịch), 0 = Đang hoạt động
    public String workspaceId;

    public Category() {
    }
}