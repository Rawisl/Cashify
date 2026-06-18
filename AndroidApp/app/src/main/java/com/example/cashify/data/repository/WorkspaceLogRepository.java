package com.example.cashify.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.example.cashify.data.model.LogItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages Workspace Audit Logs using Firestore real-time snapshot listeners.
 * Emits data strictly via LiveData.
 */
public class WorkspaceLogRepository {

    private static final String TAG             = "WorkspaceLogRepo";
    private static final String COL_WORKSPACES  = "workspaces";
    private static final String COL_LOGS        = "logs";
    private static final String FIELD_TIMESTAMP = "timestamp";

    private final FirebaseFirestore db;
    private final String workspaceId;

    private final MutableLiveData<List<LogItem>> logsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    private ListenerRegistration listenerRegistration;

    public WorkspaceLogRepository(String workspaceId) {
        this.db = FirebaseFirestore.getInstance();
        this.workspaceId = workspaceId;
    }

    public LiveData<List<LogItem>> getLogs()  { return logsLiveData; }
    public LiveData<String> getError() { return errorLiveData; }

    public void startListening() {
        if (workspaceId == null || workspaceId.trim().isEmpty()) {
            Log.e(TAG, "startListening: workspaceId is null or empty. Aborting.");
            return;
        }

        // Prevent registering multiple active listeners for the same node
        if (listenerRegistration != null) return;

        listenerRegistration = db
                .collection(COL_WORKSPACES)
                .document(workspaceId)
                .collection(COL_LOGS)
                .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Snapshot error: " + error.getMessage(), error);
                        errorLiveData.setValue(error.getMessage());
                        return;
                    }
                    if (snapshots == null) return;

                    List<LogItem> list = new ArrayList<>();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshots) {
                        LogItem log = doc.toObject(LogItem.class);
                        list.add(log);
                    }
                    logsLiveData.setValue(list);
                });
    }

    public void stopListening() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    public static void pushLog(String workspaceId, String userId, String actionType, String message) {
        if (workspaceId == null || workspaceId.isEmpty()) {
            Log.e(TAG, "pushLog: workspaceId is empty, unable to log.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Get an auto-generated DocumentReference
        DocumentReference logRef = db.collection(COL_WORKSPACES)
                .document(workspaceId)
                .collection(COL_LOGS)
                .document();

        // 2. Initialize LogItem (Firestore's @DocumentId tag will auto-ignore the 'id' field to avoid duplicate keys)
        LogItem logItem = new LogItem(
                userId,
                actionType,
                message,
                System.currentTimeMillis()
        );

        // 3. Write data using .set()
        logRef.set(logItem)
                .addOnSuccessListener(unused -> Log.d(TAG, "Push log successfully, ID: " + logRef.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Push log failed: " + e.getMessage()));
    }
}