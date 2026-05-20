package com.example.cashify.ui.FriendsActivity;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.model.User;
import com.example.cashify.data.remote.FirebaseManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class SocialViewModel extends ViewModel {
    private static final String TAG = "CASHIFY";

    public MutableLiveData<List<User>> friendList = new MutableLiveData<>();
    public MutableLiveData<List<User>> suggestionList = new MutableLiveData<>();
    public MutableLiveData<List<User>> incomingList = new MutableLiveData<>();
    public MutableLiveData<List<User>> sentList = new MutableLiveData<>();

    public MutableLiveData<String> error = new MutableLiveData<>();
    public MutableLiveData<String> toast = new MutableLiveData<>();

    private List<User> allFriendsOriginal = new ArrayList<>();
    private List<User> allSuggestionsOriginal = new ArrayList<>();

    // Các list lưu ID để check validate
    private List<String> myFriendIds = new ArrayList<>();
    private List<String> mySentRequestIds = new ArrayList<>();
    private List<String> myIncomingRequestIds = new ArrayList<>();

    // BA CAMERA GIÁM SÁT REAL-TIME
    private ListenerRegistration friendsListener;
    private ListenerRegistration incomingListener;
    private ListenerRegistration sentListener;

    // ============================================================
    // 1. LẮNG NGHE REAL-TIME DANH SÁCH BẠN BÈ
    // ============================================================
    public void fetchOnlyFriends() {
        String myUid = FirebaseManager.getInstance().getCurrentUserId();
        if (myUid == null) { error.setValue("Chưa đăng nhập!"); return; }

        // Bật luôn camera giám sát 2 list lời mời để check logic lúc tìm bạn qua Email
        fetchRequests();

        if (friendsListener != null) friendsListener.remove();
        friendsListener = FirebaseFirestore.getInstance().collection("users").document(myUid).collection("friends")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null) {
                        List<String> ids = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) ids.add(doc.getId());
                        myFriendIds = ids;

                        if (ids.isEmpty()) {
                            friendList.setValue(new ArrayList<>());
                            allFriendsOriginal.clear();
                        } else {
                            fetchUserProfilesFromIds(ids, friendList, 1); // 1 = Bạn bè
                        }
                        refreshSuggestions();
                    }
                });
    }

    // ============================================================
    // 2. LẮNG NGHE REAL-TIME DANH SÁCH LỜI MỜI
    // ============================================================
    public void fetchRequests() {
        String myUid = FirebaseManager.getInstance().getCurrentUserId();
        if (myUid == null) return;

        // Camera giám sát tab RECEIVED (Nhận)
        if (incomingListener != null) incomingListener.remove();
        incomingListener = FirebaseFirestore.getInstance().collection("users").document(myUid).collection("friend_requests")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null) {
                        List<String> ids = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) ids.add(doc.getId());
                        myIncomingRequestIds = ids;

                        if (ids.isEmpty()) incomingList.setValue(new ArrayList<>());
                        else fetchUserProfilesFromIds(ids, incomingList, 3); // 3 = Lời mời đến
                        refreshSuggestions();
                    }
                });

        // Camera giám sát tab SENT (Gửi đi)
        if (sentListener != null) sentListener.remove();
        sentListener = FirebaseFirestore.getInstance().collection("users").document(myUid).collection("sent_requests")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null) {
                        List<String> ids = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) ids.add(doc.getId());
                        mySentRequestIds = ids;

                        if (ids.isEmpty()) sentList.setValue(new ArrayList<>());
                        else fetchUserProfilesFromIds(ids, sentList, 2); // 2 = Lời mời đi
                        refreshSuggestions();
                    }
                });
    }

    // Hàm dùng chung để kéo thông tin chi tiết của User dựa vào list ID
    private void refreshSentRequests() {
        FirebaseManager.getInstance().getSentRequestIds(new FirebaseManager.DataCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> ids) {
                mySentRequestIds = ids != null ? ids : new ArrayList<>();

                if (mySentRequestIds.isEmpty()) {
                    sentList.postValue(new ArrayList<>());
                } else {
                    fetchUserProfilesFromIds(mySentRequestIds, sentList, 2);
                }
            }

            @Override
            public void onError(String message) {
                error.postValue(message);
            }
        });
    }

    public void refreshSuggestions() {
        FirebaseManager.getInstance().getFriendSuggestions(new FirebaseManager.DataCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                List<User> normalUsers = new ArrayList<>();
                if (users != null) {
                    for (User user : users) {
                        if (user != null && !isRelatedUser(user.getUid())) {
                            user.setFriendStatus(0);
                            normalUsers.add(user);
                        }
                    }
                }
                allSuggestionsOriginal = normalUsers;
                suggestionList.postValue(normalUsers);
            }

            @Override
            public void onError(String message) {
                error.postValue(message);
            }
        });
    }

    public void loadMessageChats() {
        FirebaseManager.getInstance().getFriendMessageChats(new FirebaseManager.DataCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                List<User> chatUsers = new ArrayList<>();
                if (users != null) {
                    for (User user : users) {
                        if (user != null) {
                            user.setFriendStatus(1);
                            chatUsers.add(user);
                        }
                    }
                }
                allFriendsOriginal = new ArrayList<>(chatUsers);
                friendList.postValue(chatUsers);
            }

            @Override
            public void onError(String message) {
                error.postValue(message);
            }
        });
    }

    private void fetchUserProfilesFromIds(List<String> uids, MutableLiveData<List<User>> liveData, int status) {
        FirebaseFirestore.getInstance().collection("users").whereIn("uid", uids).get()
                .addOnSuccessListener(snapshots -> {
                    List<User> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setFriendStatus(status);
                            list.add(user);
                        }
                    }
                    if (status == 1) allFriendsOriginal = new ArrayList<>(list); // Lưu backup để search nội bộ
                    liveData.postValue(list);
                });
    }

    // ============================================================
    // 3. ACTIONS - SỬ DỤNG API C# BACKEND "processFriendAction"
    // ============================================================
    public void searchAndSendRequestByEmail(String email) {
        String myUid = FirebaseManager.getInstance().getCurrentUserId();
        if (email.trim().isEmpty()) { error.postValue("Vui lòng nhập Email!"); return; }

        FirebaseManager.getInstance().searchUserByEmail(email.trim(), new FirebaseManager.DataCallback<String>() {
            @Override
            public void onSuccess(String targetUid) {
                if (targetUid.equals(myUid)) { error.postValue("Bạn không thể tự kết bạn với chính mình!"); return; }
                if (myFriendIds.contains(targetUid)) { error.postValue("Người này đã là bạn bè của bạn rồi!"); return; }
                if (mySentRequestIds.contains(targetUid)) { error.postValue("Đang chờ người này phản hồi rồi!"); return; }
                if (myIncomingRequestIds.contains(targetUid)) { error.postValue("Họ đã gửi lời mời cho bạn, hãy kiểm tra tab Requests!"); return; }

                // GỌI CÁP 1: GỬI LỜI MỜI (request)
                FirebaseManager.getInstance().processFriendAction(targetUid, "request", new FirebaseManager.DataCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        refreshSentRequests();
                        refreshSuggestions();
                        toast.postValue("Gửi lời mời thành công");
                    }

                    @Override
                    public void onError(String message) {
                        toast.postValue("Gửi lời mời thất bại");
                        error.postValue(message);
                    }
                });
            }
            @Override
            public void onError(String message) {
                toast.postValue("Gửi lời mời thất bại");
                error.postValue(message != null && !message.trim().isEmpty()
                        ? message
                        : "Không tìm thấy Email này!");
            }
        });
    }

    public void sendFriendRequest(User user) {
        if (user == null || isRelatedUser(user.getUid())) return;

        FirebaseManager.getInstance().processFriendAction(user.getUid(), "request", new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                refreshSentRequests();
                refreshSuggestions();
                toast.postValue("Gửi lời mời thành công");
            }

            @Override
            public void onError(String message) {
                toast.postValue("Gửi lời mời thất bại");
                error.postValue(message);
            }
        });
    }

    public void acceptFriendRequest(User user) {
        // GỌI CÁP 2: ĐỒNG Ý KẾT BẠN (accept)
        FirebaseManager.getInstance().processFriendAction(user.getUid(), "accept", new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                refreshSuggestions();
                toast.postValue("Đã kết bạn với " + user.getNameToShow() + "!");
            }
            @Override public void onError(String message) { error.postValue(message); }
        });
    }

    public void declineFriendRequest(User user) {
        // GỌI CÁP 3: TỪ CHỐI LỜI MỜI (remove)
        FirebaseManager.getInstance().processFriendAction(user.getUid(), "remove", new FirebaseManager.DataCallback<Void>() {
            @Override public void onSuccess(Void data) { refreshSuggestions(); }
            @Override public void onError(String message) { error.postValue(message); }
        });
    }

    public void cancelFriendRequest(User user) {
        // GỌI CÁP 4: HỦY LỜI MỜI ĐÃ GỬI (remove)
        FirebaseManager.getInstance().processFriendAction(user.getUid(), "remove", new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                refreshSuggestions();
                toast.postValue("Đã thu hồi lời mời");
            }
            @Override public void onError(String message) { error.postValue(message); }
        });
    }

    public void unfriend(User user) {
        // GỌI CÁP 5: HỦY KẾT BẠN (remove)
        FirebaseManager.getInstance().processFriendAction(user.getUid(), "remove", new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                refreshSuggestions();
                toast.postValue("Đã huỷ kết bạn");
            }
            @Override public void onError(String message) { error.postValue(message); }
        });
    }

    // ============================================================
    // 4. SEARCH LOCAL
    // ============================================================
    public void filterFriendsLocal(String query) {
        if (query == null || query.isEmpty()) {
            friendList.setValue(allFriendsOriginal);
            return;
        }
        List<User> filtered = new ArrayList<>();
        String pattern = query.toLowerCase().trim();
        for (User user : allFriendsOriginal) {
            if (user.getDisplayName() != null && user.getDisplayName().toLowerCase().contains(pattern)) {
                filtered.add(user);
            }
        }
        friendList.setValue(filtered);
    }

    public void filterSuggestionsLocal(String query) {
        if (query == null || query.isEmpty()) {
            suggestionList.setValue(allSuggestionsOriginal);
            return;
        }

        List<User> filtered = new ArrayList<>();
        String pattern = query.toLowerCase().trim();
        for (User user : allSuggestionsOriginal) {
            boolean matchesName = user.getDisplayName() != null && user.getDisplayName().toLowerCase().contains(pattern);
            boolean matchesEmail = user.getEmail() != null && user.getEmail().toLowerCase().contains(pattern);
            if (matchesName || matchesEmail) filtered.add(user);
        }
        suggestionList.setValue(filtered);
    }

    private boolean isRelatedUser(String uid) {
        return uid == null
                || myFriendIds.contains(uid)
                || mySentRequestIds.contains(uid)
                || myIncomingRequestIds.contains(uid);
    }

    // CHỐT CHẶN BẢO MẬT: Hủy camera giám sát khi thoát màn hình để không bị rò rỉ RAM
    @Override
    protected void onCleared() {
        super.onCleared();
        if (friendsListener != null) friendsListener.remove();
        if (incomingListener != null) incomingListener.remove();
        if (sentListener != null) sentListener.remove();
    }
}
