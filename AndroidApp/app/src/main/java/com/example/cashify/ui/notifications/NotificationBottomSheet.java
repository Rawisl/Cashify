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
import com.example.cashify.ui.FriendsActivity.RequestsActivity;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "NotificationBottomSheet";

    // =========================================================================
    // CONSTANTS: Notification Types
    // =========================================================================
    private static final String TYPE_FRIEND_REQUEST = "FRIEND_REQUEST";
    private static final String TYPE_WORKSPACE_INVITE = "WORKSPACE_INVITE";
    private static final String TYPE_WORKSPACE_TRANS = "WORKSPACE_TRANS";
    private static final String TYPE_WORKSPACE_CHAT = "WORKSPACE_CHAT";
    private static final String TYPE_FRIEND_CHAT = "FRIEND_CHAT";

    private NotificationAdapter adapter;
    private FirebaseFirestore db;
    private String currentUid;
    private ListenerRegistration snapshotListener; // CRITICAL: Used to prevent memory leaks

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_bottom_sheet_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupFirebase();

        if (currentUid != null) {
            loadNotificationsFromFirebase();
        }
    }

    private void initViews(View view) {
        RecyclerView rvNotifications = view.findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize Adapter and delegate click events
        adapter = new NotificationAdapter(requireContext(), this::handleNotificationClick);
        rvNotifications.setAdapter(adapter);
    }

    private void setupFirebase() {
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentUid = currentUser.getUid();
        }
    }

    private void loadNotificationsFromFirebase() {
        // Monitor notifications in real-time, sorted by newest first
        snapshotListener = db.collection("users").document(currentUid).collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Failed to listen to notifications.", e);
                        return;
                    }

                    if (snapshots != null) {
                        List<NotificationItem> list = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            NotificationItem notif = doc.toObject(NotificationItem.class);
                            if (notif != null) {
                                notif.setId(doc.getId()); // Retain ID for read-status updates
                                list.add(notif);
                            }
                        }
                        adapter.setData(list);
                    }
                });
    }

    private void handleNotificationClick(NotificationItem notification) {
        // 1. Mark as read on Firestore if it's currently unread
        if (!notification.isRead()) {
            db.collection("users").document(currentUid).collection("notifications")
                    .document(notification.getId())
                    .update("isRead", true)
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update read status", e));
        }

        // 2. Route user to the appropriate screen based on notification type
        String type = notification.getType() != null ? notification.getType() : "";

        switch (type) {
            case TYPE_FRIEND_REQUEST:
                startActivity(new Intent(requireContext(), RequestsActivity.class));
                dismiss();
                break;

            case TYPE_WORKSPACE_INVITE:
                startActivity(new Intent(requireContext(), InvitationsActivity.class));
                dismiss();
                break;

            case TYPE_WORKSPACE_TRANS:
                if (notification.getReferenceId() != null && !notification.getReferenceId().isEmpty()) {
                    Bundle transBundle = new Bundle();
                    transBundle.putString("WORKSPACE_ID", notification.getReferenceId());
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
                    navController.navigate(R.id.nav_workspace_container, transBundle);
                }
                dismiss();
                break;

            case TYPE_WORKSPACE_CHAT:
                if (notification.getReferenceId() != null && !notification.getReferenceId().isEmpty()) {
                    Bundle chatBundle = new Bundle();
                    chatBundle.putString("WORKSPACE_ID", notification.getReferenceId());
                    chatBundle.putBoolean("OPEN_CHAT_TAB", true); // Auto-open chat tab

                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
                    navController.navigate(R.id.nav_workspace_container, chatBundle);
                }
                dismiss();
                break;

            case TYPE_FRIEND_CHAT:
                if (notification.getReferenceId() != null && !notification.getReferenceId().isEmpty()) {
                    Intent intent = new Intent(requireContext(), com.example.cashify.ui.FriendsActivity.FriendChatActivity.class);
                    // Truyền UID (lấy từ referenceId do Backend trả về) sang Activity
                    intent.putExtra(com.example.cashify.ui.FriendsActivity.FriendChatActivity.EXTRA_FRIEND_UID, notification.getReferenceId());
                    startActivity(intent);
                }
                dismiss();
                break;

            default:
                // Unhandled notification type, do nothing
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
                // Force BottomSheet to occupy 90% of screen height
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                bottomSheet.getLayoutParams().height = (int) (screenHeight * 0.90);

                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // CRITICAL: Remove Firestore listener to prevent memory leaks and duplicate callbacks
        if (snapshotListener != null) {
            snapshotListener.remove();
            snapshotListener = null;
        }
    }
}