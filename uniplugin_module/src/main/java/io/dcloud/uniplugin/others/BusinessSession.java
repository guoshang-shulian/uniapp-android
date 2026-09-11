package io.dcloud.uniplugin.others;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

import io.dcloud.uniplugin.TestModule;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 原生业务会话（token 生命周期的**唯一负责人**）。
 *
 * <p>为什么需要它：原生页面（红包弹窗/领取记录/社群成员管理）会直接调业务接口，
 * 之前 token 是"uniapp 推一次快照、原生拿去用"，一旦 uniapp 侧刷新了 token，
 * 原生就拿着旧 token 一直 403，用户看到「请先登录」。修在 uniapp 的 request.js 里
 * 属于架构倒挂（请求层不该知道原生），所以改成原生自己管：
 *
 * <ul>
 *   <li><b>发请求前</b>：本地解析 JWT 的 exp，快过期就直接刷新 → 从源头避免 401/403</li>
 *   <li><b>真收到 401/403</b>：强制刷新一次后重放（同一次失败只刷一次，不风暴）</li>
 *   <li><b>刷新并发合并</b>：多个请求同时发现过期 → 只发一次刷新请求，其余等结果</li>
 *   <li><b>刷新不了</b>：如实告知调用方重新登录，绝不拿旧 token 硬撞</li>
 * </ul>
 *
 * <p>uniapp 侧只需在启动/登录后调用一次 {@code setBusinessConfig(baseUrl, accessToken, refreshToken, ...)}
 * 做初始化（混合 App 的正常边界），此后原生独立运转；若 uniapp 自己轮换了 token，
 * 也可随时调 {@code notifyBusinessTokenChanged} 同步（可选，不再是必经链路）。
 */
public final class BusinessSession {

    private BusinessSession() {
    }

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder().build();
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");

    /** 提前刷新窗口：token 剩余寿命小于它就当过期（避免"请求发出瞬间刚好过期"） */
    private static final long EXPIRY_SKEW_MS = 60_000L;

    private static volatile String baseUrl = "https://buyer-ceshi.shanxunsw.com/buyer";
    private static volatile String accessToken = "";
    private static volatile String refreshToken = "";

    private static final Object LOCK = new Object();
    private static final AtomicBoolean REFRESHING = new AtomicBoolean(false);

    /** uniapp 初始化 / 推送最新 token（baseUrl 为 null 时不覆盖） */
    public static void setConfig(String url, String access, String refresh) {
        synchronized (LOCK) {
            if (url != null && !url.isEmpty()) {
                baseUrl = url;
            }
            accessToken = access == null ? "" : access;
            refreshToken = refresh == null ? "" : refresh;
            LOCK.notifyAll();
        }
        android.util.Log.i("BusinessSession", "config updated accessLen=" + accessToken.length()
            + " refreshLen=" + refreshToken.length() + " baseUrl=" + baseUrl);
    }

    public static String getBaseUrl() {
        return baseUrl;
    }

    public static String getAccessToken() {
        return accessToken;
    }

    public static String getRefreshToken() {
        return refreshToken;
    }

    /** 登出：清空并阻止后续刷新 */
    public static void clear() {
        synchronized (LOCK) {
            accessToken = "";
            refreshToken = "";
        }
    }

    /** 当前是否有可用凭据（有 access 或能刷新的 refresh） */
    public static boolean hasCredential() {
        return !accessToken.isEmpty() || !refreshToken.isEmpty();
    }

    /**
     * 取一个"现在可用"的 token：过期/缺失则先刷新。
     *
     * <p>返回空串有两种情况，调用方必须区分对待（这是"退出登录后点红包提示过期"的根因）：
     * <ul>
     *   <li><b>从没登录过</b>（access 和 refresh 都空）→ 返回空串，调用方应提示"请先登录"，别再发注定 401 的请求</li>
     *   <li><b>有 refresh 但刷新失败</b>（refresh 也失效）→ 返回空串，调用方应提示"登录已过期"</li>
     * </ul>
     */
    public static String ensureFreshTokenBlocking() {
        String token = accessToken;
        if (!token.isEmpty() && !isExpired(token)) {
            return token;
        }
        if (refreshToken.isEmpty()) {
            // 没有 refresh 可用：不要拿空 token 去打接口（只会换来一个必然的 401）
            return "";
        }
        boolean ok = refreshBlocking();
        return ok ? accessToken : "";
    }

