package com.example.cashify.data.local;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteQuery;

import com.example.cashify.data.model.Transaction;

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

    // Lấy chi tiết các mục nằm ngoài Top 5 (Dùng cho Popup xem mục khác)
    @Query("SELECT t.categoryId, c.name as categoryName, SUM(t.amount) as total " +
            "FROM transactions t INNER JOIN categories c ON t.categoryId = c.id " +
            "WHERE t.type=0 AND t.timestamp BETWEEN :start AND :end " +
            "GROUP BY t.categoryId ORDER BY total DESC LIMIT -1 OFFSET 5")
    List<CategorySum> getOthersBreakdown(long start, long end);

    //Tổng chi theo 1 danh mục cụ thể trong 1 tgian cu the
    @Query("SELECT IFNULL(SUM(amount),0) FROM transactions "+"WHERE type = 0 AND categoryId = :category_id AND timestamp BETWEEN :start AND :end")
    long getTotalExpenseByCategory(int category_id, long start, long end);

    //N giao dịch gần nhất
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    List<Transaction> getRecentTransaction(int limit);

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    Transaction getById(int id);
    //tính sô dư trọn đời
    @Query("SELECT (SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE type = 1) - " + "(SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE type = 0)")
    long getActualBalance();

    //tính số dư trong tháng
    @Query("SELECT (SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE type = 1 AND timestamp BETWEEN :startDate AND :endDate) - " +
                  "(SELECT IFNULL(SUM(amount), 0) FROM transactions WHERE type = 0 AND timestamp BETWEEN :startDate AND :endDate)")
    long getMonthlyBalance(long startDate, long endDate);

    //đếm số lượng giao dịch trong 1 khoang thời gian
    @Query("SELECT COUNT(*) FROM transactions WHERE timestamp BETWEEN :startOfDay AND :endOfDay")
    int countTransactionsByDay(long startOfDay, long endOfDay);

    // Lấy tháng đầu tiên có giao dịch
    @Query("SELECT MIN(timestamp) FROM transactions")
    long getEarliestTransactionDate();

    // Lấy giao dịch theo loại (0: Chi, 1: Thu), sắp xếp mới nhất lên đầu
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    List<Transaction> getTransactionsByType(int type);

    // (Tùy chọn) Nếu sau này bạn muốn lọc Income/Expense trong một khoảng thời gian cụ thể
    @Query("SELECT * FROM transactions WHERE type = :type AND timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    List<Transaction> getTransactionsByTypeAndDate(int type, long start, long end);



    @Query("DELETE FROM transactions")
    void deleteAllTransactions();
    @Query("SELECT COUNT(*) FROM transactions")
    int countTransactions();

    // Lấy toàn bộ mốc thời gian của tất cả giao dịch (Dùng để gom nhóm tháng)
    @Query("SELECT timestamp FROM transactions ORDER BY timestamp DESC")
    List<Long> getAllTimestamps();

    @androidx.room.Transaction // Bắt buộc phải có để Room chạy liên kết dữ liệu
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    List<TransactionWithCategory> getAllTransactionsWithCategory();

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 5")
    LiveData<List<TransactionWithCategory>> getRecentTransactionsWithCategory();

    //lọc giao dịch theo phương thức:
    @Query("SELECT * FROM transactions WHERE paymentMethod = :method ORDER BY timestamp DESC")
    List<Transaction> getTransactionsByPaymentMethod(String method);

    //lọc giao dịch theo phương thức và theo ngày
    @Query("SELECT * FROM transactions WHERE paymentMethod = :method AND timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    List<Transaction> getTransactionsByPaymentMethodAndDate(String method, long start, long end);

    // --- LỌC ĐỘNG KẾT HỢP NHIỀU ĐIỀU KIỆN (THAY THẾ CÁC HÀM LỌC CŨ) ---
    @RawQuery
    List<Transaction> getFilteredTransactions(SupportSQLiteQuery query);
}
