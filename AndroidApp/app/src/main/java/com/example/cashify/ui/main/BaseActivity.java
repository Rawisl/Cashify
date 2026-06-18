package com.example.cashify.ui.main;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;

import com.example.cashify.R;
import com.example.cashify.data.model.User;
import com.example.cashify.data.model.Workspace;
import com.example.cashify.ui.FriendsActivity.FriendsActivity;
import com.example.cashify.ui.auth.EditProfileActivity;
import com.example.cashify.ui.notifications.InvitationsActivity;
import com.example.cashify.ui.workspace.AddWorkspaceBottomSheet;
import com.example.cashify.utils.ImageHelper;
import com.google.android.material.navigation.NavigationView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BaseActivity.java
 * Abstract class providing the Navigation Drawer (Sidebar) logic for all inheriting activities.
 * Strictly adheres to MVVM: Delegates all data fetching to MainViewModel.
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected Map<Integer, String> menuIdToWorkspaceIdMap = new HashMap<>();
    protected MainViewModel mainViewModel;

    // Must be implemented by child activities to handle specific routing
    protected abstract void onNavigationItemSelected(int itemId);

    /**
     * Initializes the Sidebar. Must be called AFTER setContentView() in child activities.
     */
    protected void setupBaseSidebar() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        if (drawerLayout == null || navigationView == null) return;

        // Init ViewModel
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // --- 1. SETUP SIDEBAR HEADER (PROFILE) ---
        View headerView = navigationView.getHeaderView(0);
        if (headerView != null) {
            LinearLayout headerProfileLayout = headerView.findViewById(R.id.headerProfileLayout);
            headerProfileLayout.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                startActivity(new Intent(this, EditProfileActivity.class));
            });
        }

        // --- 2. OBSERVE LIVE DATA (MVVM COMPLIANT) ---
        observeSidebarData();

        // --- 3. HANDLE MENU CLICKS ---
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawer(GravityCompat.START);

            // Dynamic Workspace Routing
            if (menuIdToWorkspaceIdMap.containsKey(id)) {
                String clickedWorkspaceId = menuIdToWorkspaceIdMap.get(id);
                if (this instanceof MainActivity) {
                    onNavigationItemSelected(id);
                    return true;
                } else {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("OPEN_WORKSPACE_ID", clickedWorkspaceId);
                    startActivity(intent);
                    overridePendingTransition(0, 0); // Disable transition animation
                    finish();
                }
            }
            // Personal Workspace
            else if (id == R.id.nav_workspace_personal) {
                if (this instanceof MainActivity) {
                    onNavigationItemSelected(id);
                } else {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                }
            }
            // Friends
            else if (id == R.id.nav_friends) {
                if (!(this instanceof FriendsActivity)) {
                    startActivity(new Intent(this, FriendsActivity.class));
                    overridePendingTransition(0, 0);
                }
            }
            // Invitations
            else if (id == R.id.nav_invitations) {
                if (!(this instanceof InvitationsActivity)) {
                    startActivity(new Intent(this, InvitationsActivity.class));
                    overridePendingTransition(0, 0);
                }
            }
            // Add Workspace (BottomSheet)
            else if (id == R.id.nav_add_workspace) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (this instanceof MainActivity) {
                    new AddWorkspaceBottomSheet().show(getSupportFragmentManager(), "AddWorkspaceBottomSheet");
                } else {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("OPEN_CREATE_WORKSPACE", true);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                }
                return false; // Keep return false to avoid highlighting the button permanently
            }
            // Social Feed
            else if (id == R.id.nav_social) {
                if (this instanceof MainActivity) {
                    onNavigationItemSelected(id);
                } else {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("OPEN_SOCIAL", true);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                }
            }
            return true;
        });
    }

    private void observeSidebarData() {
        // Observe Workspace List
        mainViewModel.getWorkspaces().observe(this, this::updateSidebarMenu);

        // Observe User Profile (Cleaned up Firestore code)
        mainViewModel.getUserProfile().observe(this, this::updateSidebarProfileUI);

        // Observe Invitation Badge (Cleaned up FirebaseManager code)
        mainViewModel.getInvitationCount().observe(this, count -> updateInvitationsBadge(count != null ? count : 0));
    }

    private void updateSidebarProfileUI(User user) {
        if (user == null || navigationView.getHeaderCount() == 0) return;

        View headerView = navigationView.getHeaderView(0);
        TextView tvName = headerView.findViewById(R.id.tvNameHeader);
        TextView tvEmail = headerView.findViewById(R.id.tvEmailHeader);
        ImageView imgAvatar = headerView.findViewById(R.id.imgAvatarHeader);

        if (tvName != null) tvName.setText(user.getDisplayName());
        if (tvEmail != null) tvEmail.setText(user.getEmail());
        if (imgAvatar != null) {
            String fallbackName = user.getNameToShow(); // Utilizing Model's helper
            ImageHelper.loadAvatar(user.getAvatarUrl(), imgAvatar, fallbackName);
        }
    }

    private void updateSidebarMenu(List<Workspace> workspaces) {
        if (workspaces == null) return;
        Menu menu = navigationView.getMenu();

        for (Integer itemId : menuIdToWorkspaceIdMap.keySet()) {
            menu.removeItem(itemId);
        }
        menuIdToWorkspaceIdMap.clear();

        for (Workspace w : workspaces) {
            int itemId = w.getId().hashCode();
            menuIdToWorkspaceIdMap.put(itemId, w.getId());
            MenuItem item = menu.add(R.id.group_workspaces, itemId, 1, w.getName());

            String iconName = w.getIconName();
            if (iconName == null || iconName.isEmpty()) iconName = "ic_other";

            int iconResId = getResources().getIdentifier(iconName, "drawable", getPackageName());
            item.setIcon(iconResId != 0 ? iconResId : R.drawable.ic_other);
            item.setCheckable(true);
        }
    }

    private void updateInvitationsBadge(int count) {
        Menu menu = navigationView.getMenu();
        MenuItem inviteItem = menu.findItem(R.id.nav_invitations);
        if (inviteItem == null) return;

        if (count <= 0) {
            inviteItem.setTitle(getString(R.string.invite));
            return;
        }

        String title = getString(R.string.invite);
        String badgeText = count > 9 ? "9+" : String.valueOf(count);

        TextView tv = new TextView(this);
        tv.setText(badgeText);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(10);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setBackgroundResource(R.drawable.bg_badge_red);
        tv.setGravity(Gravity.CENTER);

        int sizePx = (int) (20 * getResources().getDisplayMetrics().density);
        tv.measure(View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY));
        tv.layout(0, 0, tv.getMeasuredWidth(), tv.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(tv.getMeasuredWidth(), tv.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        tv.draw(new Canvas(bitmap));

        BitmapDrawable bd = new BitmapDrawable(getResources(), bitmap);
        int yOffset = (int) (3 * getResources().getDisplayMetrics().density);
        bd.setBounds(0, -yOffset, bitmap.getWidth(), bitmap.getHeight() - yOffset);

        SpannableStringBuilder ssb = new SpannableStringBuilder(title + "  ");
        ssb.setSpan(new ImageSpan(bd), title.length() + 1, title.length() + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        inviteItem.setTitle(ssb);
    }
}