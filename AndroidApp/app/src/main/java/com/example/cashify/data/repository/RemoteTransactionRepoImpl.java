package com.example.cashify.data.repository;

import android.util.Log;

import com.example.cashify.data.model.Transaction;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

// Repository thao tác với Giao dịch trực tiếp trên mây (Firestore)
public class RemoteTransactionRepoImpl implements ITransactionRepo {
    private final FirebaseFirestore db;
    private final String COLLECTION_NAME = "transactions";

    public RemoteTransactionRepoImpl() {
        this.db = FirebaseFirestore.getInstance();
        // Offline Cache đã được kích hoạt mặc định trên Firebase Android SDK
    }

    @Override
    public void getHistory(String workspaceId, OnDataLoadedListener listener) {
        db.collection(COLLECTION_NAME)
                .whereEqualTo("workspaceId", workspaceId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Transaction> transactions = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Transaction t = doc.toObject(Transaction.class);
                        t.id = doc.getId();
                        transactions.add(t);
                    }
                    listener.onSuccess(transactions);
                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseRepo", "Failed to fetch transaction history: ", e);
                    listener.onError(e);
                });
    }

    @Override
    public void addTransaction(Transaction transaction, OnActionCompleteListener listener) {
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            listener.onError(new Exception("Not logged in!"));
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        DocumentReference docRef;

        // Phân luồng: Cá nhân (User Collection) vs Quỹ chung (Workspace Collection)
        if (transaction.workspaceId == null || transaction.workspaceId.equals("PERSONAL")) {
            if (transaction.id != null && !transaction.id.isEmpty()) {
                docRef = db.collection("users").document(uid).collection("transactions").document(transaction.id);
            } else {
                docRef = db.collection("users").document(uid).collection("transactions").document();
                transaction.id = docRef.getId();
            }
        } else {
            if (transaction.id != null && !transaction.id.isEmpty()) {
                docRef = db.collection("workspaces").document(transaction.workspaceId).collection("transactions").document(transaction.id);
            } else {
                docRef = db.collection("workspaces").document(transaction.workspaceId).collection("transactions").document();
                transaction.id = docRef.getId();
            }
        }

        docRef.set(transaction)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e("FirebaseRepo", "Transaction save failed: ", e);
                    listener.onError(e);
                });
    }

    public void updateDebtStatus(String transactionId, boolean isPaid, OnActionCompleteListener listener) {
        db.collection(COLLECTION_NAME).document(transactionId)
                .update("isPaid", isPaid)
                .addOnSuccessListener(aVoid -> listener.onSuccess())
                .addOnFailureListener(e -> {
                    Log.e("FirebaseRepo", "Failed to update debt status: ", e);
                    listener.onError(e);
                });
    }
}