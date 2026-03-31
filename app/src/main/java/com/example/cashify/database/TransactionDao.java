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

    //Lấy tất cả giao dịch, mới nhất nằm trên
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    List<Transaction> getAll();

    // Lấy toàn bộ giao dịch trong một khoảng thời gian (dùng cho lọc tuần/tháng/năm)
    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    List<Transaction> getByDateRange(long start, long end);

    //Tổng thu trong 1 khoảng thời gian
    @Query("SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE type=1 AND timestamp BETWEEN :start AND :end")
    long getTotalIncome(long start, long end);

    //Tổng chi trong 1 khoảng tgian
    @Query("SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE type=0 AND timestamp BETWEEN :start AND :end")
    long getTotalExpense(long start, long end);

    //Top 5 danh mục chi nhiều nhất (pie chart)
    @Query("SELECT t.categoryId, c.name as categoryName, SUM(t.amount) as total " +
            "FROM transactions t INNER JOIN categories c ON t.categoryId = c.id " +
            "WHERE t.type=0 AND t.timestamp BETWEEN :start AND :end " +
            "GROUP BY t.categoryId ORDER BY total DESC LIMIT 5")
    List<CategorySum> getTop5ExpenseCategories(long start, long end);

    //Phần 'khác' trong pie chart: tổng tất cả ngoài top 5
    @Query("SELECT IFNULL(SUM(amount), 0) FROM transactions "+"WHERE type=0 AND timestamp BETWEEN :start AND :end "+"AND categoryId NOT IN("+" SELECT categoryId FROM transactions WHERE type=0 AND timestamp BETWEEN :start AND :end "+" GROUP BY categoryId ORDER BY SUM(amount) DESC LIMIT 5"+")")
    long getOtherExpenseTotal(long start, long end);

    //Tổng chi theo 1 danh mục cụ thể trong 1 tgian cu the
    @Query("SELECT IFNULL(SUM(amount),0) FROM transactions "+"WHERE type = 0 AND categoryId = :category_id AND timestamp BETWEEN :start AND :end")
    long getTotalExpenseByCategory(int category_id, long start, long end);

    //N giao dịch gần nhất
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    List<Transaction> getRecentTransaction(int limit);


    //tính sô dư
    @Query("SELECT (SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE type = 1) - " + "(SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE type = 0)")
    long getActualBalance();

    //đếm số lượng giao dịch trong 1 khoang thời gian
    @Query("SELECT COUNT(*) FROM transactions WHERE timestamp BETWEEN :startOfDay AND :endOfDay")
    int countTransactionsByDay(long startOfDay, long endOfDay);
}