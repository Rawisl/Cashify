package com.example.cashify.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Transaction;

import com.example.cashify.data.model.Budget;

import java.util.List;

@Dao
public interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Budget budget);

    @Update
    void update(Budget budget);

    @Delete
    void delete(Budget budget);

    // Lấy danh sách ngân sách đang hoạt động trong thời điểm hiện tại
    @Query("SELECT * FROM budgets WHERE startDate <= :now AND endDate >= :now")
    List<Budget> getActiveBudgets(long now);

    // Lấy ngân sách của một danh mục cụ thể theo khoảng thời gian
    @Query("SELECT * FROM budgets WHERE categoryId = :category_id AND startDate = :startTime AND endDate = :endTime AND periodType = :periodType LIMIT 1")
    Budget getBudgetByCategory(int category_id, long startTime, long endTime, String periodType);

    // Lấy ngân sách tổng (Master Budget: categoryId = -1)
    @Query("SELECT * FROM budgets WHERE categoryId = -1 AND startDate = :startTime AND endDate = :endTime AND periodType = :periodType LIMIT 1")
    Budget getMasterBudget(long startTime, long endTime, String periodType);

    // Lấy dữ liệu cho Progress Bar: Kết hợp ngân sách, danh mục và tổng chi tiêu thực tế (type = 0)
    @Query("SELECT b.*, c.name as categoryName, c.iconName as categoryIcon, c.colorCode as categoryColor, IFNULL(SUM(t.amount), 0) as spentAmount " +
            "FROM budgets b " +
            "LEFT JOIN categories c ON b.categoryId = c.id " +
            "LEFT JOIN transactions t ON t.categoryId = b.categoryId " +
            "AND t.type = 0 AND t.timestamp BETWEEN b.startDate AND b.endDate " +
            "WHERE b.startDate = :startTime AND b.endDate = :endTime AND b.periodType = :periodType " +
            "GROUP BY b.id")
    List<BudgetWithSpent> getActiveBudgetsWithSpent(long startTime, long endTime, String periodType);

    // Tính tổng chi tiêu (type = 0) trong khoảng thời gian (Dùng để kiểm tra Master Budget)
    @Query("SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE type = 0 AND timestamp BETWEEN :startDate AND :endDate")
    long getMasterSpentAmount(long startDate, long endDate);

    // Tính tổng hạn mức của các danh mục KHÁC (trừ danh mục đang sửa và Master) để validate giới hạn Master Budget
    @Query("SELECT IFNULL(SUM(limitAmount), 0) FROM budgets WHERE categoryId != -1 AND categoryId != :excludedId AND periodType = :periodType AND startDate = :startTime AND endDate = :endTime")
    long getTotalCategoryLimitExcluding(int excludedId, String periodType, long startTime, long endTime);

    // Truy vấn các khoản chi phát sinh ngoài kế hoạch (Có giao dịch nhưng chưa lập ngân sách)
    @Query("SELECT c.id as categoryId, c.name as categoryName, c.iconName as categoryIcon, c.colorCode as categoryColor, SUM(t.amount) as spentAmount, 0 as limitAmount, " +
            "0 as id, :startDate as startDate, :endDate as endDate " +
            "FROM categories c " +
            "JOIN transactions t ON c.id = t.categoryId " +
            "WHERE t.type = 0 " +
            "AND t.timestamp BETWEEN :startDate AND :endDate " +
            "AND c.id NOT IN (SELECT categoryId FROM budgets WHERE startDate = :startDate AND endDate = :endDate AND periodType = :periodType) " +
            "GROUP BY c.id")
    List<BudgetWithSpent> getUnplannedExpenses(long startDate, long endDate, String periodType);

    // Tổng hạn mức của tất cả danh mục con (Trừ Master -1)
    @Query("SELECT IFNULL(SUM(limitAmount), 0) FROM budgets WHERE categoryId != -1 AND periodType = :periodType AND startDate = :startTime AND endDate = :endTime")
    long getTotalCategoryLimits(String periodType, long startTime, long endTime);

    // Lấy mốc thời gian bắt đầu sớm nhất để chặn thao tác lướt bộ lọc thời gian
    @Query("SELECT MIN(startDate) FROM budgets WHERE periodType = :periodType")
    long getEarliestStartDate(String periodType);

    // Lấy mốc thời gian kết thúc muộn nhất để chặn thao tác lướt bộ lọc thời gian
    @Query("SELECT MAX(endDate) FROM budgets WHERE periodType = :periodType")
    long getLatestEndDate(String periodType);

    // Lấy ngân sách theo chu kỳ cụ thể
    @Query("SELECT * FROM budgets WHERE periodType = :periodType AND startDate = :startDate LIMIT 1")
    Budget getBudgetByPeriod(String periodType, long startDate);

    @Transaction
    @Query("SELECT * FROM budgets WHERE periodType = :periodType")
    List<BudgetWithCategory> getAllBudgetsWithCategory(String periodType);

    // Gộp các ngân sách danh mục của Tuần thành 1 Tháng lớn
    @Query("SELECT b.categoryId as categoryId, c.name as categoryName, c.iconName as categoryIcon, c.colorCode as categoryColor, " +
            "SUM(b.limitAmount) as limitAmount, " +
            "(SELECT IFNULL(SUM(t.amount), 0) FROM transactions t WHERE t.categoryId = b.categoryId AND t.type = 0 AND t.timestamp BETWEEN :monthStart AND :monthEnd) as spentAmount, " +
            "0 as id, :monthStart as startDate, :monthEnd as endDate, 'MONTH_LINKED' as periodType " +
            "FROM budgets b " +
            "LEFT JOIN categories c ON b.categoryId = c.id " +
            "WHERE b.periodType = :weekPeriod AND b.categoryId != -1 AND b.startDate >= :monthStart AND b.startDate <= :monthEnd " +
            "GROUP BY b.categoryId")
    List<BudgetWithSpent> getLinkedMonthlyCategoryBudgets(long monthStart, long monthEnd, String weekPeriod);

    // Lấy tổng hạn mức các danh mục Tuần trong 1 Tháng (Để validate lúc set Master Tháng)
    @Query("SELECT IFNULL(SUM(limitAmount), 0) FROM budgets WHERE periodType = :weekPeriod AND categoryId != -1 AND startDate >= :monthStart AND startDate <= :monthEnd")
    long getSumLinkedWeeklyCategoryLimits(long monthStart, long monthEnd, String weekPeriod);

    // Lấy tổng hạn mức Master Tuần trong 1 Tháng (Để validate Master Tuần không vượt quá Tháng)
    @Query("SELECT IFNULL(SUM(limitAmount), 0) FROM budgets WHERE periodType = :weekPeriod AND categoryId = -1 AND startDate >= :monthStart AND startDate <= :monthEnd AND startDate != :currentWeekStart")
    long getSumOtherWeeklyMasterLimits(long monthStart, long monthEnd, long currentWeekStart, String weekPeriod);

    // Xóa toàn bộ ngân sách
    @Query("DELETE FROM budgets")
    void deleteAllBudgets();
}