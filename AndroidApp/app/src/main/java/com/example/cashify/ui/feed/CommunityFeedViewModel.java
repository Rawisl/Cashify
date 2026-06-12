package com.example.cashify.ui.feed;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.utils.ApiClient;
import com.example.cashify.utils.ApiService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class CommunityFeedViewModel extends ViewModel {
    // LiveData để Fragment đứng dòm
    private final MutableLiveData<List<ApiService.AchievementSuggestion>> achievements = new MutableLiveData<>();
    public LiveData<List<ApiService.AchievementSuggestion>> getAchievements() { return achievements; }

    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return errorMessage; }

    // Logic lấy API đẩy hết vào đây
    public void fetchAvailableAchievements() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        user.getIdToken(true).addOnSuccessListener(tokenResult -> {
            String token = "Bearer " + tokenResult.getToken();
            ApiClient.getClient().create(ApiService.class).getAvailableAchievements(token)
                    .enqueue(new retrofit2.Callback<List<ApiService.AchievementSuggestion>>() {
                        @Override
                        public void onResponse(Call<List<ApiService.AchievementSuggestion>> call, Response<List<ApiService.AchievementSuggestion>> response) {
                            if (response.isSuccessful()) achievements.postValue(response.body());
                            else errorMessage.postValue("Server error: " + response.code());
                        }
                        @Override
                        public void onFailure(Call<List<ApiService.AchievementSuggestion>> call, Throwable t) {
                            errorMessage.postValue("Lỗi mạng: " + t.getMessage());
                        }
                    });
        });
    }
}