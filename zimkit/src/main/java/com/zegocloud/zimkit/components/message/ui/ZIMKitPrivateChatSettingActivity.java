package com.zegocloud.zimkit.components.message.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import androidx.activity.ComponentActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.databinding.DataBindingUtil;
import com.zegocloud.zimkit.R;
import com.zegocloud.zimkit.common.ZIMKitConstant;
import com.zegocloud.zimkit.common.ZIMKitConstant.MessagePageConstant;
import com.zegocloud.zimkit.common.glide.ZIMKitGlideLoader;
import com.zegocloud.zimkit.components.message.ui.AsynchronousSwitch.Asynchronous;
import com.zegocloud.zimkit.databinding.ActivityPrivateChatSettingBinding;
import com.zegocloud.zimkit.services.internal.GroupSettingBridge;
import com.zegocloud.zimkit.services.internal.ZIMKitCore;
import com.zegocloud.zimkit.services.model.ZIMKitConversation;
import im.zego.zim.callback.ZIMConversationNotificationStatusSetCallback;
import im.zego.zim.callback.ZIMConversationPinnedStateUpdatedCallback;
import im.zego.zim.callback.ZIMUsersInfoQueriedCallback;
import im.zego.zim.entity.ZIMConversation;
import im.zego.zim.entity.ZIMError;
import im.zego.zim.entity.ZIMErrorUserInfo;
import im.zego.zim.entity.ZIMUserFullInfo;
import im.zego.zim.entity.ZIMUsersInfoQueryConfig;
import im.zego.zim.enums.ZIMConversationNotificationStatus;
import im.zego.zim.enums.ZIMConversationType;
import im.zego.zim.enums.ZIMErrorCode;
import java.util.ArrayList;
import java.util.Collections;

public class ZIMKitPrivateChatSettingActivity extends ComponentActivity {

    private ActivityPrivateChatSettingBinding binding;

    /** 对方 ZIM userId（本页会话ID）：置顶/免打扰/发起群聊/看资料都用它 */
    private String peerId = "";

    /** 当前实例：供宿主（uniplugin）在建群成功后关闭本页（返回时回到单聊而不是设置页） */
    private static ZIMKitPrivateChatSettingActivity sInstance;

