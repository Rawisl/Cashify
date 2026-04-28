package com.example.cashify.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class InvoiceParser {

    public interface ParseCallback {
        void onSuccess(ParsedInvoice result);

        void onFailure(String error);
    }

    public static class ParsedInvoice {
        public long amount;
        public String description;
        public String categoryName;
        public String paymentMethod;
    }

    private static final String API_KEY = "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";
    private static final String API_URL ="https://openrouter.ai/api/v1/chat/completions";

    private static final OkHttpClient CLIENT = new OkHttpClient();

    public static void parse(String ocrText, ParseCallback callback) {
        new Thread(() -> {
            try {
                ParsedInvoice result = callGemini(ocrText);
                callback.onSuccess(result);
            } catch (Exception e) {
                callback.onFailure(e.getMessage());
            }
        }).start();
    }


    private static ParsedInvoice callGemini(String ocrText) throws Exception {
        String categories = "Ăn uống, Cafe, Mua sắm, Di chuyển, Xăng xe, " +
                "Hóa đơn, Giải trí, Tiền trọ, Sức khỏe, Giáo dục, Khác";

        String prompt = "Văn bản OCR từ hóa đơn: " + ocrText + "\n\n" +
                "Hãy trích xuất thông tin và trả về JSON gồm: amount (số nguyên), " +
                "description (tên cửa hàng), category (chọn 1 trong: " + categories + "), " +
                "paymentMethod (Cash, Card, hoặc Bank). Chỉ trả về JSON, không giải thích.";

        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);

        JSONArray messages = new JSONArray();
        messages.put(message);

        JSONObject body = new JSONObject();
        body.put("model", "openrouter/free");
        body.put("messages", messages);

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(),
                        MediaType.get("application/json")))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + ": " + response.body().string());
            }
            return parseOpenRouterResponse(response.body().string());
        }
    }

    private static ParsedInvoice parseOpenRouterResponse(String rawResponse) throws Exception {
        JSONObject resp = new JSONObject(rawResponse);
        String text = resp
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();

        if (text.contains("{")) {
            text = text.substring(text.indexOf("{"), text.lastIndexOf("}") + 1);
        }

        JSONObject json = new JSONObject(text);
        ParsedInvoice result = new ParsedInvoice();
        result.amount = json.optLong("amount", 0);
        result.description = json.optString("description", "Hóa đơn mới");
        result.categoryName = json.optString("category", "Khác");
        result.paymentMethod = json.optString("paymentMethod", "Cash");
        return result;
    }
}