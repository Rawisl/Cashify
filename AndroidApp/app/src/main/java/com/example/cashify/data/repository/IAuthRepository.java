package com.example.cashify.data.repository;

/**
 * Interface defining authentication operations.
 * Decouples the application from specific Auth providers (like Firebase).
 */
public interface IAuthRepository {

    /**
     * Retrieves the authorization token (already prefixed with "Bearer ")
     * for API calls.
     */
    void getAccessToken(TokenCallback callback);

    String getCurrentUserId();

    boolean isLoggedIn();

    interface TokenCallback {
        void onSuccess(String token);
        void onFailure(String error);
    }
}