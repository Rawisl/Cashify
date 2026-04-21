package com.example.cashify.utils;

import android.widget.ImageView;

/**
 * Utility bọc thư viện Glide để xử lý ảnh tập trung.
 * Tránh việc mỗi màn hình lại dùng một kiểu load ảnh khác nhau.
 */
public class ImageHelper {
    //Chứa code bọc thư viện (Glide/Picasso) để load ảnh từ URL Firebase về bo tròn làm Avatar.

    //Placeholder: Nhắc bạn làm UI thiết kế sẵn một cái ảnh ic_default_avatar.png (hình người xám xám) bỏ vào folder drawable. Nếu không có cái này, lúc mạng chậm cái chỗ Avatar nó sẽ trắng xóa, nhìn app rất "phèn".
    //Firebase URLs: Link ảnh từ Firebase Storage thường rất dài và có token. Glide xử lý cái này rất ngon, nhưng nhắc bạn dev là khi lưu vào Firestore, hãy lưu cái Download URL (link trực tiếp) chứ đừng lưu cái Path trong Storage nhé.
    //Performance: Glide mặc định sẽ cache ảnh vào bộ nhớ máy. Điều này giúp user lần sau mở app là thấy ảnh ngay, không tốn thêm 1 byte dung lượng 4G nào để load lại.

    // ============================================================
    // TODO 1: LOAD ẢNH AVATAR BO TRÒN (CIRCLE CROP)
    // - Nhận vào: String url, ImageView target.
    // - Sử dụng Glide.with(target.getContext()).load(url).
    // - Áp dụng .circleCrop() để ảnh tự động tròn, không cần chỉnh layout.
    // - Thêm .placeholder(R.drawable.ic_default_avatar) để hiện lúc đang load.
    // - Thêm .error(R.drawable.ic_default_avatar) để hiện khi link ảnh bị lỗi.
    // ============================================================
    public static void loadAvatar(String url, ImageView target) {
        // Viết code Glide ở đây
    }

    // ============================================================
    // TODO 2: LOAD ẢNH COVER HOẶC ẢNH QUỸ (CENTER CROP)
    // - Dành cho các ảnh hình chữ nhật/hình vuông cần bo góc nhẹ.
    // - Sử dụng .centerCrop() và có thể thêm Transform để bo góc (RoundedCorners).
    // ============================================================
    public static void loadRectImage(String url, ImageView target) {
        // Viết code Glide ở đây
    }

    // ============================================================
    // TODO 3: CLEAR CACHE (TÙY CHỌN)
    // - Viết hàm để clear memory/disk cache của Glide khi cần thiết.
    // ============================================================
}
