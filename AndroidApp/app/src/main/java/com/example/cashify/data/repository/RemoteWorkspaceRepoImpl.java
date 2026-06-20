package com.example.cashify.data.repository;

import com.example.cashify.data.model.Transaction;
import com.example.cashify.data.model.Workspace;
import com.example.cashify.data.model.User;
import com.example.cashify.ui.transactions.TransactionViewModel;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RemoteWorkspaceRepoImpl implements IWorkspaceRepo {

    private final FirebaseFirestore db;
    private DocumentSnapshot lastVisibleTransaction = null;
    private boolean isLastPageTransactions = false;
    private static final int PAGE_LIMIT = 10;

    public RemoteWorkspaceRepoImpl() {
        this.db = FirebaseFirestore.getInstance();
    }

    @Override
    public void getWorkspaces(String userId, OnWorkspacesLoadedListener listener) {
        db.collection("workspaces")
                .whereArrayContains("members", userId)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        listener.onError(e);
                        return;
                    }
                    if (queryDocumentSnapshots != null) {
                        List<Workspace> list = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Workspace w = doc.toObject(Workspace.class);
                            w.setId(doc.getId());
                            list.add(w);
                        }
                        listener.onSuccess(list);
                    }
                });
    }

    @Override
    public void createWorkspace(Workspace workspace, OnActionCompleteListener listener) {
        db.collection("workspaces")
                .add(workspace)
                .addOnSuccessListener(documentReference -> {
                    String newId = documentReference.getId();
                    documentReference.update("id", newId)
                            .addOnSuccessListener(aVoid -> listener.onSuccess())
                            .addOnFailureListener(e -> listener.onError(new Exception("Created but failed to update ID: " + e.getMessage())));
                })
                .addOnFailureListener(listener::onError);
    }

    @Override
    public void addMember(String workspaceId, String memberEmail, OnActionCompleteListener listener) {
        db.collection("users")
                .whereEqualTo("email", memberEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        listener.onError(new Exception("User not found with this email!"));
                        return;
                    }

                    DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                    String memberUid = userDoc.getString("uid");

                    if (memberUid == null) {
                        listener.onError(new Exception("Found user but UID is missing!"));
                        return;
                    }

                    DocumentReference wsRef = db.collection("workspaces").document(workspaceId);
                    wsRef.update("members", FieldValue.arrayUnion(memberUid))
                            .addOnSuccessListener(aVoid -> listener.onSuccess())
                            .addOnFailureListener(e -> listener.onError(new Exception("Failed to add member: " + e.getMessage())));
                })
                .addOnFailureListener(e -> listener.onError(new Exception("Error searching user: " + e.getMessage())));
    }

    @Override
    public void getWorkspaceById(String workspaceId, OnWorkspaceDetailLoadedListener listener) {
        db.collection("workspaces").document(workspaceId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Workspace workspace = documentSnapshot.toObject(Workspace.class);
                        if (workspace != null) {
                            workspace.setId(documentSnapshot.getId());
                            listener.onSuccess(workspace);
                        } else {
                            listener.onError(new Exception("Workspace data casting error"));
                        }
                    } else {
                        listener.onError(new Exception("No data found for this fund"));
                    }
                })
                .addOnFailureListener(listener::onError);
    }

    @Override
    public void getWorkspaceTransactions(String workspaceId, OnTransactionsLoadedListener listener) {
        // Hàm cũ này không xài nữa vì đã thay bằng getWorkspaceTransactionsPaginated
    }

    private List<TransactionViewModel.HistoryItem> mapToHistoryItems(List<Transaction> transactions) {
        List<com.example.cashify.ui.transactions.TransactionViewModel.HistoryItem> historyItems = new ArrayList<>();
        if (transactions == null || transactions.isEmpty()) return historyItems;

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.ENGLISH);
        String lastDate = "";

        for (com.example.cashify.data.model.Transaction t : transactions) {
            String currentDate = sdf.format(new java.util.Date(t.timestamp));

            if (!currentDate.equals(lastDate)) {
                historyItems.add(new TransactionViewModel.HistoryItem(currentDate));
                lastDate = currentDate;
            }

            historyItems.add(new TransactionViewModel.HistoryItem(
                    t,
                    "Category " + t.categoryId, // Fallback name
                    "ic_other",                 // Default icon
                    "#808080"                   // Default color
            ));
        }
        return historyItems;
    }

    @Override
    public void getWorkspaceMembers(String workspaceId, OnMembersLoadedListener listener) {
        final com.google.firebase.firestore.ListenerRegistration[] usersListener = {null};

        db.collection("workspaces").document(workspaceId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    if (documentSnapshot == null || !documentSnapshot.exists()) return;

                    Workspace ws = documentSnapshot.toObject(Workspace.class);
                    if (ws == null || ws.getMembers() == null || ws.getMembers().isEmpty()) {
                        if (usersListener[0] != null) usersListener[0].remove();
                        listener.onSuccess(new ArrayList<>());
                        return;
                    }

                    List<String> uids = ws.getMembers();

                    if (usersListener[0] != null) {
                        usersListener[0].remove();
                    }

                    usersListener[0] = db.collection("users")
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), uids)
                            .addSnapshotListener((userSnaps, e) -> {
                                if (e != null) {
                                    listener.onError(e);
                                    return;
                                }

                                if (userSnaps != null) {
                                    List<User> memberList = new ArrayList<>();
                                    for (com.google.firebase.firestore.QueryDocumentSnapshot userDoc : userSnaps) {
                                        memberList.add(userDoc.toObject(User.class));
                                    }
                                    listener.onSuccess(memberList);
                                }
                            });
                });
    }

    public void getWorkspaceTransactionsPaginated(String workspaceId, boolean isRefresh, OnTransactionsLoadedListener listener) {
        if (isRefresh) {
            lastVisibleTransaction = null;
            isLastPageTransactions = false;
        }

        if (isLastPageTransactions) {
            listener.onSuccess(new ArrayList<>());
            return;
        }

        com.google.firebase.firestore.Query query = db.collection("workspaces")
                .document(workspaceId).collection("transactions")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(PAGE_LIMIT);

        if (lastVisibleTransaction != null) {
            query = query.startAfter(lastVisibleTransaction);
        }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.isEmpty()) {
                isLastPageTransactions = true;
                // Trả về list rỗng để giải phóng khóa lock
                listener.onSuccess(new ArrayList<>());
                return;
            }

            lastVisibleTransaction = queryDocumentSnapshots.getDocuments()
                    .get(queryDocumentSnapshots.size() - 1);

            if (queryDocumentSnapshots.size() < PAGE_LIMIT) {
                isLastPageTransactions = true;
            }

            List<Transaction> list = new ArrayList<>();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Transaction t = doc.toObject(Transaction.class);
                t.id = doc.getId();
                list.add(t);
            }
            listener.onSuccess(mapToHistoryItems(list));
        }).addOnFailureListener(listener::onError);
    }
}