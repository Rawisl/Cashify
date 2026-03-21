package com.example.cashify.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface BudgetDao {
    @Insert
    void insert(Budget budget);

    @Update
    void update(Budget budget);

    @Delete
    void delete(Budget budget);

    //lấy tất cả ngân sách hoạt động trong khoảng tgian
    @Query("SELECT * FROM budgets WHERE start_date<= :now AND end_date >= :now")
    List<Budget> getActiveBudgets(long now);

    //lấy ngân sách của 1 danh mục cụ thể
    @Query("SELECT * FROM budgets WHERE category_id = :category_id AND start_date <= :now AND end_date >= :now LIMIT 1")
    Budget getBudgetByCategory(int category_id, long now);

    //Lấy ngân sách tổng (category=-1)
    @Query("SELECT * FROM budgets WHERE category_id = -1 AND start_date <= :now AND end_date >= :now LIMIT 1")
    Budget getMasterBudget(long now);
}