package com.example.cashify.ui.FriendsActivity;

import androidx.lifecycle.LiveData;
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
    private static final String TAG = "SocialNewsfeedViewModel";

    // ============================================================
    // 1. STATE LIVEDATA (Internal Mutable, External Immutable)
    // ============================================================
    private final MutableLiveData<List<User>> _friendList = new MutableLiveData<>();
    public final LiveData<List<User>> friendList = _friendList;

    private final MutableLiveData<List<User>> _suggestionList = new MutableLiveData<>();
    public final LiveData<List<User>> suggestionList = _suggestionList;

    private final MutableLiveData<List<User>> _incomingList = new MutableLiveData<>();
    public final LiveData<List<User>> incomingList = _incomingList;

    private final MutableLiveData<List<User>> _sentList = new MutableLiveData<>();
    public final LiveData<List<User>> sentList = _sentList;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public final LiveData<String> error = _error;

    private final MutableLiveData<String> _toast = new MutableLiveData<>();
    public final LiveData<String> toast = _toast;

    // Cache lists for local filtering and validation
    private List<User> allFriendsOriginal = new ArrayList<>();
    private List<User> allSuggestionsOriginal = new ArrayList<>();
    private List<String> myFriendIds = new ArrayList<>();
    private List<String> mySentRequestIds = new ArrayList<>();
    private List<String> myIncomingRequestIds = new ArrayList<>();

    // Real-time Snapshot Listeners
    private ListenerRegistration friendsListener;
    private ListenerRegistration incomingListener;
    private ListenerRegistration sentListener;

    // ============================================================
    // 2. REAL-TIME LISTENERS
    // ============================================================
    public void fetchOnlyFriends() {
        String myUid = FirebaseManager.getInstance().getCurrentUserId();
        if (myUid == null) {
            _error.setValue("Authentication required.");
            return;
        }

        // Initialize request listeners simultaneously to ensure validation lists are up-to-date
        fetchRequests();

        if (friendsListener != null) friendsListener.remove();
        friendsListener = FirebaseFirestore.getInstance()
                .collection("users").document(myUid).collection("friends")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null) {
                        List<String> ids = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) ids.add(doc.getId());
                        myFriendIds = ids;

                        if (ids.isEmpty()) {
                            _friendList.setValue(new ArrayList<>());
                            allFriendsOriginal.clear();
                        } else {
                            fetchUserProfilesFromIds(ids, _friendList, 1); // 1 = Friends
                        }
                        refreshSuggestions();
                    }
                });
    }

    public void fetchRequests() {
        String myUid = FirebaseManager.getInstance().getCurrentUserId();
        if (myUid == null) return;

        // Monitor RECEIVED requests tab
        if (incomingListener != null) incomingListener.remove();
        incomingListener = FirebaseFirestore.getInstance()
                .collection("users").document(myUid).collection("friend_requests")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null) {
                        List<String> ids = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) ids.add(doc.getId());
                        myIncomingRequestIds = ids;

                        if (ids.isEmpty()) _incomingList.setValue(new ArrayList<>());
                        else fetchUserProfilesFromIds(ids, _incomingList, 3); // 3 = Incoming
                        refreshSuggestions();
                    }
                });

        // Monitor SENT requests tab
        if (sentListener != null) sentListener.remove();
        sentListener = FirebaseFirestore.getInstance()
                .collection("users").document(myUid).collection("sent_requests")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null) {
                        List<String> ids = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) ids.add(doc.getId());
                        mySentRequestIds = ids;

                        if (ids.isEmpty()) _sentList.setValue(new ArrayList<>());
                        else fetchUserProfilesFromIds(ids, _sentList, 2); // 2 = Sent
                        refreshSuggestions();
                    }
                });
    }

    private void refreshSentRequests() {
        FirebaseManager.getInstance().getSentRequestIds(new FirebaseManager.DataCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> ids) {
                mySentRequestIds = ids != null ? ids : new ArrayList<>();

                if (mySentRequestIds.isEmpty()) {
                    _sentList.postValue(new ArrayList<>());
                } else {
                    fetchUserProfilesFromIds(mySentRequestIds, _sentList, 2);
                }
            }

            @Override
            public void onError(String message) {
                _error.postValue(message);
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
                            user.setFriendStatus(0); // 0 = Not friends
                            normalUsers.add(user);
                        }
                    }
                }
                allSuggestionsOriginal = normalUsers;
                _suggestionList.postValue(normalUsers);
            }

            @Override
            public void onError(String message) {
                _error.postValue(message);
            }
        });
    }

    private void fetchUserProfilesFromIds(List<String> uids, MutableLiveData<List<User>> liveData, int status) {
        // Firebase Firestore constraint: 'whereIn' supports a maximum of 30 items.
        // We truncate the list safely to prevent application crashes.
        List<String> safeUids = uids.size() > 30 ? uids.subList(0, 30) : uids;

        FirebaseFirestore.getInstance().collection("users")
                .whereIn("uid", safeUids).get()
                .addOnSuccessListener(snapshots -> {
                    List<User> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setFriendStatus(status);
                            list.add(user);
                        }
                    }
                    if (status == 1) allFriendsOriginal = new ArrayList<>(list); // Cache for local search
                    liveData.postValue(list);
                })
                .addOnFailureListener(e -> _error.postValue("Failed to fetch user profiles: " + e.getMessage()));
    }

    // ============================================================
    // 3. API ACTIONS - "processFriendAction"
    // ============================================================

    public void searchAndSendRequestByEmail(String email) {
        String myUid = FirebaseManager.getInstance().getCurrentUserId();
        if (email.trim().isEmpty()) {
            _error.postValue("Please enter an Email address.");
            return;
        }

        FirebaseManager.getInstance().searchUserByEmail(email.trim(), new FirebaseManager.DataCallback<String>() {
            @Override
            public void onSuccess(String targetUid) {
                if (targetUid.equals(myUid)) { _error.postValue("You cannot add yourself."); return; }
                if (myFriendIds.contains(targetUid)) { _error.postValue("This user is already in your friend list."); return; }
                if (mySentRequestIds.contains(targetUid)) { _error.postValue("Friend request already sent."); return; }
                if (myIncomingRequestIds.contains(targetUid)) { _error.postValue("Check your incoming requests, they already added you."); return; }

                // ACTION: SEND REQUEST
                FirebaseManager.getInstance().processFriendAction(targetUid, "request", new FirebaseManager.DataCallback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        refreshSentRequests();
                        refreshSuggestions();
                        _toast.postValue("Request sent successfully.");
                    }

                    @Override
                    public void onError(String message) {
                        _toast.postValue("Failed to send request.");
                        _error.postValue(message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                _toast.postValue("Failed to find user.");
                _error.postValue(message != null && !message.trim().isEmpty() ? message : "User not found.");
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
                _toast.postValue("Request sent successfully.");
            }

            @Override
            public void onError(String message) {
                _toast.postValue("Failed to send request.");
                _error.postValue(message);
            }
        });
    }

    public void acceptFriendRequest(User user) {
        // Enforce the 30-friend architectural limit
        if (myFriendIds.size() >= 30) {
            _error.postValue("Friend limit reached. Firebase constraint limits lists to 30 friends.");
            return;
        }

        // ACTION: ACCEPT REQUEST
        FirebaseManager.getInstance().processFriendAction(user.getUid(), "accept", new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                refreshSuggestions();
                _toast.postValue("You are now friends with " + user.getNameToShow() + "!");
            }
            @Override public void onError(String message) { _error.postValue(message); }
        });
    }

    public void declineFriendRequest(User user) {
        // ACTION: DECLINE REQUEST
        FirebaseManager.getInstance().processFriendAction(user.getUid(), "remove", new FirebaseManager.DataCallback<Void>() {
            @Override public void onSuccess(Void data) { refreshSuggestions(); }
            @Override public void onError(String message) { _error.postValue(message); }
        });
    }

    public void cancelFriendRequest(User user) {
        // ACTION: CANCEL OUTGOING REQUEST
        FirebaseManager.getInstance().processFriendAction(user.getUid(), "remove", new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                refreshSuggestions();
                _toast.postValue("Request cancelled.");
            }
            @Override public void onError(String message) { _error.postValue(message); }
        });
    }

    public void unfriend(User user) {
        // ACTION: REMOVE FRIEND
        FirebaseManager.getInstance().processFriendAction(user.getUid(), "remove", new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                refreshSuggestions();
                _toast.postValue("Removed from friend list.");
            }
            @Override public void onError(String message) { _error.postValue(message); }
        });
    }

    // ============================================================
    // 4. LOCAL SEARCH FILTERS
    // ============================================================

    public void filterFriendsLocal(String query) {
        if (query == null || query.isEmpty()) {
            _friendList.setValue(allFriendsOriginal);
            return;
        }
        List<User> filtered = new ArrayList<>();
        String pattern = query.toLowerCase().trim();
        for (User user : allFriendsOriginal) {
            if (user.getDisplayName() != null && user.getDisplayName().toLowerCase().contains(pattern)) {
                filtered.add(user);
            }
        }
        _friendList.setValue(filtered);
    }

    public void filterSuggestionsLocal(String query) {
        if (query == null || query.isEmpty()) {
            _suggestionList.setValue(allSuggestionsOriginal);
            return;
        }

        List<User> filtered = new ArrayList<>();
        String pattern = query.toLowerCase().trim();
        for (User user : allSuggestionsOriginal) {
            boolean matchesName = user.getDisplayName() != null && user.getDisplayName().toLowerCase().contains(pattern);
            boolean matchesEmail = user.getEmail() != null && user.getEmail().toLowerCase().contains(pattern);
            if (matchesName || matchesEmail) filtered.add(user);
        }
        _suggestionList.setValue(filtered);
    }

    private boolean isRelatedUser(String uid) {
        return uid == null
                || myFriendIds.contains(uid)
                || mySentRequestIds.contains(uid)
                || myIncomingRequestIds.contains(uid);
    }

    // ============================================================
    // LIFECYCLE CLEANUP
    // ============================================================

    @Override
    protected void onCleared() {
        super.onCleared();
        // Prevent memory leaks by detaching listeners when ViewModel is destroyed
        if (friendsListener != null) friendsListener.remove();
        if (incomingListener != null) incomingListener.remove();
        if (sentListener != null) sentListener.remove();
    }
}