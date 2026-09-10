package com.zegocloud.zimkit.services.internal;

/** 群设置页动作桥：邀请/踢出/退出 由 uniplugin（TestModule）登记处理器后驱动 */
public class GroupSettingBridge {

    public interface Listener {
        void onInvite(String groupId);
        void onInvitePeer(String peerUserId);
        void onKick(String groupId);
        void onExit(String groupId);
        void onMemberClick(String groupId, String memberUserId);
        /** 群二维码：点击群设置里的「群二维码」栏（带当前群名，便于秒显） */
        void onQrcode(String groupId, String groupName);
    }

    private static Listener sListener;

    public static void setListener(Listener listener) {
        sListener = listener;
    }

    public static Listener getListener() {
        return sListener;
    }
}
