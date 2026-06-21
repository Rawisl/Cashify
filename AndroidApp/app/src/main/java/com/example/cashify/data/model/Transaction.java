package com.example.cashify.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "transactions",
        foreignKeys = @ForeignKey(
                entity = Category.class,
                parentColumns = "id",
                childColumns = "categoryId",
                onDelete = ForeignKey.CASCADE // Xóa danh mục -> tự động xóa các giao dịch liên quan
        ),
        indices = {@Index("categoryId")} // Đánh index để tăng tốc độ truy vấn JOIN
)
public class Transaction {
    @PrimaryKey
    @NonNull
    public String id;

    public long amount;
    public int categoryId; // Khóa ngoại liên kết với bảng categories
    public String note;
    public long timestamp; // Lưu Timestamp (Long) để tối ưu tốc độ lọc/truy vấn thay vì String Date
    public String paymentMethod; // "Cash", "Bank", "Card"
    public int type; // 0 = Chi, 1 = Thu (Lưu dư thừa để tăng tốc query, tránh join bảng tốn tài nguyên)

    public String workspaceId; // NULL = Cá nhân, Có giá trị = Thuộc quỹ nhóm

    @Ignore
    // @Ignore để Room bỏ qua biến này khi tạo bảng SQLite (Dùng riêng cho việc map dữ liệu từ Firestore)
    public String userId;

    public String firestoreCategoryId;

    public Transaction() {
        // Kỹ thuật Offline-first: Luôn có ID ngẫu nhiên để an toàn lưu Local trước khi đồng bộ lên Server
        this.id = java.util.UUID.randomUUID().toString();
    }
}