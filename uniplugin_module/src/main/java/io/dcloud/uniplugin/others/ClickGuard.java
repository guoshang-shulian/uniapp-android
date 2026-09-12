package io.dcloud.uniplugin.others;

import android.os.SystemClock;
import android.view.View;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按钮防抖（按 key 独立计时，默认 800ms 窗口）。
 *
 * <p><b>为什么不用 {@code ZIMKitCheckDoubleClick}】</b>：它内部只有一个全局 {@code lastClickTime}，
 * 只能防"同一个按钮连点"；页面上两个不同按钮先后点击会互相把对方判成"连点"。
 * 这里按 key 分别记录，互不干扰。
 *
 * <p><b>用途</b>：原生页的写操作按钮（发消息/店铺/退出/加删好友/踢出/禁言）大多没有防抖，
 * 连点会导致重复请求、重复建群、多次 finish、多次跳转。用一行接入：
 * <pre>
 *   ClickGuard.bind(chatBtn, "chat", this::startChat);
 * </pre>
 *
 * <p>说明：只挡"重复触发"，不做 loading 态（不改 UI）。
 */
public final class ClickGuard {

    /** 默认防抖窗口：800ms（与项目既有的 ZIMKitCheckDoubleClick 默认值一致） */
    public static final long DEFAULT_WINDOW_MS = 800;

    private static final Map<String, Long> LAST_CLICK = new ConcurrentHashMap<>();

    private ClickGuard() {
    }

    /**
     * 是否允许这次点击。
     *
     * @param key 唯一标识（建议 "页面_动作"，如 "MemberInfo_chat"）
     * @return true=允许；false=在窗口内重复点击，应丢弃
     */
    public static boolean allow(String key) {
        return allow(key, DEFAULT_WINDOW_MS);
    }

    public static boolean allow(String key, long windowMs) {
        if (key == null || key.isEmpty()) {
            return true;   // 没给 key 就不拦（调用方问题，但不该因此卡死功能）
        }
        long now = SystemClock.elapsedRealtime();
        Long last = LAST_CLICK.get(key);
        if (last != null && now - last < windowMs) {
            return false;
        }
        LAST_CLICK.put(key, now);
        return true;
    }

    /**
     * 给按钮绑定防抖点击（一行接入）。
     *
     * @param view   目标按钮（为 null 时安全跳过）
     * @param key    唯一标识
     * @param action 真正的动作
     */
    public static void bind(View view, String key, Runnable action) {
        if (view == null || action == null) {
            return;
        }
        view.setOnClickListener(v -> {
            if (allow(key)) {
                action.run();
            } else {
                android.util.Log.i("ClickGuard", "ignored rapid tap: " + key);
            }
        });
    }

    /** 页面销毁时清理自己的 key，避免长期驻留（可选，不做也不影响正确性） */
    public static void clear(String keyPrefix) {
        if (keyPrefix == null) {
            LAST_CLICK.clear();
            return;
        }
        LAST_CLICK.keySet().removeIf(k -> k.startsWith(keyPrefix));
    }
}
