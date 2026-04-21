package com.example.cashify.ui.workspace;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.repository.IWorkspaceRepo;

public class WorkspaceViewModel extends ViewModel {
    //Xử lý logic tạo quỹ, mời thành viên.
    //Tính nguyên tử (Atomicity): Khi tạo Workspace thành công, nhắc bạn đó phải gán cái documentId của Firebase vào trường id của đối tượng Workspace. Nếu không, sau này muốn mời người vào hay xóa quỹ sẽ không biết ID là gì để mà gọi lệnh đâu.
    //Reset Status: Trong DialogFragment, sau khi đóng Dialog thì nhớ gọi viewModel.resetActionStatus(). Nếu không, lần sau mở Dialog lên nó vẫn đang ở trạng thái isSuccess = true và sẽ tự đóng ngay lập tức đấy (lỗi này cực kỳ phổ biến).
    //Validation: Đừng tin tưởng vào UI hoàn toàn. Trong ViewModel này, trước khi gọi Repo, hãy check lại một lần nữa xem workspaceId hoặc email có bị null không cho chắc cú.
    private final IWorkspaceRepo workspaceRepo;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<Boolean> _actionSuccess = new MutableLiveData<>(false);
    public LiveData<Boolean> actionSuccess = _actionSuccess;

    // TODO 1: Inject IWorkspaceRepo vào Constructor (thường là RemoteWorkspaceRepoImpl)
    public WorkspaceViewModel(IWorkspaceRepo repo) {
        this.workspaceRepo = repo;
    }

    // ============================================================
    // TODO 2: LOGIC TẠO QUỸ MỚI (CREATE)
    // - Khởi tạo đối tượng Workspace mới.
    // - Gán ownerId = FirebaseAuth.getInstance().getUid().
    // - Khởi tạo mảng members và thêm ownerId vào mảng này.
    // - Gọi workspaceRepo.createWorkspace(workspace, listener).
    // - Bật/tắt _isLoading và cập nhật _actionSuccess khi xong.
    // ============================================================
    public void createNewWorkspace(String name, String type) {
        // Viết code tạo quỹ ở đây
    }

    // ============================================================
    // TODO 3: LOGIC MỜI THÀNH VIÊN (INVITE)
    // - Gọi workspaceRepo.addMember(workspaceId, memberEmail, listener).
    // - Xử lý các trường hợp: Email không tồn tại, User đã có trong nhóm...
    // - Cập nhật _statusMessage để UI hiển thị thông báo cho user.
    // ============================================================
    public void inviteMember(String workspaceId, String email) {
        // Viết code mời thành viên ở đây
    }

    // ============================================================
    // TODO 4: LẤY CHI TIẾT THÀNH VIÊN (DÙNG CHO DETAIL)
    // - Nếu cần hiện danh sách tên/avatar thành viên trong Quỹ,
    //   hãy viết thêm hàm truy vấn collection "users" dựa trên mảng "members".
    // ============================================================

    // Hàm reset trạng thái để dùng cho lần sau
    public void resetActionStatus() {
        _actionSuccess.setValue(false);
        _errorMessage.setValue(null);
    }
}
