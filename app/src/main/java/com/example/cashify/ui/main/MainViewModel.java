package com.example.cashify.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.model.Workspace;
import com.example.cashify.data.repository.IWorkspaceRepo;

import java.util.List;

public class MainViewModel extends ViewModel {
    //Cần một biến MutableLiveData<Workspace> currentWorkspace.
    //
    //Khi user bấm vào Side Bar chọn "Quỹ Nhóm A", MainViewModel sẽ cập nhật cái ID này.
    //
    //Cực kỳ quan trọng: Tất cả các Fragment khác (Home, History) sẽ quan sát cái ID này để tải dữ liệu tương ứng
    //Cơ chế "Loa phóng thanh": Khi MainActivity và các Fragment (Home, History, Budget) cùng sử dụng chung một instance của MainViewModel (dùng requireActivity() khi khởi tạo ViewModel trong Fragment), chúng sẽ luôn nhìn thấy cùng một currentWorkspace.
    //Trải nghiệm người dùng: Nhắc bạn dev UI là khi currentWorkspace thay đổi, nên có một hiệu ứng chuyển cảnh nhẹ hoặc ProgressBar để user thấy app đang tải lại dữ liệu của quỹ mới.
    //Lưu trạng thái: Nếu muốn xịn hơn, hãy lưu ID của Workspace cuối cùng user chọn vào SharedPreferences. Lần sau mở app, nó sẽ tự động vào đúng quỹ đó luôn mà không cần chờ load lại từ đầu.
    private final IWorkspaceRepo workspaceRepo;

    // Danh sách tất cả Workspace để hiển thị lên Side Bar
    private final MutableLiveData<List<Workspace>> _workspaces = new MutableLiveData<>();
    public LiveData<List<Workspace>> workspaces = _workspaces;

    // Workspace đang được chọn (Linh hồn của việc chuyển đổi dữ liệu)
    private final MutableLiveData<Workspace> _currentWorkspace = new MutableLiveData<>();
    public LiveData<Workspace> currentWorkspace = _currentWorkspace;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    // TODO 1: Inject IWorkspaceRepo (Remote hoặc Local tùy cấu hình) vào Constructor
    public MainViewModel(IWorkspaceRepo repo) {
        this.workspaceRepo = repo;
    }

    // ============================================================
    // TODO 2: TẢI DANH SÁCH WORKSPACE
    // - Khi MainActivity khởi tạo, gọi hàm này để lấy tất cả Quỹ của User.
    // - Sau khi lấy được List:
    //      + Cập nhật vào _workspaces để Side Bar hiển thị.
    //      + Mặc định chọn Quỹ đầu tiên (thường là PERSONAL) gán vào _currentWorkspace.
    // ============================================================
    public void loadWorkspaces(String userId) {
        _isLoading.setValue(true);
        // Gọi workspaceRepo.getWorkspaces...
    }

    // ============================================================
    // TODO 3: LOGIC CHUYỂN ĐỔI WORKSPACE
    // - Hàm này được gọi khi User click vào một dòng trên Side Bar.
    // - Cập nhật Workspace mới vào _currentWorkspace.
    // - Các Fragment (Home, History) đang observe cái này sẽ tự động
    //   nhận được thông báo và reload lại Transaction theo ID mới.
    // ============================================================
    public void selectWorkspace(Workspace workspace) {
        if (workspace != null) {
            _currentWorkspace.setValue(workspace);
        }
    }

    // ============================================================
    // TODO 4: LÀM MỚI SỐ DƯ (REFRESH BALANCE)
    // - Viết hàm cập nhật lại số dư của currentWorkspace sau khi
    //   User thêm/xóa giao dịch ở các Fragment con.
    // ============================================================
}
