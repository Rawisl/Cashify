package com.example.cashify.utils;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.json.JSONObject;
import java.io.File;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;

public class CloudinaryHelper {

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String error);
    }

    // Vẫn giữ 1 biến OkHttpClient cho vụ Upload thẳng ảnh lên Cloudinary
    private static final OkHttpClient UPLOAD_CLIENT = new OkHttpClient();

    public static void uploadImage(File imageFile, UploadCallback callback) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { callback.onFailure("Chưa đăng nhập!"); return; }

        user.getIdToken(true).addOnSuccessListener(getTokenResult -> {
            String token = "Bearer " + getTokenResult.getToken();

            // --- BƯỚC 1: LẤY CHỮ KÝ BẰNG RETROFIT ---
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            apiService.getCloudinarySignature(token).enqueue(new Callback<ApiService.CloudinarySignatureResponse>() {
                @Override
                public void onResponse(Call<ApiService.CloudinarySignatureResponse> call, retrofit2.Response<ApiService.CloudinarySignatureResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        // --- BƯỚC 2: UP ẢNH BẰNG OKHTTP TRONG THREAD ---
                        new Thread(() -> uploadToCloudinary(imageFile, response.body(), callback)).start();
                    } else {
                        callback.onFailure("Không lấy được chữ ký, Mã lỗi: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<ApiService.CloudinarySignatureResponse> call, Throwable t) {
                    callback.onFailure("Lỗi kết nối Server: " + t.getMessage());
                }
            });
        }).addOnFailureListener(e -> callback.onFailure("Lỗi Auth: " + e.getMessage()));
    }

    private static void uploadToCloudinary(File imageFile, ApiService.CloudinarySignatureResponse signData, UploadCallback callback) {
        try {
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", imageFile.getName(), RequestBody.create(imageFile, MediaType.parse("image/*")))
                    .addFormDataPart("api_key", signData.apiKey)
                    .addFormDataPart("timestamp", String.valueOf(signData.timestamp))
                    .addFormDataPart("signature", signData.signature)
                    .addFormDataPart("folder", signData.folder)
                    .build();

            String uploadUrl = "https://api.cloudinary.com/v1_1/" + signData.cloudName + "/image/upload";
            Request uploadRequest = new Request.Builder().url(uploadUrl).post(requestBody).build();

            try (okhttp3.Response uploadResponse = UPLOAD_CLIENT.newCall(uploadRequest).execute()) {
                if (!uploadResponse.isSuccessful()) {
                    Log.e("CLOUDINARY_BUG", "Chi tiết lỗi: " + uploadResponse.body().string());
                    throw new Exception("Cloudinary từ chối upload!");
                }
                JSONObject uploadResult = new JSONObject(uploadResponse.body().string());
                callback.onSuccess(uploadResult.getString("secure_url"));
            }
        } catch (Exception e) {
            callback.onFailure("Upload ảnh thất bại: " + e.getMessage());
        }
    }
}