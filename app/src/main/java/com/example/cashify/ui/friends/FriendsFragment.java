package com.example.cashify.ui.friends;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.Nullable;

public class FriendsFragment extends Fragment {
    //Danh sách bạn bè/người quen.
    //Phân loại: Ghệ nên bảo bạn dev làm thêm 2 cái Tab nhỏ trong này: 1 cái là "Bạn bè" (đã đồng ý), 1 cái là "Lời mời" (đang chờ duyệt). Nhìn app sẽ cực kỳ giống các mạng xã hội thực thụ.
    //Real-time: Nhắc lính dùng .addSnapshotListener() trong Firebase để khi có người vừa đồng ý kết bạn là danh sách tự nhảy, không cần user phải vuốt để load lại (Pull-to-refresh).

    //TODO cho logic: acceptWorkspaceInvite(workspaceId) và declineWorkspaceInvite(workspaceId)

    // TODO 1: Khai báo RecyclerView và Adapter (FriendsAdapter) để hiển thị danh sách
    // TODO 2: Khai báo ViewModel (có thể dùng chung AuthViewModel hoặc tạo FriendsViewModel riêng)

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // TODO 3: return inflater.inflate(R.layout.fragment_friends, container, false);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        observeData();
    }

    private void initViews(View view) {
        // ============================================================
        // TODO 4: SETUP RECYCLERVIEW
        // - Cấu hình LayoutManager (LinearLayoutManager).
        // - Gán Adapter cho RecyclerView.
        // ============================================================

        // ============================================================
        // TODO 5: BẮT SỰ KIỆN NÚT "THÊM BẠN" (FAB hoặc Button)
        // - Khi bấm: Hiển thị AddFriendDialog đã tạo ở bước trước.
        // - dialog.show(getChildFragmentManager(), "AddFriendDialog");
        // ============================================================
    }

    private void observeData() {
        // ============================================================
        // TODO 6: LẤY DANH SÁCH BẠN BÈ TỪ FIREBASE
        // - Gọi hàm lấy dữ liệu từ Firebase thông qua ViewModel.
        // - Cập nhật dữ liệu vào Adapter khi có thay đổi (Real-time).
        // - Xử lý trạng thái trống (Empty State): Nếu chưa có bạn thì hiện 1 cái ảnh/text mời gọi.
        // ============================================================
    }

    // ============================================================
    // TODO 7: XỬ LÝ KHI BẤM VÀO MỘT NGƯỜI BẠN
    // - Có thể mở Profile của người đó hoặc xem các Workspace chung.
    // ============================================================
}