    /** 401/403 之后调用：强制刷新一次（内部合并并发）。刷新失败返回 false */
    public static boolean forceRefreshBlocking() {
        if (refreshToken.isEmpty()) {
            return false;
        }
        return refreshBlocking();
    }

    /**
     * 阻塞式刷新（合并并发：任意时刻只有一个刷新请求在飞，其余线程等结果）。
     *
     * @return true=拿到了新的 accessToken；false=没能刷新（refreshToken 缺失/失效）
     */
    private static boolean refreshBlocking() {
        if (!REFRESHING.compareAndSet(false, true)) {
            // 已有线程在刷新：等它结束（最多 8s），然后看结果
            synchronized (LOCK) {
                long deadline = System.currentTimeMillis() + 8000L;
                while (REFRESHING.get() && System.currentTimeMillis() < deadline) {
                    try {
                        LOCK.wait(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
            return !accessToken.isEmpty();
        }
        boolean ok = false;
        try {
            final String rt = refreshToken;
            if (rt.isEmpty()) {
                return false;
            }
            String url = baseUrl + "/passport/member/refresh/" + rt;
            android.util.Log.i("BusinessSession", "refreshing token…");
            Request req = new Request.Builder().url(url).get().build();
            try (Response res = CLIENT.newCall(req).execute()) {
                String body = res.body() == null ? "" : res.body().string();
                if (!res.isSuccessful()) {
                    android.util.Log.w("BusinessSession", "refresh HTTP " + res.code() + " body=" + brief(body));
                    return false;
                }
                JSONObject json = JSON.parseObject(body);
                JSONObject result = json == null ? null : json.getJSONObject("result");
                String newAccess = result == null ? null : result.getString("accessToken");
                if (newAccess == null || newAccess.isEmpty()) {
                    android.util.Log.w("BusinessSession", "refresh no accessToken, body=" + brief(body));
                    return false;
                }
                String newRefresh = result.getString("refreshToken");
                synchronized (LOCK) {
                    accessToken = newAccess;
                    if (newRefresh != null && !newRefresh.isEmpty()) {
                        refreshToken = newRefresh;
                    }
                }
                ok = true;
                android.util.Log.i("BusinessSession", "refresh ok accessLen=" + newAccess.length());
                // 关键：刷新发生在原生侧，而 uniapp 的 storage 还是旧 token ——
                // 若不回传，两边各持一份会互相踩（一侧刷新后另一侧仍用旧值 → 401 → 再刷 → 抖动）。
                // 这里把新值同步回 uniapp 的登录态（只推这一件事，不参与请求层）。
                TestModule.notifyNativeTokenRefreshed(newAccess, refreshToken);
            }
        } catch (Exception e) {
            android.util.Log.w("BusinessSession", "refresh fail: " + e);
        } finally {
            REFRESHING.set(false);
            synchronized (LOCK) {
                LOCK.notifyAll();   // 唤醒等待刷新的线程
            }
        }
        return ok;
    }

    /** 解析 JWT 的 exp 判断是否过期；解析不出来时保守认为"没过期"（交给 401 兜底） */
    private static boolean isExpired(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) {
                return false;
            }
            String payload = new String(android.util.Base64.decode(
                parts[1], android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP), "UTF-8");
            JSONObject json = JSON.parseObject(payload);
            long exp = json == null ? 0 : json.getLongValue("exp");   // 秒
            if (exp <= 0) {
                return false;
            }
            long expMs = exp * 1000L;
            return System.currentTimeMillis() + EXPIRY_SKEW_MS >= expMs;
        } catch (Exception e) {
            return false;
        }
    }

    private static String brief(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 200 ? s.substring(0, 200) : s;
    }
}
