package com.example.cashify.ui.workspace;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.model.User;
import com.example.cashify.data.model.Workspace;
import com.example.cashify.data.repository.RemoteWorkspaceRepoImpl;
import com.example.cashify.ui.transactions.TransactionViewModel;
import com.example.cashify.data.repository.IWorkspaceRepo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;

import java.util.List;

public class WorkspaceViewModel extends ViewModel {

    private IWorkspaceRepo workspaceRepo;

    // ============================================================
    // 1. CÁC BIẾN LIVEDATA LƯU TRỮ TRẠNG THÁI
    // ============================================================
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<Boolean> _actionSuccess = new MutableLiveData<>(false);
    public LiveData<Boolean> actionSuccess = _actionSuccess;

    // ============================================================
    // 2. CÁC BIẾN LIVEDATA CUNG CẤP DỮ LIỆU
    // ============================================================
    private final MutableLiveData<Workspace> _workspaceLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<User>> _membersLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<TransactionViewModel.HistoryItem>> _transactionsLiveData = new MutableLiveData<>();

    public WorkspaceViewModel() {

        this.workspaceRepo = new RemoteWorkspaceRepoImpl();
    }

    // ============================================================
    // 3. GETTERS
    // ============================================================
    public LiveData<Workspace> getWorkspaceLiveData() {
        return _workspaceLiveData;
    }

    public LiveData<List<User>> getMembersLiveData() {
        return _membersLiveData;
    }

    public LiveData<List<TransactionViewModel.HistoryItem>> getTransactionsLiveData() {
        return _transactionsLiveData;
    }

    public LiveData<String> getErrorMessage() {
        return _errorMessage;
    }

    // ============================================================
    // 4. CÁC HÀM TẢI DỮ LIỆU TỪ DB (ĐÃ SỬA LỖI LAMBDA)
    // ============================================================

    public void loadWorkspaceDetails(String workspaceId) {
        if (workspaceId == null || workspaceId.isEmpty()) return;
        _isLoading.setValue(true);

        workspaceRepo.getWorkspaceTransactions(workspaceId, new IWorkspaceRepo.OnTransactionsLoadedListener() {
            @Override
            public void onSuccess(List<TransactionViewModel.HistoryItem> transactions) {
                // Đổ vào LiveData để Fragment/Activity nhận được và vẽ lên màn hình
                _transactionsLiveData.postValue(transactions); // ĐÃ FIX: _transactions -> _transactionsLiveData
            }

            @Override
            public void onError(Exception e) {
                _errorMessage.postValue("Failed to load transactions: " + e.getMessage());
            }
        });
    }

    public void loadWorkspaceMembers(String workspaceId) {
        if (workspaceId == null || workspaceId.isEmpty()) return;

        workspaceRepo.getWorkspaceMembers(workspaceId, new IWorkspaceRepo.OnMembersLoadedListener() {
            @Override
            public void onSuccess(List<User> members) {
                _membersLiveData.setValue(members);
            }

            @Override
            public void onError(Exception e) {
                _errorMessage.setValue("Failed to load members: " + e.getMessage());
            }
        });
    }

    public void loadWorkspaceTransactions(String workspaceId) {
        if (workspaceId == null || workspaceId.isEmpty()) return;

        workspaceRepo.getWorkspaceTransactions(workspaceId, new IWorkspaceRepo.OnTransactionsLoadedListener() {
            @Override
            public void onSuccess(List<TransactionViewModel.HistoryItem> transactions) {
                _transactionsLiveData.setValue(transactions);
            }

            @Override
            public void onError(Exception e) {
                _errorMessage.setValue("Failed to load transactions: " + e.getMessage());
            }
        });
    }

    // ============================================================
    // LOGIC TẠO QUỸ MỚI
    // ============================================================
    public void createNewWorkspace(String name, String type, String iconName) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            _errorMessage.setValue("Error: You are not logged in!");
            return;
        }

        _isLoading.setValue(true);
        String uid = auth.getCurrentUser().getUid();

        // 1. Khởi tạo cục data Quỹ mới
        Workspace newWorkspace = new Workspace();
        newWorkspace.setName(name);
        newWorkspace.setType(type); // Tùy thuộc vào model Workspace của ghệ có trường này ko
        newWorkspace.setIconName(iconName != null ? iconName : "ic_other");
        newWorkspace.setOwnerId(uid); // Đánh dấu chủ quyền
        newWorkspace.setBalance(0L);
        newWorkspace.setTotalIncome(0L);
        newWorkspace.setTotalExpense(0L);

        // 2. Tự add bản thân vào danh sách thành viên đầu tiên
        java.util.List<String> initialMembers = new java.util.ArrayList<>();
        initialMembers.add(uid);
        newWorkspace.setMembers(initialMembers);

        // 3. Đẩy xuống Repository để Firebase lo phần còn lại
        workspaceRepo.createWorkspace(newWorkspace, new IWorkspaceRepo.OnActionCompleteListener() {
            @Override
            public void onSuccess() {
                _isLoading.postValue(false);
                _actionSuccess.postValue(true); // Bắn tín hiệu tạo thành công cho UI đóng BottomSheet
            }

            @Override
            public void onError(Exception e) {
                _isLoading.postValue(false);
                _errorMessage.postValue("Could not create fund: " + e.getMessage());
            }
        });
    }

    // ============================================================
    // LOGIC THÊM THÀNH VIÊN VÀO QUỸ BẰNG EMAIL
    // ============================================================
    public void addMemberByEmail(String workspaceId, String email) {
        _isLoading.setValue(true);
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        // Bước 1: Tìm UID của người dùng dựa vào Email
        db.collection("users").whereEqualTo("email", email.trim()).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Nếu không tìm thấy ai xài email này
                    if (queryDocumentSnapshots.isEmpty()) {
                        _errorMessage.postValue("No user found with this email!");
                        _isLoading.postValue(false);
                        return;
                    }

                    // Lấy ra UID của người dùng đó (Lấy người đầu tiên tìm thấy)
                    String newMemberUid = queryDocumentSnapshots.getDocuments().get(0).getId();

                    // Bước 2: Nhét UID đó vào mảng "members" của Quỹ
                    db.collection("workspaces").document(workspaceId)
                            .update("members", FieldValue.arrayUnion(newMemberUid))
                            .addOnSuccessListener(aVoid -> {
                                _actionSuccess.postValue(true);
                                _isLoading.postValue(false);
                                loadWorkspaceMembers(workspaceId);
                            })
                            .addOnFailureListener(e -> {
                                _errorMessage.postValue("Error adding member: " + e.getMessage());
                                _isLoading.postValue(false);
                            });
                })
                .addOnFailureListener(e -> {
                    _errorMessage.postValue("Search error: " + e.getMessage());
                    _isLoading.postValue(false);
                });
    }

    public void resetActionStatus() {
        _actionSuccess.setValue(false);
        _errorMessage.setValue(null);
    }
}