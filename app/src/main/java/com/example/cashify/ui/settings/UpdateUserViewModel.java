package com.example.cashify.ui.settings;

import android.net.Uri;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class UpdateUserViewModel extends ViewModel {
    //Nén ảnh (Compression): Nhắc bạn đó trước khi gọi updateAvatar(uri), nên dùng một thư viện (như Luban hoặc Compressor) để nén cái ảnh lại. Đừng để user up quả ảnh 10MB lên Firebase Storage, vừa tốn băng thông vừa load chậm vãi chưởng.
    //Thông báo (Feedback): Vì việc up ảnh và đổi pass tốn thời gian, cái _isLoading phải được bật lên để hiện ProgressBar, tránh việc user tưởng app bị đơ rồi bấm loạn xạ.
    //Bảo mật: Nhắc lính là không bao giờ lưu Password vào Firestore nhé, chỉ làm việc trực tiếp với FirebaseUser thôi.
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _statusMessage = new MutableLiveData<>();
    public LiveData<String> statusMessage = _statusMessage;

    private final MutableLiveData<Boolean> _updateSuccess = new MutableLiveData<>(false);
    public LiveData<Boolean> updateSuccess = _updateSuccess;

    // ============================================================
    // TODO 1: CẬP NHẬT THÔNG TIN CƠ BẢN (DISPLAY NAME)
    // - Lấy UID hiện tại.
    // - Dùng FirebaseFirestore để update field "displayName" trong collection "users".
    // - Sau khi update Firestore thành công, nhớ update luôn Profile của FirebaseUser
    //   (user.updateProfile(request)) để dữ liệu đồng nhất.
    // ============================================================
    public void updateDisplayName(String newName) {
        // Viết logic update tên ở đây
    }

    // ============================================================
    // TODO 2: UPLOAD VÀ CẬP NHẬT AVATAR
    // - Nhận vào một Uri (đường dẫn ảnh từ máy).
    // - B1: Dùng FirebaseStorage để upload ảnh vào thư mục "avatars/{uid}.jpg".
    // - B2: Sau khi upload thành công, lấy DownloadURL của ảnh.
    // - B3: Lưu cái URL này vào trường "avatarUrl" trong Firestore của User.
    // ============================================================
    public void updateAvatar(Uri imageUri) {
        // Viết logic upload ảnh và lấy link ở đây
    }

    // ============================================================
    // TODO 3: ĐỔI MẬT KHẨU (CHANGE PASSWORD)
    // - Nhận vào oldPassword và newPassword.
    // - Lưu ý: Firebase bắt buộc phải Re-authenticate (xác thực lại) user
    //   bằng oldPassword trước khi cho phép updatePassword.
    // - Nếu thành công: Báo thành công.
    // - Nếu sai mật khẩu cũ: Báo lỗi "Mật khẩu cũ không chính xác".
    // ============================================================
    public void changePassword(String oldPass, String newPass) {
        // Viết logic xác thực lại và đổi mật khẩu ở đây
    }
}