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
import com.example.cashify.data.repository.RemoteWorkspaceRepoImpl;
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
        this.workspaceRepo = new RemoteWorkspaceRepoImpl();
    }

    public LiveData<List<Workspace>> getWorkspaces() {
        return workspaces;
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
        workspaceRepo.getWorkspaces(userId, new IWorkspaceRepo.OnWorkspacesLoadedListener() {
            @Override
            public void onSuccess(List<Workspace> list) {
                _isLoading.postValue(false);
                // Đẩy danh sách lên cho MainActivity nhận được và vẽ Sidebar
                _workspaces.postValue(list);
            }

            @Override
            public void onError(Exception e) {
                _isLoading.postValue(false);
                Log.e("MainViewModel", "Unable to load workspace list: " + e.getMessage());
            }
        });
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

                        // Lấy danh sách local ra để chuẩn bị đối chiếu
                        List<Category> localCats = db.categoryDao().getAll();

                        for (DocumentSnapshot doc : catDocs) { // (Hoặc snapshots.getDocuments() tùy hàm)
                            Category c = new Category();
                            c.firestoreId = doc.getId();
                            c.name = doc.getString("name");
                            c.iconName = doc.getString("iconName");
                            c.colorCode = doc.getString("colorCode");

                            Long type = doc.getLong("type");
                            c.type = (type != null) ? type.intValue() : 0;
                            c.workspaceId = "PERSONAL";

                            // 🔥 FIX BUG 2: ĐỌC isDeleted TỪ MÂY, KHÔNG GÁN CỨNG BẰNG 0 NỮA!
                            Long isDef = doc.getLong("isDefault");
                            c.isDefault = (isDef != null) ? isDef.intValue() : 0;

                            Long isDel = doc.getLong("isDeleted");
                            c.isDeleted = (isDel != null) ? isDel.intValue() : 0;

                            // 🔥 FIX BUG 1: CHỐNG XÓA CASCADE GIAO DỊCH
                            boolean existsLocally = false;
                            for (Category local : localCats) {
                                boolean matchId = (local.firestoreId != null && local.firestoreId.equals(c.firestoreId));
                                boolean matchName = (local.name != null && local.name.equalsIgnoreCase(c.name) && local.type == c.type);

                                if (matchId || matchName) {
                                    c.id = local.id; // Kế thừa ID
                                    existsLocally = true;
                                    break;
                                }
                            }

                            // Nếu đã tồn tại thì DÙNG LỆNH UPDATE (Không dùng Insert REPLACE nữa để bảo toàn giao dịch)
                            if (existsLocally) {
                                db.categoryDao().update(c);
                            } else {
                                db.categoryDao().insert(c);
                            }
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
            public void onError(String message) {
                _isLoading.postValue(false);
            }
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
            public void onError(String m) {
            }
        });
    }

    private void fetchTransactions(Context context) {
        firebaseManager.getAllTransactionsFromCloud("PERSONAL", new FirebaseManager.DataCallback<List<DocumentSnapshot>>() {
            @Override
            public void onSuccess(List<DocumentSnapshot> documents) {
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        AppDatabase db = AppDatabase.getInstance(context);
                        List<Category> localCats = db.categoryDao().getAll(); // Lấy danh mục để map ID

                        for (DocumentSnapshot doc : documents) {
                            // BỌC TRY-CATCH CHO TỪNG ITEM: Lỗi 1 cái thì bỏ qua 1 cái, KHÔNG sập cả danh sách!
                            try {
                                Transaction t = new Transaction();
                                t.id = doc.getId();

                                Object amountObj = doc.get("amount");
                                t.amount = (amountObj instanceof Number) ? ((Number) amountObj).longValue() : 0L;

                                // LÕI MAP CATEGORY VÀ CHỐNG LỖI KHÓA NGOẠI (FOREIGN KEY)
                                String firestoreCatId = doc.getString("firestoreCategoryId");
                                boolean categoryMapped = false;

                                // BƯỚC 1: Tìm theo firestoreCategoryId
                                if (firestoreCatId != null && !firestoreCatId.isEmpty()) {
                                    for (Category c : localCats) {
                                        if (firestoreCatId.equals(c.firestoreId)) {
                                            t.categoryId = c.id;
                                            categoryMapped = true;
                                            break;
                                        }
                                    }
                                }

                                // BƯỚC 2: TÌM THEO ID CŨ (Dành cho đồ tạo Offline)
                                if (!categoryMapped) {
                                    Object catIdObj = doc.get("categoryId");
                                    int oldCatId = (catIdObj instanceof Number) ? ((Number) catIdObj).intValue() : 1;

                                    // 🔥 FIX CHÍNH: Tìm Category có ID mới nhưng mang firestoreId giống ID cũ!
                                    for (Category c : localCats) {
                                        if (c.id == oldCatId || (c.firestoreId != null && c.firestoreId.equals(String.valueOf(oldCatId)))) {
                                            t.categoryId = c.id; // Map thành công với ID mới chuẩn
                                            categoryMapped = true;
                                            break;
                                        }
                                    }

                                    // BƯỚC 3: TẠO TEMP CAT NẾU MẠNG CHẬM HOẶC LỖI
                                    if (!categoryMapped) {
                                        t.categoryId = oldCatId; // Tạm dùng ID cũ

                                        boolean idExists = false;
                                        for (Category c : localCats) { if (c.id == t.categoryId) { idExists = true; break; } }

                                        if (!idExists) {
                                            Category tempCat = new Category();
                                            tempCat.id = t.categoryId;
                                            // Gắn luôn firestoreId để lát Category thật về nó tự Update đè lên thằng ảo này!
                                            tempCat.firestoreId = (firestoreCatId != null && !firestoreCatId.isEmpty()) ? firestoreCatId : String.valueOf(oldCatId);
                                            tempCat.name = "Đang đồng bộ...";
                                            tempCat.iconName = "ic_other";
                                            tempCat.colorCode = "#A9A9A9";
                                            tempCat.type = doc.getLong("type") != null ? doc.getLong("type").intValue() : 0;
                                            tempCat.workspaceId = "PERSONAL";

                                            try { db.categoryDao().insert(tempCat); } catch(Exception ignored){}
                                            localCats.add(tempCat);
                                        }
                                    }
                                }

                                t.note = doc.getString("note");

                                // ÉP KIỂU THỜI GIAN AN TOÀN (Cả Timestamp lẫn Number)
                                Object tsObj = doc.get("timestamp");
                                if (tsObj instanceof com.google.firebase.Timestamp) {
                                    t.timestamp = ((com.google.firebase.Timestamp) tsObj).toDate().getTime();
                                } else if (tsObj instanceof Number) {
                                    t.timestamp = ((Number) tsObj).longValue();
                                } else {
                                    t.timestamp = System.currentTimeMillis();
                                }

                                t.paymentMethod = doc.getString("paymentMethod");
                                if (t.paymentMethod == null) t.paymentMethod = "cash";

                                Object typeObj = doc.get("type");
                                t.type = (typeObj instanceof Number) ? ((Number) typeObj).intValue() : 0;

                                t.workspaceId = "PERSONAL";

                                db.transactionDao().insert(t);
                            } catch (Exception parseEx) {
                                Log.e("SYNC", "Bỏ qua giao dịch bị lỗi format: " + doc.getId(), parseEx);
                            }
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
            public void onError(String m) {
                _isLoading.postValue(false);
            }
        });
    }

    public void uploadTransactionToFirebase(Transaction t) {
        Map<String, Object> data = new HashMap<>();
        data.put("amount", t.amount);
        data.put("note", t.note);
        data.put("categoryId", t.categoryId); // Vẫn giữ ID local
        data.put("timestamp", t.timestamp);
        data.put("paymentMethod", t.paymentMethod);
        data.put("type", t.type);

        data.put("workspaceId", t.workspaceId != null ? t.workspaceId : "PERSONAL");
        if (t.firestoreCategoryId != null && !t.firestoreCategoryId.isEmpty()) {
            data.put("firestoreCategoryId", t.firestoreCategoryId);
        }

        String docId = t.id;

        firebaseManager.syncLocalToCloud(t.workspaceId, "transactions", docId, data, new FirebaseManager.DataCallback<Void>() {
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
    // TÍNH NĂNG ĐỒNG BỘ REAL-TIME ĐA THIẾT BỊ (ĐÃ CẬP NHẬT SUB-COLLECTION)
    // ============================================================
    public void startRealTimeSync(Context context) {
        Context appContext = context.getApplicationContext();

        // 1. Lấy thông tin user hiện tại
        com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.e("REALTIME_SYNC", "User chưa đăng nhập, không thể đồng bộ!");
            return;
        }
        String uid = auth.getCurrentUser().getUid();
        com.google.firebase.firestore.FirebaseFirestore dbCloud = com.google.firebase.firestore.FirebaseFirestore.getInstance();

        // 2. LẮNG NGHE GIAO DỊCH (TRANSACTIONS)
        dbCloud.collection("users").document(uid).collection("transactions")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("REALTIME_SYNC", "Listen failed.", e);
                        return;
                    }
                    if (snapshots != null) {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                AppDatabase db = AppDatabase.getInstance(appContext);

                                if (snapshots.isEmpty()) {
                                    db.transactionDao().deleteAllTransactions("PERSONAL");
                                    syncCompleted.postValue(true);
                                    return;
                                }

                                List<Category> localCats = db.categoryDao().getAll(); // Lấy danh mục để map

                                // TRẢ LẠI CODE PARSE TRANSACTION CHUẨN
                                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                    try {
                                        Transaction t = new Transaction();
                                        t.id = doc.getId();

                                        Object amountObj = doc.get("amount");
                                        t.amount = (amountObj instanceof Number) ? ((Number) amountObj).longValue() : 0L;

                                        // LÕI MAP CATEGORY VÀ CHỐNG LỖI KHÓA NGOẠI (FOREIGN KEY)
                                        String firestoreCatId = doc.getString("firestoreCategoryId");
                                        boolean categoryMapped = false;

                                        // BƯỚC 1: Tìm theo firestoreCategoryId
                                        if (firestoreCatId != null && !firestoreCatId.isEmpty()) {
                                            for (Category c : localCats) {
                                                if (firestoreCatId.equals(c.firestoreId)) {
                                                    t.categoryId = c.id;
                                                    categoryMapped = true;
                                                    break;
                                                }
                                            }
                                        }

                                        // BƯỚC 2: TÌM THEO ID CŨ (Dành cho đồ tạo Offline)
                                        if (!categoryMapped) {
                                            Object catIdObj = doc.get("categoryId");
                                            int oldCatId = (catIdObj instanceof Number) ? ((Number) catIdObj).intValue() : 1;

                                            //Tìm Category có ID mới nhưng mang firestoreId giống ID cũ!
                                            for (Category c : localCats) {
                                                if (c.id == oldCatId || (c.firestoreId != null && c.firestoreId.equals(String.valueOf(oldCatId)))) {
                                                    t.categoryId = c.id; // Map thành công với ID mới chuẩn
                                                    categoryMapped = true;
                                                    break;
                                                }
                                            }

                                            // BƯỚC 3: TẠO TEMP CAT NẾU MẠNG CHẬM HOẶC LỖI
                                            if (!categoryMapped) {
                                                t.categoryId = oldCatId; // Tạm dùng ID cũ

                                                boolean idExists = false;
                                                for (Category c : localCats) { if (c.id == t.categoryId) { idExists = true; break; } }

                                                if (!idExists) {
                                                    Category tempCat = new Category();
                                                    tempCat.id = t.categoryId;
                                                    //Gắn luôn firestoreId để lát Category thật về nó tự Update đè lên thằng ảo này!
                                                    tempCat.firestoreId = (firestoreCatId != null && !firestoreCatId.isEmpty()) ? firestoreCatId : String.valueOf(oldCatId);
                                                    tempCat.name = "Đang đồng bộ...";
                                                    tempCat.iconName = "ic_other";
                                                    tempCat.colorCode = "#A9A9A9";
                                                    tempCat.type = doc.getLong("type") != null ? doc.getLong("type").intValue() : 0;
                                                    tempCat.workspaceId = "PERSONAL";

                                                    try { db.categoryDao().insert(tempCat); } catch(Exception ignored){}
                                                    localCats.add(tempCat);
                                                }
                                            }
                                        }

                                        t.note = doc.getString("note");

                                        Object tsObj = doc.get("timestamp");
                                        if (tsObj instanceof com.google.firebase.Timestamp) {
                                            t.timestamp = ((com.google.firebase.Timestamp) tsObj).toDate().getTime();
                                        } else if (tsObj instanceof Number) {
                                            t.timestamp = ((Number) tsObj).longValue();
                                        } else {
                                            t.timestamp = System.currentTimeMillis();
                                        }

                                        t.paymentMethod = doc.getString("paymentMethod");
                                        if (t.paymentMethod == null) t.paymentMethod = "cash";

                                        Object typeObj = doc.get("type");
                                        t.type = (typeObj instanceof Number) ? ((Number) typeObj).intValue() : 0;

                                        t.workspaceId = "PERSONAL";

                                        db.transactionDao().insert(t); // Transaction insert bình thường
                                    } catch (Exception parseEx) {
                                        Log.e("REALTIME_SYNC", "Lỗi parse dữ liệu ở transaction: " + doc.getId(), parseEx);
                                    }
                                }
                                syncCompleted.postValue(true);
                            } catch (Exception ex) {
                                Log.e("REALTIME_SYNC", "Transaction sync error: " + ex.getMessage());
                            }
                        });
                    }
                });

        // 3. LẮNG NGHE DANH MỤC (CATEGORIES)
        dbCloud.collection("users").document(uid).collection("categories")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null) {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                AppDatabase db = AppDatabase.getInstance(appContext);

                                // Kéo danh sách local ra
                                List<Category> localCats = db.categoryDao().getAll();

                                for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                    Category c = new Category();
                                    c.firestoreId = doc.getId();
                                    c.name = doc.getString("name");
                                    c.iconName = doc.getString("iconName");
                                    c.colorCode = doc.getString("colorCode");

                                    Number type = (Number) doc.get("type");
                                    c.type = (type != null) ? type.intValue() : 0;

                                    c.workspaceId = "PERSONAL";

                                    // ĐỌC isDeleted TỪ MÂY
                                    Long isDef = doc.getLong("isDefault");
                                    c.isDefault = (isDef != null) ? isDef.intValue() : 0;

                                    Long isDel = doc.getLong("isDeleted");
                                    c.isDeleted = (isDel != null) ? isDel.intValue() : 0;

                                    // CHỐNG XÓA CASCADE GIAO DỊCH
                                    boolean existsLocally = false;
                                    for (Category local : localCats) {
                                        boolean matchId = (local.firestoreId != null && local.firestoreId.equals(c.firestoreId));
                                        boolean matchName = (local.name != null && local.name.equalsIgnoreCase(c.name) && local.type == c.type);

                                        if (matchId || matchName) {
                                            c.id = local.id; // Kế thừa ID
                                            existsLocally = true;
                                            break;
                                        }
                                    }

                                    // DÙNG LỆNH UPDATE NẾU ĐÃ CÓ, KHÔNG DÙNG INSERT REPLACE
                                    if (existsLocally) {
                                        db.categoryDao().update(c);
                                    } else {
                                        db.categoryDao().insert(c);
                                    }
                                }
                                syncCompleted.postValue(true);
                            } catch (Exception ex) {
                                Log.e("REALTIME_SYNC", "Category sync error: " + ex.getMessage());
                            }
                        });
                    }
                });

        // 4. LẮNG NGHE NGÂN SÁCH (BUDGETS)
        // Đường dẫn mới: users/{uid}/budgets
        dbCloud.collection("users").document(uid).collection("budgets")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    if (snapshots != null) {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            try {
                                AppDatabase db = AppDatabase.getInstance(appContext);
                                for (DocumentSnapshot doc : snapshots.getDocuments()) {
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
                                syncCompleted.postValue(true);
                            } catch (Exception ex) {
                                Log.e("REALTIME_SYNC", "Budget sync error: " + ex.getMessage());
                            }
                        });
                    }
                });
    }
}
