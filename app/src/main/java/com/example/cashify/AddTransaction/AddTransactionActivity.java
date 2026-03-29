package com.example.cashify.AddTransaction;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
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

import java.util.ArrayList;
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
        // Kiểm tra đúng tên file XML của ông là add_transaction hay activity_add_transaction
        setContentView(R.layout.add_transaction);

        initViews();
        setupTabs();
        setupDatePicker();
        setupPaymentMethods();

        // 1. Khởi tạo danh sách category mặc định (CHI) ngay khi vào màn hình
        loadCategories(0);

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

        // Mặc định ngày hôm nay
        updateDateText();
    }

    // --- 1. Logic Switch Tab (Expense/Income) ---
    private void setupTabs() {
        View.OnClickListener tabListener = v -> {
            isExpense = (v.getId() == R.id.tabChi);
            updateTabUI();
            // 2. Khi đổi tab, load lại danh sách category tương ứng
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
        // Reset selected category khi chuyển tab để tránh chọn nhầm category cũ
        selectedCategory = null;
    }

    // --- 2. Logic Load Category ---
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

    // --- 3. Logic Payment Selection ---
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

        btnCash.setActivated(true); // Mặc định chọn Cash
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

    // --- 4. Logic Date Picker ---
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

    // --- 5. Validate Form & Save ---
    private void validateAndSave() {
        String amountStr = edtAmount.getText().toString().trim();

        // Kiểm tra tiền
        if (amountStr.isEmpty() || amountStr.equals("0")) {
            edtAmount.setBackgroundResource(R.drawable.bg_input_error);
            Toast.makeText(this, "Vui lòng nhập số tiền hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        } else {
            // Reset lại viền bình thường nếu đã nhập
            edtAmount.setBackgroundResource(R.drawable.bg_input_fields);
        }

        // Kiểm tra Category
        if (selectedCategory == null) {
            Toast.makeText(this, "Vui lòng chọn một danh mục!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Nếu mọi thứ OK, thực hiện lưu (Chỗ này ông sẽ gọi DAO để Insert Transaction)
        Toast.makeText(this, "Đã lưu giao dịch " + (isExpense ? "Chi" : "Thu") + ": " + amountStr + "đ", Toast.LENGTH_LONG).show();
        finish();
    }
}