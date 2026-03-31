
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
import com.example.cashify.ui.NumpadBottomSheet;
import com.example.cashify.utils.CurrencyFormatter;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class AddTransactionActivity extends AppCompatActivity {

    // Khai báo trên đầu class:
    private final java.util.concurrent.ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    // Khai báo ở cấp class để lưu lại, tránh bug ng dùng bị delay xong bấm 2 3 lần vô hiện 2 3 cái date picker
    private DatePickerDialog datePickerDialog;
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

        btnConfirm.setOnClickListener(v ->validateAndSave());

        findViewById(R.id.btnBack).setOnClickListener(v ->
                hideKeyboardAndFinish()
        );

        // Bắt sự kiện vuốt viền hoặc phím cứng Back của điện thoại
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                hideKeyboardAndFinish();
            }
        });
    }

    private void initViews() {
        edtAmount = findViewById(R.id.edtAmount);

        // 1. Chặn Focus để không bao giờ hiện bàn phím hệ thống
        edtAmount.setFocusable(false);
        edtAmount.setFocusableInTouchMode(false);

        // 2. Lắng nghe sự kiện Click để mở Numpad
        edtAmount.setOnClickListener(v -> openNumpadBottomSheet());

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
        databaseExecutor.execute(() -> {
            List<Category> data = AppDatabase.getInstance(this).categoryDao().getCategoriesByType(type);
            runOnUiThread(() -> {
                catAdapter = new CategoryPickerAdapter(this, data, category -> selectedCategory = category);
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
            if (datePickerDialog == null || !datePickerDialog.isShowing()) {
                datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    updateDateText();
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show();
            }
        });
    }

    private void updateDateText()
    {
        //Luôn dùng Locale.ENGLISH để tránh lỗi hiển thị/lưu trữ ở các máy dùng ngôn ngữ Ả Rập, Farsi...
        String format = String.format(Locale.ENGLISH, "%02d/%02d/%04d",
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR));
        tvDate.setText(format);
    }

    // --- 5. Validate Form & Save ---
    private void validateAndSave() {
        String amountStr = edtAmount.getText().toString();

        // 1. Quăng toàn bộ trách nhiệm parse số cho CurrencyFormatter
        double amount = CurrencyFormatter.parseVNDToDouble(amountStr);

        // 2. Validate kết quả toán học trả về
        if (amount <= 0) {
            showAmountError();
            return;
        }

        // Reset lại viền bình thường nếu dữ liệu đã chuẩn
        edtAmount.setBackgroundResource(R.drawable.bg_input_fields);

        // Kiểm tra Category
        if (selectedCategory == null) {
            Toast.makeText(this, getString(R.string.error_transaction_empty_category), Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Gọi DAO Insert

        Toast.makeText(this, getString(R.string.noti_save_transaction_successfully), Toast.LENGTH_LONG).show();
        hideKeyboardAndFinish();
    }

    private void showAmountError() {
        edtAmount.setBackgroundResource(R.drawable.bg_input_error);
        Toast.makeText(this, getString(R.string.error_invalid_money_amount), Toast.LENGTH_SHORT).show();
    }

    // Bổ sung hàm onDestroy để giải phóng bộ nhớ:
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Giải phóng Executor khi Activity chết để tránh Memory Leak
        if (!databaseExecutor.isShutdown()) {
            databaseExecutor.shutdown();
        }
    }

    private void hideKeyboardAndFinish()
    {
        supportFinishAfterTransition();
    }

    private void openNumpadBottomSheet() {
        NumpadBottomSheet numpad = new NumpadBottomSheet();

        // Lấy số tiền hiện tại đang hiển thị (nếu có) để truyền vào Numpad
        // Cần cẩn thận loại bỏ các dấu phẩy/chấm (nếu bạn đã format hiển thị trước đó)
        String currentText = edtAmount.getText().toString().replaceAll("[^\\d]", "");
        if (currentText.isEmpty()) {
            currentText = "0";
        }

        numpad.setInitialAmount(currentText);

        // Lắng nghe kết quả trả về từ Numpad
        numpad.setListener((rawAmount, formattedAmount) ->
        {
            // rawAmount: "50000" (dùng để lưu DB)
            // formattedAmount: "50,000" (dùng để hiển thị lên UI)

            // Cập nhật giao diện
            edtAmount.setText(formattedAmount);

            // Nếu trước đó đang bị viền đỏ báo lỗi, thì giờ người dùng nhập xong xóa viền đỏ đi
            edtAmount.setBackgroundResource(R.drawable.bg_input_fields);
        });

        // Gọi BottomSheet lên.
        // Lưu ý: Trong Activity phải dùng getSupportFragmentManager() thay vì getChildFragmentManager()
        numpad.show(getSupportFragmentManager(), "NumpadBottomSheet");
    }

}