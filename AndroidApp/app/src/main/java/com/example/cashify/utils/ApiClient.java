package com.example.cashify.utils;

import com.example.cashify.BuildConfig;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// Singleton class để khởi tạo Retrofit Client giao tiếp với Backend C#
public class ApiClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    // Tự động load URL từ local.properties thông qua BuildConfig để bảo mật
                    .baseUrl(BuildConfig.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}