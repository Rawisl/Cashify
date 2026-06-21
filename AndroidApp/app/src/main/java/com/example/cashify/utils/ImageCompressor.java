package com.example.cashify.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

public class ImageCompressor {

    private static final String TAG = "ImageCompressor";
    private static final long COMPRESSION_THRESHOLD = 2 * 1024 * 1024; // 2MB

    // Nén ảnh nếu kích thước vượt ngưỡng cho phép, trả về file gốc nếu lỗi
    public static File compressIfNeeded(File original) {
        if (original == null || original.length() < COMPRESSION_THRESHOLD) {
            return original;
        }

        try {
            Bitmap bitmap = BitmapFactory.decodeFile(original.getAbsolutePath());
            if (bitmap == null) return original;

            // Dynamic quality: dung lượng càng lớn, nén càng mạnh
            int quality = original.length() > 7 * 1024 * 1024 ? 60
                    : original.length() > 4 * 1024 * 1024 ? 72
                    : 82;

            File compressed = new File(original.getParent(), "compressed_" + original.getName());
            try (FileOutputStream out = new FileOutputStream(compressed)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            }
            bitmap.recycle();

            Log.d(TAG, String.format("Compressed: %dKB -> %dKB (Quality: %d)",
                    original.length() / 1024, compressed.length() / 1024, quality));

            return compressed;
        } catch (Exception e) {
            Log.w(TAG, "Compression failed, fallback to original. Error: " + e.getMessage());
            return original;
        }
    }
}