package com.example.cashify.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.example.cashify.data.model.Transaction;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    // Lấy tất cả giao dịch của một quỹ, sắp xếp mới nhất lên đầu
    @Query("SELECT * FROM transactions WHERE workspaceId = :workspaceId ORDER BY timestamp DESC")
    List<Transaction> getAll(String workspaceId);

    // Lấy toàn bộ giao dịch trong một khoảng thời gian (Dùng cho bộ lọc tuần/tháng/năm)
    @Query("SELECT * FROM transactions WHERE workspaceId = :workspaceId AND timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    List<Transaction> getByDateRange(String workspaceId, long start, long end);

    // Tính tổng thu (type = 1) trong một khoảng thời gian
    @Query("SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE workspaceId = :workspaceId AND type = 1 AND timestamp BETWEEN :start AND :end")
    long getTotalIncome(String workspaceId, long start, long end);

    // Tính tổng chi (type = 0) trong một khoảng thời gian
    @Query("SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE workspaceId = :workspaceId AND type = 0 AND timestamp BETWEEN :start AND :end")
    long getTotalExpense(String workspaceId, long start, long end);

    // Lấy Top 5 danh mục có tổng chi tiêu cao nhất (Dùng cho biểu đồ Pie Chart)
    @Query("SELECT t.categoryId, c.name as categoryName, SUM(t.amount) as total " +
            "FROM transactions t INNER JOIN categories c ON t.categoryId = c.id " +
            "WHERE t.workspaceId = :workspaceId AND t.type = 0 AND t.timestamp BETWEEN :start AND :end " +
            "GROUP BY t.categoryId ORDER BY total DESC LIMIT 5")
    List<CategorySum> getTop5ExpenseCategories(String workspaceId, long start, long end);

    // Lấy tổng chi tiêu của phần "Khác" (Tất cả các danh mục nằm ngoài Top 5)
    @Query("SELECT IFNULL(SUM(amount), 0) FROM transactions " +
            "WHERE workspaceId = :workspaceId AND type = 0 AND timestamp BETWEEN :start AND :end " +
            "AND categoryId NOT IN (" +
            "   SELECT categoryId FROM transactions WHERE workspaceId = :workspaceId AND type = 0 AND timestamp BETWEEN :start AND :end " +
            "   GROUP BY categoryId ORDER BY SUM(amount) DESC LIMIT 5" +
            ")")
    long getOtherExpenseTotal(String workspaceId, long start, long end);

    // Lấy chi tiết các danh mục nằm ngoài Top 5 (Dùng khi click vào phần "Khác" trên biểu đồ)
    // Dùng OFFSET 5 để bỏ qua 5 dòng đầu tiên
    @Query("SELECT t.categoryId, c.name as categoryName, SUM(t.amount) as total " +
            "FROM transactions t INNER JOIN categories c ON t.categoryId = c.id " +
            "WHERE t.workspaceId = :workspaceId AND t.type = 0 AND t.timestamp BETWEEN :start AND :end " +
            "GROUP BY t.categoryId ORDER BY total DESC LIMIT -1 OFFSET 5")
    List<CategorySum> getOthersBreakdown(String workspaceId, long start, long end);

    // Tính tổng chi tiêu theo một danh mục cụ thể
    @Query("SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE workspaceId = :workspaceId AND type = 0 AND categoryId = :category_id AND timestamp BETWEEN :start AND :end")
    long getTotalExpenseByCategory(String workspaceId, int category_id, long start, long end);

    // Lấy N giao dịch gần nhất
    @Query("SELECT * FROM transactions WHERE workspaceId = :workspaceId ORDER BY timestamp DESC LIMIT :limit")
    List<Transaction> getRecentTransaction(String workspaceId, int limit);

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    Transaction getById(String id);

    // Tính số dư trọn đời (Tổng Thu - Tổng Chi) của một quỹ
    @Query("SELECT " +
            "(SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE workspaceId = :workspaceId AND type = 1) - " +
            "(SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE workspaceId = :workspaceId AND type = 0)")
    long getActualBalance(String workspaceId);

    // Tính số dư theo khoảng thời gian (Ví dụ: Số dư trong tháng)
    @Query("SELECT " +
            "(SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE workspaceId = :workspaceId AND type = 1 AND timestamp BETWEEN :startDate AND :endDate) - " +
            "(SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE workspaceId = :workspaceId AND type = 0 AND timestamp BETWEEN :startDate AND :endDate)")
    long getMonthlyBalance(String workspaceId, long startDate, long endDate);

    // Đếm số lượng giao dịch phát sinh trong 1 ngày (Dùng để check điều kiện Gamification/Streak)
    @Query("SELECT COUNT(*) FROM transactions WHERE workspaceId = :workspaceId AND timestamp BETWEEN :startOfDay AND :endOfDay")
    int countTransactionsByDay(String workspaceId, long startOfDay, long endOfDay);

    // Lấy thời điểm của giao dịch cũ nhất (Dùng để chặn giới hạn lướt bộ lọc thời gian)
    @Query("SELECT MIN(timestamp) FROM transactions WHERE workspaceId = :workspaceId")
    long getEarliestTransactionDate(String workspaceId);

    // Lấy danh sách giao dịch lọc theo loại (0: Chi, 1: Thu)
    @Query("SELECT * FROM transactions WHERE workspaceId = :workspaceId AND type = :type ORDER BY timestamp DESC")
    List<Transaction> getTransactionsByType(String workspaceId, int type);

    // Lấy danh sách giao dịch lọc theo loại và theo thời gian
    @Query("SELECT * FROM transactions WHERE workspaceId = :workspaceId AND type = :type AND timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    List<Transaction> getTransactionsByTypeAndDate(String workspaceId, int type, long start, long end);

    @Query("DELETE FROM transactions WHERE workspaceId = :workspaceId")
    void deleteAllTransactions(String workspaceId);

    // Đếm tổng số giao dịch của một quỹ cụ thể
    @Query("SELECT COUNT(*) FROM transactions WHERE workspaceId = :workspaceId")
    int countTransactions(String workspaceId);

    // Lấy mốc thời gian của toàn bộ giao dịch (Dùng để logic UI gom nhóm theo tháng/năm)
    @Query("SELECT timestamp FROM transactions WHERE workspaceId = :workspaceId ORDER BY timestamp DESC")
    List<Long> getAllTimestamps(String workspaceId);

    // Bắt buộc dùng @Transaction vì có kết nối @Relation để tự động map Category vào Transaction
    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE workspaceId = :workspaceId ORDER BY timestamp DESC")
    List<TransactionWithCategory> getAllTransactionsWithCategory(String workspaceId);

    // Quan sát LiveData 5 giao dịch gần nhất (Tự động cập nhật UI khi có giao dịch mới)
    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE workspaceId = :workspaceId ORDER BY timestamp DESC LIMIT 5")
    LiveData<List<TransactionWithCategory>> getRecentTransactionsWithCategory(String workspaceId);

    @Query("SELECT * FROM transactions WHERE workspaceId = :workspaceId AND paymentMethod = :method ORDER BY timestamp DESC")
    List<Transaction> getTransactionsByPaymentMethod(String workspaceId, String method);

    @Query("SELECT * FROM transactions WHERE workspaceId = :workspaceId AND paymentMethod = :method AND timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    List<Transaction> getTransactionsByPaymentMethodAndDate(String workspaceId, String method, long start, long end);

    // --- LỌC ĐỘNG KẾT HỢP NHIỀU ĐIỀU KIỆN ---
    // Nhận câu lệnh SQL động được build từ tầng Repository (Ví dụ: kết hợp khoảng thời gian + loại + phương thức)
    @RawQuery
    List<Transaction> getFilteredTransactions(SupportSQLiteQuery query);
}