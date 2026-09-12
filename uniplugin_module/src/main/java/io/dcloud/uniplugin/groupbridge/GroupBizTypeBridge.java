package io.dcloud.uniplugin.groupbridge;

import java.util.ArrayList;
import java.util.HashMap;

import im.zego.zim.ZIM;
import im.zego.zim.callback.ZIMEventHandler;
import im.zego.zim.entity.ZIMError;
import im.zego.zim.entity.ZIMGroupMemberInfo;
import im.zego.zim.entity.ZIMGroupMemberQueryConfig;
import im.zego.zim.entity.ZIMGroupOperatedInfo;
import im.zego.zim.enums.ZIMErrorCode;
import im.zego.zim.enums.ZIMGroupMemberEvent;
import im.zego.zim.enums.ZIMGroupMemberState;
import com.zegocloud.uikit.plugin.signaling.ZegoSignalingPlugin;

/**
 * 私聊群(2人) 自动升级普通群聊：
 * bizType=private 的群，一旦成员数 > 2（拉人），自动把群属性改为 temp。
 * 通过 ZegoSignalingPlugin.registerZIMEventHandler 挂载，不影响 ZIMKit 自身事件。
 */
public class GroupBizTypeBridge extends ZIMEventHandler {

    private static GroupBizTypeBridge sInstance;
    private static boolean sRegistered = false;

    public static void register() {
        if (!sRegistered) {
            sRegistered = true;
            ZegoSignalingPlugin.getInstance().registerZIMEventHandler(getInstance());
        }
    }

    private static synchronized GroupBizTypeBridge getInstance() {
        if (sInstance == null) {
            sInstance = new GroupBizTypeBridge();
        }
        return sInstance;
    }

    @Override
    public void onGroupMemberStateChanged(ZIM zim, ZIMGroupMemberState state, ZIMGroupMemberEvent event,
        ArrayList<ZIMGroupMemberInfo> userList, ZIMGroupOperatedInfo operatedInfo, String groupID) {
        if (groupID == null || groupID.isEmpty()) {
            return;
        }
        try {
            zim.queryGroupAllAttributes(groupID, (gid, attributes, errorInfo) -> {
                if (errorInfo != null && errorInfo.code == ZIMErrorCode.SUCCESS
                    && attributes != null && "private".equals(attributes.get("bizType"))) {
                    ZIMGroupMemberQueryConfig config = new ZIMGroupMemberQueryConfig();
                    config.count = 100;
                    zim.queryGroupMemberList(groupID, config, (g2, members, flag, err2) -> {
                        if (err2 != null && err2.code == ZIMErrorCode.SUCCESS
                            && members != null && members.size() > 2) {
                            HashMap<String, String> attrs = new HashMap<>();
                            attrs.put("bizType", "temp");
                            zim.setGroupAttributes(attrs, groupID, (g3, keys, err3) -> {
                                // 升级完成
                            });
                            // 升级为群聊后，群名自动改为“群聊”（后续成员可再改名）
                            zim.updateGroupName("聊天", groupID, (g4, newName, err4) -> {
                            });
                        }
                    });
                }
            });
        } catch (Exception ignored) {
        }
    }
}
