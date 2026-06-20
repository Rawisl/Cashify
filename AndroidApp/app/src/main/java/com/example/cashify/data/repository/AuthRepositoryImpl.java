package com.example.cashify.data.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Firebase implementation of the AuthRepository.
 * Handles all direct interactions with FirebaseAuth.
 */
public class AuthRepositoryImpl implements IAuthRepository {

    private final FirebaseAuth auth;

    public AuthRepositoryImpl() {
        this.auth = FirebaseAuth.getInstance();
    }

    @Override
    public void getAccessToken(TokenCallback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            callback.onFailure("Authentication required.");
            return;
        }

        user.getIdToken(true)
                .addOnSuccessListener(getTokenResult -> {
                    // Attach "Bearer " prefix here so other layers don't have to worry about it
                    callback.onSuccess("Bearer " + getTokenResult.getToken());
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to secure auth token: " + e.getMessage()));
    }

    @Override
    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    @Override
    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }
}