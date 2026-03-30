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

    //progress bar ngân sách (Đã chỉnh sửa)
    @Query("SELECT b.*, c.name as categoryName, IFNULL(SUM(t.amount), 0) as spentAmount " +
            "FROM budgets b " +
            "LEFT JOIN categories c ON b.categoryId = c.id " +
            "LEFT JOIN transactions t ON t.categoryId = b.categoryId " +
            "AND t.type = 0 AND t.timestamp BETWEEN b.startDate AND b.endDate " +
            "WHERE b.startDate <= :now AND b.endDate >= :now " +
            "GROUP BY b.id")
    List<BudgetWithSpent> getActiveBudgetsWithSpent(long now);

    // Tính tổng tất cả khoản chi (type = 0) trong khoảng thời gian để dành riêng cho Master Budget (Mới them)
    @Query("SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE type = 0 AND timestamp BETWEEN :startDate AND :endDate")
    long getMasterSpentAmount(long startDate, long endDate);

    // Tính tổng hạn mức của tất cả danh mục KHÁC (loại trừ danh mục đang sửa) để kiểm tra xem có vượt Master Budget hay không
    @Query("SELECT SUM(limitAmount) FROM budgets WHERE categoryId != -1 AND categoryId != :excludedId AND periodType = :periodType")
    long getTotalCategoryLimitExcluding(int excludedId, String periodType);

    // Lấy các danh mục CÓ chi tiêu nhưng CHƯA có ngân sách (Ngoài kế hoạch)
    @Query("SELECT c.id as categoryId, c.name as categoryName, SUM(t.amount) as spentAmount, 0 as limitAmount, " +
            "0 as id, :startDate as startDate, :endDate as endDate " +
            "FROM categories c " +
            "JOIN transactions t ON c.id = t.categoryId " +
            "WHERE t.type = 0 " + // Chỉ lấy loại Chi (Expense)
            "AND t.timestamp BETWEEN :startDate AND :endDate " +
            "AND c.id NOT IN (SELECT categoryId FROM budgets WHERE startDate <= :now AND endDate >= :now AND periodType = :periodType) " +
            "GROUP BY c.id")
    List<BudgetWithSpent> getUnplannedExpenses(long startDate, long endDate, long now, String periodType);

    // Tính tổng hạn mức của tất cả danh mục con (không bao gồm Master -1)
    @Query("SELECT IFNULL(SUM(limitAmount), 0) FROM budgets WHERE categoryId != -1 AND periodType = :periodType")
    long getTotalCategoryLimits(String periodType);
}