package com.example.cashify.ui.transactions;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.cashify.data.model.Category;
import com.example.cashify.data.model.Transaction;
import com.example.cashify.data.repository.AddTransactionRepository;
import com.example.cashify.data.repository.TransactionRepository;

import java.util.Calendar;
import java.util.List;

public class AddTransactionViewModel extends AndroidViewModel {

    private final AddTransactionRepository addRepo; // Quản lý Categories
    private final TransactionRepository transRepo; // Quản lý Transactions

    private String currentWorkspaceId = "PERSONAL";

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

    //Hàm để Activity truyền workspaceId vào
    public void setCurrentWorkspaceId(String workspaceId) {
        if (workspaceId != null && !workspaceId.isEmpty()) {
            this.currentWorkspaceId = workspaceId;
        }
    }

    // --- LOGIC CHO EDIT MODE ---
    public void loadTransactionForEdit(String transactionId) {
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
        // 1. Kiểm tra tiền trước
        if (amountStr.isEmpty()) return;

        Category selected = selectedCategory.getValue();
        Transaction existing = existingTransaction.getValue();
        boolean editMode = Boolean.TRUE.equals(isEditMode.getValue());

        // 2. Chốt ID Category (Đây là đoạn quan trọng nhất)
        int finalCategoryId;
        if (selected != null) {
            // Nếu người dùng có bấm chọn cái mới -> Lấy cái mới
            finalCategoryId = selected.id;
        } else if (editMode && existing != null) {
            // Nếu đang sửa và không chọn cái mới -> Lấy lại ID cũ từ database
            finalCategoryId = existing.categoryId;
        } else {
            // Trường hợp thêm mới mà không chọn gì -> Dừng lại
            return;
        }

        // 3. Chuẩn bị Object để lưu
        Transaction t;
        if (editMode && existing != null) {
            t = existing; // Dùng lại object cũ (giữ nguyên ID gốc)
        } else {
            t = new Transaction();
        }

        t.amount = Long.parseLong(amountStr);
        t.note = note;
        t.categoryId = finalCategoryId; // Gán ID đã chốt ở trên
        t.timestamp = calendar.getValue().getTimeInMillis();
        t.type = Boolean.TRUE.equals(isExpense.getValue()) ? 0 : 1;
        t.paymentMethod = selectedPayment.getValue();
        t.workspaceId = this.currentWorkspaceId;
        // 4. Thực thi vào Database
        new Thread(() -> {
            if (editMode) {
                transRepo.update(t);
            } else {
                transRepo.insert(t);
            }
            saveSuccess.postValue(true);
        }).start();
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