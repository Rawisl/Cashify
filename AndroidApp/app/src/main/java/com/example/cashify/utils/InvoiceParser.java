package com.example.cashify.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InvoiceParser {

    public interface ParseCallback {
        void onSuccess(ParsedInvoice result);
        void onFailure(String error);
    }

    // Bê nguyên cái cấu trúc ParsedInvoice cũ của sếp
    public static class ParsedInvoice {
        public long amount;
        public String description;
        public String category; // Trùng tên field JSON trả về từ C# (category thay vì categoryName)
        public String paymentMethod;
    }

    public static void parse(String ocrText, ParseCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { callback.onFailure("Chưa đăng nhập!"); return; }

        user.getIdToken(true).addOnSuccessListener(getTokenResult -> {
            String token = "Bearer " + getTokenResult.getToken();

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            ApiService.ScanRequest request = new ApiService.ScanRequest(ocrText);

            // Retrofit tự động chạy ngầm, không cần "new Thread" thủ công nữa
            apiService.scanBill(token, request).enqueue(new Callback<ParsedInvoice>() {
                @Override
                public void onResponse(Call<ParsedInvoice> call, Response<ParsedInvoice> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        callback.onSuccess(response.body());
                    } else {
                        callback.onFailure("Lỗi từ AI Server: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<ParsedInvoice> call, Throwable t) {
                    callback.onFailure("Mất kết nối mạng: " + t.getMessage());
                }
            });
        }).addOnFailureListener(e -> callback.onFailure("Lỗi Auth: " + e.getMessage()));
    }
}