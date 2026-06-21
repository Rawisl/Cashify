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
import com.example.cashify.utils.DialogHelper;
import com.example.cashify.utils.InvoiceParser;
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

    private String editTransactionId = null;
    private boolean isEditMode = false;
    private String workspaceId = null;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private com.google.mlkit.vision.text.TextRecognizer recognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        viewModel = new ViewModelProvider(this).get(AddTransactionViewModel.class);

        editTransactionId = getIntent().getStringExtra("TRANSACTION_ID");
        isEditMode = (editTransactionId != null);
        workspaceId = getIntent().getStringExtra("WORKSPACE_ID");

        if (isWorkspaceMode()) viewModel.setCurrentWorkspaceId(workspaceId);

        initViews();
        setupObservers();
        setupListeners();

        if (isEditMode) {
            if (isWorkspaceMode()) viewModel.loadWorkspaceTransactionForEdit(workspaceId, editTransactionId);
            else viewModel.loadTransactionForEdit(editTransactionId);
        } else {
            viewModel.fetchCategories(workspaceId, 0); // Tải mặc định tab Chi
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
            tvTitle.setText(isEditMode ? (isWorkspaceMode() ? "Edit Fund Transaction" : "Edit Transaction")
                    : (isWorkspaceMode() ? "Add Fund Transaction" : "Add Transaction"));
            if (btnDelete != null) btnDelete.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        if (btnDelete != null) btnDelete.setOnClickListener(v -> showDeleteConfirmation());

        tabChi.setOnClickListener(v -> {
            if (!isEditMode) {
                viewModel.setType(true);
                viewModel.fetchCategories(workspaceId, 0);
            }
        });

        tabThu.setOnClickListener(v -> {
            if (!isEditMode) {
                viewModel.setType(false);
                viewModel.fetchCategories(workspaceId, 1);
            }
        });

        tvDate.setOnClickListener(v -> {
            Calendar c = viewModel.calendar.getValue();
            new DatePickerDialog(this, (view, y, m, d) -> viewModel.setDate(y, m, d),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        ImageView btnScan = findViewById(R.id.btnScan);
        if (btnScan != null) btnScan.setOnClickListener(v -> showImageSourceOptions());

        btnCash.setOnClickListener(v -> viewModel.setPayment("Cash"));
        btnCard.setOnClickListener(v -> viewModel.setPayment("Card"));
        btnBank.setOnClickListener(v -> viewModel.setPayment("Bank"));

        btnConfirm.setOnClickListener(v -> validateAndSave());
    }

    private void setupObservers() {
        // Observers UI Data
        viewModel.existingTransaction.observe(this, t -> {
            if (t != null) {
                edtAmount.setText(CurrencyFormatter.formatFullVND((double) t.amount));
                edtNote.setText(t.note);
                if (t.paymentMethod != null) viewModel.setPayment(t.paymentMethod);

                rvCategories.post(() -> {
                    if (rvCategories.getAdapter() instanceof CategoryPickerAdapter) {
                        CategoryPickerAdapter adapter = (CategoryPickerAdapter) rvCategories.getAdapter();
                        if (isWorkspaceMode()) adapter.setSelectedByFirestoreId(t.firestoreCategoryId);
                        else adapter.setSelectedById(t.categoryId);
                    }
                });
            }
        });

        viewModel.categories.observe(this, list -> {
            if (rvCategories.getAdapter() == null) {
                CategoryPickerAdapter adapter = new CategoryPickerAdapter(this, list, cat -> viewModel.selectedCategory.setValue(cat));
                rvCategories.setAdapter(adapter);
            } else {
                ((CategoryPickerAdapter) rvCategories.getAdapter()).setNewData(list);
            }

            Transaction t = viewModel.existingTransaction.getValue();
            if (isEditMode && t != null) {
                rvCategories.post(() -> {
                    CategoryPickerAdapter adapter = (CategoryPickerAdapter) rvCategories.getAdapter();
                    if (isWorkspaceMode()) adapter.setSelectedByFirestoreId(t.firestoreCategoryId);
                    else adapter.setSelectedById(t.categoryId);

                    for (Category cat : list) {
                        if (isWorkspaceMode() ? cat.firestoreId.equals(t.firestoreCategoryId) : cat.id == t.categoryId) {
                            viewModel.selectedCategory.setValue(cat);
                            break;
                        }
                    }
                });
            }
        });

        viewModel.isExpense.observe(this, this::updateTabUI);

        viewModel.calendar.observe(this, cal -> {
            tvDate.setText(String.format(Locale.ENGLISH, "%02d/%02d/%04d",
                    cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR)));
        });

        viewModel.selectedPayment.observe(this, method -> {
            resetPaymentUI(btnCash, R.id.imgCash, R.id.tvCash);
            resetPaymentUI(btnCard, R.id.imgCard, R.id.tvCard);
            resetPaymentUI(btnBank, R.id.imgBank, R.id.tvBank);
            if (method.equals("Cash")) setActivePaymentUI(btnCash, R.id.imgCash, R.id.tvCash);
            else if (method.equals("Card")) setActivePaymentUI(btnCard, R.id.imgCard, R.id.tvCard);
            else setActivePaymentUI(btnBank, R.id.imgBank, R.id.tvBank);
        });

        // ---------------------------------------------------------
        // EVENT OBSERVERS NGHE VIEWMODEL BÁO CÁO
        // ---------------------------------------------------------
        viewModel.saveSuccess.observe(this, success -> {
            if (success) {
                if (isEditMode) finish();
                else viewModel.triggerPersonalGamification(CurrencyFormatter.parseVNDToDouble(edtAmount.getText().toString()));
            }
        });

        viewModel.achievementEvent.observe(this, message -> {
            if (message.contains("deleted")) ToastHelper.show(this, message);
            else ToastHelper.showAchievement(getApplicationContext(), message);
        });

        viewModel.closeScreenEvent.observe(this, shouldClose -> {
            if (shouldClose) finish();
        });

        viewModel.errorEvent.observe(this, error -> {
            btnConfirm.setEnabled(true);
            if (btnDelete != null) btnDelete.setEnabled(true);
            ToastHelper.show(this, error);
        });
    }

    private void validateAndSave() {
        double amount = CurrencyFormatter.parseVNDToDouble(edtAmount.getText().toString());
        String note = edtNote.getText().toString().trim();

        if (amount <= 0) {
            edtAmount.setBackgroundResource(R.drawable.bg_input_error);
            ToastHelper.show(this, getString(R.string.error_invalid_money_amount));
            return;
        }

        if (!isEditMode && viewModel.selectedCategory.getValue() == null) {
            ToastHelper.show(this, getString(R.string.error_transaction_empty_category));
            return;
        }

        btnConfirm.setEnabled(false); // Chống spam click

        if (isWorkspaceMode()) {
            viewModel.saveWorkspaceTransaction(workspaceId, amount, note, isEditMode, editTransactionId);
        } else {
            viewModel.saveOrUpdate(String.valueOf((long) amount), note);
        }
    }

    private void showDeleteConfirmation() {
        DialogHelper.showCustomDialog(this, "Delete Transaction", "Are you sure you want to delete this record?",
                "Delete", "Cancel", DialogHelper.DialogType.DANGER, true,
                () -> {
                    if (isWorkspaceMode()) {
                        if (btnDelete != null) btnDelete.setEnabled(false);
                        viewModel.deleteWorkspaceTransaction(workspaceId, editTransactionId);
                    } else {
                        viewModel.deleteCurrentTransaction();
                    }
                }, null);
    }

    // Các hàm UI Helper giữ nguyên
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

    private boolean isWorkspaceMode() {
        return workspaceId != null && !workspaceId.trim().isEmpty() && !workspaceId.equals("null") && !workspaceId.equals("PERSONAL");
    }

    // ==========================================
    // ML KIT OCR
    // ==========================================
    private void showImageSourceOptions() {
        new AlertDialog.Builder(this).setTitle("Scan the receipt")
                .setItems(new String[]{"Capture a receipt", "Select photo from library"}, (dialog, which) -> {
                    if (which == 0) dispatchTakePictureIntent();
                    else openGalleryIntent();
                }).show();
    }

    private void openGalleryIntent() {
        startActivityForResult(new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI), 2);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                processImage((android.graphics.Bitmap) data.getExtras().get("data"));
            } else if (requestCode == 2) {
                try {
                    processImage(android.provider.MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData()));
                } catch (Exception e) { ToastHelper.show(this, "Failed to load image!"); }
            }
        }
    }

    private void processImage(android.graphics.Bitmap bitmap) {
        ToastHelper.show(this, "Scanning...");
        btnConfirm.setEnabled(false);
        recognizer.process(com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener(visionText -> {
                    String ocrText = visionText.getText();
                    if (ocrText == null || ocrText.trim().isEmpty()) {
                        btnConfirm.setEnabled(true);
                        ToastHelper.show(this, "Text not recognized. Try another photo!");
                    } else extractDataFromText(ocrText);
                })
                .addOnFailureListener(e -> {
                    btnConfirm.setEnabled(true);
                    ToastHelper.show(this, "OCR failed: " + e.getMessage());
                });
    }

    private void extractDataFromText(String ocrText) {
        InvoiceParser.parse(ocrText, new InvoiceParser.ParseCallback() {
            @Override
            public void onSuccess(InvoiceParser.ParsedInvoice result) {
                runOnUiThread(() -> {
                    if (result.amount > 0) edtAmount.setText(CurrencyFormatter.formatFullVND((double) result.amount));
                    if (result.description != null) edtNote.setText(result.description);
                    if (result.paymentMethod != null) viewModel.setPayment(result.paymentMethod);
                    viewModel.setType(true);
                    btnConfirm.setEnabled(true);
                    ToastHelper.show(AddTransactionActivity.this, "Scanning finished! Please check again.");
                });
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

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.stay, R.anim.slide_out_down);
    }
}