package com.example.cashify.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.cashify.database.Category;
import com.example.cashify.database.Transaction;
import com.example.cashify.repository.AddTransactionRepository;
import com.example.cashify.repository.TransactionRepository;

import java.util.Calendar;
import java.util.List;

public class AddTransactionViewModel extends AndroidViewModel {

    private final AddTransactionRepository addRepo; // Quản lý Categories
    private final TransactionRepository transRepo; // Quản lý Transactions

    // Trạng thái giao diện
    public MutableLiveData<Boolean> isExpense = new MutableLiveData<>(true);
    public MutableLiveData<Boolean> isEditMode = new MutableLiveData<>(false);
    public MutableLiveData<Calendar> calendar = new MutableLiveData<>(Calendar.getInstance());
    public MutableLiveData<String> selectedPayment = new MutableLiveData<>("Cash");

    // Dữ liệu từ Database
    public MutableLiveData<List<Category>> categories = new MutableLiveData<>();
    public MutableLiveData<Category> selectedCategory = new MutableLiveData<>();
    public MutableLiveData<Transaction> existingTransaction = new MutableLiveData<>();
    public MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>(false);

    public AddTransactionViewModel(@NonNull Application application) {
        super(application);
        this.addRepo = new AddTransactionRepository(application);
        this.transRepo = new TransactionRepository(application);
    }

    // --- LOGIC CHO EDIT MODE ---
    public void loadTransactionForEdit(int transactionId) {
        isEditMode.setValue(true);
        transRepo.getById(transactionId, transaction -> {
            if (transaction != null) {
                existingTransaction.postValue(transaction);

                // Cập nhật các state dựa trên transaction cũ
                isExpense.postValue(transaction.type == 0);

                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(transaction.timestamp);
                calendar.postValue(cal);

                // Sau khi xác định type, load categories tương ứng
                loadCategories(transaction.type);
            }
        });
    }

    // --- LOGIC CHO CATEGORY ---
    public void setType(boolean expense) {
        isExpense.setValue(expense);
        loadCategories(expense ? 0 : 1);
    }

    public void loadCategories(int type) {
        addRepo.getCategoriesByType(type, result -> categories.postValue(result));
    }

    // --- LOGIC LƯU / CẬP NHẬT ---
    public void saveOrUpdate(String amountStr, String note) {
        if (amountStr.isEmpty() || selectedCategory.getValue() == null) return;

        Transaction t;
        if (Boolean.TRUE.equals(isEditMode.getValue()) && existingTransaction.getValue() != null) {
            t = existingTransaction.getValue(); // Lấy object cũ để giữ nguyên ID
        } else {
            t = new Transaction();
        }

        t.amount = Long.parseLong(amountStr);
        t.note = note;
        t.categoryId = selectedCategory.getValue().id;
        t.timestamp = calendar.getValue().getTimeInMillis();
        t.type = Boolean.TRUE.equals(isExpense.getValue()) ? 0 : 1;
        // t.paymentMethod = selectedPayment.getValue(); // Nếu Entity có field này

        if (Boolean.TRUE.equals(isEditMode.getValue())) {
            transRepo.update(t);
            saveSuccess.postValue(true);
        } else {
            transRepo.insert(t);
            saveSuccess.postValue(true);
        }
    }

    public void deleteCurrentTransaction() {
        if (existingTransaction.getValue() != null) {
            transRepo.delete(existingTransaction.getValue());
            saveSuccess.postValue(true);
        }
    }

    // --- HELPER SETTERS ---
    public void setPayment(String method) {
        selectedPayment.setValue(method);
    }

    public void setDate(int y, int m, int d) {
        Calendar cal = calendar.getValue();
        if (cal != null) {
            cal.set(y, m, d);
            calendar.setValue(cal);
        }
    }
}