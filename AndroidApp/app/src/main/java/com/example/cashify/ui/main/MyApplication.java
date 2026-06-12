package com.example.cashify.ui.main;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import com.example.cashify.utils.CurrencyManager;
import java.util.HashMap;
import java.util.Map;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CurrencyManager.init(this);

        // Cấu hình Cloudinary
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", "ddczhtpbu");

        try {
            MediaManager.init(this, config);
        } catch (Exception e) {
            // Try-catch để chặn lỗi nhỡ hệ thống gọi init 2 lần
            e.printStackTrace();
        }
    }
}
