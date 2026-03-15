package com.example.cashify.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    void insert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    // Lấy toàn bộ giao dịch trong một khoảng thời gian (dùng cho Lịch sử & Biểu đồ)
    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    List<Transaction> getTransactionsBetweenDates(long start, long end);

    // Tính tổng chi tiêu theo loại (để vẽ thanh Progress Bar của Ngân sách)
    @Query("SELECT SUM(amount) FROM transactions WHERE categoryId = :catId AND timestamp BETWEEN :start AND :end")
    double getTotalSpentByCategory(int catId, long start, long end);
}