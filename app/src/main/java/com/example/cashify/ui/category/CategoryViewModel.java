package com.example.cashify.ui.category;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cashify.data.model.Category;
import com.example.cashify.data.repository.CategoryRepository;
import java.util.List;

public class CategoryViewModel extends AndroidViewModel {

    private final CategoryRepository repository;
    private final MutableLiveData<List<Category>> incomeCategories = new MutableLiveData<>();
    private final MutableLiveData<List<Category>> expenseCategories = new MutableLiveData<>();

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        repository = new CategoryRepository(application);
        // Load dữ liệu lần đầu ngay khi khởi tạo
        refreshData();
    }
    // Getter để Activity "quan sát"
    public LiveData<List<Category>> getIncomeCategories() { return incomeCategories; }
    public LiveData<List<Category>> getExpenseCategories() { return expenseCategories; }

    public void refreshData() {
        // PostValue sẽ báo cho các Activity đang quan sát biết là có hàng mới về
        repository.getCategoriesByType(0, expenseCategories::postValue);
        repository.getCategoriesByType(1, incomeCategories::postValue);
    }

    public void insert(Category category)
    {
        repository.insert(category, this::refreshData);
    }

    public void update(Category category)
    {
        repository.update(category, this::refreshData);
    }

    // Gọi cái này khi user bấm xóa danh mục, tự xử lý soft/hard delete
    public void deleteCategory(int categoryId)
    {
        repository.deleteCategory(categoryId, this::refreshData);
    }

    public void getCategoriesByType(int type, CategoryRepository.Callback<List<Category>> callback) {
        repository.getCategoriesByType(type, callback);
    }

    public void getCategoryById(int id, CategoryRepository.Callback<Category> callback) {
        repository.getCategoryById(id, callback);
    }

    public void getAllActive(CategoryRepository.Callback<List<Category>> callback) {
        repository.getAllActive(callback);
    }

}