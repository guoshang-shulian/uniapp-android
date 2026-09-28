package com.zegocloud.zimkit.common.utils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 群属性 {@code bizType} 的**内存缓存**：只为"进页面第一帧就对"服务，不作为业务真相。
 *
 * <p><b>为什么需要</b>：聊天设置页要按 {@code bizType} 决定隐藏哪些项（社群频道要隐藏退出群聊、群二维码、
 * 群ID+复制、群聊名称行…），而 {@code bizType} 只能靠异步 {@code queryGroupAllAttributes} 拿。
 * 等它回来的这段时间页面会按"普通群聊"渲染 → 用户看到的是"进去先一堆项、过一会儿才消失"（实测反馈）。
 *
 * <p><b>做法</b>：把**已经查到过**的 {@code bizType} 存下来，设置页 {@code onCreate} 能**同步**读到 →
 * 第一帧就是最终布局；异步结果回来后覆盖缓存并再纠正一次 UI（兜住缓存猜错/过期）。
 *
 * <p><b>写入点</b>（都只在真的拿到群信息时写，查失败不动缓存，避免把"未知"写成"非社群"）：
 * <ol>
 *   <li>{@code ZIMKitMessageActivity.setupStoreBall()} 的 {@code queryGroupInfo}（每个群聊页都会跑，已拿到 bizType）</li>
 *   <li>{@code ZIMKitGroupChatSettingActivity} 的 {@code queryGroupAllAttributes} 回调（权威结果）</li>
 * </ol>
 *
 * <p>只存内存、不落盘：进程重启后第一次仍可能不命中，此时退回原来的"异步判定"行为，不会更差。
 */
public final class GroupBizTypeCache {

    /** 社群频道的 bizType 取值（与后端/ZIM 群属性约定一致） */
    public static final String BIZ_TYPE_COMMUNITY = "community";

    private static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<>();

    private GroupBizTypeCache() {
    }

    /** 记录某群的 bizType（groupId 为空则忽略；bizType 为 null 按空串存 = "非社群"） */
    public static void put(String groupId, String bizType) {
        if (groupId == null || groupId.isEmpty()) {
            return;
        }
        CACHE.put(groupId, bizType == null ? "" : bizType);
    }

    /** 取缓存的 bizType；没有缓存时返回空串（调用方据此走"未知"分支） */
    public static String get(String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            return "";
        }
        String bizType = CACHE.get(groupId);
        return bizType == null ? "" : bizType;
    }

    /** 缓存里是否明确是社群频道 */
    public static boolean isCommunity(String groupId) {
        return BIZ_TYPE_COMMUNITY.equals(get(groupId));
    }
}
