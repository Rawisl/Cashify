package com.example.cashify.ui.workspace;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.model.User;
import com.example.cashify.data.model.Workspace;
import com.example.cashify.data.remote.FirebaseManager;
import com.example.cashify.data.repository.RemoteWorkspaceRepoImpl;
import com.example.cashify.ui.transactions.TransactionViewModel;
import com.example.cashify.data.repository.IWorkspaceRepo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class WorkspaceViewModel extends ViewModel {

    // Thêm biến này ở đầu file ViewModel để quản lý Listener, tránh rò rỉ RAM
    private ListenerRegistration workspaceListener;

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
    private final MutableLiveData<Boolean> _isKickedOut = new MutableLiveData<>(false);
    public LiveData<Boolean> isKickedOut = _isKickedOut;
    private final MutableLiveData<List<com.example.cashify.data.model.ChatMessage>> _chatMessages = new MutableLiveData<>();
    public LiveData<List<com.example.cashify.data.model.ChatMessage>> getChatMessages() { return _chatMessages; }
    private ListenerRegistration chatListener;

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
    // 4. CÁC HÀM TẢI DỮ LIỆU TỪ DB (ĐÃ NÂNG CẤP LÊN REAL-TIME)
    // ============================================================
    public void loadWorkspaceDetails(String workspaceId) {
        if (workspaceId == null || workspaceId.isEmpty()) return;
        _isLoading.setValue(true);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (workspaceListener != null) workspaceListener.remove();

        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        workspaceListener = db.collection("workspaces").document(workspaceId)
                .addSnapshotListener((snapshot, e) -> {
                    _isLoading.postValue(false);
                    if (e != null) {
                        // Nếu bị mất quyền đọc (Do bị kick)
                        if (e.getCode() == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            _isKickedOut.postValue(true);
                        }
                        return;
                    }

                    // Nếu Quỹ bị Owner xóa (snapshot không tồn tại nữa)
                    if (snapshot == null || !snapshot.exists()) {
                        _isKickedOut.postValue(true);
                        return;
                    }

                    // Nếu Quỹ còn tồn tại, nhưng ID của mình bốc hơi khỏi mảng members
                    List<String> members = (List<String>) snapshot.get("members");
                    if (members != null && !members.contains(myUid)) {
                        _isKickedOut.postValue(true); // KÍCH HOẠT BẪY ĐUỔI KHÁCH!
                        return;
                    }

                    Workspace workspace = snapshot.toObject(Workspace.class);
                    if (workspace != null) {
                        workspace.setId(snapshot.getId());
                        _workspaceLiveData.postValue(workspace);
                    }
                });
    }

    public void loadWorkspaceMembers(String workspaceId) {
        if (workspaceId == null || workspaceId.isEmpty()) return;

        workspaceRepo.getWorkspaceMembers(workspaceId, new IWorkspaceRepo.OnMembersLoadedListener() {
            @Override
            public void onSuccess(List<User> members) {
                _membersLiveData.postValue(members);
            }

            @Override
            public void onError(Exception e) {
                _errorMessage.postValue("Failed to load members: " + e.getMessage());
            }
        });
    }

    public void loadWorkspaceTransactions(String workspaceId) {
        if (workspaceId == null || workspaceId.isEmpty()) return;

        workspaceRepo.getWorkspaceTransactions(workspaceId, new IWorkspaceRepo.OnTransactionsLoadedListener() {
            @Override
            public void onSuccess(List<TransactionViewModel.HistoryItem> transactions) {
                _transactionsLiveData.postValue(transactions);
            }

            @Override
            public void onError(Exception e) {
                _errorMessage.postValue("Failed to load transactions: " + e.getMessage());
            }
        });
    }

    // ============================================================
    // LOGIC TẠO QUỸ MỚI (ĐÃ CHUYỂN QUA C# BACKEND CQRS)
    // ============================================================
    public void createNewWorkspace(String name, String type, String iconName) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            _errorMessage.setValue("Error: You are not logged in!");
            return;
        }

        _isLoading.setValue(true);

        FirebaseManager.getInstance().createSharedWorkspace(name, type, iconName, new java.util.ArrayList<>(), new FirebaseManager.DataCallback<String>() {
            @Override
            public void onSuccess(String newWorkspaceId) {
                // KHÔNG gọi WorkspaceLogHelper.pushLog nữa! C# Backend đã lo trọn gói vụ ghi Audit Log.
                _isLoading.postValue(false);
                _actionSuccess.postValue(true); // Bắn tín hiệu tạo thành công cho UI đóng BottomSheet
            }

            @Override
            public void onError(String message) {
                _isLoading.postValue(false);
                _errorMessage.postValue("Could not create fund: " + message);
            }
        });
    }

    public void resetActionStatus() {
        _actionSuccess.setValue(false);
        _errorMessage.setValue(null);
    }

    public void listenForChatMessages(String workspaceId) {
        if (workspaceId == null || workspaceId.isEmpty()) return;
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        // Hủy listener cũ nếu có
        if (chatListener != null) chatListener.remove();

        chatListener = db.collection("workspaces").document(workspaceId)
                .collection("messages")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        _errorMessage.postValue("Listen failed: " + e.getMessage());
                        return;
                    }
                    if (snapshots != null) {
                        List<com.example.cashify.data.model.ChatMessage> msgs = new java.util.ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                            com.example.cashify.data.model.ChatMessage msg = doc.toObject(com.example.cashify.data.model.ChatMessage.class);
                            if (msg != null) {
                                msg.setMessageId(doc.getId());
                                msgs.add(msg);
                            }
                        }
                        _chatMessages.setValue(msgs);
                    }
                });
    }

    public void sendChatMessage(String workspaceId, String text, String imageUrl) {
        sendChatMessage(workspaceId, text, imageUrl, null);
    }

    // Thêm overload này vào WorkspaceViewModel
    public void sendChatMessage(String workspaceId, String text, String imageUrl,
                                Runnable onSuccess) {
        if ((text == null || text.trim().isEmpty()) && (imageUrl == null || imageUrl.isEmpty())) {
            if (onSuccess != null) onSuccess.run();
            return;
        }
        FirebaseManager.getInstance().sendWorkspaceMessage(workspaceId, text, imageUrl,
                new FirebaseManager.DataCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        if (onSuccess != null) onSuccess.run(); // ← báo xong để gửi tiếp
                    }
                    @Override
                    public void onError(String message) {
                        _errorMessage.postValue("Failed to send message: " + message);
                        if (onSuccess != null) onSuccess.run(); // vẫn tiếp tục gửi ảnh tiếp theo
                    }
                });
    }

    public void deleteChatMessage(String workspaceId, String messageId) {
        if (workspaceId == null || messageId == null) return;

        FirebaseManager.getInstance().recallMessage(workspaceId, messageId, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                // Listener tự cập nhật UI
            }

            @Override
            public void onError(String message) {
                _errorMessage.postValue("Failed to unsend message: " + message);
            }
        });
    }

    // Đừng quên dọn rác khi đóng màn hình
    @Override
    protected void onCleared() {
        super.onCleared();
        if (chatListener != null) chatListener.remove();
        if (workspaceListener != null) workspaceListener.remove(); // Hủy nghe khi tắt App
    }

    // ============================================================
    // LOGIC INVITE MULTIPLE FRIENDS
    // ============================================================
    public MutableLiveData<List<User>> availableFriends = new MutableLiveData<>();

    public void loadAvailableFriends(String workspaceId) {
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        // 1. Lấy list bạn bè của mình
        db.collection("users").document(myUid).collection("friends").get().addOnSuccessListener(friendSnap -> {
            List<String> friendIds = new ArrayList<>();
            for (com.google.firebase.firestore.DocumentSnapshot d : friendSnap.getDocuments()) friendIds.add(d.getId());
            if (friendIds.isEmpty()) { availableFriends.setValue(new ArrayList<>()); return; }

            // 2. Lấy list member hiện tại của Quỹ để loại trừ
            db.collection("workspaces").document(workspaceId).get().addOnSuccessListener(wsSnap -> {
                List<String> currentMembers = (List<String>) wsSnap.get("members");
                if (currentMembers != null) friendIds.removeAll(currentMembers); // Trừ đi những người đã ở trong quỹ
                if (friendIds.isEmpty()) { availableFriends.setValue(new ArrayList<>()); return; }

                // 3. Lấy Profile của những người còn lại
                db.collection("users").get().addOnSuccessListener(usersSnap -> {
                    List<User> result = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot uDoc : usersSnap) {
                        if (friendIds.contains(uDoc.getId())) {
                            User u = uDoc.toObject(User.class);
                            if(u != null) result.add(u);
                        }
                    }
                    availableFriends.setValue(result);
                });
            });
        });
    }

    public void addSelectedMembers(String workspaceId, String workspaceName, List<String> selectedUids) {
        if (selectedUids.isEmpty()) return;
        _isLoading.setValue(true);

        // BẮN THẲNG QUA FIREBASE MANAGER (ĐỂ NÓ GỌI C# BACKEND)
        FirebaseManager.getInstance().sendWorkspaceInvites(workspaceId, workspaceName, selectedUids, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                _actionSuccess.postValue(true);
                _isLoading.postValue(false);
            }

            @Override
            public void onError(String message) {
                _errorMessage.postValue("Invite failed: " + message);
                _isLoading.postValue(false);
            }
        });
    }
    // ============================================================
    // LOGIC XÓA GIAO DỊCH QUỸ NHÓM (CALL C# BACKEND)
    // ============================================================
    public void deleteWorkspaceTransaction(String workspaceId, String transactionId) {
        if (workspaceId == null || transactionId == null) {
            _errorMessage.setValue("Thiếu thông tin để xóa giao dịch");
            return;
        }

        _isLoading.setValue(true);

        // Gọi thẳng qua FirebaseManager (Nơi chứa API Retrofit C# sếp đã viết)
        FirebaseManager.getInstance().deleteWorkspaceTransaction(workspaceId, transactionId, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                _isLoading.postValue(false);
                _actionSuccess.postValue(true); // Báo UI xóa thành công để đóng Dialog/Activity
            }

            @Override
            public void onError(String message) {
                _isLoading.postValue(false);
                _errorMessage.postValue("Lỗi xóa giao dịch: " + message);
            }
        });
    }
}