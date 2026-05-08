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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.Category;
import com.example.cashify.data.model.Transaction;
import com.example.cashify.utils.InvoiceParser;
import com.example.cashify.utils.NumpadBottomSheet;
import com.example.cashify.utils.CurrencyFormatter;
import com.example.cashify.utils.ToastHelper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddTransactionActivity extends AppCompatActivity {

    private AddTransactionViewModel viewModel;
    private EditText edtAmount, edtNote;
    private TextView tabChi, tabThu, tvDate, tvTitle;
    private LinearLayout btnCash, btnCard, btnBank;
    private Button btnConfirm;
    private ImageView btnDelete, btnBack;
    private RecyclerView rvCategories;

    private String editTransactionId = null;
    private boolean isEditMode = false;
    private String workspaceId = null; // Cờ nhận biết Quỹ chung

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private com.google.mlkit.vision.text.TextRecognizer recognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        viewModel = new ViewModelProvider(this).get(AddTransactionViewModel.class);

        editTransactionId = getIntent().getStringExtra("TRANSACTION_ID");
        isEditMode = (editTransactionId != null);

        // Lấy ID Quỹ để rẽ nhánh logic
        workspaceId = getIntent().getStringExtra("WORKSPACE_ID");
        if (workspaceId != null && !workspaceId.trim().isEmpty() && !workspaceId.equals("null") && !workspaceId.equals("PERSONAL")) {
            viewModel.setCurrentWorkspaceId(workspaceId);
        }

        initViews();
        setupObservers();
        setupListeners();

        if (isEditMode) {
            if (workspaceId != null && !workspaceId.trim().isEmpty() && !workspaceId.equals("null") && !workspaceId.equals("PERSONAL")) {
                loadTransactionFromFirestoreForEdit(editTransactionId);
            } else {
                viewModel.loadTransactionForEdit(editTransactionId);
            }
        } else {
            // Gọi load từ hàm Rẽ nhánh mới
            loadCategoriesForCurrentMode(0);
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
        btnDelete = findViewById(R.id.btnDelete);
        btnBack = findViewById(R.id.btnBack);
        rvCategories = findViewById(R.id.rvCategories);
        rvCategories.setLayoutManager(new GridLayoutManager(this, 4));

        if (tvTitle != null) {
            if (isEditMode) {
                tvTitle.setText(workspaceId != null && !workspaceId.trim().isEmpty() && !workspaceId.equals("null") && !workspaceId.equals("PERSONAL") ? "Edit Fund Transaction" : "Edit Transaction");
                if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
            } else {
                tvTitle.setText(workspaceId != null && !workspaceId.trim().isEmpty() && !workspaceId.equals("null") && !workspaceId.equals("PERSONAL") ? "Add Fund Transaction" : "Add Transaction");
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
            if (!isEditMode) {
                viewModel.setType(true);
                loadCategoriesForCurrentMode(0); // Tải lại danh mục Thu/Chi
            }
        });

        tabThu.setOnClickListener(v -> {
            if (!isEditMode) {
                viewModel.setType(false);
                loadCategoriesForCurrentMode(1); // Tải lại danh mục Thu/Chi
            }
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

    private void loadCategoriesForCurrentMode(int type) {
        if (workspaceId != null && !workspaceId.trim().isEmpty() && !workspaceId.equals("null") && !workspaceId.equals("PERSONAL")) {
            // Chế độ Quỹ: Kéo từ Cloud về
            FirebaseFirestore.getInstance()
                    .collection("workspaces").document(workspaceId)
                    .collection("categories")
                    .whereEqualTo("type", type)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<Category> list = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Category c = doc.toObject(Category.class);
                            c.firestoreId = doc.getId();
                            list.add(c);
                        }
                        // Bơm ngược vào ViewModel để tận dụng Adapter cũ
                        viewModel.categories.postValue(list);
                    });
        } else {
            // Chế độ Cá nhân: Giữ nguyên logic cũ gọi DB Room
            viewModel.loadCategories(type);
        }
    }

    private void setupObservers() {
        viewModel.existingTransaction.observe(this, t -> {
            if (t != null) {
                edtAmount.setText(CurrencyFormatter.formatDoubleToVND((double) t.amount));
                edtNote.setText(t.note);
                if (t.paymentMethod != null) {
                    viewModel.setPayment(t.paymentMethod);
                }

                // BỔ SUNG: Nếu là Quỹ chung, bảo Adapter highlight Category theo ID String
                if (workspaceId != null && !workspaceId.equals("PERSONAL") && !workspaceId.equals("null")) {
                    // Highlight cho Quỹ (ID String)
                    rvCategories.post(() -> {
                        if (rvCategories.getAdapter() instanceof CategoryPickerAdapter) {
                            ((CategoryPickerAdapter) rvCategories.getAdapter()).setSelectedByFirestoreId(t.firestoreCategoryId);
                        }
                    });
                } else {
                    // HIGHLIGHT CHO CÁ NHÂN (ID Int) - Sếp đang thiếu đoạn này!
                    rvCategories.post(() -> {
                        if (rvCategories.getAdapter() instanceof CategoryPickerAdapter) {
                            ((CategoryPickerAdapter) rvCategories.getAdapter()).setSelectedById(t.categoryId);
                        }
                    });
                }
            }
        });

        viewModel.categories.observe(this, list -> {
            // 1. Tái chế Adapter như cũ
            if (rvCategories.getAdapter() == null) {
                CategoryPickerAdapter adapter = new CategoryPickerAdapter(this, list,
                        cat -> viewModel.selectedCategory.setValue(cat));
                rvCategories.setAdapter(adapter);
            } else {
                CategoryPickerAdapter adapter = (CategoryPickerAdapter) rvCategories.getAdapter();
                adapter.setNewData(list);
            }

            // 2. BỔ SUNG ĐOẠN NÀY: Khôi phục lại highlight nếu đang ở chế độ Edit
            Transaction t = viewModel.existingTransaction.getValue();
            if (isEditMode && t != null) {
                // Dùng post để đảm bảo Adapter đã vẽ xong danh sách mới
                rvCategories.post(() -> {
                    CategoryPickerAdapter adapter = (CategoryPickerAdapter) rvCategories.getAdapter();
                    if (workspaceId != null && !workspaceId.equals("PERSONAL") && !workspaceId.equals("null")) {
                        // Highlight Quỹ chung
                        adapter.setSelectedByFirestoreId(t.firestoreCategoryId);
                    } else {
                        // Highlight Cá nhân
                        adapter.setSelectedById(t.categoryId);
                    }

                    // Cập nhật luôn cục selectedCategory trong ViewModel để lúc Save không bị rỗng
                    for (Category cat : list) {
                        if (workspaceId != null && !workspaceId.equals("PERSONAL")) {
                            if (cat.firestoreId != null && cat.firestoreId.equals(t.firestoreCategoryId)) {
                                viewModel.selectedCategory.setValue(cat);
                                break;
                            }
                        } else {
                            if (cat.id == t.categoryId) {
                                viewModel.selectedCategory.setValue(cat);
                                break;
                            }
                        }
                    }
                });
            }
        });

        viewModel.isExpense.observe(this, this::updateTabUI);

        viewModel.calendar.observe(this, cal -> {
            String format = String.format(Locale.ENGLISH, "%02d/%02d/%04d",
                    cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR));
            tvDate.setText(format);
        });

        viewModel.selectedPayment.observe(this, method -> {
            resetPaymentUI(btnCash, R.id.imgCash, R.id.tvCash);
            resetPaymentUI(btnCard, R.id.imgCard, R.id.tvCard);
            resetPaymentUI(btnBank, R.id.imgBank, R.id.tvBank);

            if (method.equals("Cash")) setActivePaymentUI(btnCash, R.id.imgCash, R.id.tvCash);
            else if (method.equals("Card")) setActivePaymentUI(btnCard, R.id.imgCard, R.id.tvCard);
            else setActivePaymentUI(btnBank, R.id.imgBank, R.id.tvBank);
        });

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
        String note = edtNote.getText().toString().trim();
        if (amount <= 0) {
            edtAmount.setBackgroundResource(R.drawable.bg_input_error);
            ToastHelper.show(this, getString(R.string.error_invalid_money_amount));
            return;
        }

        Category selected = viewModel.selectedCategory.getValue();
        if (!isEditMode && selected == null) {
            ToastHelper.show(this, getString(R.string.error_transaction_empty_category));
            return;
        }

        // =============================================================
        // RẼ NHÁNH: LƯU LÊN FIRESTORE NẾU LÀ QUỸ
        // =============================================================
        // ĐIỀU KIỆN CHUẨN: Chỉ vào Firestore nếu workspaceId KHÁC null và KHÁC "PERSONAL"
        if (workspaceId != null && !workspaceId.equals("PERSONAL") && !workspaceId.equals("null")) {
            saveToFirestore(amount, note, selected);
        } else {
            // Chạy Room cho cá nhân
            viewModel.saveOrUpdate(String.valueOf((long) amount), note);
        }
    }

    private void saveToFirestore(double amount, String note, Category selectedCat) {
        btnConfirm.setEnabled(false); // Chống spam click

        Transaction t = new Transaction();
        if (isEditMode) t.id = editTransactionId;

        t.amount = (long) amount;
        t.note = note;
        t.timestamp = viewModel.calendar.getValue().getTimeInMillis();
        t.paymentMethod = viewModel.selectedPayment.getValue();
        t.type = Boolean.TRUE.equals(viewModel.isExpense.getValue()) ? 0 : 1;

        t.workspaceId = workspaceId;
        t.userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Ghi nhận người chi

        // XỬ LÝ ID CATEGORY THÔNG MINH:
        if (selectedCat != null) {
            // Nếu có chọn cái mới
            t.firestoreCategoryId = selectedCat.firestoreId;
        } else if (isEditMode && viewModel.existingTransaction.getValue() != null) {
            // Nếu không chọn mới, lấy lại cái cũ đang có trong mây
            t.firestoreCategoryId = viewModel.existingTransaction.getValue().firestoreCategoryId;
        }
        t.categoryId = 0;

        FirebaseFirestore.getInstance()
                .collection("workspaces").document(workspaceId)
                .collection("transactions").document(t.id)
                .set(t)
                .addOnSuccessListener(a -> {
                    ToastHelper.show(this, isEditMode ? "Updated on Cloud!" : "Saved to Cloud!");
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirm.setEnabled(true);
                    ToastHelper.show(this, "Cloud Error: " + e.getMessage());
                });
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this record?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (workspaceId != null && !workspaceId.trim().isEmpty() && !workspaceId.equals("null") && !workspaceId.equals("PERSONAL")) {
                        // XÓA TRÊN CLOUD
                        FirebaseFirestore.getInstance()
                                .collection("workspaces").document(workspaceId)
                                .collection("transactions").document(editTransactionId)
                                .delete()
                                .addOnSuccessListener(a -> {
                                    ToastHelper.show(this, "Deleted from Cloud!");
                                    finish();
                                });
                    } else {
                        viewModel.deleteCurrentTransaction();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadTransactionFromFirestoreForEdit(String id) {
        FirebaseFirestore.getInstance()
                .collection("workspaces").document(workspaceId)
                .collection("transactions").document(id)
                .get()
                .addOnSuccessListener(doc -> {
                    Transaction t = doc.toObject(Transaction.class);
                    if (t != null) {
                        viewModel.existingTransaction.postValue(t);
                        // Bật sáng đúng Tab Thu hoặc Chi
                        viewModel.isExpense.postValue(t.type == 0);
                        // Kéo đúng danh sách Category của loại đó từ trên mây về
                        loadCategoriesForCurrentMode(t.type);
                    }
                });
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
        ((ImageView) findViewById(imgId)).setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.item_description)));
        ((TextView) findViewById(txtId)).setTextColor(ContextCompat.getColor(this, R.color.item_description));
    }

    private void setActivePaymentUI(LinearLayout layout, int imgId, int txtId) {
        layout.setActivated(true);
        ((ImageView) findViewById(imgId)).setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brand_primary)));
        ((TextView) findViewById(txtId)).setTextColor(ContextCompat.getColor(this, R.color.brand_primary));
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

    private void showImageSourceOptions() {
        String[] options = {"Capture a receipt", "Select photo from library"};
        new AlertDialog.Builder(this)
                .setTitle("Scan the receipt")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) dispatchTakePictureIntent();
                    else openGalleryIntent();
                })
                .show();
    }

    private void openGalleryIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 2);
    }

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
                Bundle extras = data.getExtras();
                android.graphics.Bitmap imageBitmap = (android.graphics.Bitmap) extras.get("data");
                processImage(imageBitmap);
            } else if (requestCode == 2) {
                android.net.Uri imageUri = data.getData();
                try {
                    android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    processImage(bitmap);
                } catch (java.io.IOException e) {
                    ToastHelper.show(this, "Failed to load image!");
                }
            }
        }
    }

    private void processImage(android.graphics.Bitmap bitmap) {
        ToastHelper.show(this, "Scanning...");
        btnConfirm.setEnabled(false);

        com.google.mlkit.vision.common.InputImage image =
                com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String ocrText = visionText.getText();
                    if (ocrText == null || ocrText.trim().isEmpty()) {
                        runOnUiThread(() -> {
                            btnConfirm.setEnabled(true);
                            ToastHelper.show(this, "Text not recognized. Try another photo!");
                        });
                        return;
                    }
                    extractDataFromText(ocrText);
                })
                .addOnFailureListener(e -> runOnUiThread(() -> {
                    btnConfirm.setEnabled(true);
                    ToastHelper.show(this, "OCR failed: " + e.getMessage());
                }));
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
                    ToastHelper.show(AddTransactionActivity.this, "Analysis error: " + error);
                });
            }
        });
    }

    private void fillFormAndConfirm(InvoiceParser.ParsedInvoice result) {
        if (result.amount > 0)
            edtAmount.setText(CurrencyFormatter.formatDoubleToVND((double) result.amount));
        if (result.description != null) edtNote.setText(result.description);
        if (result.paymentMethod != null) viewModel.setPayment(result.paymentMethod);

        viewModel.setType(true);

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

                if (!found) {
                    for (Category cat : categories) {
                        if (cat.name.contains("Khác") || cat.name.toLowerCase().contains("other")) {
                            viewModel.selectedCategory.setValue(cat);
                            break;
                        }
                    }
                }

                viewModel.categories.removeObserver(this);
                btnConfirm.setEnabled(true);
                ToastHelper.show(AddTransactionActivity.this, "Scanning finished! Please check again.");
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_out_down);
    }
}