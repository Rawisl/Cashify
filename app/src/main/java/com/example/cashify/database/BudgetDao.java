package com.example.cashify.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BudgetDao {
    @Insert
    void insert(Budget budget);

    @Update
    void update(Budget budget);

    @Query("SELECT * FROM budgets WHERE month = :m AND year = :y")
    List<Budget> getBudgetsByMonth(int m, int y);

    @Query("SELECT * FROM budgets WHERE categoryId = :catId AND month = :m AND year = :y LIMIT 1")
    Budget getBudgetByCategory(int catId, int m, int y);
}