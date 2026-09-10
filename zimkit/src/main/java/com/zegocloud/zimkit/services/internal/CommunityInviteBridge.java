package com.zegocloud.zimkit.services.internal;

/** 社群邀请卡片动作桥：确认加入 → uniplugin（TestModule）执行申请/直接加入 */
public class CommunityInviteBridge {

    public interface Callback {
        /** @param joined true=已直接加入；false=已提交申请（待审核） */
        void onResult(boolean joined, String message);
    }

    public interface Listener {
        void applyJoin(String groupId, Callback callback);
    }

    private static Listener sListener;

    public static void setListener(Listener listener) {
        sListener = listener;
    }

    public static Listener getListener() {
        return sListener;
    }
}
