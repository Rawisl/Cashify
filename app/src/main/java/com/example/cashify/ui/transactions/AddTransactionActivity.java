package com.example.cashify.ui.transactions;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.database.Category;
import com.example.cashify.utils.InvoiceParser;
import com.example.cashify.data.model.Category;
import com.example.cashify.utils.NumpadBottomSheet;
import com.example.cashify.utils.CurrencyFormatter;
import com.example.cashify.utils.ToastHelper;

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

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private com.google.mlkit.vision.text.TextRecognizer recognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Gán Layout trước để tránh NullPointerException khi findViewById
        setContentView(R.layout.activity_add_transaction);

        // 2. Kiểm tra ID từ Intent ngay lập tức để xác định chế độ (Add hay Edit)
        editTransactionId = getIntent().getIntExtra("TRANSACTION_ID", -1);
        isEditMode = (editTransactionId != -1);

        // 3. Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(AddTransactionViewModel.class);

        // 4. Khởi tạo Giao diện và Gán sự kiện
        initViews();       // Đã bao gồm logic đổi Title sang "Edit Transaction"
        setupObservers();   // Quan sát dữ liệu từ ViewModel
        setupListeners();   // Lắng nghe sự kiện click nút bấm

        // 5. Nạp dữ liệu khởi tạo
        if (isEditMode) {
            // Mode EDIT: Bảo ViewModel tìm lại giao dịch cũ để đổ vào Form
            viewModel.loadTransactionForEdit(editTransactionId);
        } else {
            // Mode ADD: Mặc định load danh mục Chi (Type 0)
            viewModel.loadCategories(0);
        }
        recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
                com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS);
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

        if (tvTitle != null) {
            if (isEditMode) {
                tvTitle.setText("Edit Transaction");
                if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
            } else {
                tvTitle.setText("Add Transaction");
                if (btnDelete != null) btnDelete.setVisibility(View.GONE);
            }
        }
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

        ImageView btnScan = findViewById(R.id.btnScan);
        if (btnScan != null) {
            btnScan.setOnClickListener(v -> showImageSourceOptions());
        }

        btnCash.setOnClickListener(v -> viewModel.setPayment("Cash"));
        btnCard.setOnClickListener(v -> viewModel.setPayment("Card"));
        btnBank.setOnClickListener(v -> viewModel.setPayment("Bank"));

        btnConfirm.setOnClickListener(v -> validateAndSave());

    }
    private void showImageSourceOptions() {
        String[] options = {"Chụp ảnh hóa đơn", "Chọn ảnh từ thư viện"};
        new AlertDialog.Builder(this)
                .setTitle("Quét hóa đơn")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        dispatchTakePictureIntent(); // Gọi hàm mở camera cũ
                    } else {
                        openGalleryIntent(); // Gọi hàm mở thư viện mới
                    }
                })
                .show();
    }

    private void openGalleryIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Mình dùng số 2 (REQUEST_PICK_IMAGE) để phân biệt với camera (số 1)
        startActivityForResult(intent, 2);
    }

    private void setupObservers() {
        // Observe dữ liệu để điền vào Form khi ở chế độ Sửa
        viewModel.existingTransaction.observe(this, t -> {
            if (t != null) {
                edtAmount.setText(CurrencyFormatter.formatDoubleToVND((double) t.amount));
                edtNote.setText(t.note);
                if (t.paymentMethod != null) {
                    viewModel.setPayment(t.paymentMethod);
                }
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
                ToastHelper.show(this, isEditMode ? "Updated!" : "Saved!");
                finish();
            }
        });
    }

    private void validateAndSave() {
        String amountStr = edtAmount.getText().toString();
        double amount = CurrencyFormatter.parseVNDToDouble(amountStr);

        // Kiểm tra số tiền hợp lệ
        if (amount <= 0) {
            edtAmount.setBackgroundResource(R.drawable.bg_input_error);
            ToastHelper.show(this, getString(R.string.error_invalid_money_amount));
            return;
        }

        // Logic xử lý Danh mục (Category)
        Category selected = viewModel.selectedCategory.getValue();

        // Nếu là mode THÊM MỚI mà chưa chọn Category thì mới chặn
        // Nếu là mode EDIT mà chưa chọn cái mới, hệ thống sẽ tự lấy cái cũ trong ViewModel/DB
        if (!isEditMode && selected == null) {
            ToastHelper.show(this, getString(R.string.error_transaction_empty_category));
            return;
        }

        // Thực hiện lưu hoặc cập nhật
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

    //mở cam me ra
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                //  1: Chụp trực tiếp
                Bundle extras = data.getExtras();
                android.graphics.Bitmap imageBitmap = (android.graphics.Bitmap) extras.get("data");
                processImage(imageBitmap);

            } else if (requestCode == 2) { // 2 là mã của Gallery
                // 2: Chọn từ máy (Uri)
                android.net.Uri imageUri = data.getData();
                try {
                    // Chuyển từ "Địa chỉ ảnh" sang "Dữ liệu ảnh Bitmap" để ML Kit đọc
                    android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    processImage(bitmap);
                } catch (java.io.IOException e) {
                    ToastHelper.show(this, "Failed to load image!");
                }
            }
        }
    }

    private void extractDataFromText(String ocrText) {
        InvoiceParser.parse(ocrText, new InvoiceParser.ParseCallback() {

            @Override
            public void onSuccess(InvoiceParser.ParsedInvoice result) {
                runOnUiThread(() -> fillFormAndConfirm(result));
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    btnConfirm.setEnabled(true);
                     ToastHelper.show(AddTransactionActivity.this,
                            "Analysis error: " + error);
                });
            }
        });
    }
    private void fillFormAndConfirm(InvoiceParser.ParsedInvoice result) {
        // 1. Điền các thông tin cơ bản trước
        if (result.amount > 0) {
            edtAmount.setText(CurrencyFormatter.formatDoubleToVND((double) result.amount));
        }
        if (result.description != null) {
            edtNote.setText(result.description);
        }
        if (result.paymentMethod != null) {
            viewModel.setPayment(result.paymentMethod);
        }

        // 2. Chuyển sang tab Chi (Type 0)
        viewModel.setType(true);

        // 3. Quan sát danh sách Category đổ về từ DB
        // Chúng ta dùng "observe" để khi nào DB trả về dữ liệu mới bắt đầu khớp tên
        viewModel.categories.observe(this, new androidx.lifecycle.Observer<java.util.List<Category>>() {
            @Override
            public void onChanged(java.util.List<Category> categories) {
                if (categories == null || categories.isEmpty()) return;

                boolean found = false;
                for (Category cat : categories) {
                    if (cat.name.equalsIgnoreCase(result.categoryName)) {
                        viewModel.selectedCategory.setValue(cat);
                        found = true;
                        break;
                    }
                }

                // Fallback nếu không tìm thấy tên khớp
                if (!found) {
                    for (Category cat : categories) {
                        if (cat.name.contains("Khác")) {
                            viewModel.selectedCategory.setValue(cat);
                            break;
                        }
                    }
                }

                // Hủy quan sát ngay sau khi khớp xong để tránh chạy lại nhiều lần
                viewModel.categories.removeObserver(this);

                // 4. Cho phép lưu
                btnConfirm.setEnabled(true);
                Toast.makeText(AddTransactionActivity.this, "✅ Đã khớp danh mục!", Toast.LENGTH_SHORT).show();

                // Tự động lưu sau 1s nếu muốn
                edtAmount.postDelayed(() -> validateAndSave(), 1000);
            }
        });
    }
    private void processImage(android.graphics.Bitmap bitmap) {
        // Hiện loading cho user biết đang xử lý
        Toast.makeText(this, "Đang quét hóa đơn...", Toast.LENGTH_SHORT).show();
        btnConfirm.setEnabled(false);

        com.google.mlkit.vision.common.InputImage image =
                com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String ocrText = visionText.getText();
                    if (ocrText == null || ocrText.trim().isEmpty()) {
                        runOnUiThread(() -> {
                            btnConfirm.setEnabled(true);
                            Toast.makeText(this,
                                    "Không đọc được chữ trong ảnh. Thử ảnh khác!",
                                    Toast.LENGTH_SHORT).show();
                        });
                        return;
                    }
                    // Gửi OCR text lên Claude để parse thông minh
                    extractDataFromText(ocrText);
                })
                .addOnFailureListener(e -> runOnUiThread(() -> {
                    btnConfirm.setEnabled(true);
                    Toast.makeText(this, "OCR thất bại: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }));
    }
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_out_down);
    }
}