package com.example.cashify.ui.auth;

import android.net.Uri;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Map;

public class UpdateUserViewModel extends ViewModel {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getMessage() { return message; }

    public void updateProfile(String newName, Uri imageUri) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            message.setValue("Error: User not found.");
            return;
        }

        isLoading.setValue(true);

        if (imageUri != null) {
            // --- BƯỚC 1: ĐẨY ẢNH LÊN CLOUDINARY ---
            MediaManager.get().upload(imageUri)
                    .unsigned("cashify_avatar")
                    .callback(new UploadCallback() {
                        @Override
                        public void onStart(String requestId) {
                            Log.d("CLOUDINARY", "Starting upload...");
                        }

                        @Override
                        public void onProgress(String requestId, long bytes, long totalBytes) {
                            // Có thể tính % tiến độ ở đây nếu muốn
                        }

                        @Override
                        public void onSuccess(String requestId, Map resultData) {
                            // Lấy link URL ảnh đã được Cloudinary xử lý (link https cho bảo mật)
                            String imageUrl = (String) resultData.get("secure_url");
                            Log.d("CLOUDINARY", "Image uploaded successfully: " + imageUrl);

                            // --- BƯỚC 2: CẬP NHẬT THÔNG TIN VÀO FIREBASE AUTH ---
                            updateFirebaseAuth(user, newName, Uri.parse(imageUrl));
                        }

                        @Override
                        public void onError(String requestId, ErrorInfo error) {
                            isLoading.setValue(false);
                            message.setValue("Cloudinary Error: " + error.getDescription());
                            Log.e("CLOUDINARY", "Error: " + error.getDescription());
                        }

                        @Override
                        public void onReschedule(String requestId, ErrorInfo error) {
                            // Xử lý khi mạng yếu cần đẩy lại sau
                        }
                    }).dispatch();
        } else {
            // Nếu không đổi ảnh, chỉ cập nhật tên
            updateFirebaseAuth(user, newName, null);
        }
    }

    private void updateFirebaseAuth(FirebaseUser user, String newName, Uri photoUri) {
        // Chặn đứng nếu không có gì thay đổi để đỡ tốn tài nguyên mạng
        if ((newName == null || newName.trim().isEmpty()) && photoUri == null) {
            isLoading.setValue(false);
            message.setValue("No information has changed.");
            return;
        }

        UserProfileChangeRequest.Builder profileBuilder = new UserProfileChangeRequest.Builder();

        // 1. Tạo một Map để chuẩn bị tống sang Firestore
        java.util.Map<String, Object> firestoreUpdates = new java.util.HashMap<>();

        if (newName != null && !newName.trim().isEmpty()) {
            profileBuilder.setDisplayName(newName.trim());
            firestoreUpdates.put("displayName", newName.trim()); // Đưa vào mâm cúng Firestore
        }
        if (photoUri != null) {
            profileBuilder.setPhotoUri(photoUri);
            firestoreUpdates.put("avatarUrl", photoUri.toString()); // Đưa vào mâm cúng Firestore
        }

        // 2. Cập nhật bên Firebase Auth trước
        user.updateProfile(profileBuilder.build())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {

                        // 3. Auth thành công thì CẬP NHẬT TIẾP BÊN FIRESTORE
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(user.getUid())
                                // Dùng SetOptions.merge() để nếu có lỡ thiếu field nó tự gộp vào chứ ko xóa các field cũ (như email)
                                .set(firestoreUpdates, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    isLoading.setValue(false);
                                    message.setValue("Update profile successfully!");
                                })
                                .addOnFailureListener(e -> {
                                    isLoading.setValue(false);
                                    message.setValue("Auth updated but Firestore error: " + e.getMessage());
                                });

                    } else {
                        isLoading.setValue(false);
                        message.setValue("Update error: " + task.getException().getMessage());
                    }
                });
    }

    public void clearMessage() {
        message.setValue(null);
    }
}