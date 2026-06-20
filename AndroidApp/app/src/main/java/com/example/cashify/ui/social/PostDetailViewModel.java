package com.example.cashify.ui.social;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cashify.data.remote.ApiDto;
import com.example.cashify.utils.ApiClient;
import com.example.cashify.utils.ApiService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostDetailViewModel extends ViewModel {

    private final ApiService apiService = ApiClient.getClient().create(ApiService.class);

    private final MutableLiveData<ApiDto.SocialPostDetailResponse> postDetail = new MutableLiveData<>();
    public LiveData<ApiDto.SocialPostDetailResponse> getPostDetail() { return postDetail; }

    private final MutableLiveData<List<Object>> comments = new MutableLiveData<>();
    public LiveData<List<Object>> getComments() { return comments; }

    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() { return errorMessage; }

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    private final MutableLiveData<Boolean> isActionSuccess = new MutableLiveData<>();
    public LiveData<Boolean> getIsActionSuccess() { return isActionSuccess; }

    // Tải bài viết
    public void loadPost(String postId, String token) {
        isLoading.setValue(true);
        apiService.getPostDetail(postId, token).enqueue(new Callback<ApiDto.SocialPostDetailResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiDto.SocialPostDetailResponse> call, @NonNull Response<ApiDto.SocialPostDetailResponse> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    postDetail.setValue(response.body());
                } else {
                    errorMessage.setValue("Post does not exist");
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiDto.SocialPostDetailResponse> call, @NonNull Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Internet error: " + t.getMessage());
            }
        });
    }

    // Tải bình luận
    public void loadComments(String postId, String token) {
        apiService.getComments(postId, token).enqueue(new Callback<List<Object>>() {
            @Override
            public void onResponse(@NonNull Call<List<Object>> call, @NonNull Response<List<Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    comments.setValue(response.body());
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<Object>> call, @NonNull Throwable t) {
                Log.e("PostDetailVM", "Failed to load comments: " + t.getMessage());
            }
        });
    }

    // Like Bài
    public void toggleLike(String postId, String token, boolean isLiked) {
        apiService.toggleLike(token, new ApiDto.LikeActionRequest(postId, isLiked)).enqueue(new Callback<Object>() {
            @Override public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {}
            @Override public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {}
        });
    }

    // Gửi bình luận
    public void addComment(String postId, String token, String content) {
        apiService.addComment(token, new ApiDto.AddCommentRequest(postId, content)).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                if (response.isSuccessful()) loadComments(postId, token); // Reload lại list
                else errorMessage.setValue("Failed to send comment");
            }
            @Override
            public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                errorMessage.setValue("Network error: " + t.getMessage());
            }
        });
    }

    public void deletePost(String postId, String token) {
        isLoading.setValue(true);
        apiService.deletePost(token, new ApiDto.DeletePostRequest(postId)).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                isLoading.setValue(false);
                if (response.isSuccessful()) isActionSuccess.setValue(true); // Báo về UI để đóng màn hình
                else errorMessage.setValue("Failed to delete post");
            }
            @Override
            public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Network error");
            }
        });
    }

    public void deleteComment(String postId, String commentId, String token) {
        apiService.deleteComment(token, new ApiDto.DeleteCommentRequest(postId, commentId)).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                if (response.isSuccessful()) loadComments(postId, token);
                else errorMessage.setValue("Failed to delete comment");
            }
            @Override public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {}
        });
    }

    public void editComment(String postId, String commentId, String newContent, String token) {
        ApiDto.EditCommentRequest req = new ApiDto.EditCommentRequest();
        req.PostId = postId;
        req.CommentId = commentId;
        req.NewContent = newContent;

        apiService.editComment(token, req).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                if (response.isSuccessful()) loadComments(postId, token);
                else errorMessage.setValue("Failed to update comment");
            }
            @Override public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {}
        });
    }
    // ==========================================
    // API SỬA BÀI VIẾT
    // ==========================================
    public void editPost(String postId, String newContent, String token) {
        isLoading.setValue(true);

        ApiDto.EditPostRequest req = new ApiDto.EditPostRequest();
        req.PostId = postId;
        req.NewContent = newContent;

        apiService.editPost(token, req).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(@NonNull Call<Object> call, @NonNull Response<Object> response) {
                isLoading.setValue(false);
                if (response.isSuccessful()) {
                    loadPost(postId, token); // Sửa xong thì load lại bài mới
                } else {
                    errorMessage.setValue("Failed to update post");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Object> call, @NonNull Throwable t) {
                isLoading.setValue(false);
                errorMessage.setValue("Network error: " + t.getMessage());
            }
        });
    }
}