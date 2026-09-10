package com.zegocloud.zimkit.components.message.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.bumptech.glide.Glide;
import com.zegocloud.zimkit.R;
import com.zegocloud.zimkit.common.ZIMKitConstant;
import com.zegocloud.zimkit.common.utils.ZIMKitToastUtils;
import com.zegocloud.zimkit.components.group.bean.ZIMKitGroupMemberInfo;
import com.zegocloud.zimkit.components.group.ui.ZIMKitGroupMembersActivity;
import com.zegocloud.zimkit.components.message.adapter.GroupMemberShortcutAdapter;
import com.zegocloud.zimkit.components.message.ui.AsynchronousSwitch.Asynchronous;
import com.zegocloud.zimkit.components.message.utils.OnRecyclerViewItemTouchListener;
import com.zegocloud.zimkit.databinding.ActivityGroupChatSettingBinding;
import com.zegocloud.zimkit.services.ZIMKit;
import com.zegocloud.zimkit.services.ZIMKitDelegate;
import com.zegocloud.zimkit.services.callback.InviteUsersToJoinGroupCallback;
import com.zegocloud.zimkit.services.callback.QueryGroupInfoCallback;
import com.zegocloud.zimkit.services.callback.QueryGroupMemberListCallback;
import com.zegocloud.zimkit.services.internal.ZIMKitCore;
import com.zegocloud.zimkit.services.model.ZIMKitConversation;
import com.zegocloud.zimkit.services.model.ZIMKitGroupInfo;

import im.zego.zim.callback.ZIMConversationNotificationStatusSetCallback;
import im.zego.zim.callback.ZIMConversationPinnedStateUpdatedCallback;
import im.zego.zim.entity.ZIMError;
import im.zego.zim.entity.ZIMErrorUserInfo;
import im.zego.zim.entity.ZIMGroupMemberInfo;
import im.zego.zim.entity.ZIMGroupMemberQueryConfig;
import im.zego.zim.entity.ZIMGroupOperatedInfo;
import im.zego.zim.enums.ZIMConversationNotificationStatus;
import im.zego.zim.enums.ZIMConversationType;
import im.zego.zim.enums.ZIMErrorCode;
import im.zego.zim.enums.ZIMGroupMemberEvent;
import im.zego.zim.enums.ZIMGroupMemberState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;

public class ZIMKitGroupChatSettingActivity extends ComponentActivity {


    private ActivityGroupChatSettingBinding binding;
    private String mId;
    private GroupMemberShortcutAdapter shortcutAdapter;

    private static final String TAG = "ZIMKitGroupChatSettingA";
    private ZIMKitDelegate zimKitDelegate;

    private static ZIMKitGroupChatSettingActivity sInstance;

