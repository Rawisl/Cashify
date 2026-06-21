package com.example.cashify.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.cashify.data.model.Budget;
import com.example.cashify.data.model.Category;
import com.example.cashify.data.model.Transaction;
import com.example.cashify.data.model.Workspace;

@Database(entities = {Category.class, Transaction.class, Budget.class, Workspace.class}, version = 7, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;

    // Các "Trạm trung chuyển" dữ liệu (DAO)
    public abstract CategoryDao categoryDao();

    public abstract TransactionDao transactionDao();

    public abstract BudgetDao budgetDao();
    public abstract WorkspaceDao workspaceDao();

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE transactions ADD COLUMN workspaceId TEXT DEFAULT 'PERSONAL'");
        }
    };

    // Hàm lấy Database (Singleton pattern - Đảm bảo cả app chỉ có 1 Database duy nhất)
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            // Giúp app chạy nhanh hơn vì chỉ khóa luồng ở lần khởi tạo đầu tiên.
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "money_manager_db")
                            //warning: fallbackToDestructiveMigration sẽ xóa trắng data local nếu  tăng version mà quên viết Migration.
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}