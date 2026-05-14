package com.example.cashify.ui.notifications;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cashify.R;
import com.example.cashify.data.model.NotificationItem;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationBottomSheet extends BottomSheetDialogFragment {

    private NotificationAdapter adapter;
    private FirebaseFirestore db;
    private String currentUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_bottom_sheet_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvNotifications = view.findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        // Khởi tạo Adapter và Xử lý sự kiện CLICK
        adapter = new NotificationAdapter(requireContext(), notification -> {
            handleNotificationClick(notification);
        });
        rvNotifications.setAdapter(adapter);

        if (currentUid != null) {
            loadNotificationsFromFirebase();
        }
    }

    private void loadNotificationsFromFirebase() {
        // Lắng nghe Real-time từ Collection "notifications" của User, sắp xếp mới nhất lên đầu
        db.collection("users").document(currentUid).collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("NOTIF", "Listen failed.", e);
                        return;
                    }

                    if (snapshots != null) {
                        List<NotificationItem> list = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            NotificationItem notif = doc.toObject(NotificationItem.class);
                            if (notif != null) {
                                notif.setId(doc.getId()); // Gắn ID để lát update trạng thái Read
                                list.add(notif);
                            }
                        }
                        adapter.setData(list);
                    }
                });
    }

    private void handleNotificationClick(NotificationItem notification) {
        // 1. Đánh dấu đã đọc trên Firebase
        if (!notification.isRead()) {
            db.collection("users").document(currentUid).collection("notifications")
                    .document(notification.getId())
                    .update("isRead", true); // Đổi isRead thành true
        }

        // 2. Chuyển hướng theo từng loại (Routing)
        String type = notification.getType() != null ? notification.getType() : "";

        switch (type) {
            case "FRIEND_REQUEST":
                // Chuyển sang màn Requests (Lời mời kết bạn)
                startActivity(new Intent(requireContext(), com.example.cashify.ui.FriendsActivity.RequestsActivity.class));
                dismiss();
                break;

            case "WORKSPACE_INVITE":
                // Chuyển sang màn Invitations (Lời mời vào quỹ)
                startActivity(new Intent(requireContext(), InvitationsActivity.class));
                dismiss();
                break;

            case "WORKSPACE_TRANS":
                if (notification.getReferenceId() != null && !notification.getReferenceId().isEmpty()) {
                    Bundle bundle = new Bundle();
                    bundle.putString("WORKSPACE_ID", notification.getReferenceId());
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
                    navController.navigate(R.id.nav_workspace_container, bundle);
                }
                dismiss();
                break;

            case "WORKSPACE_CHAT":
                if (notification.getReferenceId() != null && !notification.getReferenceId().isEmpty()) {
                    Bundle bundle = new Bundle();
                    bundle.putString("WORKSPACE_ID", notification.getReferenceId());

                    bundle.putBoolean("OPEN_CHAT_TAB", true);

                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
                    navController.navigate(R.id.nav_workspace_container, bundle);
                }
                dismiss();
                break;

            default:
                // Không làm gì cả
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                bottomSheet.getLayoutParams().height = (int) (screenHeight * 0.90);
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }
}