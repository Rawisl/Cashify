package com.example.cashify.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.cashify.data.model.Workspace;

import java.util.List;

@Dao
public interface WorkspaceDao {

    // Ghi đè hoặc chèn mới thông tin Quỹ nhóm từ mây xuống bộ nhớ máy
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Workspace workspace);

    // Chèn danh sách nhiều Quỹ cùng một lúc khi đồng bộ hàng loạt
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Workspace> workspaces);

    @Update
    void update(Workspace workspace);

    @Delete
    void delete(Workspace workspace);

    // Truy vấn một Quỹ cụ thể bằng ID (Hỗ trợ nạp nhanh data khi mất mạng)
    @Query("SELECT * FROM workspaces WHERE id = :id LIMIT 1")
    Workspace getWorkspaceById(String id);

    // Lấy toàn bộ danh sách Quỹ để vẽ lên Sidebar của ứng dụng
    @Query("SELECT * FROM workspaces")
    List<Workspace> getAllWorkspaces();

    // Xóa trắng dữ liệu bộ nhớ đệm Quỹ (Dùng khi user Đăng xuất tài khoản)
    @Query("DELETE FROM workspaces")
    void deleteAllWorkspaces();
}