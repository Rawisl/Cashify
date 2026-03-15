package com.example.cashify.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface CategoryDao {
    @Insert
    void insert(Category category);

    @Query("SELECT * FROM categories WHERE type = :type")
    List<Category> getCategoriesByType(int type); // 0 cho Chi, 1 cho Thu

    @Delete
    void delete(Category category);
}