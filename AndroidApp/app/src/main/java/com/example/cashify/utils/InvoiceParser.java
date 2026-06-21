package com.example.cashify.utils;

import androidx.annotation.NonNull;

import com.example.cashify.data.remote.ApiDto;
import com.example.cashify.data.repository.AuthRepositoryImpl;
import com.example.cashify.data.repository.IAuthRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InvoiceParser {

    public interface ParseCallback {
        void onSuccess(ParsedInvoice result);
        void onFailure(String error);
    }

    public static class ParsedInvoice {
        public long amount;
        public String description;
        public String category;
        public String paymentMethod;
    }

    // Khởi tạo AuthRepo
    private static final IAuthRepository authRepository = new AuthRepositoryImpl();

    public static void parse(String ocrText, ParseCallback callback) {

        // Gọi AuthRepo lấy token, không cần biết bên dưới là Firebase hay gì
        authRepository.getAccessToken(new IAuthRepository.TokenCallback() {
            @Override
            public void onSuccess(String token) {
                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                ApiDto.ScanRequest request = new ApiDto.ScanRequest(ocrText);

                apiService.scanBill(token, request).enqueue(new Callback<ParsedInvoice>() {
                    @Override
                    public void onResponse(@NonNull Call<ParsedInvoice> call, @NonNull Response<ParsedInvoice> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            callback.onSuccess(response.body());
                        } else {
                            callback.onFailure("AI Server rejected request. Code: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ParsedInvoice> call, @NonNull Throwable t) {
                        callback.onFailure("Network error during parsing: " + t.getMessage());
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure(error);
            }
        });
    }
}