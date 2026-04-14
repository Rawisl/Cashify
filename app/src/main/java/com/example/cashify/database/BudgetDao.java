package com.example.cashify.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Transaction;

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
    @Query("SELECT * FROM budgets WHERE categoryId = :category_id AND startDate = :startTime AND endDate = :endTime AND periodType = :periodType LIMIT 1")
    Budget getBudgetByCategory(int category_id, long startTime, long endTime, String periodType);

    //Lấy ngân sách tổng (category=-1)
    @Query("SELECT * FROM budgets WHERE categoryId = -1 AND startDate = :startTime AND endDate = :endTime AND periodType = :periodType LIMIT 1")
    Budget getMasterBudget(long startTime, long endTime, String periodType);

    //progress bar ngân sách (Đã chỉnh sửa)
    @Query("SELECT b.*, c.name as categoryName, c.iconName as categoryIcon, c.colorCode as categoryColor, IFNULL(SUM(t.amount), 0) as spentAmount " +
            "FROM budgets b " +
            "LEFT JOIN categories c ON b.categoryId = c.id " +
            "LEFT JOIN transactions t ON t.categoryId = b.categoryId " +
            "AND t.type = 0 AND t.timestamp BETWEEN b.startDate AND b.endDate " +
            "WHERE b.startDate = :startTime AND b.endDate = :endTime AND b.periodType = :periodType " +
            "GROUP BY b.id")
    List<BudgetWithSpent> getActiveBudgetsWithSpent(long startTime, long endTime, String periodType);

    // Tính tổng tất cả khoản chi (type = 0) trong khoảng thời gian để dành riêng cho Master Budget (Mới them)
    @Query("SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE type = 0 AND timestamp BETWEEN :startDate AND :endDate")
    long getMasterSpentAmount(long startDate, long endDate);

    // Tính tổng hạn mức của tất cả danh mục KHÁC (loại trừ danh mục đang sửa) để kiểm tra xem có vượt Master Budget hay không
    @Query("SELECT IFNULL(SUM(limitAmount), 0) FROM budgets WHERE categoryId != -1 AND categoryId != :excludedId AND periodType = :periodType AND startDate = :startTime AND endDate = :endTime")
    long getTotalCategoryLimitExcluding(int excludedId, String periodType, long startTime, long endTime);

    // Lấy các danh mục CÓ chi tiêu nhưng CHƯA có ngân sách (Ngoài kế hoạch)
    @Query("SELECT c.id as categoryId, c.name as categoryName, c.iconName as categoryIcon, c.colorCode as categoryColor, SUM(t.amount) as spentAmount, 0 as limitAmount, " +
            "0 as id, :startDate as startDate, :endDate as endDate " +
            "FROM categories c " +
            "JOIN transactions t ON c.id = t.categoryId " +
            "WHERE t.type = 0 " +
            "AND t.timestamp BETWEEN :startDate AND :endDate " +
            "AND c.id NOT IN (SELECT categoryId FROM budgets WHERE startDate = :startDate AND endDate = :endDate AND periodType = :periodType) " +
            "GROUP BY c.id")
    List<BudgetWithSpent> getUnplannedExpenses(long startDate, long endDate, String periodType);

    // Tính tổng hạn mức của tất cả danh mục con (không bao gồm Master -1)
    @Query("SELECT IFNULL(SUM(limitAmount), 0) FROM budgets WHERE categoryId != -1 AND periodType = :periodType AND startDate = :startTime AND endDate = :endTime")
    long getTotalCategoryLimits(String periodType, long startTime, long endTime);

    //Lấy cái mốc tgian của cái giao dịch đầu tiên để chặn bộ lọc tgian
    @Query("SELECT MIN(startDate) FROM budgets WHERE periodType = :periodType ")
    long getEarliestStartDate(String periodType);

    //lấy cái mốc tgian của cái giao dịch cuối cùng để chặn lọc tgian
    @Query("SELECT MAX(endDate) FROM budgets WHERE periodType = :periodType")
    long getLatestEndDate(String periodType);

    //Lấy ngân sách theo tuần/tháng cụ thể
    @Query ("SELECT * FROM budgets WHERE periodType = :periodType AND startDate = :startDate LIMIT 1")
    Budget getBudgetByPeriod(String periodType, long startDate);

    @Transaction
    @Query("SELECT * FROM budgets WHERE periodType = :periodType")
    List<BudgetWithCategory> getAllBudgetsWithCategory(String periodType);

    // Gộp các category budget của tuần thành 1 tháng lớn
    @Query("SELECT b.categoryId as categoryId, c.name as categoryName, c.iconName as categoryIcon, c.colorCode as categoryColor, " +
            "SUM(b.limitAmount) as limitAmount, " +
            "(SELECT IFNULL(SUM(t.amount), 0) FROM transactions t WHERE t.categoryId = b.categoryId AND t.type = 0 AND t.timestamp BETWEEN :monthStart AND :monthEnd) as spentAmount, " +
            "0 as id, :monthStart as startDate, :monthEnd as endDate, 'MONTH_LINKED' as periodType " +
            "FROM budgets b " +
            "LEFT JOIN categories c ON b.categoryId = c.id " +
            "WHERE b.periodType = :weekPeriod AND b.categoryId != -1 AND b.startDate >= :monthStart AND b.startDate <= :monthEnd " +
            "GROUP BY b.categoryId")
    List<BudgetWithSpent> getLinkedMonthlyCategoryBudgets(long monthStart, long monthEnd, String weekPeriod);

    // Lấy tổng tất cả limitAmount của danh mục tuần trong 1 tháng (Để kiểm tra lúc set Master Tháng)
    @Query("SELECT IFNULL(SUM(limitAmount), 0) FROM budgets WHERE periodType = :weekPeriod AND categoryId != -1 AND startDate >= :monthStart AND startDate <= :monthEnd")
    long getSumLinkedWeeklyCategoryLimits(long monthStart, long monthEnd, String weekPeriod);

    // Lấy tổng tất cả limitAmount của Master tuần trong 1 tháng (Để kiểm tra lúc set Master Tuần không vượt Tháng)
    @Query("SELECT IFNULL(SUM(limitAmount), 0) FROM budgets WHERE periodType = :weekPeriod AND categoryId = -1 AND startDate >= :monthStart AND startDate <= :monthEnd AND startDate != :currentWeekStart")
    long getSumOtherWeeklyMasterLimits(long monthStart, long monthEnd, long currentWeekStart, String weekPeriod);

    //tẩy trắng time
    @Query("DELETE FROM budgets")
    void deleteAllBudgets();
}