package com.example.cashify.ui.workspace;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import org.jetbrains.annotations.Nullable;

public class CreateWorkspaceDialog extends DialogFragment {
    //Một cái pop-up hiện lên khi user bấm dấu (+) trên Side Bar để nhập tên Quỹ mới.
    //Trải nghiệm người dùng (UX): Bảo bạn làm Layout thiết kế cái Dialog này có bo góc cho đẹp. Nên có một cái Switch hoặc RadioGroup nếu ghệ muốn cho người dùng chọn thêm icon đại diện cho Quỹ ngay lúc tạo (ví dụ: icon hình cái ví, hình vương miện, hình ăn uống...).
    //Logic ngầm: Nhắc bạn dev Firebase là khi tạo một Workspace mới, nhớ tự động thêm currentUserId vào mảng members của Workspace đó, nếu không là tạo xong chính mình cũng không thấy quỹ đó đâu đấy!
    //Tự động chuyển: Xịn hơn nữa là sau khi tạo thành công và đóng Dialog, ghệ ra lệnh cho app tự động chuyển sang Workspace mới đó luôn để user bắt đầu nhập giao dịch ngay.
    private WorkspaceViewModel workspaceViewModel;

    // ============================================================
    // TODO 1: KHỞI TẠO VIEWMODEL
    // - Dùng ViewModelProvider để lấy instance của WorkspaceViewModel.
    // - Lưu ý: Nếu muốn Side Bar ở MainActivity tự cập nhật ngay khi tạo xong,
    //   có thể cân nhắc dùng shared ViewModel (activityViewModel).
    // ============================================================

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // TODO 2: return inflater.inflate(R.layout.dialog_create_workspace, container, false);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        observeViewModel();
    }

    private void initViews(View view) {
        // ============================================================
        // TODO 3: XỬ LÝ NÚT "TẠO QUỸ"
        // - Lấy tên quỹ từ EditText.
        // - Kiểm tra: Không được để trống tên quỹ.
        // - Mặc định: type sẽ là "GROUP" (vì PERSONAL thường chỉ có một cái duy nhất).
        // - Gọi workspaceViewModel.createNewWorkspace(name, "GROUP").
        // ============================================================

        // ============================================================
        // TODO 4: NÚT "HỦY"
        // - Đóng dialog ngay lập tức: dismiss().
        // ============================================================
    }

    private void observeViewModel() {
        // ============================================================
        // TODO 5: THEO DÕI TRẠNG THÁI TẠO
        // - isLoading: Hiện ProgressBar trên dialog để báo đang tạo trên Firebase.
        // - isSuccess: Nếu TRUE -> Hiện Toast "Tạo quỹ thành công" và gọi dismiss().
        // - errorMessage: Nếu lỗi (trùng tên, lỗi mạng...) -> Hiện thông báo cho user.
        // ============================================================
    }
}
