package com.zegocloud.zimkit.common.utils;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 原生页的图片上传（multipart/form-data，字段名 {@code file}）。
 *
 * <p>为什么放在 zimkit 层：群聊设置页 / 群二维码页都是 zimkit 的原生页，而 zimkit 是**最底层模块**
 * （uniplugin_module 依赖 zimkit，反向依赖会成环）。所以上传能力必须自带在 zimkit 里，
 * 不能再依赖 uniplugin_module 的 RedPacketApi。
 *
 * <p>刻意不引入新依赖：用 {@link HttpURLConnection} 做 multipart，用 org.json 解析响应
 * （zimkit 只有 gson，没有 fastjson / okhttp）。
 *
 * <p>token 通过 {@link #setTokenProvider(TokenProvider)} 由上层注入（uniplugin_module 在登录后注册），
 * zimkit 自己不认识业务登录态 —— 没注入 token 也能传（后端若不需要鉴权则无影响）。
 */
public class MediaUploader {

    private static final String TAG = "MediaUploader";

    /** 与 uniapp 侧 api/common.js 的 upload 常量、image-upload 组件默认值一致 */
    public static final String DEFAULT_UPLOAD_URL = "https://test.ioevisa.com/jk.php";

    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 60000;

    /** 取 accessToken 的口子（由上层注入，返回空串表示暂无） */
    public interface TokenProvider {
        String getAccessToken();
    }

    public interface Callback {
        /** @param imageUrl 上传后的图片地址（保证非空） */
        void onSuccess(String imageUrl);

        void onError(String message);
    }

    private static volatile TokenProvider tokenProvider;

    public static void setTokenProvider(TokenProvider provider) {
        tokenProvider = provider;
    }

    private static String currentToken() {
        TokenProvider p = tokenProvider;
        if (p == null) {
            return "";
        }
        try {
            String t = p.getAccessToken();
            return t == null ? "" : t;
        } catch (Exception e) {
            return "";
        }
    }

    /** 上传本地图片文件（后台线程执行，回调也在后台线程 → 调用方自己切主线程） */
    public static void uploadImage(final File file, final Callback callback) {
        uploadImage(file, DEFAULT_UPLOAD_URL, callback);
    }

    public static void uploadImage(final File file, final String uploadUrl, final Callback callback) {
        if (file == null || !file.exists() || file.length() <= 0) {
            if (callback != null) {
                callback.onError("图片文件不存在");
            }
            return;
        }
        new Thread(() -> {
            String result = null;
            String error = null;
            try {
                result = doUpload(file, uploadUrl);
            } catch (Exception e) {
                error = e.getMessage() == null ? "上传失败" : e.getMessage();
            }
            if (callback == null) {
                return;
            }
            if (result != null && !result.isEmpty()) {
                callback.onSuccess(result);
            } else {
                callback.onError(error == null ? "上传成功但未获取到图片地址" : error);
            }
        }, "media-upload").start();
    }

    private static String doUpload(File file, String uploadUrl) throws Exception {
        final String boundary = "----ZIMKitBoundary" + System.currentTimeMillis();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setRequestProperty("Charset", "UTF-8");
            String token = currentToken();
            if (!token.isEmpty()) {
                // 与 uniapp image-upload 组件同口径：accessToken 头
                conn.setRequestProperty("accessToken", token);
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
                writeFormField(out, boundary, "name", "file");
                writeFilePart(out, boundary, "file", file);
                out.writeBytes("--" + boundary + "--\r\n");
                out.flush();
            }

            int code = conn.getResponseCode();
            InputStream in = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            String body = readAll(in);
            Log.i(TAG, "HTTP " + code + " url=" + uploadUrl + " body="
                + (body.length() > 300 ? body.substring(0, 300) : body));
            if (code < 200 || code >= 300) {
                throw new Exception("HTTP " + code);
            }
            String imageUrl = extractImageUrl(body);
            if (imageUrl == null || imageUrl.isEmpty()) {
                throw new Exception("上传成功但未获取到图片地址");
            }
            return imageUrl;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static void writeFormField(DataOutputStream out, String boundary, String name, String value)
        throws Exception {
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        out.write(value.getBytes(StandardCharsets.UTF_8));
        out.writeBytes("\r\n");
    }

    private static void writeFilePart(DataOutputStream out, String boundary, String name, File file)
        throws Exception {
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"; filename=\""
            + file.getName() + "\"\r\n");
        out.writeBytes("Content-Type: " + guessMimeType(file.getName()) + "\r\n\r\n");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = fis.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        }
        out.writeBytes("\r\n");
    }

    private static String readAll(InputStream in) throws Exception {
        if (in == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private static String guessMimeType(String fileName) {
        String n = fileName == null ? "" : fileName.toLowerCase();
        if (n.endsWith(".png")) {
            return "image/png";
        }
        if (n.endsWith(".webp")) {
            return "image/webp";
        }
        if (n.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/jpeg";
    }

    /**
     * 从上传响应里找图片地址。与 uniapp image-upload 的 extractImageUrl 同规则：
     * 支持裸字符串 / 数组 / 常见字段名嵌套（file/result/data/url/src/path…），
     * 后端字段名改了也不用跟着改原生代码。
     */
    public static String extractImageUrl(String body) {
        if (body == null || body.trim().isEmpty()) {
            return "";
        }
        String trimmed = body.trim();
        try {
            Object parsed = new JSONTokener(trimmed).nextValue();
            return findImageUrl(parsed, 0);
        } catch (Exception e) {
            // 有的后端口径就是直接返回一行 URL 文本
            return trimmed.startsWith("http") ? trimmed : "";
        }
    }

    private static final String[] IMAGE_URL_KEYS = {
        "file", "result", "data", "url", "src", "location", "path", "urlPath", "imageUrl", "imgUrl"
    };

    private static String findImageUrl(Object node, int depth) {
        if (node == null || depth > 6) {
            return "";
        }
        if (node instanceof String) {
            String s = ((String) node).trim();
            return s.startsWith("http") ? s : "";
        }
        if (node instanceof JSONArray) {
            JSONArray arr = (JSONArray) node;
            for (int i = 0; i < arr.length(); i++) {
                String found = findImageUrl(arr.opt(i), depth + 1);
                if (!found.isEmpty()) {
                    return found;
                }
            }
            return "";
        }
        if (node instanceof JSONObject) {
            JSONObject obj = (JSONObject) node;
            for (String key : IMAGE_URL_KEYS) {
                if (obj.has(key)) {
                    String found = findImageUrl(obj.opt(key), depth + 1);
                    if (!found.isEmpty()) {
                        return found;
                    }
                }
            }
            // 兜底：只有一个字段的对象 { xxx: "http..." }
            if (obj.length() == 1) {
                java.util.Iterator<String> it = obj.keys();
                while (it.hasNext()) {
                    String found = findImageUrl(obj.opt(it.next()), depth + 1);
                    if (!found.isEmpty()) {
                        return found;
                    }
                }
            }
        }
        return "";
    }
}