    /** 退出群聊成功后一键回到社群首页（uniplugin 调用） */
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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_group_chat_setting);
        // 显示「群聊名称 / 免打扰 / 置顶」设置块（XML 默认 gone，从未显示过）
        View chatSetting = binding.getRoot().findViewById(R.id.chat_setting);
        if (chatSetting != null) {
            chatSetting.setVisibility(View.VISIBLE);
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Bundle bundle = getIntent().getBundleExtra(ZIMKitConstant.RouterConstant.KEY_BUNDLE);
        String title = bundle.getString(ZIMKitConstant.MessagePageConstant.KEY_TITLE);
        mId = bundle.getString(ZIMKitConstant.MessagePageConstant.KEY_ID);
        String ma = bundle.getString(ZIMKitConstant.GroupPageConstant.KEY_TITLE);

        binding.groupSetTitleBar.hideRightButton();
        binding.groupSetTitleBar.setTitle(getString(R.string.chat_setting));
        //        binding.groupChatMembersShortcut.setAdapter();
        GroupMemberShortcutAdapter shortcutAdapterLocal = new GroupMemberShortcutAdapter();
        shortcutAdapter = shortcutAdapterLocal;
        List<ZIMKitGroupMemberInfo> groupMemberList = ZIMKitCore.getInstance().getGroupMemberList(mId);
        if (groupMemberList != null) {
            binding.groupMembersCount.setText(getString(R.string.group_members_detail, groupMemberList.size()));
            shortcutAdapter.setMemberList(groupMemberList);
        }

        ImageView groupQrCode = findViewById(R.id.group_qr_code);
        TextView groupIn = findViewById(R.id.tv_group_id);
        String groupId = "GROUP_"+mId; // Dynamic variable identifier string

// 2. Compute the graphics data using background threads
        Executors.newSingleThreadExecutor().execute(() -> {
            // 400x400 pixels delivers razor-sharp resolution in your 220dp container box
            final Bitmap qrBitmap = QRCodeGenerator.generate(groupId, 400);

            // 3. Post the calculated bitmap image asset structure back onto the UI thread
            if (qrBitmap != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    groupQrCode.setImageBitmap(qrBitmap);
                });
            }
        });

        ZIMKit.queryGroupInfo(mId, new QueryGroupInfoCallback() {
            @Override
            public void onQueryGroupInfo(ZIMKitGroupInfo info, ZIMError error) {
                if (error.code == ZIMErrorCode.SUCCESS) {
                    String title = info.getName();
                    groupIn.setText(groupId);
                    binding.groupSetTitleBar.setTitle(title);
                    TextView groupNameValue = findViewById(R.id.group_name_value);
                    if (groupNameValue != null) {
                        groupNameValue.setText(title);
                    }
                }
            }
        });
        TextView btnCopyGroupId = findViewById(R.id.btn_copy_group_id);


        btnCopyGroupId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Access the system clipboard service wrapper
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Group ID","GROUP_"+mId);

                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);

                    // Notification toast confirming action
                    Toast.makeText(ZIMKitGroupChatSettingActivity.this, "群ID已复制", Toast.LENGTH_SHORT).show();
                }
            }
        });

        refreshGroupMembers();
        // 社群频道：隐藏「退出群聊」（退出社群=退社群+删聊天，在社群管理页操作）+ 隐藏二维码相关
        try {
            ZIMKitCore.getInstance().zim().queryGroupAllAttributes(mId, (g, attrs, e) -> {
                if (attrs != null && "community".equals(attrs.get("bizType"))) {
                    runOnUiThread(() -> {
                        View exitBtn = binding.getRoot().findViewById(R.id.exit_group_btn);
                        if (exitBtn != null) {
                            exitBtn.setVisibility(View.GONE);
                        }
                        View qrRow = binding.getRoot().findViewById(R.id.qr_code_container);
                        if (qrRow != null) {
                            qrRow.setVisibility(View.GONE);
                        }
                        View qrRow2 = binding.getRoot().findViewById(R.id.group_qr_code);
                        if (qrRow2 != null) {
                            qrRow2.setVisibility(View.GONE);
                        }
                        // 社群频道：隐藏群ID + 复制按钮
                        View infoLayout = binding.getRoot().findViewById(R.id.group_info_layout);
                        if (infoLayout != null) {
                            infoLayout.setVisibility(View.GONE);
                        }
                        // 社群频道：隐藏邀请(ADD) 与 踢出(KICK) 快捷项（成员宫格仍保留）
                        shortcutAdapter.setShowInvite(false);
                        shortcutAdapter.setShowKick(false);
                        refreshGroupMembers();
                        // 社群频道：隐藏「群聊名称」（名称由社群资料管理页维护，频道只读跟随）
                        View nameRow = binding.getRoot().findViewById(R.id.group_name_row);
                        if (nameRow != null) {
                            nameRow.setVisibility(View.GONE);
                        }
                        View divider1 = binding.getRoot().findViewById(R.id.chat_setting_divider1);
                        if (divider1 != null) {
                            divider1.setVisibility(View.GONE);
                        }
                        // 社群频道：禁止展示「群二维码」（社群二维码在管理页，群ID在社群资料）
                        View qrRowHide = binding.getRoot().findViewById(R.id.group_qr_row);
                        if (qrRowHide != null) {
                            qrRowHide.setVisibility(View.GONE);
                        }
                        View qrDivHide = binding.getRoot().findViewById(R.id.chat_setting_divider_qr);
                        if (qrDivHide != null) {
                            qrDivHide.setVisibility(View.GONE);
                        }
                        // 社群频道同样允许：置顶 + 免打扰（按用户设置）
                        binding.pinChat.setVisibility(View.VISIBLE);
                        binding.doNotDisturb.setVisibility(View.VISIBLE);
                        View divider2c = binding.getRoot().findViewById(R.id.chat_setting_divider2);
                        if (divider2c != null) {
                            divider2c.setVisibility(View.VISIBLE);
                        }
                    });
                }
            });
        } catch (Exception ignored) {
        }
        binding.groupChatMembersRecyclerview.setAdapter(shortcutAdapter);
        binding.groupChatMembersRecyclerview.setLayoutManager(new GridLayoutManager(this, 5));
        binding.groupChatMembersRecyclerview.addOnItemTouchListener(
            new OnRecyclerViewItemTouchListener(binding.groupChatMembersRecyclerview) {
                @Override
                public void onItemClick(ViewHolder vh) {
                    super.onItemClick(vh);
                    int position = vh.getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        ZIMKitGroupMemberInfo groupMember = shortcutAdapter.getItemData(position);
                        if (groupMember == GroupMemberShortcutAdapter.ADD) {
                            if (com.zegocloud.zimkit.services.internal.GroupSettingBridge.getListener() != null) {
                                com.zegocloud.zimkit.services.internal.GroupSettingBridge.getListener().onInvite(mId);
                            }
                            return;
                        }
                        if (groupMember == GroupMemberShortcutAdapter.KICK) {
                            if (com.zegocloud.zimkit.services.internal.GroupSettingBridge.getListener() != null) {
                                com.zegocloud.zimkit.services.internal.GroupSettingBridge.getListener().onKick(mId);
                            }
                            return;
                        }
                        // 点击普通成员 → 打开成员信息页（图3/图4：群主看用户 / 用户看群主）
                        if (groupMember != null && groupMember.getId() != null) {
                            if (com.zegocloud.zimkit.services.internal.GroupSettingBridge.getListener() != null) {
                                com.zegocloud.zimkit.services.internal.GroupSettingBridge.getListener()
                                    .onMemberClick(mId, groupMember.getId());
                            }
                            return;
                        }
                    }
                }
            });

        binding.groupMembersCount.setOnClickListener(v -> {
            Intent intent = new Intent(ZIMKitGroupChatSettingActivity.this, ZIMKitGroupMembersActivity.class);
            intent.putExtra(ZIMKitConstant.MessagePageConstant.KEY_ID, mId);
            startActivity(intent);
        });
        // 群聊名称修改（ZIM updateGroupName，列表/聊天头同步）
        View groupNameRow = binding.getRoot().findViewById(R.id.group_name_row);
        if (groupNameRow != null) {
            groupNameRow.setOnClickListener(v -> {
                TextView nameValue = findViewById(R.id.group_name_value);
                String currentName = nameValue != null ? String.valueOf(nameValue.getText()) : "群聊";
                android.widget.EditText input = new android.widget.EditText(ZIMKitGroupChatSettingActivity.this);
                input.setText(currentName == null ? "" : currentName);
                input.setHint("请输入群聊名称");
                new AlertDialog.Builder(ZIMKitGroupChatSettingActivity.this)
                    .setTitle("修改群聊名称")
                    .setView(input)
                    .setPositiveButton("确定", (d, w) -> {
                        String nn = input.getText().toString().trim();
                        if (nn.isEmpty()) {
                            return;
                        }
                        // ZIM 签名：updateGroupName(groupName, groupID, cb)
                        ZIMKitCore.getInstance().zim().updateGroupName(nn, mId, (gid, newName, err) -> {
                            if (err != null && err.code == ZIMErrorCode.SUCCESS) {
                                TextView groupNameValue = findViewById(R.id.group_name_value);
                                if (groupNameValue != null) {
                                    groupNameValue.setText(newName);
                                }
                                binding.groupSetTitleBar.setTitle(newName);
                            } else {
                                com.zegocloud.zimkit.common.utils.ZIMKitToastUtils.showToast(
                                    err == null ? "修改失败" : err.message);
                            }
                        });
                    })
                    .setNegativeButton("取消", null)
                    .show();
            });
        }

        // 群二维码：点击 → 桥回 uniapp 打开群二维码页（二维码 + 群ID + 复制）
        View groupQrRow = binding.getRoot().findViewById(R.id.group_qr_row);
        if (groupQrRow != null) {
            groupQrRow.setOnClickListener(v -> {
                String nameNow = "";
                TextView nameValue = findViewById(R.id.group_name_value);
                if (nameValue != null) {
                    nameNow = String.valueOf(nameValue.getText());
                }
                if (com.zegocloud.zimkit.services.internal.GroupSettingBridge.getListener() != null) {
                    com.zegocloud.zimkit.services.internal.GroupSettingBridge.getListener()
                        .onQrcode(mId, nameNow);
                }
            });
        }
        // 隐藏原有内嵌二维码区（改由「群二维码」栏进入专页）
        View oldQr = binding.getRoot().findViewById(R.id.qr_code_container);
        if (oldQr != null) {
            oldQr.setVisibility(View.GONE);
        }
        // 群ID + 复制：统一移到「群二维码」页展示（设置页不再显示）
        View idRow = binding.getRoot().findViewById(R.id.group_info_layout);
        if (idRow != null) {
            idRow.setVisibility(View.GONE);
        }

        // 退出群聊：群主 → 转让（管理员→最早加入→随机）再退出；普通成员直接退出
        View exitGroupBtn = binding.getRoot().findViewById(R.id.exit_group_btn);
        if (exitGroupBtn != null) {
            exitGroupBtn.setOnClickListener(v -> {
                new AlertDialog.Builder(ZIMKitGroupChatSettingActivity.this)
                    .setTitle("退出群聊")
                    .setMessage("确定退出该群聊？群主退出后群主将交给管理员（入群最早者优先）")
                    .setPositiveButton("确定", (d, w) -> {
                        if (com.zegocloud.zimkit.services.internal.GroupSettingBridge.getListener() != null) {
                            com.zegocloud.zimkit.services.internal.GroupSettingBridge.getListener().onExit(mId);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            });
        }

        ZIMKitConversation conversation = ZIMKitCore.getInstance().getZIMKitConversation(mId);
        if (conversation != null) {
            binding.pinChat.setVisibility(View.VISIBLE);
            binding.doNotDisturb.setVisibility(View.VISIBLE);
            View divider2 = binding.getRoot().findViewById(R.id.chat_setting_divider2);
            if (divider2 != null) {
                divider2.setVisibility(View.VISIBLE);
            }
            binding.pinChat.realSetChecked(conversation.getZimConversation().isPinned);
            binding.pinChat.setAsynchronous(new Asynchronous() {
                @Override
                public void beforeApplyState(AsynchronousSwitch aSwitch, boolean originalCheck) {
                    ZIMKitCore.getInstance().setConversationPinnedState(originalCheck, mId, ZIMConversationType.GROUP,
                        new ZIMConversationPinnedStateUpdatedCallback() {
                            @Override
                            public void onConversationPinnedStateUpdated(String conversationID,
                                ZIMConversationType conversationType, ZIMError errorInfo) {
                                ZIMKitConversation conversation1 = ZIMKitCore.getInstance().getZIMKitConversation(mId);
                                if (errorInfo.code == ZIMErrorCode.SUCCESS) {
                                    aSwitch.realSetChecked(originalCheck);
                                } else {
                                    aSwitch.realSetChecked(!originalCheck);
                                }
                            }
                        });
                }
            });

            ZIMConversationNotificationStatus notificationStatus = conversation.getZimConversation().notificationStatus;
            binding.doNotDisturb.realSetChecked(notificationStatus == ZIMConversationNotificationStatus.DO_NOT_DISTURB);
            binding.doNotDisturb.setAsynchronous(new Asynchronous() {
                @Override
                public void beforeApplyState(AsynchronousSwitch aSwitch, boolean originalCheck) {
                    ZIMConversationNotificationStatus target;
                    if (originalCheck) {
                        target = ZIMConversationNotificationStatus.DO_NOT_DISTURB;
                    } else {
                        target = ZIMConversationNotificationStatus.NOTIFY;
                    }
                    ZIMKitCore.getInstance().setConversationNotificationStatus(target, mId, ZIMConversationType.GROUP,
                        new ZIMConversationNotificationStatusSetCallback() {
                            @Override
                            public void onConversationNotificationStatusSet(String conversationID,
                                ZIMConversationType conversationType, ZIMError errorInfo) {
                                ZIMKitConversation conversation1 = ZIMKitCore.getInstance().getZIMKitConversation(mId);
                                if (errorInfo.code == ZIMErrorCode.SUCCESS) {
                                    aSwitch.realSetChecked(originalCheck);
                                } else {
                                    aSwitch.realSetChecked(!originalCheck);
                                }
                            }
                        });
                }
            });
        } else {
            binding.pinChat.setVisibility(View.GONE);
            binding.doNotDisturb.setVisibility(View.GONE);
        }

        zimKitDelegate = new ZIMKitDelegate() {
            @Override
            public void onGroupMemberStateChanged(ZIMGroupMemberState state, ZIMGroupMemberEvent event,
                ArrayList<ZIMGroupMemberInfo> userList, ZIMGroupOperatedInfo operatedInfo, String groupID) {
                refreshGroupMembers();
            }
        };
        ZIMKit.registerZIMKitDelegate(zimKitDelegate);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ZIMKit.unRegisterZIMKitDelegate(zimKitDelegate);
        if (sInstance == this) {
            sInstance = null;
        }
    }

    /** 成员列表实时刷新（退群/拉人后立即更新，避免残留） */
    private void refreshGroupMembers() {
        try {
            ZIMGroupMemberQueryConfig config = new ZIMGroupMemberQueryConfig();
            config.count = 100;
            ZIMKitCore.getInstance().queryGroupMemberList(mId, config, new QueryGroupMemberListCallback() {
                @Override
                public void onGroupMemberListQueried(String groupID, ArrayList<ZIMKitGroupMemberInfo> userList,
                    int nextFlag, ZIMError errorInfo) {
                    if (userList != null) {
                        runOnUiThread(() -> {
                            binding.groupMembersCount.setText(
                                getString(R.string.group_members_detail, userList.size()));
                            shortcutAdapter.setMemberList(userList);
                        });
                    }
                }
            });
        } catch (Exception ignored) {
        }
    }

    public static int dp2px(float v, DisplayMetrics displayMetrics) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, displayMetrics);
    }
}