package com.example.cashify.ui.workspace;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class WorkspaceDetailActivity extends AppCompatActivity {
    //Màn hình riêng cho một cái Quỹ chung. Ở đây sẽ hiện ai đã đóng tiền, ai chưa, ai vừa chi tiêu.
    //Trạng thái đóng tiền: Ở danh sách thành viên, ghệ nên bảo bạn dev thêm một cái label (nhãn) nhỏ: "Đã đóng" (màu xanh) hoặc "Nợ" (màu đỏ) nếu nhóm có quy định đóng quỹ định kỳ. Nhìn vào phát biết ngay ai đang "nợ" quỹ.
    //Phân quyền: Nhắc lính là chỉ có Chủ quỹ (Owner) mới thấy nút "Giải tán quỹ" hoặc "Xóa thành viên". Những người khác chỉ thấy nút "Rời nhóm" thôi.
    //Thống kê chi tiêu: Nếu rảnh, hãy làm thêm một mục "Ai chi nhiều nhất?". Nó sẽ rất hữu ích để biết ai là người thường xuyên đứng ra chi trả các khoản chung cho nhóm.
    private WorkspaceViewModel workspaceViewModel;
    private String workspaceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO 1: setContentView(R.layout.activity_workspace_detail);

        // ============================================================
        // TODO 2: LẤY DỮ LIỆU TỪ INTENT
        // - Lấy workspaceId được truyền từ MainActivity hoặc Side Bar.
        // - Nếu workspaceId null -> finish() Activity ngay để tránh crash.
        // ============================================================

        initViewModel();
        initViews();
        observeViewModel();
    }

    private void initViewModel() {
        workspaceViewModel = new ViewModelProvider(this).get(WorkspaceViewModel.class);
        // TODO 3: Gọi hàm lấy chi tiết Quỹ và danh sách thành viên từ Firebase
    }

    private void initViews() {
        // ============================================================
        // TODO 4: HIỂN THỊ THÔNG TIN TỔNG QUAN
        // - Đổ Tên Quỹ và Tổng số dư hiện tại lên Toolbar/Header.
        // - Hiển thị biểu đồ tròn (PieChart) hoặc Progress nhỏ
        //   về tỉ lệ đóng góp của các thành viên (nếu có thể).
        // ============================================================

        // ============================================================
        // TODO 5: DANH SÁCH THÀNH VIÊN (RecyclerView 1)
        // - Hiển thị Avatar, Tên và số tiền mỗi người đã đóng vào Quỹ.
        // - Có nút "Mời thêm" để mở InviteMemberBottomSheet.
        // ============================================================

        // ============================================================
        // TODO 6: LỊCH SỬ CHI TIÊU CỦA NHÓM (RecyclerView 2)
        // - Hiển thị danh sách giao dịch gần đây của Quỹ này.
        // - Quan trọng: Phải hiện được "Ai" là người đã tạo giao dịch đó.
        // ============================================================
    }

    private void observeViewModel() {
        // ============================================================
        // TODO 7: CẬP NHẬT GIAO DIỆN REAL-TIME
        // - Theo dõi danh sách thành viên: Cập nhật Adapter thành viên.
        // - Theo dõi danh sách giao dịch: Cập nhật Adapter lịch sử.
        // - Xử lý nút "Rời nhóm" hoặc "Giải tán quỹ" (nếu là Owner).
        // ============================================================
    }
}
