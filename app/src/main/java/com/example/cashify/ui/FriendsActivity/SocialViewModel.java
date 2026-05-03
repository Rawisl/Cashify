package com.example.cashify.ui.FriendsActivity;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.model.User;
import com.example.cashify.data.remote.FirebaseManager;

import java.util.ArrayList;
import java.util.List;

public class SocialViewModel extends ViewModel {
    private static final String TAG = "CASHIFY";

    public MutableLiveData<List<User>> userList = new MutableLiveData<>();
    public MutableLiveData<String> error = new MutableLiveData<>();
    public MutableLiveData<String> toast = new MutableLiveData<>(); // Toast thành công

    private List<User> allUsersList = new ArrayList<>();

    // 3 danh sách status
    private List<String> myFriendIds = new ArrayList<>();
    private List<String> mySentRequestIds = new ArrayList<>();
    private List<String> myIncomingRequestIds = new ArrayList<>();

    // ============================================================
    // FETCH DỮ LIỆU BAN ĐẦU (3 bước song song)
    // ============================================================
    public void fetchUsers() {
        String myUid = FirebaseManager.getInstance().getCurrentUserId();
        Log.e(TAG, "fetchUsers: myUid = " + myUid);
        if (myUid == null) { error.setValue("Chưa đăng nhập!"); return; }

        // Dùng counter để chờ cả 3 request xong rồi mới fetch users
        final int[] doneCount = {0};
        Runnable checkAndFetch = () -> {
            doneCount[0]++;
            if (doneCount[0] == 3) fetchAllUsersFromFirebase(myUid);
        };

        FirebaseManager.getInstance().getFriendIds(myUid, new FirebaseManager.DataCallback<List<String>>() {
            @Override public void onSuccess(List<String> data) { myFriendIds = data; checkAndFetch.run(); }
            @Override public void onError(String message) { myFriendIds = new ArrayList<>(); checkAndFetch.run(); }
        });

        FirebaseManager.getInstance().getSentRequestIds(new FirebaseManager.DataCallback<List<String>>() {
            @Override public void onSuccess(List<String> data) { mySentRequestIds = data; checkAndFetch.run(); }
            @Override public void onError(String message) { mySentRequestIds = new ArrayList<>(); checkAndFetch.run(); }
        });

        FirebaseManager.getInstance().getIncomingRequestIds(new FirebaseManager.DataCallback<List<String>>() {
            @Override public void onSuccess(List<String> data) { myIncomingRequestIds = data; checkAndFetch.run(); }
            @Override public void onError(String message) { myIncomingRequestIds = new ArrayList<>(); checkAndFetch.run(); }
        });
    }

    private void fetchAllUsersFromFirebase(String myUid) {
        FirebaseManager.getInstance().getAllUsers(new FirebaseManager.DataCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> data) {
                List<User> processed = new ArrayList<>();
                for (User user : data) {
                    if (user.getUid() == null || user.getUid().equals(myUid)) continue;

                    String uid = user.getUid();
                    if (myFriendIds.contains(uid)) {
                        user.setFriendStatus(1);       // Đã là bạn
                    } else if (mySentRequestIds.contains(uid)) {
                        user.setFriendStatus(2);       // Mình đã gửi lời mời
                    } else if (myIncomingRequestIds.contains(uid)) {
                        user.setFriendStatus(3);       // Họ gửi lời mời cho mình
                    } else {
                        user.setFriendStatus(0);       // Người lạ
                    }
                    processed.add(user);
                }
                Log.e(TAG, "fetchAllUsers xong: " + processed.size() + " người");
                allUsersList = new ArrayList<>(processed);
                userList.setValue(processed);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "fetchAllUsers lỗi: " + message);
                error.setValue(message);
            }
        });
    }

    // ============================================================
    // ACTIONS
    // ============================================================

    public void sendFriendRequest(User user) {
        FirebaseManager.getInstance().sendFriendRequest(user.getUid(), new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                // Cập nhật local luôn, không cần fetch lại
                user.setFriendStatus(2);
                mySentRequestIds.add(user.getUid());
                refreshList();
                toast.setValue("Đã gửi lời mời tới " + user.getNameToShow());
            }
            @Override
            public void onError(String message) { error.setValue(message); }
        });
    }

    public void cancelFriendRequest(User user) {
        FirebaseManager.getInstance().cancelFriendRequest(user.getUid(), new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                user.setFriendStatus(0);
                mySentRequestIds.remove(user.getUid());
                refreshList();
                toast.setValue("Đã huỷ lời mời");
            }
            @Override
            public void onError(String message) { error.setValue(message); }
        });
    }

    public void acceptFriendRequest(User user) {
        FirebaseManager.getInstance().acceptFriendRequest(user.getUid(), new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                user.setFriendStatus(1);
                myIncomingRequestIds.remove(user.getUid());
                myFriendIds.add(user.getUid());
                refreshList();
                toast.setValue("Đã kết bạn với " + user.getNameToShow() + "!");
            }
            @Override
            public void onError(String message) { error.setValue(message); }
        });
    }

    public void declineFriendRequest(User user) {
        FirebaseManager.getInstance().declineFriendRequest(user.getUid(), new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                user.setFriendStatus(0);
                myIncomingRequestIds.remove(user.getUid());
                refreshList();
            }
            @Override
            public void onError(String message) { error.setValue(message); }
        });
    }

    public void unfriend(User user) {
        FirebaseManager.getInstance().unfriend(user.getUid(), new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                user.setFriendStatus(0);
                myFriendIds.remove(user.getUid());
                refreshList();
                toast.setValue("Đã huỷ kết bạn");
            }
            @Override
            public void onError(String message) { error.setValue(message); }
        });
    }

    // ============================================================
    // SEARCH
    // ============================================================

    public void filterUsers(String query) {
        if (query == null || query.isEmpty()) {
            userList.setValue(allUsersList);
            return;
        }
        List<User> filtered = new ArrayList<>();
        String pattern = query.toLowerCase().trim();
        for (User user : allUsersList) {
            if (user.getDisplayName() != null && user.getDisplayName().toLowerCase().contains(pattern)) {
                filtered.add(user);
            }
        }
        userList.setValue(filtered);
    }

    // Cập nhật lại list sau khi thay đổi status
    private void refreshList() {
        userList.setValue(new ArrayList<>(allUsersList));
    }
}