package com.example.cashify.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.model.User;

public class SettingsViewModel extends ViewModel {
    //(Bộ não xử lý hiện Avatar, Tên, Logout)
    //Real-time là vua: Tại sao tớ khuyên dùng addSnapshotListener? Vì nếu bạn dev UI làm thêm cái Dialog đổi tên ở chỗ khác, user vừa bấm "Lưu" xong, quay lại tab Settings là thấy tên mới luôn mà không cần load lại. Cảm giác app rất "mượt".
    //Xử lý Logout: Nhắc bạn dev UI là khi isLoggedOut về true, ngoài việc chuyển sang LoginActivity, phải dùng Flag Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK để xóa sạch lịch sử các màn hình trước đó. Tránh việc user bấm nút Back mà lại quay ngược vào được app khi đã logout.
    private final MutableLiveData<User> _userData = new MutableLiveData<>();
    public LiveData<User> userData = _userData;

    private final MutableLiveData<Boolean> _isLoggedOut = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoggedOut = _isLoggedOut;

    public SettingsViewModel() {
        // TODO 1: Khởi tạo Firebase instances cần thiết
        loadUserProfile();
    }

    // ============================================================
    // TODO 2: TẢI THÔNG TIN NGƯỜI DÙNG
    // - Lấy UID từ FirebaseAuth.getCurrentUser().
    // - Truy cập Firestore collection "users" document(uid).
    // - Dùng .addSnapshotListener() để nếu user có đổi tên ở màn hình khác,
    //   màn hình Settings cũng tự cập nhật theo (Real-time).
    // - Update dữ liệu vào _userData.
    // ============================================================
    public void loadUserProfile() {
        // Viết logic lấy data real-time ở đây
    }

    // ============================================================
    // TODO 3: LOGIC ĐĂNG XUẤT (LOGOUT)
    // - Gọi FirebaseAuth.getInstance().signOut().
    // - Xóa trắng các thông tin cache nếu cần (SharedPreferences).
    // - Set _isLoggedOut = true để Fragment biết mà "đá" user về Login.
    // ============================================================
    public void logout() {
        // Viết code đăng xuất ở đây
    }

    // ============================================================
    // TODO 4: XỬ LÝ QUYỀN RIÊNG TƯ (TÙY CHỌN)
    // - Có thể thêm logic ẩn/hiện số dư ở đây nếu ghệ muốn
    //   làm tính năng "con mắt" che số tiền đi cho riêng tư.
    // ============================================================
}
