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
    public MutableLiveData<List<User>> incomingList = new MutableLiveData<>();
    public MutableLiveData<List<User>> sentList = new MutableLiveData<>();

    public MutableLiveData<String> error = new MutableLiveData<>();
    public MutableLiveData<String> toast = new MutableLiveData<>();

    private List<User> allFriendsOriginal = new ArrayList<>();

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
                    }
                });
    }

    // Hàm dùng chung để kéo thông tin chi tiết của User dựa vào list ID
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
                    liveData.setValue(list);
                });
    }

    // ============================================================
    // 3. ACTIONS - Không cần gọi load lại data nữa vì Listener đã tự làm
    // ============================================================
    public void searchAndSendRequestByEmail(String email) {
        String myUid = FirebaseManager.getInstance().getCurrentUserId();
        if (email.trim().isEmpty()) { error.setValue("Vui lòng nhập Email!"); return; }

        FirebaseManager.getInstance().searchUserByEmail(email.trim(), new FirebaseManager.DataCallback<String>() {
            @Override
            public void onSuccess(String targetUid) {
                if (targetUid.equals(myUid)) { error.setValue("Bạn không thể tự kết bạn với chính mình!"); return; }
                if (myFriendIds.contains(targetUid)) { error.setValue("Người này đã là bạn bè của bạn rồi!"); return; }
                if (mySentRequestIds.contains(targetUid)) { error.setValue("Đang chờ người này phản hồi rồi!"); return; }
                if (myIncomingRequestIds.contains(targetUid)) { error.setValue("Họ đã gửi lời mời cho bạn, hãy kiểm tra tab Requests!"); return; }

                FirebaseManager.getInstance().sendFriendRequest(targetUid, new FirebaseManager.DataCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) { toast.setValue("Đã gửi lời mời!"); }
                    @Override
                    public void onError(String message) { error.setValue(message); }
                });
            }
            @Override
            public void onError(String message) { error.setValue("Không tìm thấy Email này!"); }
        });
    }

    public void acceptFriendRequest(User user) {
        FirebaseManager.getInstance().acceptFriendRequest(user.getUid(), new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) { toast.setValue("Đã kết bạn với " + user.getNameToShow() + "!"); }
            @Override public void onError(String message) { error.setValue(message); }
        });
    }

    public void declineFriendRequest(User user) {
        FirebaseManager.getInstance().declineFriendRequest(user.getUid(), new FirebaseManager.DataCallback<Void>() {
            @Override public void onSuccess(Void data) {}
            @Override public void onError(String message) { error.setValue(message); }
        });
    }

    public void cancelFriendRequest(User user) {
        FirebaseManager.getInstance().cancelFriendRequest(user.getUid(), new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) { toast.setValue("Đã thu hồi lời mời"); }
            @Override public void onError(String message) { error.setValue(message); }
        });
    }

    public void unfriend(User user) {
        FirebaseManager.getInstance().unfriend(user.getUid(), new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) { toast.setValue("Đã huỷ kết bạn"); }
            @Override public void onError(String message) { error.setValue(message); }
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

    // CHỐT CHẶN BẢO MẬT: Hủy camera giám sát khi thoát màn hình để không bị rò rỉ RAM
    @Override
    protected void onCleared() {
        super.onCleared();
        if (friendsListener != null) friendsListener.remove();
        if (incomingListener != null) incomingListener.remove();
        if (sentListener != null) sentListener.remove();
    }
}