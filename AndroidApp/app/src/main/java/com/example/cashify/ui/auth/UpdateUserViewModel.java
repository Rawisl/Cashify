package com.example.cashify.ui.auth;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

// Thêm thư viện của sếp vào
import com.example.cashify.utils.CloudinaryHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class UpdateUserViewModel extends ViewModel {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getMessage() { return message; }

    // ĐÃ SỬA: Thêm tham số Context để có quyền đọc File từ Uri
    public void updateProfile(Context context, String newName, Uri imageUri) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            message.setValue("Error: User not found.");
            return;
        }

        isLoading.setValue(true);

        if (imageUri != null) {
            // --- BƯỚC 1: CHUYỂN URI THÀNH FILE VẬT LÝ ---
            File tempFile = new File(context.getCacheDir(), "temp_avatar_" + System.currentTimeMillis() + ".jpg");
            try (InputStream is = context.getContentResolver().openInputStream(imageUri);
                 OutputStream os = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
            } catch (Exception e) {
                isLoading.setValue(false);
                message.setValue("Lỗi đọc file ảnh: " + e.getMessage());
                return;
            }

            // --- BƯỚC 2: UP ẢNH QUA CỔNG BẢO MẬT ---
            CloudinaryHelper.uploadImage(tempFile, new CloudinaryHelper.UploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    Log.d("CLOUDINARY", "Image uploaded successfully: " + imageUrl);
                    tempFile.delete(); // Up xong nhớ xóa file tạm cho sạch rác

                    // --- BƯỚC 3: CẬP NHẬT FIREBASE ---
                    updateFirebaseAuth(user, newName, Uri.parse(imageUrl));
                }

                @Override
                public void onFailure(String error) {
                    // Dùng postValue vì callback này đang nằm ở Background Thread
                    isLoading.postValue(false);
                    message.postValue("Cloudinary Error: " + error);
                    Log.e("CLOUDINARY", "Error: " + error);
                    tempFile.delete(); // Lỗi cũng phải xóa rác
                }
            });
        } else {
            // Nếu không đổi ảnh, chỉ cập nhật tên
            updateFirebaseAuth(user, newName, null);
        }
    }

    private void updateFirebaseAuth(FirebaseUser user, String newName, Uri photoUri) {
        if ((newName == null || newName.trim().isEmpty()) && photoUri == null) {
            isLoading.postValue(false);
            message.postValue("No information has changed.");
            return;
        }

        UserProfileChangeRequest.Builder profileBuilder = new UserProfileChangeRequest.Builder();
        Map<String, Object> firestoreUpdates = new HashMap<>();

        if (newName != null && !newName.trim().isEmpty()) {
            profileBuilder.setDisplayName(newName.trim());
            firestoreUpdates.put("displayName", newName.trim());
        }
        if (photoUri != null) {
            profileBuilder.setPhotoUri(photoUri);
            firestoreUpdates.put("avatarUrl", photoUri.toString());
        }

        user.updateProfile(profileBuilder.build())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(user.getUid())
                                .set(firestoreUpdates, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    isLoading.postValue(false);
                                    message.postValue("Update profile successfully!");
                                })
                                .addOnFailureListener(e -> {
                                    isLoading.postValue(false);
                                    message.postValue("Auth updated but Firestore error: " + e.getMessage());
                                });

                    } else {
                        isLoading.postValue(false);
                        message.postValue("Update error: " + task.getException().getMessage());
                    }
                });
    }

    public void clearMessage() {
        message.setValue(null);
    }
}