package com.example.cashify.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.example.cashify.R;
import com.example.cashify.data.model.Workspace;
import com.example.cashify.data.remote.FirebaseManager;
import com.example.cashify.ui.auth.EditProfileActivity;
import com.example.cashify.utils.ImageHelper;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected Map<Integer, String> menuIdToWorkspaceIdMap = new HashMap<>();
    protected MainViewModel mainViewModel;

    protected abstract void onNavigationItemSelected(int itemId);


    // HÀM NÀY ĐỂ CÁC MÀN HÌNH CON GỌI SAU KHI setContentView
    protected void setupBaseSidebar() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        if (drawerLayout == null || navigationView == null) return;

        updateSidebarProfileUI();

        // 1. CLICK PROFILE -> MỞ SETTING
        View headerView = navigationView.getHeaderView(0);
        LinearLayout headerProfileLayout = headerView.findViewById(R.id.headerProfileLayout);
        headerProfileLayout.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(this, EditProfileActivity.class));
        });

        // 2. CLICK MENU ITEM -> CHUYỂN MÀN HÌNH THÔNG MINh
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawer(GravityCompat.START);

            // Xử lý các điều hướng chung (ai cũng giống nhau)
            if (menuIdToWorkspaceIdMap.containsKey(id)) {
                String clickedWorkspaceId = menuIdToWorkspaceIdMap.get(id);
                if (this instanceof MainActivity) {
                    onNavigationItemSelected(id);
                    return true;
                } else {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("OPEN_WORKSPACE_ID", clickedWorkspaceId);
                    startActivity(intent);
                    overridePendingTransition(0, 0); // TẮT HIỆU ỨNG CHUYỂN CẢNH
                    finish();
                }
            } else if (id == R.id.nav_workspace_personal) {
                if (this instanceof MainActivity) {
                    onNavigationItemSelected(id);
                } else {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                }
            } else if (id == R.id.nav_friends) {
                if (!(this.getClass().getSimpleName().equals("FriendsActivity"))) {
                    startActivity(new Intent(this, com.example.cashify.ui.FriendsActivity.FriendsActivity.class));
                    overridePendingTransition(0, 0); // TẮT HIỆU ỨNG CHUYỂN CẢNH
                }
            } else if (id == R.id.nav_invitations) {
                if (!(this.getClass().getSimpleName().equals("InvitationsActivity"))) {
                    startActivity(new Intent(this, com.example.cashify.ui.notifications.InvitationsActivity.class));
                    overridePendingTransition(0, 0); // TẮT HIỆU ỨNG CHUYỂN CẢNH
                }
            }else if (id == R.id.nav_add_workspace) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (this instanceof MainActivity) {
                    // Đang ở MainActivity thì show thẳng
                    new com.example.cashify.ui.workspace.AddWorkspaceBottomSheet()
                            .show(getSupportFragmentManager(), "AddWorkspaceBottomSheet");
                } else {
                    // Đang ở Activity khác thì bay về MainActivity
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("OPEN_CREATE_WORKSPACE", true);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                }
                return false; // ⚠️ Giữ nguyên return false, không đổi thành true
            }else if (id == R.id.nav_social) {
                // Đang ở MainActivity thì navigate qua NavController
                if (this instanceof MainActivity) {
                    onNavigationItemSelected(id);
                } else {
                    // Activity khác thì quay về MainActivity và mở Social
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("OPEN_SOCIAL", true);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                }
            }
            return true;
        });

        // 3. LOAD DANH SÁCH QUỸ LÊN SIDEBAR
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
            mainViewModel.getWorkspaces().observe(this, this::updateSidebarMenu);
            mainViewModel.loadWorkspaces(currentUser.getUid());
        }

        // 4. LẮNG NGHE CHẤM ĐỎ LỜI MỜI
        FirebaseManager.getInstance().listenToWorkspaceInvitations(new FirebaseManager.DataCallback<>() {
            @Override
            public void onSuccess(List<com.example.cashify.data.model.WorkspaceInvitation> data) {
                updateInvitationsBadge((data != null) ? data.size() : 0);
            }
            @Override
            public void onError(String message) { updateInvitationsBadge(0); }
        });
    }

    // ==========================================
    // CÁC HÀM TIỆN ÍCH (COPY TỪ MAIN ACTIVITY QUA)
    // ==========================================
    private void updateSidebarProfileUI() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && navigationView.getHeaderCount() > 0) {
            View headerView = navigationView.getHeaderView(0);
            TextView tvName = headerView.findViewById(R.id.tvNameHeader);
            TextView tvEmail = headerView.findViewById(R.id.tvEmailHeader);
            android.widget.ImageView imgAvatar = headerView.findViewById(R.id.imgAvatarHeader);

            tvEmail.setText(currentUser.getEmail());
            if (imgAvatar != null) {
                ImageHelper.loadAvatar(currentUser.getPhotoUrl(), imgAvatar,
                        firstNonEmpty(currentUser.getDisplayName(), currentUser.getEmail(), currentUser.getUid()));
            }
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users")
                    .document(currentUser.getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String displayName = doc.getString("displayName");
                            if (tvName != null) tvName.setText(displayName);
                            if (imgAvatar != null) {
                                ImageHelper.loadAvatar(doc.getString("avatarUrl"), imgAvatar,
                                        firstNonEmpty(displayName, currentUser.getEmail(), currentUser.getUid()));
                            }
                        }
                    });
        }
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private void updateSidebarMenu(List<Workspace> workspaces) {
        android.view.Menu menu = navigationView.getMenu();
        for (Integer itemId : menuIdToWorkspaceIdMap.keySet()) menu.removeItem(itemId);
        menuIdToWorkspaceIdMap.clear();

        for (Workspace w : workspaces) {
            int itemId = w.getId().hashCode();
            menuIdToWorkspaceIdMap.put(itemId, w.getId());
            android.view.MenuItem item = menu.add(R.id.group_workspaces, itemId, 1, w.getName());

            String iconName = w.getIconName();
            if (iconName == null || iconName.isEmpty()) iconName = "ic_other";

            int iconResId = getResources().getIdentifier(iconName, "drawable", getPackageName());
            item.setIcon(iconResId != 0 ? iconResId : R.drawable.ic_other);
            item.setCheckable(true);
        }
    }

    private void updateInvitationsBadge(int count) {
        android.view.Menu menu = navigationView.getMenu();
        android.view.MenuItem inviteItem = menu.findItem(R.id.nav_invitations);
        if (count <= 0) {
            inviteItem.setTitle("Lời mời");
            return;
        }

        String title = "Lời mời";
        String badgeText = count > 9 ? "9+" : String.valueOf(count);

        TextView tv = new TextView(this);
        tv.setText(badgeText);
        tv.setTextColor(android.graphics.Color.WHITE);
        tv.setTextSize(10);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setBackgroundResource(R.drawable.bg_badge_red);
        tv.setGravity(android.view.Gravity.CENTER);

        int sizePx = (int) (20 * getResources().getDisplayMetrics().density);
        tv.measure(View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY));
        tv.layout(0, 0, tv.getMeasuredWidth(), tv.getMeasuredHeight());

        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(tv.getMeasuredWidth(), tv.getMeasuredHeight(), android.graphics.Bitmap.Config.ARGB_8888);
        tv.draw(new android.graphics.Canvas(bitmap));

        android.graphics.drawable.BitmapDrawable bd = new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
        int yOffset = (int) (3 * getResources().getDisplayMetrics().density);
        bd.setBounds(0, -yOffset, bitmap.getWidth(), bitmap.getHeight() - yOffset);

        android.text.SpannableStringBuilder ssb = new android.text.SpannableStringBuilder(title + "  ");
        ssb.setSpan(new android.text.style.ImageSpan(bd), title.length() + 1, title.length() + 2, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        inviteItem.setTitle(ssb);
    }
}
