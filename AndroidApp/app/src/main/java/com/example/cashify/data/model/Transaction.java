package com.example.cashify.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions",
        foreignKeys = @ForeignKey(entity = Category.class,
                parentColumns = "id",
                childColumns = "categoryId",
                onDelete = ForeignKey.CASCADE),// Xóa danh mục thì xóa luôn giao dịch liên quan
                indices = {@Index("categoryId")}
)

public class Transaction {
    @PrimaryKey @NonNull
    public String id;
    public long amount;
    public int categoryId; // Liên kết với bảng Danh mục
    public String note;
    public long timestamp; // Lưu ngày tháng bằng số Long để tính toán/lọc cho nhanh
    public String paymentMethod; //"cash", "bank", "card"
    public int type; // 0=chi, 1=thu (lưu lại để truy vấn nhanh, khỏi join)

    public String workspaceId; // Nếu null là Cá nhân, có chữ là Quỹ
    @Ignore //@Ignore để Room DB bỏ qua biến này, không bị lỗi Schema
    public String userId;      // UID của người tạo giao dịch
    public String firestoreCategoryId;
    public Transaction() {
        // Luôn tạo ID ngẫu nhiên khi khởi tạo để không bao giờ bị null
        this.id = java.util.UUID.randomUUID().toString();
    }
}

