package com.example.cashify.ui.workspace;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.model.ChatMessage;
import com.example.cashify.data.model.User;
import com.example.cashify.data.model.Workspace;
import com.example.cashify.data.remote.FirebaseManager;
import com.example.cashify.data.repository.AuthRepositoryImpl;
import com.example.cashify.data.repository.IAuthRepository;
import com.example.cashify.data.repository.IWorkspaceRepo;
import com.example.cashify.data.repository.MediaRepository;
import com.example.cashify.data.repository.RemoteWorkspaceRepoImpl;
import com.example.cashify.ui.transactions.TransactionViewModel;
import com.google.firebase.firestore.AggregateField;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WorkspaceViewModel extends ViewModel {

    private ListenerRegistration workspaceListener;
    private ListenerRegistration chatListener;
    private ListenerRegistration realtimeTriggerListener;

    private final IWorkspaceRepo workspaceRepo;
    private final MediaRepository mediaRepository = new MediaRepository();
    private final IAuthRepository authRepository = new AuthRepositoryImpl();

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<Boolean> _actionSuccess = new MutableLiveData<>(false);
    public LiveData<Boolean> actionSuccess = _actionSuccess;

    private final MutableLiveData<Boolean> _isKickedOut = new MutableLiveData<>(false);
    public LiveData<Boolean> isKickedOut = _isKickedOut;

    private final MutableLiveData<Boolean> isUploading = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsUploading() { return isUploading; }

    private final MutableLiveData<List<TransactionViewModel.HistoryItem>> _transactionsLiveData = new MutableLiveData<>();
    public LiveData<List<TransactionViewModel.HistoryItem>> getTransactionsLiveData() { return _transactionsLiveData; }
    private final List<TransactionViewModel.HistoryItem> currentTransactionList = new ArrayList<>();
    private final MutableLiveData<Long> totalTransactionCountLiveData = new MutableLiveData<>(0L);
    public LiveData<Long> getTotalTransactionCountLiveData() { return totalTransactionCountLiveData; }

    //KHÓA CHỐNG SPAM LOAD MẠNG
    private boolean isFetchingMore = false;

    private final MutableLiveData<Long> totalIncomeLiveData = new MutableLiveData<>(0L);
    public LiveData<Long> getTotalIncomeLiveData() { return totalIncomeLiveData; }

    private final MutableLiveData<Long> totalExpenseLiveData = new MutableLiveData<>(0L);
    public LiveData<Long> getTotalExpenseLiveData() { return totalExpenseLiveData; }

    private final MutableLiveData<Long> actualBalanceLiveData = new MutableLiveData<>(0L);
    public LiveData<Long> getActualBalanceLiveData() { return actualBalanceLiveData; }

    private final MutableLiveData<Integer> unreadNotificationCount = new MutableLiveData<>(0);
    public LiveData<Integer> getUnreadNotificationCount() { return unreadNotificationCount; }

    private final MutableLiveData<Workspace> _workspaceLiveData = new MutableLiveData<>();
    public LiveData<Workspace> getWorkspaceLiveData() { return _workspaceLiveData; }

    private final MutableLiveData<List<User>> _membersLiveData = new MutableLiveData<>();
    public LiveData<List<User>> getMembersLiveData() { return _membersLiveData; }

    private final MutableLiveData<List<ChatMessage>> _chatMessages = new MutableLiveData<>();
    public LiveData<List<ChatMessage>> getChatMessages() { return _chatMessages; }

    private final MutableLiveData<List<String>> pendingImages = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<String>> getPendingImages() { return pendingImages; }

    public MutableLiveData<List<User>> availableFriends = new MutableLiveData<>();

    public WorkspaceViewModel() {
        this.workspaceRepo = new RemoteWorkspaceRepoImpl();
        loadUnreadNotifications();
    }

    public void loadUnreadNotifications() {
        FirebaseManager.getInstance().listenToUnreadNotifications(new FirebaseManager.DataCallback<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                unreadNotificationCount.postValue(count);
            }
            @Override
            public void onError(String message) {}
        });
    }

    public void uploadChatImage(File imageFile) {
        isUploading.setValue(true);
        mediaRepository.uploadImage(imageFile, new MediaRepository.UploadCallback() {
            @Override
            public void onProgress(int percent) {}

            @Override
            public void onSuccess(String imageUrl) {
                isUploading.postValue(false);
                List<String> current = pendingImages.getValue();
                if (current != null) {
                    current.add(imageUrl);
                    pendingImages.postValue(current);
                }
                imageFile.delete();
            }

            @Override
            public void onFailure(String error) {
                isUploading.postValue(false);
                _errorMessage.postValue("Image upload failed: " + error);
                imageFile.delete();
            }
        });
    }

    public void removePendingImage(String imageUrl) {
        List<String> current = pendingImages.getValue();
        if (current != null) {
            current.remove(imageUrl);
            pendingImages.setValue(current);
        }
    }

    public boolean submitChatMessages(String workspaceId, String text) {
        List<String> urls = pendingImages.getValue();
        if (text.isEmpty() && (urls == null || urls.isEmpty())) return false;

        List<String> urlsToSend = new ArrayList<>(urls != null ? urls : new ArrayList<>());
        pendingImages.setValue(new ArrayList<>());
        sendMessagesSequentially(workspaceId, text, urlsToSend, 0);
        return true;
    }

    private void sendMessagesSequentially(String workspaceId, String text, List<String> urls, int index) {
        if (index >= urls.size()) {
            if (urls.isEmpty()) sendChatMessage(workspaceId, text, null);
            return;
        }

        String imgUrl = urls.get(index);
        String msgText = (index == 0) ? text : "";

        sendChatMessage(workspaceId, msgText, imgUrl,
                () -> sendMessagesSequentially(workspaceId, text, urls, index + 1));
    }

    public void loadWorkspaceDetails(String workspaceId) {
        if (workspaceId == null || workspaceId.isEmpty()) return;
        _isLoading.setValue(true);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (workspaceListener != null) workspaceListener.remove();

        String myUid = authRepository.getCurrentUserId();
        if (myUid == null) {
            _errorMessage.setValue("Authentication required.");
            _isLoading.setValue(false);
            return;
        }

        workspaceListener = db.collection("workspaces").document(workspaceId)
                .addSnapshotListener((snapshot, e) -> {
                    _isLoading.postValue(false);
                    if (e != null) {
                        if (e.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) _isKickedOut.postValue(true);
                        return;
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        _isKickedOut.postValue(true);
                        return;
                    }

                    List<String> members = (List<String>) snapshot.get("members");
                    if (members != null && !members.contains(myUid)) {
                        _isKickedOut.postValue(true);
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

    public void createNewWorkspace(String name, String type, String iconName) {
        if (!authRepository.isLoggedIn()) {
            _errorMessage.setValue("Authentication required.");
            return;
        }

        _isLoading.setValue(true);

        FirebaseManager.getInstance().createSharedWorkspace(name, type, iconName, new ArrayList<>(), new FirebaseManager.DataCallback<String>() {
            @Override
            public void onSuccess(String newWorkspaceId) {
                _isLoading.postValue(false);
                _actionSuccess.postValue(true);
            }

            @Override
            public void onError(String message) {
                _isLoading.postValue(false);
                _errorMessage.postValue("Workspace creation failed: " + message);
            }
        });
    }

    public void resetActionStatus() {
        _actionSuccess.setValue(false);
        _errorMessage.setValue(null);
    }

    public void deleteWorkspaceTransaction(String workspaceId, String transactionId) {
        if (workspaceId == null || transactionId == null) {
            _errorMessage.setValue("Missing data to delete transaction.");
            return;
        }

        _isLoading.setValue(true);

        FirebaseManager.getInstance().deleteWorkspaceTransaction(workspaceId, transactionId, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                _isLoading.postValue(false);
                _actionSuccess.postValue(true);
            }

            @Override
            public void onError(String message) {
                _isLoading.postValue(false);
                _errorMessage.postValue("Failed to delete transaction: " + message);
            }
        });
    }

    public void loadWorkspaceTransactions(String workspaceId, boolean isRefresh) {
        if (workspaceId == null || workspaceId.isEmpty()) return;

        // Cắm chốt chặn spam
        if (isFetchingMore) return;
        isFetchingMore = true;

        if (isRefresh) {
            currentTransactionList.clear();
            _isLoading.setValue(true);
        }

        ((RemoteWorkspaceRepoImpl) workspaceRepo).getWorkspaceTransactionsPaginated(
                workspaceId, isRefresh, new IWorkspaceRepo.OnTransactionsLoadedListener() {
                    @Override
                    public void onSuccess(List<TransactionViewModel.HistoryItem> newItems) {
                        _isLoading.postValue(false);
                        isFetchingMore = false; // Mở khóa

                        if (newItems != null && !newItems.isEmpty()) {
                            if (!currentTransactionList.isEmpty()) {
                                TransactionViewModel.HistoryItem firstNewItem = newItems.get(0);
                                if (firstNewItem.getType() == TransactionViewModel.HistoryItem.TYPE_DATE_HEADER) {
                                    String lastDateInOldList = "";
                                    for (int i = currentTransactionList.size() - 1; i >= 0; i--) {
                                        if (currentTransactionList.get(i).getType() == TransactionViewModel.HistoryItem.TYPE_TRANSACTION) {
                                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.ENGLISH);
                                            lastDateInOldList = sdf.format(new java.util.Date(currentTransactionList.get(i).getTransaction().timestamp));
                                            break;
                                        }
                                    }
                                    if (firstNewItem.getDate() != null && firstNewItem.getDate().equals(lastDateInOldList)) {
                                        newItems.remove(0);
                                    }
                                }
                            }
                            currentTransactionList.addAll(newItems);
                            _transactionsLiveData.postValue(new ArrayList<>(currentTransactionList));
                        } else if (isRefresh) {
                            _transactionsLiveData.postValue(new ArrayList<>());
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        _isLoading.postValue(false);
                        isFetchingMore = false; // Mở khóa
                        _errorMessage.postValue("Failed to load transactions: " + e.getMessage());
                    }
                });
    }

    public void calculateWorkspaceBalance(String workspaceId) {
        AggregateField sumField = AggregateField.sum("amount");
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("workspaces").document(workspaceId).collection("transactions")
                .count()
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(snap -> totalTransactionCountLiveData.postValue(snap.getCount()));

        db.collection("workspaces").document(workspaceId).collection("transactions")
                .whereEqualTo("type", 1)
                .aggregate(sumField)
                .get(AggregateSource.SERVER)
                .addOnSuccessListener(incomeSnap -> {
                    Number incomeObj = (Number) incomeSnap.get(sumField);
                    long totalIncome = incomeObj != null ? incomeObj.longValue() : 0L;
                    totalIncomeLiveData.postValue(totalIncome);

                    db.collection("workspaces").document(workspaceId).collection("transactions")
                            .whereEqualTo("type", 0)
                            .aggregate(sumField)
                            .get(AggregateSource.SERVER)
                            .addOnSuccessListener(expenseSnap -> {
                                Number expenseObj = (Number) expenseSnap.get(sumField);
                                long totalExpense = expenseObj != null ? expenseObj.longValue() : 0L;
                                totalExpenseLiveData.postValue(totalExpense);
                                actualBalanceLiveData.postValue(totalIncome - totalExpense);
                            })
                            .addOnFailureListener(e -> Log.e("Cashify", "Index error: ", e));
                })
                .addOnFailureListener(e -> Log.e("Cashify", "Index error: ", e));
    }

    public void startRealtimeSyncTrigger(String workspaceId) {
        if (workspaceId == null || workspaceId.isEmpty()) return;
        if (realtimeTriggerListener != null) return;

        realtimeTriggerListener = FirebaseFirestore.getInstance()
                .collection("workspaces").document(workspaceId).collection("logs")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) return;
                    // Auto refresh khi Quỹ nhóm có cập nhật mới
                    loadWorkspaceTransactions(workspaceId, true);
                    calculateWorkspaceBalance(workspaceId);
                });
    }

    public void listenForChatMessages(String workspaceId) {
        if (workspaceId == null || workspaceId.isEmpty()) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (chatListener != null) chatListener.remove();

        chatListener = db.collection("workspaces").document(workspaceId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        _errorMessage.postValue("Chat sync failed: " + e.getMessage());
                        return;
                    }
                    if (snapshots != null) {
                        List<ChatMessage> msgs = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            ChatMessage msg = doc.toObject(ChatMessage.class);
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

    public void sendChatMessage(String workspaceId, String text, String imageUrl, Runnable onSuccess) {
        if ((text == null || text.trim().isEmpty()) && (imageUrl == null || imageUrl.isEmpty())) {
            if (onSuccess != null) onSuccess.run();
            return;
        }

        FirebaseManager.getInstance().sendWorkspaceMessage(workspaceId, text, imageUrl,
                new FirebaseManager.DataCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        if (onSuccess != null) onSuccess.run();
                    }
                    @Override
                    public void onError(String message) {
                        _errorMessage.postValue("Failed to send message: " + message);
                        if (onSuccess != null) onSuccess.run();
                    }
                });
    }

    public void deleteChatMessage(String workspaceId, String messageId) {
        if (workspaceId == null || messageId == null) return;

        FirebaseManager.getInstance().recallMessage(workspaceId, messageId, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {}

            @Override
            public void onError(String message) {
                _errorMessage.postValue("Failed to unsend message: " + message);
            }
        });
    }

    public void loadAvailableFriends(String workspaceId) {
        String myUid = authRepository.getCurrentUserId();
        if (myUid == null) {
            _errorMessage.setValue("Authentication required.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(myUid).collection("friends").get().addOnSuccessListener(friendSnap -> {
            List<String> friendIds = new ArrayList<>();
            for (DocumentSnapshot d : friendSnap.getDocuments()) friendIds.add(d.getId());

            if (friendIds.isEmpty()) {
                availableFriends.setValue(new ArrayList<>());
                return;
            }

            db.collection("workspaces").document(workspaceId).get().addOnSuccessListener(wsSnap -> {
                List<String> currentMembers = (List<String>) wsSnap.get("members");
                if (currentMembers != null) friendIds.removeAll(currentMembers);

                if (friendIds.isEmpty()) {
                    availableFriends.setValue(new ArrayList<>());
                    return;
                }

                List<com.google.android.gms.tasks.Task<QuerySnapshot>> tasks = new ArrayList<>();

                for (int i = 0; i < friendIds.size(); i += 10) {
                    List<String> chunk = friendIds.subList(i, Math.min(friendIds.size(), i + 10));
                    tasks.add(db.collection("users").whereIn("uid", chunk).get());
                }

                com.google.android.gms.tasks.Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                    List<User> resultList = new ArrayList<>();
                    for (Object res : results) {
                        QuerySnapshot snap = (QuerySnapshot) res;
                        for (DocumentSnapshot uDoc : snap.getDocuments()) {
                            User u = uDoc.toObject(User.class);
                            if(u != null) resultList.add(u);
                        }
                    }
                    availableFriends.setValue(resultList);
                }).addOnFailureListener(e -> {
                    _errorMessage.setValue("Failed to load friends: " + e.getMessage());
                });
            });
        });
    }

    public void addSelectedMembers(String workspaceId, String workspaceName, List<String> selectedUids) {
        if (selectedUids.isEmpty()) return;
        _isLoading.setValue(true);

        FirebaseManager.getInstance().sendWorkspaceInvites(workspaceId, workspaceName, selectedUids, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                _actionSuccess.postValue(true);
                _isLoading.postValue(false);
            }

            @Override
            public void onError(String message) {
                _errorMessage.postValue("Invitation failed: " + message);
                _isLoading.postValue(false);
            }
        });
    }

    public String getCurrentUserId() {
        return authRepository.getCurrentUserId();
    }

    public void leaveWorkspace(String workspaceId) {
        _isLoading.setValue(true);
        FirebaseManager.getInstance().leaveWorkspace(workspaceId, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                _isLoading.postValue(false);
            }

            @Override
            public void onError(String message) {
                _isLoading.postValue(false);
                _errorMessage.postValue("Action failed: " + message);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (chatListener != null) chatListener.remove();
        if (workspaceListener != null) workspaceListener.remove();
        if (realtimeTriggerListener != null) realtimeTriggerListener.remove();
    }

    public static class MemberActionResult {
        public boolean isSuccess;
        public String message;
        public String actionType;

        public MemberActionResult(boolean isSuccess, String message, String actionType) {
            this.isSuccess = isSuccess;
            this.message = message;
            this.actionType = actionType;
        }
    }

    private final MutableLiveData<MemberActionResult> memberActionResult = new MutableLiveData<>();
    public LiveData<MemberActionResult> getMemberActionResult() { return memberActionResult; }
    public void clearMemberActionResult() { memberActionResult.setValue(null); }

    public void kickMember(String workspaceId, String targetUid) {
        FirebaseManager.getInstance().kickMember(workspaceId, targetUid, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                loadWorkspaceMembers(workspaceId);
                memberActionResult.postValue(new MemberActionResult(true, "Member removed!", "KICK"));
            }

            @Override
            public void onError(String message) {
                memberActionResult.postValue(new MemberActionResult(false, message, "KICK"));
            }
        });
    }

    public void transferOwnership(String workspaceId, String targetUid) {
        FirebaseManager.getInstance().transferOwnership(workspaceId, targetUid, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                loadWorkspaceDetails(workspaceId);
                memberActionResult.postValue(new MemberActionResult(true, "You are no longer the owner.", "TRANSFER"));
            }

            @Override
            public void onError(String message) {
                memberActionResult.postValue(new MemberActionResult(false, message, "TRANSFER"));
            }
        });
    }
    public void clearWorkspaceData() {
        // CHẶT ĐỨT MỌI VÒI HÚT DATA CỦA QUỸ CŨ TRƯỚC TIÊN
        if (workspaceListener != null) {
            workspaceListener.remove();
            workspaceListener = null;
        }
        if (chatListener != null) {
            chatListener.remove();
            chatListener = null;
        }
        if (realtimeTriggerListener != null) {
            realtimeTriggerListener.remove();
            realtimeTriggerListener = null;
        }

        // Sau đó mới clear data trên UI như cũ
        _workspaceLiveData.setValue(null);
        _membersLiveData.setValue(new ArrayList<>());
        _transactionsLiveData.setValue(new ArrayList<>());
        currentTransactionList.clear(); // Nhớ clear cả cái mảng đệm chống spam này nữa!
        totalIncomeLiveData.setValue(0L);
        totalExpenseLiveData.setValue(0L);
        actualBalanceLiveData.setValue(0L);
    }
}