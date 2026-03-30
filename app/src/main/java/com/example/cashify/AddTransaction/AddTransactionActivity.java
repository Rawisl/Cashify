package com.example.cashify.AddTransaction;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.database.AppDatabase;
import com.example.cashify.database.Category;
import com.example.cashify.database.DatabaseSeeder;
import com.example.cashify.database.Transaction;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class AddTransactionActivity extends AppCompatActivity {

    private EditText edtAmount, edtNote;
    private TextView tabChi, tabThu, tvDate;
    private LinearLayout btnCash, btnCard, btnBank;
    private Button btnConfirm;

    private boolean isExpense = true; // Mặc định là CHI
    private String selectedPayment = "Cash"; // Mặc định Cash
    private Calendar calendar = Calendar.getInstance();

    private RecyclerView rvCategories;
    private CategoryPickerAdapter catAdapter;
    private Category selectedCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_transaction);

        initViews();
        setupTabs();
        setupDatePicker();
        setupPaymentMethods();

        // --- CHỈNH SỬA TẠI ĐÂY: Đổ dữ liệu ---
        // 1. Seed dữ liệu nếu DB trống (Lần đầu chạy app)
        // 2. Load danh sách category ngay lập tức
        loadInitialData();

        btnConfirm.setOnClickListener(v -> validateAndSave());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void initViews() {
        edtAmount = findViewById(R.id.edtAmount);
        edtNote = findViewById(R.id.edtNote);
        tabChi = findViewById(R.id.tabChi);
        tabThu = findViewById(R.id.tabThu);
        tvDate = findViewById(R.id.tvDate);
        btnCash = findViewById(R.id.btnPayCash);
        btnCard = findViewById(R.id.btnPayCard);
        btnBank = findViewById(R.id.btnPayBank);
        btnConfirm = findViewById(R.id.btnConfirm);
        rvCategories = findViewById(R.id.rvCategories);

        updateDateText();
    }

    private void loadInitialData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // Đảm bảo có dữ liệu mẫu trước khi lấy
            DatabaseSeeder.seedIfEmpty(this);

            // Sau khi seed xong thì load lên UI
            loadCategories(isExpense ? 0 : 1);
        });
    }

    private void setupTabs() {
        View.OnClickListener tabListener = v -> {
            isExpense = (v.getId() == R.id.tabChi);
            updateTabUI();
            loadCategories(isExpense ? 0 : 1);
        };
        tabChi.setOnClickListener(tabListener);
        tabThu.setOnClickListener(tabListener);
    }

    private void updateTabUI() {
        if (isExpense) {
            tabChi.setBackgroundResource(R.drawable.bg_tab_outline);
            tabChi.setTextColor(ContextCompat.getColor(this, R.color.status_red));
            tabThu.setBackground(null);
            tabThu.setTextColor(ContextCompat.getColor(this, R.color.item_description));
            edtAmount.setTextColor(ContextCompat.getColor(this, R.color.status_red));
        } else {
            tabThu.setBackgroundResource(R.drawable.bg_tab_outline);
            tabThu.setTextColor(ContextCompat.getColor(this, R.color.status_green));
            tabChi.setBackground(null);
            tabChi.setTextColor(ContextCompat.getColor(this, R.color.item_description));
            edtAmount.setTextColor(ContextCompat.getColor(this, R.color.status_green));
        }
        selectedCategory = null;
    }

    private void loadCategories(int type) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Category> data = AppDatabase.getInstance(this).categoryDao().getCategoriesByType(type);

            runOnUiThread(() -> {
                catAdapter = new CategoryPickerAdapter(this, data, category -> {
                    selectedCategory = category;
                });
                rvCategories.setLayoutManager(new GridLayoutManager(this, 4));
                rvCategories.setAdapter(catAdapter);
            });
        });
    }

    // --- Logic Lưu Dữ Liệu Thực Tế ---
    private void validateAndSave() {
        String amountStr = edtAmount.getText().toString().trim();

        if (amountStr.isEmpty() || amountStr.equals("0")) {
            edtAmount.setBackgroundResource(R.drawable.bg_input_error);
            Toast.makeText(this, "Vui lòng nhập số tiền!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategory == null) {
            Toast.makeText(this, "Vui lòng chọn danh mục!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo đối tượng Transaction để lưu
        Transaction transaction = new Transaction();
        transaction.amount = Long.parseLong(amountStr);
        transaction.categoryId = selectedCategory.id;
        transaction.note = edtNote.getText().toString().trim();
        transaction.timestamp = calendar.getTimeInMillis();
        transaction.type = isExpense ? 0 : 1;

        // Gọi DB để lưu
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase.getInstance(this).transactionDao().insert(transaction);

            runOnUiThread(() -> {
                Toast.makeText(this, "Lưu thành công!", Toast.LENGTH_SHORT).show();
                finish(); // Đóng màn hình sau khi lưu
            });
        });
    }

    private void setupPaymentMethods() {
        View.OnClickListener payListener = v -> {
            resetPaymentUI(btnCash, R.id.imgCash, R.id.tvCash);
            resetPaymentUI(btnCard, R.id.imgCard, R.id.tvCard);
            resetPaymentUI(btnBank, R.id.imgBank, R.id.tvBank);

            v.setActivated(true);
            if (v.getId() == R.id.btnPayCash) {
                selectedPayment = "Cash";
                setActivePaymentUI(btnCash, R.id.imgCash, R.id.tvCash);
            } else if (v.getId() == R.id.btnPayCard) {
                selectedPayment = "Card";
                setActivePaymentUI(btnCard, R.id.imgCard, R.id.tvCard);
            } else {
                selectedPayment = "Bank";
                setActivePaymentUI(btnBank, R.id.imgBank, R.id.tvBank);
            }
        };
        btnCash.setOnClickListener(payListener);
        btnCard.setOnClickListener(payListener);
        btnBank.setOnClickListener(payListener);
        btnCash.setActivated(true);
    }

    private void resetPaymentUI(LinearLayout layout, int imgId, int txtId) {
        layout.setActivated(false);
        ((ImageView)findViewById(imgId)).setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.item_description)));
        ((TextView)findViewById(txtId)).setTextColor(ContextCompat.getColor(this, R.color.item_description));
    }

    private void setActivePaymentUI(LinearLayout layout, int imgId, int txtId) {
        ((ImageView)findViewById(imgId)).setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brand_primary)));
        ((TextView)findViewById(txtId)).setTextColor(ContextCompat.getColor(this, R.color.brand_primary));
    }

    private void setupDatePicker() {
        tvDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                updateDateText();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void updateDateText() {
        String format = String.format(Locale.getDefault(), "%02d/%02d/%d",
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR));
        tvDate.setText(format);
    }
}