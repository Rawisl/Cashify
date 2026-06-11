package com.example.cashify.ui.social;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.cashify.R;

public class SocialContainerFragment extends Fragment {
    private static final int ACTIVE_COLOR = Color.rgb(26, 35, 126);
    private static final int INACTIVE_COLOR = Color.rgb(96, 125, 139);

    private View navNewsfeed;
    private View navProfile;
    private ImageView imgNewsfeed;
    private ImageView imgProfile;
    private TextView txtNewsfeed;
    private TextView txtProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_social_container, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupInnerNavigation(view);
    }

    private void setupInnerNavigation(View view) {
        NavHostFragment navHostFragment = (NavHostFragment) getChildFragmentManager()
                .findFragmentById(R.id.social_inner_nav_host);
        if (navHostFragment == null) return;

        NavController navController = navHostFragment.getNavController();
        navNewsfeed = view.findViewById(R.id.navSocialNewsfeed);
        navProfile = view.findViewById(R.id.navSocialProfile);
        imgNewsfeed = view.findViewById(R.id.imgSocialNewsfeed);
        imgProfile = view.findViewById(R.id.imgSocialProfile);
        txtNewsfeed = view.findViewById(R.id.txtSocialNewsfeed);
        txtProfile = view.findViewById(R.id.txtSocialProfile);

        navNewsfeed.setOnClickListener(v -> navigateIfNeeded(navController, R.id.nav_social_newsfeed));
        navProfile.setOnClickListener(v -> navigateIfNeeded(navController, R.id.nav_social_profile));

        navController.addOnDestinationChangedListener((controller, destination, arguments) ->
                updateSelectedState(destination.getId()));
        updateSelectedState(navController.getCurrentDestination() == null
                ? R.id.nav_social_newsfeed
                : navController.getCurrentDestination().getId());
    }

    private void navigateIfNeeded(NavController navController, int destinationId) {
        if (navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == destinationId) {
            return;
        }
        navController.navigate(destinationId);
    }

    private void updateSelectedState(int destinationId) {
        boolean newsfeedSelected = destinationId == R.id.nav_social_newsfeed;
        bindNavItem(navNewsfeed, imgNewsfeed, txtNewsfeed, newsfeedSelected);
        bindNavItem(navProfile, imgProfile, txtProfile, !newsfeedSelected);
    }

    private void bindNavItem(View item, ImageView icon, TextView label, boolean selected) {
        if (item == null || icon == null || label == null) return;
        int color = selected ? ACTIVE_COLOR : INACTIVE_COLOR;
        float inactiveAlpha = getResources().getFraction(R.fraction.bottom_nav_social_inactive_alpha, 1, 1);
        item.setSelected(selected);
        item.setAlpha(selected ? 1f : inactiveAlpha);
        icon.setColorFilter(color);
        label.setTextColor(color);
        label.setTypeface(label.getTypeface(), selected ? Typeface.BOLD : Typeface.NORMAL);
    }
}
