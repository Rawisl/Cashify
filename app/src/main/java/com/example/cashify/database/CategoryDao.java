package com.example.cashify.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CategoryDao {
    @Insert
    void insert(Category c);

    @Update
    void update(Category c);

    //lọc danh mục theo loại 0: chi; 1: thu
    @Query("SELECT * FROM categories WHERE type = :type AND is_deleted=0")
    List<Category> getCategoriesByType(int type); //0=chi, 1=thu

    //truy vấn 1 danh mục cụ thể
    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    Category getCategoryById(int id);

    //Soft delete: đánh dấu, không xóa thật
    @Query("UPDATE categories SET is_deleted=1 WHERE id = :id")
    void softDelete(int id);

    //Hard delete: dùng khi chắc chắn danh mục chưa có giao dịch nào
    @Query("DELETE FROM categories WHERE id = :id")
    void hardDelete(int id);

    //kiểm tra xem danh mục có dag chứa giao dịch nào k trước khi xóa
    @Query("SELECT COUNT(*) FROM transactions WHERE category_id = :category_id")
    int countTransactionsByCategory(int category_id);

    //lấy tất cả danh mục đang su dụng
    @Query("SELECT * FROM categories WHERE is_deleted=0")
    List<Category>getAllActive();

}