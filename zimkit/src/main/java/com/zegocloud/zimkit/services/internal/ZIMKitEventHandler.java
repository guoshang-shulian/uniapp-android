package com.zegocloud.zimkit.services.internal;

import android.util.Log;

import com.zegocloud.zimkit.components.message.ui.BackToUniappCallback;
import com.zegocloud.zimkit.services.ZIMKitConfig;
import com.zegocloud.zimkit.services.model.ZIMKitConversation;
import com.zegocloud.zimkit.services.model.ZIMKitMessage;
import com.zegocloud.zimkit.services.utils.MessageTransform;
import com.zegocloud.zimkit.services.utils.ZIMMessageUtil;
import im.zego.zim.ZIM;
import im.zego.zim.callback.ZIMEventHandler;
import im.zego.zim.entity.ZIMConversationChangeInfo;
import im.zego.zim.entity.ZIMGroupMemberInfo;
import im.zego.zim.entity.ZIMGroupOperatedInfo;
import im.zego.zim.entity.ZIMMessage;
import im.zego.zim.entity.ZIMMessageReaction;
import im.zego.zim.entity.ZIMRevokeMessage;
import im.zego.zim.enums.ZIMConnectionEvent;
import im.zego.zim.enums.ZIMConnectionState;
import im.zego.zim.enums.ZIMConversationEvent;
import im.zego.zim.enums.ZIMConversationType;
import im.zego.zim.enums.ZIMGroupMemberEvent;
import im.zego.zim.enums.ZIMGroupMemberState;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONObject;
import timber.log.Timber;

public class ZIMKitEventHandler extends ZIMEventHandler {

    private boolean kickedOutAccount = false;

    private static CallCallback mListener;

    /** 会话变化实时同步回调（TestModule 注册 → 秒级推送到 uniapp） */
    public interface ConversationChangeCallback {
        void onConversationEvent();
    }

    private static ConversationChangeCallback mConvListener;

    public static void setConversationChangeCallback(ConversationChangeCallback listener) {
        mConvListener = listener;
    }

    private static void notifyConversationChange() {
        if (mConvListener != null) {
            try {
                mConvListener.onConversationEvent();
            } catch (Exception ignored) {
            }
        }
    }

    /** 群申请列表变化实时同步回调（TestModule 注册 → 秒级角标/审核列表） */
    public interface GroupApplicationCallback {
        void onGroupApplicationEvent();
    }

    private static GroupApplicationCallback mAppListener;

    public static void setGroupApplicationCallback(GroupApplicationCallback listener) {
        mAppListener = listener;
    }

