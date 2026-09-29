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
import com.zegocloud.zimkit.common.utils.ZIMKitCheckDoubleClick;
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

    /**
     * 是否是社群频道（ZIM 群属性 bizType=community）。
     * 社群频道连群主都不显示「群聊名称 / 群聊头像」—— 名称和头像归社群管理页维护，频道只读跟随。
     * 这个标记要在 {@link #applyOwnerOnlyRows()} 里一起判，否则成员名单异步回来时会把刚藏掉的行又显出来。
     */
    private boolean isCommunityChannel = false;

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
        // 「邀请/踢出」快捷项默认不画：它们的显隐取决于群属性(bizType)，而 bizType 是异步查的。
        // 若按适配器默认值(true)先画出来，社群频道会出现"先冒出来、随后又消失"的跳变。
        // ⚠️ 必须在 shortcutAdapter 创建之后调用（之前放在这里导致 NPE 崩溃：
        //    "setShowInvite on a null object reference" —— 2026-09-12 14:24 抓到的 FATAL）
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
        // 适配器创建后再关掉「邀请/踢出」：等 bizType 回来按类型决定是否显示（防闪）
        shortcutAdapter.setShowInvite(false);
        shortcutAdapter.setShowKick(false);
        // ★ 同步预判：bizType 缓存命中时，**第一帧**就按最终布局渲染，不必等异步 queryGroupAllAttributes。
        //   否则社群频道会出现"进去先一堆项（退出群聊/群二维码/群ID/群聊名称）、过一会儿才消失"（用户实测反馈）。
        //   缓存由 ZIMKitMessageActivity 的 queryGroupInfo 与下面的 queryGroupAllAttributes 写入；
        //   未命中（如进程刚重启）→ 保持原来的"等异步"行为，不会更差；异步结果回来会再纠正一次。
        final String cachedBizType = com.zegocloud.zimkit.common.utils.GroupBizTypeCache.get(mId);
        if (com.zegocloud.zimkit.common.utils.GroupBizTypeCache.BIZ_TYPE_COMMUNITY.equals(cachedBizType)) {
            isCommunityChannel = true;
            applyCommunityChannelVisibility();
            applyOwnerOnlyRows();
        } else if (!cachedBizType.isEmpty()) {
            // 明确不是社群频道 → 「邀请/踢出」可以立刻显示（不必等属性回来）
            shortcutAdapter.setShowInvite(true);
            shortcutAdapter.setShowKick(true);
            applyOwnerOnlyRows();
        }
        // 立刻把适配器挂上去：refreshGroupMembers() 是异步查询，回调里会操作 shortcutAdapter，
        // 若 setAdapter 放在后面（原 L256），存在"回调先于挂载"的时序风险。
        binding.groupChatMembersRecyclerview.setAdapter(shortcutAdapter);
        binding.groupChatMembersRecyclerview.setLayoutManager(new GridLayoutManager(this, 5));
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
                    // 群聊头像缩略图：优先群资料，其次会话头像（有的群只有会话上带了头像）
                    ImageView avatarThumb = findViewById(R.id.group_avatar_thumb);
                    if (avatarThumb != null) {
                        String avatarUrl = info.getAvatarUrl();
                        if (avatarUrl == null || avatarUrl.isEmpty()) {
                            ZIMKitConversation selfConv = ZIMKitCore.getInstance().getZIMKitConversation(mId);
                            if (selfConv != null && selfConv.getZimConversation() != null) {
                                avatarUrl = selfConv.getZimConversation().conversationAvatarUrl;
                            }
                        }
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(ZIMKitGroupChatSettingActivity.this).load(avatarUrl).into(avatarThumb);
                        }
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
        // 群属性(bizType)回来前，「群聊名称 / 群聊头像 / 邀请 / 踢出」全部保持隐藏（XML 默认 gone、
        // 适配器默认已关）→ 社群频道不会出现"先冒出来再消失"的跳变；
        // 普通群聊会晚一瞬出现（约 100~300ms），这是不闪的代价。
        try {
            ZIMKitCore.getInstance().zim().queryGroupAllAttributes(mId, (g, attrs, e) -> {
                boolean community = attrs != null && "community".equals(attrs.get("bizType"));
                isCommunityChannel = community;
                // 权威结果回写缓存（下次进本页 onCreate 就能"第一帧就对"）；查失败不写，避免把"未知"当"非社群"
                if (attrs != null) {
                    com.zegocloud.zimkit.common.utils.GroupBizTypeCache.put(mId,
                        String.valueOf(attrs.get("bizType")));
                }
                runOnUiThread(() -> {
                    if (community) {
                        applyCommunityChannelVisibility();
                        // 标志变了要重画宫格（ADD/KICK 快捷项由适配器按标志决定是否出现）
                        refreshGroupMembers();
                    } else {
                        // 普通群聊（含 2 人群聊）：显示「邀请 / 踢出」快捷项；
                        // 「群聊名称 / 群聊头像」由 applyOwnerOnlyRows() 按群主身份决定
                        shortcutAdapter.setShowInvite(true);
                        shortcutAdapter.setShowKick(true);
                        refreshGroupMembers();
                    }
                    // 两条路都要重判一次群主专属行（社群频道里它们恒隐藏）
                    applyOwnerOnlyRows();
                });
            });
        } catch (Exception ignored) {
        }
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
        // 权限：只有群主能看见「群聊名称」并修改（管理员也不行）
        View groupNameRow = binding.getRoot().findViewById(R.id.group_name_row);
        if (groupNameRow != null) {
            groupNameRow.setOnClickListener(v -> {
                // 防抖：连点会连弹多个改名输入框
                if (ZIMKitCheckDoubleClick.isFastDoubleClick(800)) {
                    return;
                }
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
                                // ① 本会话/其他成员的列表头像 → 通知上层刷新权威群资料覆盖表
                                try {
                                    com.zegocloud.zimkit.services.internal.ZIMKitEventHandler
                                        .notifyGroupProfileChangedFromLocal(mId);
                                } catch (Exception e) {
                                    android.util.Log.w(TAG, "notifyGroupProfileChanged fail: "
                                        + e.getMessage());
                                }
                                // ② 业务库回写（社群频道走社群管理接口，这里只处理临时群/群聊）
                                if (!isCommunityChannel) {
                                    com.zegocloud.zimkit.common.utils.GroupProfileApi
                                        .pushToBackend(mId, newName, null);
                                }
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

        // 群聊头像（新增）：同样只有群主可见可改；改完走 ZIM updateGroupAvatarUrl
        View groupAvatarRow = binding.getRoot().findViewById(R.id.group_avatar_row);
        if (groupAvatarRow != null) {
            // 防抖：连点会连续弹起选图（每次都要走裁剪/上传）
            groupAvatarRow.setOnClickListener(v -> {
                if (ZIMKitCheckDoubleClick.isFastDoubleClick(800)) {
                    return;
                }
                pickGroupAvatar();
            });
        }
        // 两个群主专属项的显隐统一在这里判（成员名单异步回来后会再判一次，见 refreshGroupMembers）
        applyOwnerOnlyRows();

        // 群二维码：点击 → 桥回 uniapp 打开群二维码页（二维码 + 群ID + 复制）
        View groupQrRow = binding.getRoot().findViewById(R.id.group_qr_row);
        if (groupQrRow != null) {
            groupQrRow.setOnClickListener(v -> {
                // 防抖：连点会连开多个二维码页
                if (ZIMKitCheckDoubleClick.isFastDoubleClick(800)) {
                    return;
                }
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
                // 防抖：连点会连续发起"查属性+查成员+转让/解散"整串请求，必须拦
                if (ZIMKitCheckDoubleClick.isFastDoubleClick(800)) {
                    return;
                }
                // 文案按身份分两套：只有群主退出才涉及"转让群主"，普通成员/管理员不该看到那句
                boolean amOwner = iAmGroupOwner();
                String msg = amOwner
                    ? "确定退出该群聊？群主退出后群主将交给管理员（入群最早者优先）"
                    : "确定退出该群聊？";
                new AlertDialog.Builder(ZIMKitGroupChatSettingActivity.this)
                    .setTitle("退出群聊")
                    .setMessage(msg)
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
        ZIMKit.unRegisterZIMKitDelegate(zimKitDelegate);
        if (sInstance == this) {
            sInstance = null;
        }
        super.onDestroy();
    }

    /**
     * 当前登录用户是否是这个群的群主（ZIM memberRole：1=群主）。
     *
     * <p>用途：群聊名称 / 群聊头像两个设置项**只有群主可见可改**（管理员不行）。
     * 判定用本地成员列表，不发请求；列表是异步拉回来的，所以除了进页面时判一次，
     * {@link #refreshGroupMembers()} 拉回新列表后会再判一次并刷新显隐（冷启动缓存未命中时也不会误藏）。
     */
    private boolean iAmGroupOwner() {
        try {
            String selfId = null;
            com.zegocloud.zimkit.services.model.ZIMKitUser local = ZIMKitCore.getInstance().getLocalUser();
            if (local != null) {
                selfId = local.getId();
            }
            if (selfId == null || selfId.isEmpty()) {
                return false;
            }
            List<ZIMKitGroupMemberInfo> members = ZIMKitCore.getInstance().getGroupMemberList(mId);
            if (members == null) {
                return false;
            }
            for (ZIMKitGroupMemberInfo info : members) {
                if (info != null && selfId.equals(info.getId())) {
                    // 直接读 ZIM 原始 memberRole（1=群主 2=管理员 3=成员）：
                    // ZIMKit 的 GroupMemberRole 只有 OWNER/MEMBER，getFrom(2) 会抛异常，别用。
                    return info.getRole() == com.zegocloud.zimkit.services.model.GroupMemberRole.OWNER;
                }
            }
        } catch (Exception e) {
            android.util.Log.w(TAG, "iAmGroupOwner check fail: " + e.getMessage());
        }
        return false;
    }

    // ── 群聊头像：选图 → 上传 → ZIM updateGroupAvatarUrl ──

    private final androidx.activity.result.ActivityResultLauncher<Intent> avatarPicker =
        registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                    return;
                }
                android.net.Uri picked = result.getData().getData();
                if (picked == null) {
                    return;
                }
                // 相册返回的是 content:// ，先落到应用缓存目录拿到可用文件（与原生语音房选图同一做法）
                java.io.File local = copyUriToCache(picked);
                if (local == null) {
                    ZIMKitToastUtils.showToast("读取图片失败");
                    return;
                }
                uploadGroupAvatar(local);
            });

    /** 点击「群聊头像」：打开系统相册选一张图 */
    private void pickGroupAvatar() {
        if (!iAmGroupOwner()) {
            ZIMKitToastUtils.showToast("只有群主可以修改群聊头像");
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            avatarPicker.launch(Intent.createChooser(intent, "选择群聊头像"));
        } catch (Exception e) {
            ZIMKitToastUtils.showToast("无法打开相册：" + e.getMessage());
        }
    }

    /** content:// → 应用缓存文件（上传需要真实文件路径） */
    private java.io.File copyUriToCache(android.net.Uri uri) {
        java.io.InputStream in = null;
        java.io.FileOutputStream out = null;
        try {
            in = getContentResolver().openInputStream(uri);
            if (in == null) {
                return null;
            }
            java.io.File cache = new java.io.File(getCacheDir(), "group_avatar_" + System.currentTimeMillis() + ".jpg");
            out = new java.io.FileOutputStream(cache);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            out.flush();
            return cache;
        } catch (Exception e) {
            android.util.Log.w(TAG, "copyUriToCache fail: " + e.getMessage());
            return null;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception ignored) {
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /** 上传图片 → 拿到 URL → 写回 ZIM 群头像（聊天头/会话列表随之更新） */
    private void uploadGroupAvatar(java.io.File file) {
        ZIMKitToastUtils.showToast("正在上传…");
        com.zegocloud.zimkit.common.utils.MediaUploader.uploadImage(file,
            new com.zegocloud.zimkit.common.utils.MediaUploader.Callback() {
                @Override
                public void onSuccess(String imageUrl) {
                    if (imageUrl == null || imageUrl.isEmpty()) {
                        runOnUiThread(() -> ZIMKitToastUtils.showToast("上传成功但未取到图片地址"));
                        return;
                    }
                    updateGroupAvatarUrl(imageUrl);
                }

                @Override
                public void onError(String message) {
                    android.util.Log.w(TAG, "avatar upload fail: " + message);
                    runOnUiThread(() -> ZIMKitToastUtils.showToast(
                        message == null || message.isEmpty() ? "头像上传失败" : message));
                }
            });
    }

    private void updateGroupAvatarUrl(String url) {
        ZIMKitCore.getInstance().zim().updateGroupAvatarUrl(url, mId, (gid, avatarUrl, err) -> {
            boolean ok = err != null && err.code == ZIMErrorCode.SUCCESS;
            if (ok) {
                // ① 关键：改完立刻通知上层刷新「群头像覆盖表」（TestModule 注册的回调）。
                // 不能只依赖 ZIM 的 onGroupAvatarUrlUpdated 事件 —— 本地发起的修改该事件不一定回调；
                // 一旦不回调，覆盖表就永远是空的，uniapp 社群列表继续显示旧头像 / 首字方块。
                try {
                    com.zegocloud.zimkit.services.internal.ZIMKitEventHandler
                        .notifyGroupProfileChangedFromLocal(mId);
                } catch (Exception e) {
                    android.util.Log.w(TAG, "notifyGroupProfileChanged fail: " + e.getMessage());
                }
                // ② 业务库回写：否则分享组件（读后端 avatar）永远显示旧头像。
                // 社群频道不打这个接口 —— 社群头像走社群管理接口（POST /social/group/update）。
                if (!isCommunityChannel) {
                    com.zegocloud.zimkit.common.utils.GroupProfileApi
                        .pushToBackend(mId, null, avatarUrl);
                }
            }
            runOnUiThread(() -> {
                if (ok) {
                    ImageView thumb = findViewById(R.id.group_avatar_thumb);
                    if (thumb != null) {
                        Glide.with(ZIMKitGroupChatSettingActivity.this).load(avatarUrl).into(thumb);
                    }
                    ZIMKitToastUtils.showToast("群聊头像已更新");
                } else {
                    ZIMKitToastUtils.showToast(err == null ? "头像更新失败" : err.message);
                }
            });
        });
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
                            // 名单到手后重新判定群主身份：本地缓存冷启动时可能没有自己，
                            // 早判定会把群主的「群聊名称/群聊头像」误藏起来，这里补一次。
                            applyOwnerOnlyRows();
                        });
                    }
                    // 无论成败都置位：成员列表可能查不到，骨架不能因此卡住
                }
            });
        } catch (Exception e) {
        }
    }

    /**
     * **社群频道**的显隐（原样搬自 queryGroupAllAttributes 的 community 分支，行为不变）：
     * 隐藏 退出群聊 / 群二维码（两处）/ 群ID+复制 / 「邀请」「踢出」快捷项 / 群聊名称行 / 群二维码行；
     * 置顶 + 免打扰保留。
     *
     * <p>两个调用点：① {@code onCreate} 里**同步**调（bizType 缓存命中 → 第一帧就对，见 GroupBizTypeCache）
     * ② {@code queryGroupAllAttributes} 回来后调（异步权威结果，兜住缓存未命中/猜错）。
     */
    private void applyCommunityChannelVisibility() {
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
        // 注意：不在这里 refreshGroupMembers()（那是"改完标志后重画宫格"的副作用，由调用方决定；
        //       onCreate 同步路径下第 205 行的 refreshGroupMembers() 已经会带上新标志重画，避免多打一次接口）
        shortcutAdapter.setShowInvite(false);
        shortcutAdapter.setShowKick(false);
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
    }

    /**
     * 群主专属设置项（群聊名称 / 群聊头像）的显隐。管理员和普通成员都看不到，
     * 所以判定条件是「我是群主」而不是「我是群主或管理员」。
     */
    private void applyOwnerOnlyRows() {
        // 社群频道：连群主也不显示（名称/头像归社群管理页维护）
        final boolean owner = !isCommunityChannel && iAmGroupOwner();
        View nameRow = binding.getRoot().findViewById(R.id.group_name_row);
        View nameDivider = binding.getRoot().findViewById(R.id.chat_setting_divider1);
        View avatarRow = binding.getRoot().findViewById(R.id.group_avatar_row);
        View avatarDivider = binding.getRoot().findViewById(R.id.chat_setting_divider_avatar);
        if (nameRow != null) {
            nameRow.setVisibility(owner ? View.VISIBLE : View.GONE);
        }
        if (nameDivider != null) {
            nameDivider.setVisibility(owner ? View.VISIBLE : View.GONE);
        }
        if (avatarRow != null) {
            avatarRow.setVisibility(owner ? View.VISIBLE : View.GONE);
        }
        if (avatarDivider != null) {
            avatarDivider.setVisibility(owner ? View.VISIBLE : View.GONE);
        }
    }

    public static int dp2px(float v, DisplayMetrics displayMetrics) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, displayMetrics);
    }
}