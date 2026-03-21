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
    @Query("SELECT * FROM budgets WHERE startDate<= :now AND endDate >= :now")
    List<Budget> getActiveBudgets(long now);

    //lấy ngân sách của 1 danh mục cụ thể
    @Query("SELECT * FROM budgets WHERE categoryId = :category_id AND startDate <= :now AND endDate >= :now LIMIT 1")
    Budget getBudgetByCategory(int category_id, long now);

    //Lấy ngân sách tổng (category=-1)
    @Query("SELECT * FROM budgets WHERE categoryId = -1 AND startDate <= :now AND endDate >= :now LIMIT 1")
    Budget getMasterBudget(long now);

    //progress bar ngân sách
    @Query("SELECT b.*, IFNULL(SUM(t.amount), 0) as spentAmount " +
            "FROM budgets b " +
            "LEFT JOIN transactions t ON t.categoryId = b.categoryId " +
            "AND t.type = 0 AND t.timestamp BETWEEN b.startDate AND b.endDate " +
            "WHERE b.startDate <= :now AND b.endDate >= :now " +
            "GROUP BY b.id")
    List<BudgetWithSpent> getActiveBudgetsWithSpent(long now);
}