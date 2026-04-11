package com.example.cashify.ui.transactions;

import android.app.AlertDialog;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.utils.NumpadBottomSheet;
import com.example.cashify.utils.CurrencyFormatter;
import com.example.cashify.viewmodel.AddTransactionViewModel;

import java.util.Calendar;
import java.util.Locale;

public class AddTransactionActivity extends AppCompatActivity {

    private AddTransactionViewModel viewModel;
    private EditText edtAmount, edtNote;
    private TextView tabChi, tabThu, tvDate, tvTitle;
    private LinearLayout btnCash, btnCard, btnBank;
    private Button btnConfirm;
    private ImageView btnDelete, btnBack;
    private RecyclerView rvCategories;

    private int editTransactionId = -1;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_transaction);

        // 1. Kiểm tra ID để xác định chế độ Sửa hay Thêm
        editTransactionId = getIntent().getIntExtra("TRANSACTION_ID", -1);
        isEditMode = (editTransactionId != -1);

        // 2. Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(AddTransactionViewModel.class);

        initViews();
        setupObservers();
        setupListeners();

        // 3. Load dữ liệu ban đầu
        if (isEditMode) {
            //setupEditUI();
            viewModel.loadTransactionForEdit(editTransactionId);
        } else {
            viewModel.loadCategories(0); // Mặc định load category Chi (type 0)
        }
    }

    private void initViews() {
        edtAmount = findViewById(R.id.edtAmount);
        edtAmount.setFocusable(false);
        edtAmount.setOnClickListener(v -> openNumpadBottomSheet());

        edtNote = findViewById(R.id.edtNote);
        tabChi = findViewById(R.id.tabChi);
        tabThu = findViewById(R.id.tabThu);
        tvDate = findViewById(R.id.tvDate);
        tvTitle = findViewById(R.id.tvTitle);
        btnCash = findViewById(R.id.btnPayCash);
        btnCard = findViewById(R.id.btnPayCard);
        btnBank = findViewById(R.id.btnPayBank);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnDelete = findViewById(R.id.btnDelete); // Đảm bảo ID này có trong XML
        btnBack = findViewById(R.id.btnBack);
        rvCategories = findViewById(R.id.rvCategories);
        rvCategories.setLayoutManager(new GridLayoutManager(this, 4));
    }



    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> showDeleteConfirmation());
        }

        tabChi.setOnClickListener(v -> {
            if (!isEditMode) viewModel.setType(true);
        });

        tabThu.setOnClickListener(v -> {
            if (!isEditMode) viewModel.setType(false);
        });

        tvDate.setOnClickListener(v -> {
            Calendar c = viewModel.calendar.getValue();
            new DatePickerDialog(this, (view, y, m, d) -> {
                viewModel.setDate(y, m, d);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnCash.setOnClickListener(v -> viewModel.setPayment("Cash"));
        btnCard.setOnClickListener(v -> viewModel.setPayment("Card"));
        btnBank.setOnClickListener(v -> viewModel.setPayment("Bank"));

        btnConfirm.setOnClickListener(v -> validateAndSave());
    }

    private void setupObservers() {
        // Observe dữ liệu để điền vào Form khi ở chế độ Sửa
        viewModel.existingTransaction.observe(this, t -> {
            if (t != null) {
                edtAmount.setText(CurrencyFormatter.formatDoubleToVND((double) t.amount));
                edtNote.setText(t.note);
            }
        });

        // Observe danh sách danh mục
        viewModel.categories.observe(this, list -> {
            CategoryPickerAdapter adapter = new CategoryPickerAdapter(this, list,
                    cat -> viewModel.selectedCategory.setValue(cat));
            rvCategories.setAdapter(adapter);
        });

        // Observe loại giao dịch (Chi/Thu)
        viewModel.isExpense.observe(this, this::updateTabUI);

        // Observe ngày tháng
        viewModel.calendar.observe(this, cal -> {
            String format = String.format(Locale.ENGLISH, "%02d/%02d/%04d",
                    cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR));
            tvDate.setText(format);
        });

        // Observe phương thức thanh toán
        viewModel.selectedPayment.observe(this, method -> {
            resetPaymentUI(btnCash, R.id.imgCash, R.id.tvCash);
            resetPaymentUI(btnCard, R.id.imgCard, R.id.tvCard);
            resetPaymentUI(btnBank, R.id.imgBank, R.id.tvBank);

            if (method.equals("Cash")) setActivePaymentUI(btnCash, R.id.imgCash, R.id.tvCash);
            else if (method.equals("Card")) setActivePaymentUI(btnCard, R.id.imgCard, R.id.tvCard);
            else setActivePaymentUI(btnBank, R.id.imgBank, R.id.tvBank);
        });

        // Observe kết quả lưu thành công
        viewModel.saveSuccess.observe(this, success -> {
            if (success) {
                Toast.makeText(this, isEditMode ? "Updated!" : "Saved!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void validateAndSave() {
        String amountStr = edtAmount.getText().toString();
        double amount = CurrencyFormatter.parseVNDToDouble(amountStr);

        if (amount <= 0) {
            edtAmount.setBackgroundResource(R.drawable.bg_input_error);
            Toast.makeText(this, getString(R.string.error_invalid_money_amount), Toast.LENGTH_SHORT).show();
            return;
        }

        if (viewModel.selectedCategory.getValue() == null) {
            Toast.makeText(this, getString(R.string.error_transaction_empty_category), Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.saveOrUpdate(String.valueOf((long)amount), edtNote.getText().toString().trim());
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this record?")
                .setPositiveButton("Delete", (dialog, which) -> viewModel.deleteCurrentTransaction())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateTabUI(boolean isExpense) {
        int color = isExpense ? R.color.status_red : R.color.status_green;
        tabChi.setBackgroundResource(isExpense ? R.drawable.bg_tab_outline : 0);
        tabChi.setTextColor(ContextCompat.getColor(this, isExpense ? color : R.color.item_description));

        tabThu.setBackgroundResource(!isExpense ? R.drawable.bg_tab_outline : 0);
        tabThu.setTextColor(ContextCompat.getColor(this, !isExpense ? color : R.color.item_description));

        edtAmount.setTextColor(ContextCompat.getColor(this, color));
    }

    private void resetPaymentUI(LinearLayout layout, int imgId, int txtId) {
        layout.setActivated(false);
        ((ImageView)findViewById(imgId)).setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.item_description)));
        ((TextView)findViewById(txtId)).setTextColor(ContextCompat.getColor(this, R.color.item_description));
    }

    private void setActivePaymentUI(LinearLayout layout, int imgId, int txtId) {
        layout.setActivated(true);
        ((ImageView)findViewById(imgId)).setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brand_primary)));
        ((TextView)findViewById(txtId)).setTextColor(ContextCompat.getColor(this, R.color.brand_primary));
    }

    private void openNumpadBottomSheet() {
        NumpadBottomSheet numpad = new NumpadBottomSheet();
        String currentText = edtAmount.getText().toString().replaceAll("[^\\d]", "");
        numpad.setInitialAmount(currentText.isEmpty() ? "0" : currentText);
        numpad.setListener((rawAmount, formattedAmount) -> {
            edtAmount.setText(formattedAmount);
            edtAmount.setBackgroundResource(R.drawable.bg_input_fields);
        });
        numpad.show(getSupportFragmentManager(), "NumpadBottomSheet");
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_out_down);
    }
}