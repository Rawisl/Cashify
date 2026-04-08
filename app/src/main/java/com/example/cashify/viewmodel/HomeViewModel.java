package com.example.cashify.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.cashify.database.Transaction;
import com.example.cashify.database.TransactionWithCategory;
import com.example.cashify.repository.TransactionRepository;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {
    private final TransactionRepository repository;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = new TransactionRepository(application);
    }

    // Lấy list kèm Category
    public LiveData<List<TransactionWithCategory>> getRecentTransactionsWithCategory() {
        return repository.getRecentTransactionsWithCategory();
    }

}