package com.zegocloud.zimkit.common.utils;

/**
 * 聊天页「社群店铺」悬浮入口 → 业务后端查询钩子。
 *
 * <p><b>为什么需要</b>：群聊页（含社群频道）要在右上角显示"社群店铺"悬浮球，是否显示由业务后端决定
 * （群主可在社群详情里开关；后端已综合「群开启展示 <b>且</b> 群主有审核通过社群店」）。
 * 轻量接口：{@code GET /buyer/social/group/{id}/store-entry} → {@code { showStore, distributionId, groupId }}，
 * 不调 Zego、不查禁言/成员。
 *
 * <p><b>为什么要注入</b>：zimkit 是最底层模块（uniplugin_module 依赖它），不能反向引用业务
 * token / baseUrl；所以这里只声明接口，由 uniplugin_module 在 setBusinessConfig 时注册实现
 * （与 {@link GroupProfileApi} / {@code MediaUploader#setTokenProvider} 同一套路）。
 *
 * <p><b>失败不打扰用户</b>：拿不到结果（网络失败/超时/非社群群）→ 悬浮球**不显示**，
 * 只打日志，绝不弹错、绝不阻塞聊天页。
 */
public final class StoreEntryApi {

    private StoreEntryApi() {
    }

    /** 查询结果：showStore=true 且 distributionId 非空才展示悬浮球 */
    public static final class Entry {
        public final boolean showStore;
        public final String distributionId;
        public final String groupId;

        public Entry(boolean showStore, String distributionId, String groupId) {
            this.showStore = showStore;
            this.distributionId = distributionId == null ? "" : distributionId;
            this.groupId = groupId == null ? "" : groupId;
        }
    }

    public interface EntryCallback {
        /** entry 为 null 表示失败/不可用 → 调用方按"不显示"处理 */
        void onResult(Entry entry);
    }

    public interface Fetcher {
        void fetch(String groupId, EntryCallback callback);
    }

    private static volatile Fetcher fetcher;

    public static void setFetcher(Fetcher f) {
        fetcher = f;
    }

    /**
     * 异步查询。fetcher 未注册（未登录 / setBusinessConfig 还没到）时回调 null。
     */
    public static void fetch(final String groupId, final EntryCallback callback) {
        final Fetcher f = fetcher;
        if (groupId == null || groupId.isEmpty()) {
            if (callback != null) {
                callback.onResult(null);
            }
            return;
        }
        if (f == null) {
            android.util.Log.w("StoreEntry", "fetcher not registered, skip gid=" + groupId);
            if (callback != null) {
                callback.onResult(null);
            }
            return;
        }
        new Thread(() -> {
            try {
                f.fetch(groupId, entry -> {
                    if (callback != null) {
                        callback.onResult(entry);
                    }
                });
            } catch (Exception e) {
                android.util.Log.w("StoreEntry", "fetch exception gid=" + groupId + " : " + e.getMessage());
                if (callback != null) {
                    callback.onResult(null);
                }
            }
        }, "store-entry").start();
    }

    /** 悬浮球点击 → 跳到 uniapp 社群店铺首页前的日志钩子（便于对照排查） */
    public static void logClick(String groupId, String distributionId) {
        android.util.Log.i("StoreEntry", "ball click gid=" + groupId + " distributionId=" + distributionId
            + " alive=" + ZIMKitActivityUtils.describeAliveActivities());
    }
}
