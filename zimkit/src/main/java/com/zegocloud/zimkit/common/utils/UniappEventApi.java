package com.zegocloud.zimkit.common.utils;

/**
 * 原生页 → uniapp 的**全局事件派发**钩子。
 *
 * <p><b>为什么需要注入</b>：zimkit 是最底层模块（uniplugin_module 依赖它），不能反向 import
 * {@code io.dcloud.uniplugin.TestModule}；而这类事件在 uniapp 侧是 {@code App.vue} 的
 * {@code startSyncPipeline(cb)} 回调里处理的，只有业务模块拿得到那个回调。
 * 做法与 {@link StoreEntryApi} / {@link GroupProfileApi} 完全一致：**声明接口 + 由业务模块注册实现**。
 *
 * <p><b>为什么不用反射</b>：曾经用 {@code Class.forName("io.dcloud.uniplugin.TestModule")} + 反射调
 * {@code emitGlobalEvent(String, JSONObject)}，运行时抛
 * {@code NoSuchMethodException: emitGlobalEvent [String, org.json.JSONObject]}
 * —— 两个模块各自持有的 {@code JSONObject} 类不是同一个，反射按精确签名匹配必然失败，
 * 事件被静默吞掉（实测：点社群店铺悬浮球只关原生页、不跳 uniapp）。
 * 注入接口把类型不匹配的问题在**编译期**就消掉了。
 */
public final class UniappEventApi {

    private UniappEventApi() {
    }

    /** 由业务模块（uniplugin_module/TestModule）实现：把事件派发给 uniapp 的全局回调 */
    public interface Emitter {
        /**
         * @param event 事件名（与 App.vue 里 {@code res.event === "XXX"} 对应）
         * @param data  事件数据：类名 + 若干 key/value（value 必须是 String/Integer/Long/Boolean/Double），
         *              可为空表示无数据
         * @return 是否真的发出去了（未拿到 uniapp 回调时为 false）
         */
        boolean emit(String event, String dataClassName, String[] keys, Object[] values);
    }

    private static volatile Emitter emitter;
    /** "通道真的就绪了吗"由业务侧回答（= uniapp 已注册全局回调）；默认视为未就绪 */
    private static volatile ChannelProbe channelProbe;

    /** 通道就绪探针：由业务模块注册（返回 sGlobalJsCallback != null 之类的真实状态） */
    public interface ChannelProbe {
        boolean isReady();
    }

    public static void setEmitter(Emitter e) {
        emitter = e;
    }

    public static void setChannelProbe(ChannelProbe probe) {
        channelProbe = probe;
    }

    /**
     * 事件通道是否真的就绪。
     *
     * <p>⚠️ **不要**用"emitter 已注册"当判据：emitter 在 {@code setBusinessConfig} 时就注册了，
     * 而真正的通道要等 uniapp 调 {@code startSyncPipeline} 才建立 —— 两者是两件事。
     * 用错的判据会让"极冷启动"走到"打开店铺失败，请重试"而不是"请返回应用首页后重试"（实测踩过）。
     */
    public static boolean isReady() {
        final ChannelProbe probe = channelProbe;
        if (probe == null) {
            return false;
        }
        try {
            return probe.isReady();
        } catch (Throwable t) {
            return false;
        }
    }

    // ─────────────────── 极冷启动：事件暂存 + 补发 ───────────────────
    // 场景：进程被杀后从最近任务恢复到原生聊天页（或卡片/悬浮球在 uniapp 起来前被点），
    // 此时事件通道还没注册。旧做法是"要么静默丢弃、要么提示用户自己退回首页"（实测用户只会觉得"点了没反应"）。
    // 现在：**暂存 + 主动拉起 uniapp + 通道就绪时补发**（补发点见 flushPending 的调用方）。
    private static volatile String sPendingEvent;
    private static volatile String[] sPendingKeys;
    private static volatile Object[] sPendingValues;

    /** 由业务模块注入：把 uniapp 主界面拉起来（zimkit 不能反向依赖 uniplugin） */
    public interface Launcher {
        void bringToFront();
    }

    private static volatile Launcher launcher;

    public static void setLauncher(Launcher l) {
        launcher = l;
    }

    /** 拉起 uniapp 主界面（未注入实现时什么都不做） */
    public static void bringUniappToFront() {
        final Launcher l = launcher;
        if (l == null) {
            android.util.Log.w("StoreEntry", "bringUniappToFront skipped: launcher 未注入");
            return;
        }
        try {
            l.bringToFront();
        } catch (Throwable t) {
            android.util.Log.w("StoreEntry", "bringUniappToFront fail: " + t);
        }
    }

    /**
     * 派发事件；**通道未就绪时暂存**并返回 false。
     *
     * <p>调用方拿到 false 时应当去 {@link #bringUniappToFront()}，等 uniapp 注册通道后由业务模块调
     * {@link #flushPending()} 补发 —— 这样用户"点一次就到位"，不需要自己退回首页再进来。
     */
    public static boolean emitOrQueue(String event, String[] keys, Object[] values) {
        if (!isReady()) {
            sPendingEvent = event;
            sPendingKeys = keys;
            sPendingValues = values;
            android.util.Log.i("StoreEntry", "channel not ready → 暂存 event=" + event);
            return false;
        }
        return emit(event, keys, values);
    }

    /** 便捷方法：单键值（未就绪则暂存） */
    public static boolean emitOrQueue(String event, String key, Object value) {
        return emitOrQueue(event, keysOf(key), new Object[]{value});
    }

    /** 便捷方法：两个键值（未就绪则暂存） */
    public static boolean emitOrQueue(String event, String k1, Object v1, String k2, Object v2) {
        return emitOrQueue(event, keysOf(k1, k2), new Object[]{v1, v2});
    }

    /**
     * 通道就绪后补发暂存事件。
     *
     * @return 是否真的补发了（调用方据此收掉压在 uniapp 之上的原生页）
     */
    public static boolean flushPending() {
        final String event = sPendingEvent;
        if (event == null) {
            return false;
        }
        final String[] keys = sPendingKeys;
        final Object[] values = sPendingValues;
        sPendingEvent = null;
        sPendingKeys = null;
        sPendingValues = null;
        android.util.Log.i("StoreEntry", "通道就绪 → 补发暂存 event=" + event);
        return emit(event, keys, values);
    }

    /**
     * 派发事件。未注册实现 / 派发失败都返回 false（调用方据此给用户轻提示，不静默失败）。
     */
    public static boolean emit(String event, String[] keys, Object[] values) {
        final Emitter e = emitter;
        if (e == null) {
            android.util.Log.w("StoreEntry", "emit skipped: UniappEventApi.emitter not registered, event=" + event);
            return false;
        }
        try {
            boolean ok = e.emit(event, null, keys, values);
            android.util.Log.i("StoreEntry", "emit " + event + " ok=" + ok);
            return ok;
        } catch (Throwable t) {
            android.util.Log.w("StoreEntry", "emit fail event=" + event + " : " + t);
            return false;
        }
    }

    private static String[] keysOf(String... keys) {
        return keys;
    }

    /** 便捷方法：单键值 */
    public static boolean emit(String event, String key, Object value) {
        return emit(event, keysOf(key), new Object[]{value});
    }

    /** 便捷方法：两个键值 */
    public static boolean emit(String event, String k1, Object v1, String k2, Object v2) {
        return emit(event, keysOf(k1, k2), new Object[]{v1, v2});
    }
}
