package io.dcloud.uniplugin.others;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Random;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.concurrent.TimeUnit;

/**
 * 红包业务接口（买家端）。
 * 请求头：accessToken + Authorization Bearer；Query：nonce/timestamp/sign（与 uniapp request.js 同规则）。
 * 开发自测可走 mock（RedPacketNativeMock.ENABLE），上线前必须关闭。
 */
public class RedPacketApi {

    public interface Callback {
        void onSuccess(JSONObject result);
        void onError(int code, String message);
    }

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
    // 红包创建会扣积分，后端可能较慢：超时放大到 60s，避免“积分已扣但前端 timeout”
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build();
    private static final Random RANDOM = new Random();

    public static String baseUrl() {
        return io.dcloud.uniplugin.TestModule.getBusinessBaseUrl();
    }

    public static void create(String token, JSONObject params, Callback callback) {
        // businessBaseUrl 已含 /buyer（App.vue 传的是 api.buyer），所以相对路径用 /social/... 
        post(baseUrl() + "/social/redpacket/create", token, params, callback);
    }

    public static void detail(String token, String redPacketId, String conversationId, Callback callback) {
        JSONObject query = new JSONObject();
        query.put("redPacketId", redPacketId);
        query.put("conversationId", conversationId);
        get(baseUrl() + "/social/redpacket/detail", token, query, callback);
    }

    public static void draw(String token, JSONObject params, Callback callback) {
        post(baseUrl() + "/social/redpacket/draw", token, params, callback);
    }

    public static void get(String url, String token, JSONObject query, Callback callback) {
        StringBuilder sb = new StringBuilder(url);
        sb.append("?");
        boolean first = true;
        for (String key : query.keySet()) {
            if (!first) {
                sb.append("&");
            }
            sb.append(key).append("=").append(query.getString(key));
            first = false;
        }
        request(buildRequest(sb.toString(), token, null, true), callback);
    }

    public static void post(String url, String token, JSONObject body, Callback callback) {
        String signed = signedUrl(url, token);
        Request.Builder builder = new Request.Builder()
            .url(signed)
            .addHeader("accessToken", token == null ? "" : token)
            .addHeader("Authorization", "Bearer " + (token == null ? "" : token))
            .addHeader("uuid", randomUuid());
        if (body == null) {
            body = new JSONObject();
        }
        builder.post(RequestBody.create(body.toJSONString(), JSON_MEDIA_TYPE));
        request(builder.build(), callback);
    }

    private static Request buildRequest(String url, String token, JSONObject body, boolean isGet) {
        String signed = signedUrl(url, token);
        Request.Builder builder = new Request.Builder()
            .url(signed)
            .addHeader("accessToken", token == null ? "" : token)
            .addHeader("Authorization", "Bearer " + (token == null ? "" : token))
            .addHeader("uuid", randomUuid());
        if (isGet) {
            builder.get();
        } else {
            builder.post(RequestBody.create(body == null ? "{}" : body.toJSONString(), JSON_MEDIA_TYPE));
        }
        return builder.build();
    }

    private static String signedUrl(String url, String token) {
        long timestamp = System.currentTimeMillis() / 1000;
        String nonce = randomNonce(6);
        String sign = md5(nonce + timestamp + (token == null ? "" : token));
        return url + "?nonce=" + nonce + "&timestamp=" + timestamp + "&sign=" + sign;
    }

    private static void request(Request request, Callback callback) {
        CLIENT.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                System.out.println("[RedPacketApi] failure url=" + call.request().url()
                    + " tokenLen=" + io.dcloud.uniplugin.TestModule.getBusinessToken().length()
                    + " err=" + e.getMessage());
                if (callback != null) {
                    callback.onError(-1, e.getMessage() == null ? "网络请求失败" : e.getMessage());
                }
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                try (Response res = response) {
                    String body = res.body() == null ? "" : res.body().string();
                    String brief = body.length() > 300 ? body.substring(0, 300) : body;
                    System.out.println("[RedPacketApi] HTTP " + res.code()
                        + " url=" + call.request().url()
                        + " tokenLen=" + io.dcloud.uniplugin.TestModule.getBusinessToken().length()
                        + " body=" + brief);
                    if (callback == null) {
                        return;
                    }
                    try {
                        JSONObject json = JSON.parseObject(body);
                        if (json != null && Boolean.TRUE.equals(json.getBoolean("success"))) {
                            callback.onSuccess(json.getJSONObject("result"));
                        } else {
                            String msg = json == null ? null : json.getString("message");
                            if (msg == null || msg.isEmpty()) {
                                msg = json == null ? null : json.getString("msg");
                            }
                            if (msg == null || msg.isEmpty()) {
                                msg = "HTTP " + res.code();
                            }
                            callback.onError(res.code(), msg);
                        }
                    } catch (Exception parseError) {
                        callback.onError(res.code(), "响应解析失败: " + body);
                    }
                }
            }
        });
    }

    private static String randomNonce(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static String randomUuid() {
        return java.util.UUID.randomUUID().toString();
    }

    private static String md5(String src) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(src.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
