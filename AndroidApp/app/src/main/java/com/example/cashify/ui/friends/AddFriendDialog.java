package com.example.cashify.ui.friends;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import org.jetbrains.annotations.Nullable;

public class AddFriendDialog extends DialogFragment {
//Nhập email/sdt để tìm và kết bạn trên hệ thống.
    //Ghi chú cực ngắn cho ghệ:
    //Layout: Bảo bạn dev UI làm cái dialog này nhỏ thôi, chỉ cần 1 EditText và 1 nút Search. Nếu tìm thấy thì hiện thêm 1 cái CardView nhỏ chứa thông tin user đó là đẹp.
    //Logic: Nhắc bạn đó là không được tự kết bạn với chính mình (so sánh targetEmail với currentUser.getEmail()).

    // TODO 1: Khai báo interface Listener để báo kết quả về FriendsFragment sau khi thêm thành công

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // TODO 2: return inflater.inflate(R.layout.dialog_add_friend, container, false);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
    }

    private void initViews(View view) {
        // ============================================================
        // TODO 3: XỬ LÝ NÚT "TÌM KIẾM & KẾT BẠN"
        // - Lấy Email từ EditText.
        // - Kiểm tra định dạng Email (không được trống).
        // - Gọi FirebaseManager.searchUserByEmail(email).
        // ============================================================

        // ============================================================
        // TODO 4: XỬ LÝ KẾT QUẢ TÌM KIẾM
        // - IF (User tồn tại):
        //      + Hiển thị thông tin cơ bản (Tên, Avatar) để user xác nhận.
        //      + Bấm "Xác nhận" -> Gọi hàm addFriend(targetUid).
        // - ELSE: Hiện thông báo "Không tìm thấy người dùng này".
        // ============================================================

        // ============================================================
        // TODO 5: ĐÓNG DIALOG
        // - Sau khi gửi lời mời thành công hoặc bấm nút "Hủy" -> dismiss().
        // ============================================================
    }
}
