package io.dcloud.uniplugin.friend;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.zegocloud.uikit.plugin.signaling.ZegoSignalingPlugin;

import java.util.ArrayList;

import im.zego.zim.ZIM;
import im.zego.zim.callback.ZIMEventHandler;
import im.zego.zim.entity.ZIMFriendApplicationInfo;
import im.zego.zim.entity.ZIMUserInfo;
import im.zego.zim.enums.ZIMFriendApplicationListChangeAction;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;

/**
 * 好友申请事件桥：监听 ZIM 好友申请变更（新申请/同意/拒绝/过期），
 * 实时回调给 uniapp 更新角标与申请列表。
 * 通过 ZegoSignalingPlugin.registerZIMEventHandler 挂载，不影响 ZIMKit 自身事件。
 */
public class FriendEventBridge extends ZIMEventHandler {

    private static FriendEventBridge sInstance;
    private static UniJSCallback sCallback;
    private static boolean sRegistered = false;

    public static void setCallback(UniJSCallback callback) {
        sCallback = callback;
        if (!sRegistered && callback != null) {
            sRegistered = true;
            ZegoSignalingPlugin.getInstance().registerZIMEventHandler(getInstance());
        }
    }

    private static synchronized FriendEventBridge getInstance() {
        if (sInstance == null) {
            sInstance = new FriendEventBridge();
        }
        return sInstance;
    }

    @Override
    public void onFriendApplicationListChanged(ZIM zim, ArrayList<ZIMFriendApplicationInfo> list,
        ZIMFriendApplicationListChangeAction action) {
        fire(list);
    }

    @Override
    public void onFriendApplicationUpdated(ZIM zim, ArrayList<ZIMFriendApplicationInfo> list) {
        fire(list);
    }

    private void fire(ArrayList<ZIMFriendApplicationInfo> list) {
        if (sCallback == null) {
            return;
        }
        JSONObject result = new JSONObject();
        result.put("event", "friend_application_changed");
        result.put("list", toArray(list));
        try {
            sCallback.invokeAndKeepAlive(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JSONArray toArray(ArrayList<ZIMFriendApplicationInfo> list) {
        JSONArray arr = new JSONArray();
        if (list == null) {
            return arr;
        }
        for (ZIMFriendApplicationInfo info : list) {
            arr.add(toJson(info));
        }
        return arr;
    }

    private JSONObject toJson(ZIMFriendApplicationInfo info) {
        JSONObject obj = new JSONObject();
        if (info == null) {
            return obj;
        }
        ZIMUserInfo user = info.applyUser;
        if (user != null) {
            String zimId = user.userID == null ? "" : user.userID;
            obj.put("zimUserId", zimId);
            obj.put("memberId", zimId.startsWith("user_") ? zimId.substring("user_".length()) : zimId);
            obj.put("userName", user.userName == null ? "" : user.userName);
            obj.put("avatarUrl", user.userAvatarUrl == null ? "" : user.userAvatarUrl);
        } else {
            obj.put("zimUserId", "");
            obj.put("memberId", "");
            obj.put("userName", "");
            obj.put("avatarUrl", "");
        }
        obj.put("wording", info.wording == null ? "" : info.wording);
        obj.put("createTime", info.createTime);
        obj.put("updateTime", info.updateTime);
        obj.put("state", info.state == null ? "" : info.state.name());
        obj.put("type", info.type == null ? "" : info.type.name());
        return obj;
    }
}
