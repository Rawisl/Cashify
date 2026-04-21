package com.example.cashify.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.cashify.data.model.Budget;
import com.example.cashify.data.model.Category;
import com.example.cashify.data.model.Transaction;

@Database(entities = {Category.class, Transaction.class, Budget.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;

    // Các "Trạm trung chuyển" dữ liệu (DAO)
    public abstract CategoryDao categoryDao();

    public abstract TransactionDao transactionDao();

    public abstract BudgetDao budgetDao();

    // Hàm lấy Database (Singleton pattern - Đảm bảo cả app chỉ có 1 Database duy nhất)
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "money_manager_db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}