    public static void finishCurrent() {
        if (sInstance != null) {
            sInstance.finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sInstance = this;
        com.zegocloud.zimkit.common.utils.ZimkitStatusBar.setWhite(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_private_chat_setting);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        binding.titleBar.setTitle(getString(R.string.chat_setting));
        binding.titleBar.hideRightButton();

        Bundle bundle = getIntent().getBundleExtra(ZIMKitConstant.RouterConstant.KEY_BUNDLE);
        String title = bundle.getString(ZIMKitConstant.MessagePageConstant.KEY_TITLE);
        String id = bundle.getString(ZIMKitConstant.MessagePageConstant.KEY_ID);
        String avatar = bundle.getString(MessagePageConstant.KEY_AVATAR);
        peerId = id == null ? "" : id;

        ZIMKitConversation conversation = ZIMKitCore.getInstance().getZIMKitConversation(id);

        if (TextUtils.isEmpty(avatar)) {
            ArrayList<String> userIDs = new ArrayList<>(Collections.singletonList(id));
            ZIMKitCore.getInstance()
                .queryUserInfo(userIDs, new ZIMUsersInfoQueryConfig(), new ZIMUsersInfoQueriedCallback() {
                    @Override
                    public void onUsersInfoQueried(ArrayList<ZIMUserFullInfo> userList,
                        ArrayList<ZIMErrorUserInfo> errorUserList, ZIMError errorInfo) {
                        if (errorInfo.code == ZIMErrorCode.SUCCESS) {
                            if (!userList.isEmpty()) {
                                ZIMUserFullInfo zimUserFullInfo = userList.get(0);
                                ZIMKitGlideLoader.displayMessageAvatarImage(binding.contactIcon,
                                    zimUserFullInfo.baseInfo.userAvatarUrl);
                                binding.contactName.setText(zimUserFullInfo.baseInfo.userName);
                            }
                        }
                    }
                });
        } else {
            ZIMKitGlideLoader.displayMessageAvatarImage(binding.contactIcon, avatar);
            binding.contactName.setText(title);
        }

        // 免打扰 / 置顶：**永远显示**（本地还没有该会话时也显示，不再 GONE）。
        // 本地没有会话（例如刚建好、还没发过消息的单聊）→ 去服务端查一次拿真实状态；
        // 查询失败就按"未勾选"显示 —— 用户点开关时 ZIM 若报错会自动回弹，不会卡住。
        bindSettingSwitches(conversation == null ? null : conversation.getZimConversation());
        if (conversation == null) {
            try {
                ZIMKitCore.getInstance().zim().queryConversation(peerId, ZIMConversationType.PEER,
                    (queried, errorInfo) -> runOnUiThread(() -> {
                        if (errorInfo != null && errorInfo.code == ZIMErrorCode.SUCCESS && queried != null) {
                            bindSettingSwitches(queried);
                        }
                    }));
            } catch (Exception e) {
                android.util.Log.w("PrivateChatSetting", "queryConversation fail: " + e.getMessage());
            }
        }

        // 单聊「聊天设置」的「＋」：多选联系人 → 新建群聊并进入（微信式：自己 + 当前单聊对象 + 已选）。
        // 真正动作由宿主（uniplugin 的 TestModule）经 GroupSettingBridge 实现；
        // 未注册动作桥时隐藏按钮，避免"点了没反应"。
        GroupSettingBridge.Listener settingBridge = GroupSettingBridge.getListener();
        if (settingBridge == null) {
            binding.contactAdd.setVisibility(View.GONE);
        } else {
            binding.contactAdd.setOnClickListener(v -> {
                try {
                    settingBridge.onInvitePeer(id);
                } catch (Exception e) {
                    android.util.Log.w("PrivateChatSetting", "start group chat fail: " + e.getMessage());
                }
            });
            // 头像可点 → 对方「个人信息」页（与群设置里点头像=改群头像不同，这里是"看资料"）
            binding.contactIcon.setOnClickListener(v -> {
                try {
                    settingBridge.onPeerAvatarClick(id);
                } catch (Exception e) {
                    android.util.Log.w("PrivateChatSetting", "open peer profile fail: " + e.getMessage());
                }
            });
        }

    }

    @Override
    protected void onDestroy() {
        if (sInstance == this) {
            sInstance = null;
        }
        super.onDestroy();
    }

    /**
     * 绑定「置顶 / 免打扰」两个开关（**开关本身永远可见**）。
     *
     * @param conv 本地或服务端查到的会话；为 null 时按"未勾选"显示 ——
     *             用户仍可点，ZIM 若失败会自动回弹（不会出现"开关没了/点了没反应"）。
     */
    private void bindSettingSwitches(ZIMConversation conv) {
        binding.pinChat.realSetChecked(conv != null && conv.isPinned);
        binding.pinChat.setAsynchronous(new Asynchronous() {
            @Override
            public void beforeApplyState(AsynchronousSwitch aSwitch, boolean originalCheck) {
                ZIMKitCore.getInstance().setConversationPinnedState(originalCheck, peerId,
                    ZIMConversationType.PEER, new ZIMConversationPinnedStateUpdatedCallback() {
                        @Override
                        public void onConversationPinnedStateUpdated(String conversationID,
                            ZIMConversationType conversationType, ZIMError errorInfo) {
                            aSwitch.realSetChecked(errorInfo.code == ZIMErrorCode.SUCCESS
                                ? originalCheck : !originalCheck);
                        }
                    });
            }
        });

        ZIMConversationNotificationStatus status = conv == null
            ? ZIMConversationNotificationStatus.NOTIFY : conv.notificationStatus;
        binding.doNotDisturb.realSetChecked(status == ZIMConversationNotificationStatus.DO_NOT_DISTURB);
        binding.doNotDisturb.setAsynchronous(new Asynchronous() {
            @Override
            public void beforeApplyState(AsynchronousSwitch aSwitch, boolean originalCheck) {
                ZIMConversationNotificationStatus target = originalCheck
                    ? ZIMConversationNotificationStatus.DO_NOT_DISTURB
                    : ZIMConversationNotificationStatus.NOTIFY;
                ZIMKitCore.getInstance().setConversationNotificationStatus(target, peerId,
                    ZIMConversationType.PEER, new ZIMConversationNotificationStatusSetCallback() {
                        @Override
                        public void onConversationNotificationStatusSet(String conversationID,
                            ZIMConversationType conversationType, ZIMError errorInfo) {
                            aSwitch.realSetChecked(errorInfo.code == ZIMErrorCode.SUCCESS
                                ? originalCheck : !originalCheck);
                        }
                    });
            }
        });
    }

}