    private static void notifyGroupApplicationEvent() {
        if (mAppListener != null) {
            try {
                mAppListener.onGroupApplicationEvent();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 群资料（群名/群头像）变更回调。
     *
     * <p>为什么需要：`updateGroupName` / `updateGroupAvatarUrl` 成功只代表**服务端群资料**改了，
     * ZIM **不会**自动把新的群名/头像写回本地已有的会话对象（`ZIMConversation.conversationName`
     * / `conversationAvatarUrl`）。结果就是「群设置页显示新头像，会话列表还是旧的/首字方块」。
     * 这里把变更广播出去，让 TestModule 重新拉一次会话列表 + 群资料并合并后重推 uniapp。
     */
    public interface GroupProfileCallback {
        void onGroupProfileChanged(String groupID);
    }

    private static GroupProfileCallback mGroupProfileListener;

    public static void setGroupProfileCallback(GroupProfileCallback listener) {
        mGroupProfileListener = listener;
    }

    private static void notifyGroupProfileChanged(String groupID) {
        if (mGroupProfileListener != null) {
            try {
                mGroupProfileListener.onGroupProfileChanged(groupID);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 供 zimkit 内部主动触发群资料变更通知（不依赖 ZIM 事件）。
     * <p>场景：本地调用 {@code updateGroupAvatarUrl} / {@code updateGroupName} 成功后，
     * ZIM 不一定回调 {@code onGroupAvatarUrlUpdated}；但上层（TestModule）需要立刻刷新
     * 「群头像覆盖表」并重推 uniapp 会话列表，否则列表头像不会变。
     */
    public static void notifyGroupProfileChangedFromLocal(String groupID) {
        notifyGroupProfileChanged(groupID);
    }

    @Override
    public void onGroupNameUpdated(ZIM zim, String groupName, ZIMGroupOperatedInfo operatedInfo,
        String groupID) {
        super.onGroupNameUpdated(zim, groupName, operatedInfo, groupID);
        notifyGroupProfileChanged(groupID);
    }

    @Override
    public void onGroupAvatarUrlUpdated(ZIM zim, String groupAvatarUrl, ZIMGroupOperatedInfo operatedInfo,
        String groupID) {
        super.onGroupAvatarUrlUpdated(zim, groupAvatarUrl, operatedInfo, groupID);
        notifyGroupProfileChanged(groupID);
    }

    private  String groupId;

    public static void setOnNativeDataListener(CallCallback listener) {
        mListener = listener;
    }

    @Override
    public void onConnectionStateChanged(ZIM zim, ZIMConnectionState state, ZIMConnectionEvent event,
        JSONObject extendedData) {
        super.onConnectionStateChanged(zim, state, event, extendedData);
        Timber.d(
            "onConnectionStateChanged() called with: zim = [" + zim + "], state = [" + state + "], event = [" + event
                + "], extendedData = [" + extendedData + "]");
        if (state == ZIMConnectionState.DISCONNECTED && event == ZIMConnectionEvent.KICKED_OUT) {
            kickedOutAccount = true;
        }
        ZIMKitCore.getInstance().getZimkitNotifyList().notifyAllListener(zimKitDelegate -> {
            zimKitDelegate.onConnectionStateChange(state, event);
        });
    }

    @Override
    public void onGroupApplicationListChanged(ZIM zim,
        ArrayList<im.zego.zim.entity.ZIMGroupApplicationInfo> applicationInfoList,
        im.zego.zim.enums.ZIMGroupApplicationListChangeAction action) {
        super.onGroupApplicationListChanged(zim, applicationInfoList, action);
        notifyGroupApplicationEvent();
    }

    @Override
    public void onGroupApplicationUpdated(ZIM zim,
        ArrayList<im.zego.zim.entity.ZIMGroupApplicationInfo> applicationInfoList) {
        super.onGroupApplicationUpdated(zim, applicationInfoList);
        notifyGroupApplicationEvent();
    }

    @Override
    public void onConversationChanged(ZIM zim, ArrayList<ZIMConversationChangeInfo> conversationChangeInfoList) {
        super.onConversationChanged(zim, conversationChangeInfoList);
        handlerConversationChange(conversationChangeInfoList);
    }

    @Override
    public void onConversationTotalUnreadMessageCountUpdated(ZIM zim, int totalUnreadMessageCount) {
        super.onConversationTotalUnreadMessageCountUpdated(zim, totalUnreadMessageCount);
        ZIMKitCore.getInstance().setTotalUnreadMessageCount(totalUnreadMessageCount);
        ZIMKitCore.getInstance().getZimkitNotifyList().notifyAllListener(zimKitDelegate -> {
            zimKitDelegate.onTotalUnreadMessageCountChange(totalUnreadMessageCount);
        });
        notifyConversationChange();
    }

    @Override
    public void onTokenWillExpire(ZIM zim, int second) {
        super.onTokenWillExpire(zim, second);
        ZIMKitCore.getInstance().getZimkitNotifyList()
            .notifyAllListener(zimKitDelegate -> zimKitDelegate.onTokenWillExpire(second));
    }

    @Override
    public void onReceivePeerMessage(ZIM zim, ArrayList<ZIMMessage> messageList, String fromUserID) {
        super.onReceivePeerMessage(zim, messageList, fromUserID);
        handleReceiveNewMessages(messageList);
    }

    @Override
    public void onReceiveGroupMessage(ZIM zim, ArrayList<ZIMMessage> messageList, String fromGroupID) {

        ZIMMessage mk = messageList.get(0);
        if(mk.getExtendedData() != null && mListener != null){
            if (mk.getExtendedData().startsWith("start")) {
                String cleanedData = mk.getExtendedData().substring(5);
                mListener.callInfo(cleanedData);
                return;
            } else if (mk.getExtendedData().startsWith("stop")) {
                //String cleanedData = mk.getExtendedData().substring(5)
              mListener.callInfo(mk.getExtendedData());
                return;
            } else {
                // Handle other cases
            }
        }
        System.out.println("mListener = "+mListener);
        super.onReceiveGroupMessage(zim, messageList, fromGroupID);
        handleReceiveNewMessages(messageList);

    }

    @Override
    public void onGroupMemberInfoUpdated(ZIM zim, ArrayList<ZIMGroupMemberInfo> userList,
        ZIMGroupOperatedInfo operatedInfo, String groupID) {
        super.onGroupMemberInfoUpdated(zim, userList, operatedInfo, groupID);
        // 成员信息变化（含个体被禁言/解禁、昵称头像等）→ 刷新聊天页禁言态
        notifyMuteChanged(groupID);
    }

    @Override
    public void onGroupMutedInfoUpdated(ZIM zim, im.zego.zim.entity.ZIMGroupMuteInfo muteInfo,
        ZIMGroupOperatedInfo operatedInfo, String groupID) {
        super.onGroupMutedInfoUpdated(zim, muteInfo, operatedInfo, groupID);
        // 全员禁言开关变化 → 刷新聊天页禁言态
        notifyMuteChanged(groupID);
    }

    /** 禁言相关事件 → 让当前打开的聊天页立即刷新禁言条（秒级，不用退出重进） */
    private static void notifyMuteChanged(String groupID) {
        try {
            com.zegocloud.zimkit.components.message.ui.ZIMKitMessageFragment.notifyMuteEvent(groupID);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onGroupMemberStateChanged(ZIM zim, ZIMGroupMemberState state, ZIMGroupMemberEvent event,
        ArrayList<ZIMGroupMemberInfo> userList, ZIMGroupOperatedInfo operatedInfo, String groupID) {
        super.onGroupMemberStateChanged(zim, state, event, userList, operatedInfo, groupID);
        ZIMKitCore.getInstance().onGroupMemberStateChanged(state, event, userList, operatedInfo, groupID);
        ZIMKitCore.getInstance().getZimkitNotifyList().notifyAllListener(zimKitDelegate -> {
            zimKitDelegate.onGroupMemberStateChanged(state, event, userList, operatedInfo, groupID);
        });
    }

    @Override
    public void onMessageRevokeReceived(ZIM zim, ArrayList<ZIMRevokeMessage> messageList) {
        super.onMessageRevokeReceived(zim, messageList);

        if (messageList.isEmpty()) {
            return;
        }

        ArrayList<ZIMMessage> messages = new ArrayList<>(messageList);
        String conversationID = messageList.get(0).getConversationID();
        ZIMConversationType type = messageList.get(0).getConversationType();
        ArrayList<ZIMKitMessage> kitMessages = MessageTransform.parseMessageList(messages);

        ZIMKitCore.getInstance().getZimkitNotifyList().notifyAllListener(zimKitDelegate -> {
            zimKitDelegate.onMessageRevokeReceived(conversationID, type, kitMessages);
        });
    }

    @Override
    public void onMessageReactionsChanged(ZIM zim, ArrayList<ZIMMessageReaction> reactions) {
        super.onMessageReactionsChanged(zim, reactions);

        ZIMKitCore.getInstance().getZimkitNotifyList().notifyAllListener(zimKitDelegate -> {
            zimKitDelegate.onMessageReactionsChanged(reactions);
        });
    }

    @Override
    public void onMessageRepliedInfoChanged(ZIM zim, ArrayList<ZIMMessage> messageList) {
        super.onMessageRepliedInfoChanged(zim, messageList);

        if (messageList.isEmpty()) {
            return;
        }

        String conversationID = messageList.get(0).getConversationID();
        ZIMConversationType type = messageList.get(0).getConversationType();
        ArrayList<ZIMKitMessage> kitMessages = MessageTransform.parseMessageList(messageList);

        ZIMKitCore.getInstance().getZimkitNotifyList().notifyAllListener(zimKitDelegate -> {
            zimKitDelegate.onMessageRepliedInfoChanged(conversationID, type, kitMessages);
        });
    }

    private static final String TAG = "ZIMKitEventHandler";

    @Override
    public void onReceiveRoomMessage(ZIM zim, ArrayList<ZIMMessage> messageList, String fromRoomID) {
        super.onReceiveRoomMessage(zim, messageList, fromRoomID);
        handleReceiveNewMessages(messageList);
    }

    private void handlerConversationChange(List<ZIMConversationChangeInfo> infos) {
        ZIMKitCore.getInstance().getZimkitNotifyList().notifyAllListener(zimKitDelegate -> {
           // zimKitDelegate.onConversationListChanged(conversations);
            zimKitDelegate.newChange();
        });
        if (infos.isEmpty()) {
            return;
        }
        for (ZIMConversationChangeInfo info : infos) {
            if (info.event == ZIMConversationEvent.ADDED) {
                ZIMKitConversation viewModel = new ZIMKitConversation(info.conversation);
                ZIMKitCore.getInstance().getConversations().add(viewModel);
            } else if (info.event == ZIMConversationEvent.UPDATED) {
                // Incremental Updates
                ZIMKitConversation oldModel = null;
                for (ZIMKitConversation model : ZIMKitCore.getInstance().getConversations()) {
                    if (model.getId().equals(info.conversation.conversationID)) {
                        oldModel = model;
                        break;
                    }
                }
                if (oldModel != null) {
                    ZIMKitCore.getInstance().getConversations().remove(oldModel);
                }
                ZIMKitCore.getInstance().getConversations().add(new ZIMKitConversation(info.conversation));
            } else if (info.event == ZIMConversationEvent.DISABLED) {
                ZIMKitConversation oldModel = null;
                for (ZIMKitConversation model : ZIMKitCore.getInstance().getConversations()) {
                    if (model.getId().equals(info.conversation.conversationID)) {
                        oldModel = model;
                        break;
                    }
                }
                if (oldModel != null) {
                    ZIMKitCore.getInstance().getConversations().remove(oldModel);
                    ZIMKitCore.getInstance().getConversations().add(new ZIMKitConversation(info.conversation));
                }
            } else if (info.event == ZIMConversationEvent.DELETED) {
                ZIMKitConversation oldModel = null;
                for (ZIMKitConversation model : ZIMKitCore.getInstance().getConversations()) {
                    if (model.getId().equals(info.conversation.conversationID)) {
                        oldModel = model;
                        break;
                    }
                }
                if (oldModel != null) {
                    ZIMKitCore.getInstance().getConversations().remove(oldModel);
                }
            }
        }

        ArrayList<ZIMKitConversation> conversations = new ArrayList<>(ZIMKitCore.getInstance().getConversations()) ;
        System.out.println("reached final here");
        ZIMKitCore.getInstance().getZimkitNotifyList().notifyAllListener(zimKitDelegate -> {
            zimKitDelegate.onConversationListChanged(conversations);
         //   zimKitDelegate.newChange();
        });
        notifyConversationChange();
    }

    private void handleReceiveNewMessages(ArrayList<ZIMMessage> messageList) {

        if (messageList.isEmpty()) {
            return;
        }
        ZIMKitCore.getInstance().setGroupMemberInfo(messageList);

        String conversationID = messageList.get(0).getConversationID();
        ZIMConversationType type = messageList.get(0).getConversationType();
        ArrayList<ZIMKitMessage> kitMessages = MessageTransform.parseMessageList(messageList);

        ZIMKitCore.getInstance().getMessageList().addAll(kitMessages);

        ZIMKitCore.getInstance().getZimkitNotifyList().notifyAllListener(zimKitDelegate -> {
            zimKitDelegate.onMessageReceived(conversationID, type, kitMessages);
        });

    }

    public boolean isKickedOutAccount() {
        return kickedOutAccount;
    }

    public void setKickedOutAccount(boolean kickedOutAccount) {
        this.kickedOutAccount = kickedOutAccount;
    }
}
