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

    private final MutableLiveData<List<com.example.cashify.data.model.ChatMessage>> _chatMessages = new MutableLiveData<>();
    public LiveData<List<com.example.cashify.data.model.ChatMessage>> getChatMessages() { return _chatMessages; }
    private com.google.firebase.firestore.ListenerRegistration chatListener;

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

        // Gọi ĐÚNG hàm lấy chi tiết Quỹ bên Repository
        workspaceRepo.getWorkspaceById(workspaceId, new IWorkspaceRepo.OnWorkspaceDetailLoadedListener() {
            @Override
            public void onSuccess(Workspace workspace) {
                _isLoading.postValue(false);
                // Bơm toàn bộ object Workspace lấy được vào LiveData
                _workspaceLiveData.postValue(workspace);
            }

            @Override
            public void onError(Exception e) {
                _isLoading.postValue(false);
                _errorMessage.postValue("Failed to load transactions:: " + e.getMessage());
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
    // LOGIC TẠO QUỸ MỚI
    // ============================================================
    public void createNewWorkspace(String name, String type, String iconName) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            _errorMessage.setValue("Error: You are not logged in!");
            return;
        }

        _isLoading.setValue(true);

        // Gọi thẳng FirebaseManager để kích hoạt cục WriteBatch tạo danh mục mẫu
        FirebaseManager.getInstance().createSharedWorkspace(name, type, iconName, new java.util.ArrayList<>(), new FirebaseManager.DataCallback<String>() {
            @Override
            public void onSuccess(String newWorkspaceId) {
                String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                        : "unknown_user";

                com.example.cashify.utils.WorkspaceLogHelper.pushLog(
                        newWorkspaceId,
                        uid,
                        com.example.cashify.data.model.LogActionType.CREATE_WORKSPACE,
                        "created workspace."
                );
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
                        _errorMessage.setValue("Listen failed: " + e.getMessage());
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

    public void sendChatMessage(String workspaceId, String text) {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || text.trim().isEmpty()) return;

        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        com.example.cashify.data.model.ChatMessage newMsg = new com.example.cashify.data.model.ChatMessage(
                user.getUid(),
                user.getDisplayName() != null ? user.getDisplayName() : "User",
                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "",
                text.trim(),
                System.currentTimeMillis()
        );

        db.collection("workspaces").document(workspaceId)
                .collection("messages")
                .add(newMsg)
                .addOnFailureListener(e -> _errorMessage.setValue("Send failed: " + e.getMessage()));
    }

    public void deleteChatMessage(String workspaceId, String messageId) {
        if (workspaceId == null || messageId == null) return;
        com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        db.collection("workspaces").document(workspaceId)
                .collection("messages").document(messageId)
                .update(
                        "text", "",
                        "recalled", true
                )
                .addOnFailureListener(e -> _errorMessage.setValue("Message recall failed: " + e.getMessage()));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (chatListener != null) chatListener.remove(); // Tránh tràn RAM
    }
}