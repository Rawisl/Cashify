package com.example.cashify.utils;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.json.JSONObject;
import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;

public class CloudinaryHelper {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    public interface UploadCallback {
        void onProgress(int percent);
        void onSuccess(String imageUrl);
        void onFailure(String error);
    }

    private static final OkHttpClient UPLOAD_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)   // quan trọng cho upload file lớn
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    public static void uploadImage(File imageFile, UploadCallback callback) {
        // CHECK 10MB TRƯỚC KHI LÀM GÌ CẢ
        if (imageFile.length() > MAX_FILE_SIZE) {
            callback.onFailure("Ảnh quá lớn! Vui lòng chọn ảnh dưới 10MB.");
            return;
        }

        File fileToUpload = compressIfNeeded(imageFile);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { callback.onFailure("Chưa đăng nhập!"); return; }

        user.getIdToken(true).addOnSuccessListener(getTokenResult -> {
            String token = "Bearer " + getTokenResult.getToken();
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            apiService.getCloudinarySignature(token).enqueue(new Callback<ApiService.CloudinarySignatureResponse>() {
                @Override
                public void onResponse(Call<ApiService.CloudinarySignatureResponse> call,
                                       retrofit2.Response<ApiService.CloudinarySignatureResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        new Thread(() -> uploadToCloudinary(fileToUpload, response.body(), callback)).start();
                    } else {
                        // THÊM LOG NÀY
                        String errorBody = "";
                        try {
                            if (response.errorBody() != null) errorBody = response.errorBody().string();
                        } catch (Exception ignored) {}
                        Log.e("CLOUDINARY", "Lấy chữ ký thất bại - code: " + response.code() + " body: " + errorBody);
                        callback.onFailure("Không lấy được chữ ký. Mã lỗi: " + response.code() + " - " + errorBody);
                    }
                }
                @Override
                public void onFailure(Call<ApiService.CloudinarySignatureResponse> call, Throwable t) {
                    // ĐỨT MẠNG Ở BƯỚC LẤY CHỮ KÝ
                    callback.onFailure("Mất kết nối mạng. Vui lòng thử lại.");
                }
            });
        }).addOnFailureListener(e -> callback.onFailure("Lỗi xác thực: " + e.getMessage()));
    }

    private static void uploadToCloudinary(File imageFile,
                                           ApiService.CloudinarySignatureResponse signData,
                                           UploadCallback callback) {
        try {
            // WRAP RequestBody để đếm progress
            RequestBody fileBody = RequestBody.create(imageFile, MediaType.parse("image/*"));
            RequestBody countingBody = new CountingRequestBody(fileBody, (bytesWritten, contentLength) -> {
                int percent = (int) (100 * bytesWritten / contentLength);
                callback.onProgress(percent);
            });

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", imageFile.getName(), countingBody)
                    .addFormDataPart("api_key", signData.apiKey)
                    .addFormDataPart("timestamp", String.valueOf(signData.timestamp))
                    .addFormDataPart("signature", signData.signature)
                    .addFormDataPart("folder", signData.folder)
                    .build();

            String uploadUrl = "https://api.cloudinary.com/v1_1/" + signData.cloudName + "/image/upload";
            Request uploadRequest = new Request.Builder().url(uploadUrl).post(requestBody).build();

            try (okhttp3.Response uploadResponse = UPLOAD_CLIENT.newCall(uploadRequest).execute()) {
                if (!uploadResponse.isSuccessful()) {
                    String detail = uploadResponse.body() != null ? uploadResponse.body().string() : "";
                    callback.onFailure("Upload thất bại (lỗi " + uploadResponse.code() + "): " + detail);
                    return;
                }
                JSONObject result = new JSONObject(uploadResponse.body().string());
                callback.onSuccess(result.getString("secure_url"));
            }
        } catch (java.net.SocketTimeoutException e) {
            callback.onFailure("Hết thời gian kết nối. Kiểm tra mạng và thử lại.");
        } catch (java.io.IOException e) {
            // ĐỨT MẠNG TRONG KHI ĐANG UPLOAD
            callback.onFailure("Mất kết nối trong lúc tải ảnh. Vui lòng thử lại.");
        } catch (Exception e) {
            callback.onFailure("Lỗi không xác định: " + e.getMessage());
        }
    }

    private static File compressIfNeeded(File original) {
        // Dưới 2MB thì không cần nén, trả nguyên bản
        if (original.length() < 2 * 1024 * 1024) return original;

        try {
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(original.getAbsolutePath());
            if (bitmap == null) return original; // Decode thất bại thì upload file gốc

            // Tính quality: file càng lớn thì nén càng mạnh
            int quality = original.length() > 7 * 1024 * 1024 ? 60
                    : original.length() > 4 * 1024 * 1024 ? 72
                    : 82;

            File compressed = new File(original.getParent(), "compressed_" + original.getName());
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(compressed)) {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out);
            }
            bitmap.recycle();

            Log.d("CLOUDINARY", "Nén ảnh: " + original.length()/1024 + "KB → "
                    + compressed.length()/1024 + "KB (quality=" + quality + ")");

            return compressed;
        } catch (Exception e) {
            Log.w("CLOUDINARY", "Nén thất bại, dùng file gốc: " + e.getMessage());
            return original; // Nén lỗi thì upload file gốc, không crash
        }
    }
}