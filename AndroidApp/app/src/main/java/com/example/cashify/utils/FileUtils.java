package com.example.cashify.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {

    private static final String TAG = "FileUtils";

    // Retrieves accurate file size from Android ContentResolver
    public static long getFileSizeFromUri(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                    return cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get file size: " + e.getMessage());
        }
        return 0;
    }

    // Converts virtual Uri into a physical temporary File for network upload
    public static File getFileFromUri(Context context, Uri uri) {
        try {
            String fileName = "chat_img_" + System.currentTimeMillis() + ".jpg";
            try (Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIdx != -1) fileName = cursor.getString(nameIdx);
                }
            }

            File outputFile = new File(context.getCacheDir(), "chat_" + System.currentTimeMillis() + "_" + fileName);

            try (InputStream is = context.getContentResolver().openInputStream(uri);
                 OutputStream os = new FileOutputStream(outputFile)) {
                if (is == null) return null;
                byte[] buffer = new byte[4096]; // 4KB buffer for optimal stream copying
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            return outputFile.length() > 0 ? outputFile : null;
        } catch (Exception e) {
            Log.e(TAG, "File conversion failed: " + e.getMessage(), e);
            return null;
        }
    }
}