package com.example.cashify.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cashify.data.model.Workspace;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.example.cashify.data.model.LogItem;

import java.util.ArrayList;
import java.util.List;

/**
 * WorkspaceLogRepository.java
 * Tầng duy nhất trong app được phép nói chuyện với Firestore về logs.
 *
 * Cung cấp:
 *  - getLogs()     → LiveData<List<LogItem>>  (realtime snapshot)
 *  - startListening() / stopListening()       (quản lý vòng đời listener)
 *  - pushLog()     → ghi log mới (dùng .add() đúng theo spec)
 *
 * Đường dẫn Firestore: workspaces/{workspaceId}/logs
 */
public class WorkspaceLogRepository {

    private static final String TAG            = "WorkspaceLogRepo";
    private static final String COL_WORKSPACES = "workspaces";
    private static final String COL_LOGS       = "logs";
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_ID        = "id";

    private final FirebaseFirestore          db;
    private final String                     workspaceId;
    private final MutableLiveData<List<LogItem>> logsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String>    errorLiveData    = new MutableLiveData<>();

    private ListenerRegistration listenerRegistration;

    public WorkspaceLogRepository(String workspaceId) {
        this.db          = FirebaseFirestore.getInstance();
        this.workspaceId = workspaceId;
    }

    // ── Getter LiveData (ViewModel observe, Fragment không chạm vào đây) ──────
    public LiveData<List<LogItem>> getLogs()  { return logsLiveData; }
    public LiveData<String>        getError() { return errorLiveData; }

    // ── Bắt đầu lắng nghe realtime ────────────────────────────────────────────
    public void startListening() {
        // LOG 4: Kiểm tra ID truyền xuống tầng dữ liệu
        Log.d("DEBUG_FLOW", "4. Repository bắt đầu query cho ID: " + workspaceId);

        if (workspaceId == null || workspaceId.isEmpty()) {
            Log.e("DEBUG_FLOW", "=> QUÁ TRÌNH THẤT BẠI: ID bị rỗng ở tầng Repository!");
            return;
        }
        // Nếu workspaceId bị rỗng, thoát ngay lập tức
        if (workspaceId == null || workspaceId.trim().isEmpty()) {
            Log.e(TAG, "startListening: workspaceId is null or empty. Aborting.");
            return;
        }
        if (listenerRegistration != null) return; // tránh đăng ký 2 lần

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
                        com.example.cashify.data.model.LogItem log = doc.toObject(com.example.cashify.data.model.LogItem.class);
                        if (log != null) {
                            log.setId(doc.getId());
                            list.add(log); // list ở đây phải là List<LogItem>
                        }
                    }
                    logsLiveData.setValue(list);
                });
    }

    // ── Dừng lắng nghe (gọi khi ViewModel cleared) ───────────────────────────
    public void stopListening() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    // ── Ghi log mới: .add() → update id ──────────────────────────────────────
    /**
     * Đẩy một bản ghi log mới vào Firestore.
     * Dùng .add() để Firestore tự sinh document ID,
     * sau đó update ngược field "id" vào document vừa tạo (đúng theo spec).
     */
    public static void pushLog(String workspaceId, String userId, String actionType, String message) {
        if (workspaceId == null || workspaceId.isEmpty()) {
            Log.e(TAG, "pushLog: workspaceId rỗng, không thể lưu log.");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Tạo DocumentReference trước để lấy ID tự động từ Firestore
        DocumentReference logRef = db.collection(COL_WORKSPACES)
                .document(workspaceId)
                .collection(COL_LOGS)
                .document();

        // 2. Tạo Object LogItem
        // LƯU Ý: Nếu LogItem có biến id được đánh dấu @DocumentId,
        // Firestore sẽ TỰ HIỂU và KHÔNG ghi đè trường này vào database dưới dạng một Field.
        LogItem logItem = new LogItem(
                userId,
                actionType,
                message,
                System.currentTimeMillis()
        );

        // 3. Sử dụng .set() thay vì .add() để đẩy dữ liệu lên
        logRef.set(logItem)
                .addOnSuccessListener(unused -> Log.d(TAG, "Push log sucessfully ID: " + logRef.getId()))
                .addOnFailureListener(e -> Log.e(TAG, "Push log failed: " + e.getMessage()));
    }
}