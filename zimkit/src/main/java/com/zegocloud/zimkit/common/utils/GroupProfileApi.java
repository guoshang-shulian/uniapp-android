package com.zegocloud.zimkit.common.utils;

/**
 * 群资料变更 → 业务后端回写钩子。
 *
 * <p><b>为什么需要</b>：群聊（含 2 人群聊）名称/头像目前只写 ZIM（{@code updateGroupName} /
 * {@code updateGroupAvatarUrl}），业务库完全不知道 → uniapp 侧那些读业务后端的页面
 * （分享组件 share-picker 读 {@code /social/getSortedConversationList} 的 avatar/title）
 * 会一直显示旧值。所以改完 ZIM 还要回写一次业务库：
 * {@code PUT /buyer/social/temp-group/profile { groupId, groupLogo?, groupName? }}。
 *
 * <p><b>为什么要注入</b>：zimkit 是最底层模块（uniplugin_module 依赖它），不能反向引用业务
 * token / baseUrl；所以这里只声明接口，由 uniplugin_module 在 setBusinessConfig 时注册实现
 * （与 {@link MediaUploader#setTokenProvider} 同一套路）。
 *
 * <p><b>失败不打断用户</b>：ZIM 那边已经改成功了（本地/其他成员都能看到新头像），回写失败
 * 只影响 share-picker 这类读业务库的地方，所以只打日志、不弹错误。
 */
public final class GroupProfileApi {

    private GroupProfileApi() {
    }

    public interface BackendSyncCallback {
        void onDone(boolean success, String message);
    }

    /**
     * 把群名称/头像回写业务库。两个值都可为 null（表示这一项没改）。
     */
    public interface BackendSync {
        void syncToBackend(String groupId, String groupName, String groupLogo, BackendSyncCallback callback);
    }

    private static volatile BackendSync backendSync;

    public static void setBackendSync(BackendSync sync) {
        backendSync = sync;
    }

    /**
     * 回写业务库（fire-and-forget）。
     *
     * @param groupName 新群名；null = 本次没改名
     * @param groupLogo 新头像 URL；null = 本次没改头像
     */
    public static void pushToBackend(final String groupId, final String groupName, final String groupLogo) {
        final BackendSync sync = backendSync;
        if (sync == null) {
            android.util.Log.w("GroupProfileApi", "backend sync not registered, skip gid=" + groupId);
            return;
        }
        if (groupId == null || groupId.isEmpty()) {
            return;
        }
        if ((groupName == null || groupName.isEmpty()) && (groupLogo == null || groupLogo.isEmpty())) {
            return;
        }
        new Thread(() -> {
            try {
                sync.syncToBackend(groupId, groupName, groupLogo, (success, message) -> {
                    if (!success) {
                        // 只记日志：ZIM 已改成功，回写失败不影响用户看到的头像/名称
                        android.util.Log.w("GroupProfileApi", "backend sync FAILED gid=" + groupId
                            + " name=" + groupName + " logo=" + groupLogo + " msg=" + message);
                    } else {
                        android.util.Log.i("GroupProfileApi", "backend sync ok gid=" + groupId);
                    }
                });
            } catch (Exception e) {
                android.util.Log.w("GroupProfileApi", "backend sync exception: " + e.getMessage());
            }
        }, "group-profile-sync").start();
    }
}
