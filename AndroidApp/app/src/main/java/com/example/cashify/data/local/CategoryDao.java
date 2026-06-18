package com.example.cashify.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cashify.data.model.Category;

import java.util.List;

@Dao
public interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Category c);

    @Update
    void update(Category c);

    // Lọc danh mục đang hoạt động theo loại (0 = Expense, 1 = Income)
    @Query("SELECT * FROM categories WHERE type = :type AND isDeleted = 0")
    List<Category> getCategoriesByType(int type);

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    Category getCategoryById(int id);

    // Xóa mềm: Đánh dấu ẩn để không hiển thị lên UI, nhưng giữ lại để không làm lỗi các giao dịch lịch sử đã dùng danh mục này
    @Query("UPDATE categories SET isDeleted = 1 WHERE id = :id")
    void softDelete(int id);

    // Xóa cứng: Xóa vĩnh viễn khỏi DB, CHỈ DÙNG khi danh mục chưa từng phát sinh giao dịch nào
    @Query("DELETE FROM categories WHERE id = :id")
    void hardDelete(int id);

    // Kiểm tra ràng buộc dữ liệu: Đếm số giao dịch đang dùng danh mục này để quyết định cho phép xóa mềm hay xóa cứng
    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    int countTransactionsByCategory(int categoryId);

    @Query("SELECT * FROM categories WHERE isDeleted = 0")
    List<Category> getAllActive();

    @Query("DELETE FROM categories")
    void deleteAllCategories();

    @Query("SELECT * FROM categories")
    List<Category> getAll();
}