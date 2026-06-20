package com.example.cashify.data.repository;

import android.util.Log;

import com.example.cashify.data.remote.ApiDto;
import com.example.cashify.utils.ApiClient;
import com.example.cashify.utils.ApiService;
import com.example.cashify.utils.CountingRequestBody;
import com.example.cashify.utils.ImageCompressor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Callback;

public class MediaRepository {

    private static final String TAG = "MediaRepository";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private final ApiService apiService;
    private final OkHttpClient uploadClient;

    public interface UploadCallback {
        void onProgress(int percent);
        void onSuccess(String imageUrl);
        void onFailure(String error);
    }

    public MediaRepository() {
        this.apiService = ApiClient.getClient().create(ApiService.class);
        this.uploadClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    // Hàm public duy nhất để ViewModel tương tác
    public void uploadImage(File imageFile, UploadCallback callback) {
        if (imageFile.length() > MAX_FILE_SIZE) {
            callback.onFailure("Image exceeds the 10MB limit.");
            return;
        }

        File fileToUpload = ImageCompressor.compressIfNeeded(imageFile);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onFailure("Authentication required.");
            return;
        }

        // Luồng thực thi: Get Firebase Token -> Get Cloudinary Signature -> Upload via OkHttp
        user.getIdToken(true)
                .addOnSuccessListener(getTokenResult -> fetchSignatureAndUpload(getTokenResult.getToken(), fileToUpload, callback))
                .addOnFailureListener(e -> callback.onFailure("Failed to retrieve auth token: " + e.getMessage()));
    }

    private void fetchSignatureAndUpload(String token, File fileToUpload, UploadCallback callback) {
        String bearerToken = "Bearer " + token;

        apiService.getCloudinarySignature(bearerToken).enqueue(new Callback<ApiDto.CloudinarySignatureResponse>() {
            @Override
            public void onResponse(Call<ApiDto.CloudinarySignatureResponse> call, retrofit2.Response<ApiDto.CloudinarySignatureResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Chuyển việc upload file nặng sang background thread riêng biệt
                    new Thread(() -> executeCloudinaryUpload(fileToUpload, response.body(), callback)).start();
                } else {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) errorBody = response.errorBody().string();
                    } catch (Exception ignored) {}

                    Log.e(TAG, "Signature request failed. Code: " + response.code() + ", Body: " + errorBody);
                    callback.onFailure("Failed to secure upload signature.");
                }
            }

            @Override
            public void onFailure(Call<ApiDto.CloudinarySignatureResponse> call, Throwable t) {
                callback.onFailure("Network error while securing upload signature.");
            }
        });
    }

    private void executeCloudinaryUpload(File imageFile, ApiDto.CloudinarySignatureResponse signData, UploadCallback callback) {
        try {
            RequestBody fileBody = RequestBody.create(imageFile, MediaType.parse("image/*"));
            CountingRequestBody countingBody = new CountingRequestBody(fileBody, (bytesWritten, contentLength) -> {
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
            Request uploadRequest = new Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .build();

            try (Response uploadResponse = uploadClient.newCall(uploadRequest).execute()) {
                if (!uploadResponse.isSuccessful()) {
                    String detail = uploadResponse.body() != null ? uploadResponse.body().string() : "Unknown error";
                    callback.onFailure("Upload rejected by server (Code " + uploadResponse.code() + ")");
                    return;
                }

                JSONObject result = new JSONObject(uploadResponse.body().string());
                callback.onSuccess(result.getString("secure_url"));
            }

        } catch (SocketTimeoutException e) {
            callback.onFailure("Connection timed out. Please verify your network.");
        } catch (IOException e) {
            callback.onFailure("Network disconnected during upload.");
        } catch (Exception e) {
            callback.onFailure("An unexpected error occurred: " + e.getMessage());
        }
    }
}