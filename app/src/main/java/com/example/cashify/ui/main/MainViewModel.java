package com.example.cashify.ui.main;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.local.AppDatabase;
import com.example.cashify.data.model.Category;
import com.example.cashify.data.model.Budget;
import com.example.cashify.data.model.Transaction;
import com.example.cashify.data.model.Workspace;
import com.example.cashify.data.remote.FirebaseManager;
import com.example.cashify.data.repository.IWorkspaceRepo;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class MainViewModel extends ViewModel {
    //Cần một biến MutableLiveData<Workspace> currentWorkspace.
    //
    //Khi user bấm vào Side Bar chọn "Quỹ Nhóm A", MainViewModel sẽ cập nhật cái ID này.
    //
    //Cực kỳ quan trọng: Tất cả các Fragment khác (Home, History) sẽ quan sát cái ID này để tải dữ liệu tương ứng
    //Cơ chế "Loa phóng thanh": Khi MainActivity và các Fragment (Home, History, Budget) cùng sử dụng chung một instance của MainViewModel (dùng requireActivity() khi khởi tạo ViewModel trong Fragment), chúng sẽ luôn nhìn thấy cùng một currentWorkspace.
    //Trải nghiệm người dùng: Nhắc bạn dev UI là khi currentWorkspace thay đổi, nên có một hiệu ứng chuyển cảnh nhẹ hoặc ProgressBar để user thấy app đang tải lại dữ liệu của quỹ mới.
    //Lưu trạng thái: Nếu muốn xịn hơn, hãy lưu ID của Workspace cuối cùng user chọn vào SharedPreferences. Lần sau mở app, nó sẽ tự động vào đúng quỹ đó luôn mà không cần chờ load lại từ đầu.
    private IWorkspaceRepo workspaceRepo;
    private final FirebaseManager firebaseManager;

    // Danh sách tất cả Workspace để hiển thị lên Side Bar
    private final MutableLiveData<List<Workspace>> _workspaces = new MutableLiveData<>();
    public LiveData<List<Workspace>> workspaces = _workspaces;

    // Workspace đang được chọn (Linh hồn của việc chuyển đổi dữ liệu)
    private final MutableLiveData<Workspace> _currentWorkspace = new MutableLiveData<>();
    public LiveData<Workspace> currentWorkspace = _currentWorkspace;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    public final MutableLiveData<Boolean> syncCompleted = new MutableLiveData<>(false);

    public MainViewModel() {
        this.firebaseManager = FirebaseManager.getInstance();
    }

    // TODO 1: Inject IWorkspaceRepo (Remote hoặc Local tùy cấu hình) vào Constructor
    public MainViewModel(IWorkspaceRepo repo) {
        this.workspaceRepo = repo;
        this.firebaseManager = FirebaseManager.getInstance();
    }

    // ============================================================
    // TODO 2: TẢI DANH SÁCH WORKSPACE
    // - Khi MainActivity khởi tạo, gọi hàm này để lấy tất cả Quỹ của User.
    // - Sau khi lấy được List:
    //      + Cập nhật vào _workspaces để Side Bar hiển thị.
    //      + Mặc định chọn Quỹ đầu tiên (thường là PERSONAL) gán vào _currentWorkspace.
    // ============================================================
    public void loadWorkspaces(String userId) {
        _isLoading.setValue(true);
        // Gọi workspaceRepo.getWorkspaces...
    }

    // ============================================================
    // TODO 3: LOGIC CHUYỂN ĐỔI WORKSPACE
    // - Hàm này được gọi khi User click vào một dòng trên Side Bar.
    // - Cập nhật Workspace mới vào _currentWorkspace.
    // - Các Fragment (Home, History) đang observe cái này sẽ tự động
    //   nhận được thông báo và reload lại Transaction theo ID mới.
    // ============================================================
    public void selectWorkspace(Workspace workspace) {
        if (workspace != null) {
            _currentWorkspace.setValue(workspace);
        }
    }

    // ============================================================
    // TODO 4: LÀM MỚI SỐ DƯ (REFRESH BALANCE)
    // - Viết hàm cập nhật lại số dư của currentWorkspace sau khi
    //   User thêm/xóa giao dịch ở các Fragment con.
    // ============================================================

    // ============================================================
    // TODO Bonus: Lấy data từ server
    // ============================================================


    public void syncAllDataFromServer(Context context) {
        Context appContext = context.getApplicationContext();
        _isLoading.postValue(true);

        // Tải Categories tự tạo trước
        firebaseManager.getAllCategoriesFromCloud(new FirebaseManager.DataCallback<List<DocumentSnapshot>>() {
            @Override
            public void onSuccess(List<DocumentSnapshot> catDocs) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        AppDatabase db = AppDatabase.getInstance(appContext);
                        for (DocumentSnapshot doc : catDocs) {
                            Category c = new Category();
                            c.id = Integer.parseInt(doc.getId()); // Lấy ID từ tên Document
                            c.name = doc.getString("name");
                            c.iconName = doc.getString("iconName");
                            c.colorCode = doc.getString("colorCode");
                            Long type = doc.getLong("type");
                            c.type = (type != null) ? type.intValue() : 0;
                            c.isDefault = 0;
                            c.isDeleted = 0;
                            db.categoryDao().insert(c);
                        }
                        Log.d("SYNC", "Downloaded " + catDocs.size() + " category");

                        // Sau khi xong Category mới tải tiếp 2 cái kia
                        fetchBudgets(appContext);
                        fetchTransactions(appContext);

                    } catch (Exception e) {
                        Log.e("SYNC", "Category sync error: " + e.getMessage());
                    }
                });
            }
            @Override
            public void onError(String message) { _isLoading.postValue(false); }
        });
    }

    private void fetchBudgets(Context context) {
        firebaseManager.getAllBudgetsFromCloud(new FirebaseManager.DataCallback<List<DocumentSnapshot>>() {
            @Override
            public void onSuccess(List<DocumentSnapshot> documents) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        AppDatabase db = AppDatabase.getInstance(context);
                        for (DocumentSnapshot doc : documents) {
                            Budget b = new Budget();
                            b.id = Integer.parseInt(doc.getId());
                            b.limitAmount = doc.getLong("limitAmount") != null ? doc.getLong("limitAmount") : 0L;
                            b.categoryId = doc.getLong("categoryId") != null ? doc.getLong("categoryId").intValue() : -1;
                            b.startDate = doc.getLong("startDate") != null ? doc.getLong("startDate") : 0L;
                            b.endDate = doc.getLong("endDate") != null ? doc.getLong("endDate") : 0L;
                            b.periodType = doc.getString("periodType");
                            db.budgetDao().insert(b);
                        }
                        Log.d("SYNC", "Downloaded " + documents.size() + " budgets.");
                    } catch (Exception e) {
                        Log.e("SYNC", "Budget sync error: " + e.getMessage());
                    }
                });
            }
            @Override
            public void onError(String m) {}
        });
    }

    private void fetchTransactions(Context context) {
        firebaseManager.getAllTransactionsFromCloud(new FirebaseManager.DataCallback<List<DocumentSnapshot>>() {
            @Override
            public void onSuccess(List<DocumentSnapshot> documents) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        AppDatabase db = AppDatabase.getInstance(context);
                        for (DocumentSnapshot doc : documents) {
                            Transaction t = new Transaction();
                            t.id = Integer.parseInt(doc.getId());

                            Number amount = (Number) doc.get("amount");
                            t.amount = (amount != null) ? amount.longValue() : 0L;

                            Number catId = (Number) doc.get("categoryId");
                            t.categoryId = (catId != null) ? catId.intValue() : 1;

                            t.note = doc.getString("note");

                            Number timestamp = (Number) doc.get("timestamp");
                            t.timestamp = (timestamp != null) ? timestamp.longValue() : System.currentTimeMillis();

                            t.paymentMethod = doc.getString("paymentMethod");
                            if (t.paymentMethod == null) t.paymentMethod = "cash";

                            Number type = (Number) doc.get("type");
                            t.type = (type != null) ? type.intValue() : 0;

                            t.workspaceId = doc.getString("workspaceId");
                            if (t.workspaceId == null) t.workspaceId = "PERSONAL";

                            db.transactionDao().insert(t);
                        }
                        Log.d("SYNC", "Downloaded " + documents.size() + " transactions.");
                        _isLoading.postValue(false);
                        syncCompleted.postValue(true);
                    } catch (Exception e) {
                        Log.e("SYNC", "Transaction sync error: " + e.getMessage());
                        _isLoading.postValue(false);
                    }
                });
            }
            @Override
            public void onError(String m) { _isLoading.postValue(false); }
        });
    }

    public void uploadTransactionToFirebase(Transaction t) {
        // Chuyển đổi Object Transaction sang Map để Firebase hiểu được
        Map<String, Object> data = new HashMap<>();
        data.put("amount", t.amount);
        data.put("note", t.note);
        data.put("categoryId", t.categoryId);
        data.put("timestamp", t.timestamp);
        data.put("paymentMethod", t.paymentMethod);
        data.put("type", t.type);

        // Đẩy lên Firestore theo đường dẫn: users -> {uid} -> transactions -> {id_tự_sinh}
        // Dùng cái ID của Transaction làm Document ID luôn cho dễ quản lý
        String docId = String.valueOf(t.id);

        firebaseManager.syncLocalToCloud("transactions", docId, data, new FirebaseManager.DataCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d("FIREBASE_SYNC", "Upload transaction " + docId + " to cloud successfully!");
            }

            @Override
            public void onError(String message) {
                Log.e("FIREBASE_SYNC", "Uploading transaction to cloud failed: " + message);
            }
        });
    }

    // ============================================================
    // TÍNH NĂNG ĐỒNG BỘ REAL-TIME ĐA THIẾT BỊ
    // ============================================================
    public void startRealTimeSync(Context context) {
        Context appContext = context.getApplicationContext();

        // LẮNG NGHE GIAO DỊCH (TRANSACTIONS)
        firebaseManager.listenToPersonalChanges("transactions", new FirebaseManager.DataCallback<List<DocumentSnapshot>>() {
            @Override
            public void onSuccess(List<DocumentSnapshot> documents) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        AppDatabase db = AppDatabase.getInstance(appContext);

                        if (documents == null || documents.isEmpty()) {
                            db.transactionDao().deleteAllTransactions();
                            syncCompleted.postValue(true);
                            return;
                        }

                        for (DocumentSnapshot doc : documents) {
                            Transaction t = new Transaction();
                            t.id = Integer.parseInt(doc.getId());

                            Number amount = (Number) doc.get("amount");
                            t.amount = (amount != null) ? amount.longValue() : 0L;

                            Number catId = (Number) doc.get("categoryId");
                            t.categoryId = (catId != null) ? catId.intValue() : 1;

                            t.note = doc.getString("note");

                            Number timestamp = (Number) doc.get("timestamp");
                            t.timestamp = (timestamp != null) ? timestamp.longValue() : System.currentTimeMillis();

                            t.paymentMethod = doc.getString("paymentMethod");
                            if (t.paymentMethod == null) t.paymentMethod = "cash";

                            Number type = (Number) doc.get("type");
                            t.type = (type != null) ? type.intValue() : 0;

                            t.workspaceId = doc.getString("workspaceId");
                            if (t.workspaceId == null) t.workspaceId = "PERSONAL";

                            // Insert với replace sẽ tự động cập nhật nếu dữ liệu đã tồn tại
                            db.transactionDao().insert(t);
                        }
                        // Bóp còi để màn hình Home và History tự động load lại dữ liệu mới
                        syncCompleted.postValue(true);
                    } catch (Exception e) {
                        Log.e("REALTIME_SYNC", "Transaction sync error: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(String message) {
                Log.e("REALTIME_SYNC", "Firebase sync error: " + message);
            }
        });

        // LẮNG NGHE DANH MỤC (CATEGORIES)
        firebaseManager.listenToPersonalChanges("categories", new FirebaseManager.DataCallback<List<DocumentSnapshot>>() {
            @Override
            public void onSuccess(List<DocumentSnapshot> documents) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        AppDatabase db = AppDatabase.getInstance(appContext);
                        for (DocumentSnapshot doc : documents) {
                            Category c = new Category();
                            c.id = Integer.parseInt(doc.getId());
                            c.name = doc.getString("name");
                            c.iconName = doc.getString("iconName");
                            c.colorCode = doc.getString("colorCode");

                            Number type = (Number) doc.get("type");
                            c.type = (type != null) ? type.intValue() : 0;

                            c.isDefault = 0; // Các danh mục trên mây đều là do user tự tạo
                            c.isDeleted = 0;

                            db.categoryDao().insert(c);
                        }
                        // Bóp còi để màn hình tự load lại (nếu có dùng)
                        syncCompleted.postValue(true);
                    } catch (Exception e) {
                        Log.e("REALTIME_SYNC", "Category sync error: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(String message) {
                Log.e("REALTIME_SYNC", "Category sync error: " + message);
            }
        });

        // LẮNG NGHE NGÂN SÁCH (BUDGETS)
        firebaseManager.listenToPersonalChanges("budgets", new FirebaseManager.DataCallback<List<DocumentSnapshot>>() {
            @Override
            public void onSuccess(List<DocumentSnapshot> documents) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        AppDatabase db = AppDatabase.getInstance(appContext);
                        for (DocumentSnapshot doc : documents) {
                            Budget b = new Budget();
                            b.id = Integer.parseInt(doc.getId());

                            Number limitAmount = (Number) doc.get("limitAmount");
                            b.limitAmount = (limitAmount != null) ? limitAmount.longValue() : 0L;

                            Number catId = (Number) doc.get("categoryId");
                            b.categoryId = (catId != null) ? catId.intValue() : -1;

                            Number startDate = (Number) doc.get("startDate");
                            b.startDate = (startDate != null) ? startDate.longValue() : 0L;

                            Number endDate = (Number) doc.get("endDate");
                            b.endDate = (endDate != null) ? endDate.longValue() : 0L;

                            b.periodType = doc.getString("periodType");

                            db.budgetDao().insert(b);
                        }
                        // Bóp còi để màn hình Ngân sách tự vẽ lại
                        syncCompleted.postValue(true);
                    } catch (Exception e) {
                        Log.e("REALTIME_SYNC", "Budget sync error: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onError(String message) {
                Log.e("REALTIME_SYNC", "Budget sync error: " + message);
            }
        });
    }
}
