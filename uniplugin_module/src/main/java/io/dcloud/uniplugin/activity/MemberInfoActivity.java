package io.dcloud.uniplugin.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.zegocloud.zimkit.common.utils.ZimkitStatusBar;

import java.util.ArrayList;
import java.util.Calendar;

import im.zego.zim.callback.ZIMGroupMemberListQueriedCallback;
import im.zego.zim.entity.ZIMError;
import im.zego.zim.entity.ZIMErrorUserInfo;
import im.zego.zim.entity.ZIMGroupMemberInfo;
import im.zego.zim.entity.ZIMGroupMemberMuteConfig;
import im.zego.zim.entity.ZIMGroupMemberQueryConfig;
import im.zego.zim.enums.ZIMErrorCode;
import io.dcloud.uniplugin.others.ClickGuard;
import io.dcloud.uniplugin.others.MemberInfoCallback;
import io.dcloud.uniplugin.others.RedPacketApi;
import io.dcloud.uniplugin.TestModule;
import io.dcloud.uniplugin.memberpicker.Member;
import uni.dcloud.io.uniplugin_module.R;

/**
 * 群成员信息页（图3/图4）：
 * - 群主看成员：昵称/手机号/地区/等级/入群时间 + 是否禁言（可切换）
 * - 成员看群主：同前 + 他的身份：群主（无禁言开关）
 * 风格对齐原生全屏群成员页（白底、顶部导航、左标签右值行）。
 */
public class MemberInfoActivity extends android.app.Activity {

    private String groupId = "";
    private String memberUserId = "";
    private boolean updating = false;

    /**
     * 入口模式：同一个资料页服务三个来源，显示内容不同。
     * 由 {@code groupId} + ZIM 群属性 {@code bizType} 推导（见 {@link #resolveMode()}），
     * 不需要调用方传参 —— 三条原生链路（聊天设置成员宫格 / 群成员列表 / 成员选择器）
     * 都只传 groupId，改调用方容易漏。
     */
    private static final int MODE_COMMUNITY = 1;   // 社群频道成员
    private static final int MODE_GROUP = 2;       // 群聊成员（含 2 人群聊）
    private static final int MODE_FRIEND = 3;      // 好友/联系人（无群上下文）
    private int mode = MODE_FRIEND;

    /** ZIM memberRole：1=群主 2=管理员 3=成员（群主/管理员不可被禁言、不可被踢） */
    private static final int ROLE_OWNER = 1;
    private static final int ROLE_ADMIN = 2;

    private TextView title;
    private ImageView avatar;
    private TextView avatarFallback;
    private TextView nick;
    private TextView phone;
    private TextView region;
    private TextView level;
    private TextView join;
    private TextView memberIdValue;
    private LinearLayout muteRow;
    private RadioGroup muteGroup;
    private LinearLayout storeRow;
    private TextView storeValue;
    private LinearLayout identityRow;
    private TextView identityValue;
    private TextView kickBtn;
    private TextView levelChip;
    private LinearLayout communitiesCard;
    private LinearLayout communitiesRow;
    private LinearLayout actions;
    private TextView chatBtn;
    private TextView friendBtn;
    /** 是否互为好友（由 /social/contact/{memberId} 的 friend 字段决定；null=还没返回 → 按钮不显示） */
    private Boolean isFriend = null;

    public static MemberInfoCallback memberInfoCallback;

    public static void setMemberInfoCallback(MemberInfoCallback listener) {
        memberInfoCallback = listener;
    }

    /**
     * 目标是不是自己（本地登录 userId 与目标一致）。
     * 是自己 → 底部按钮区整体不显示：发消息/加好友/删好友对自己都没有意义。
     * 注意要在 fill() 里判定后重新调 applyModeVisibility()，因为 onCreate 时还不知道目标是谁。
     */
    private boolean viewingSelf = false;
    /** 页面是否已销毁（onDestroy 置位）—— 异步回调守卫用，见 isAlive() */
    private static final String TAG_MI = "MemberInfo";

    private volatile boolean destroyed = false;
    /** 正在请求资料的 memberId 集合：防止多入口重入并发打同一个接口 */
    private final java.util.Set<String> loadingProfileIds =
        java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    /** 开单聊用的 ZIM userId（接口给 zimUserId，缺了就用 memberId 拼 user_ 前缀） */
    private String zimUserId = "";
    private String storeId = "";
    private int targetRole = 3;
    private boolean selfIsAdmin = false;

    /** 骨架屏容器与真内容容器 */
    private View skeleton;
    private View content;

    /**
     * 就绪闸门：资料接口 + 群属性（入口模式）都回来了才显示真内容。
     * <p>为什么要闸门：入口模式（MODE_COMMUNITY/…）和接口字段是两条异步链，
     * 谁先到都会让页面先按"猜测的一版"渲染一下再跳变 —— 用户看到的就是"闪一下"。
     * 用骨架屏挡在前面，等两边都到齐一次性切换。
     */
    private boolean profileReady = false;
    private boolean modeReady = false;
    /** 骨架里的色块（统一跑流光动画） */
    private final java.util.List<View> skeletonBlocks = new java.util.ArrayList<>();
    private final java.util.List<android.animation.ObjectAnimator> shimmerAnimators =
        new java.util.ArrayList<>();


    public static void start(Context context, String groupId, String memberUserId) {
        Intent intent = new Intent(context, MemberInfoActivity.class);
        intent.putExtra("groupId", groupId == null ? "" : groupId);
        intent.putExtra("memberUserId", memberUserId == null ? "" : memberUserId);
        // NEW_TASK：从非 Activity 上下文（Application）调用时也能起；同一 task 内压栈，返回回到来处
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_info);
        ZimkitStatusBar.setWhite(this);

        groupId = getIntent().getStringExtra("groupId");
        memberUserId = getIntent().getStringExtra("memberUserId");
        if (groupId == null) groupId = "";
        if (memberUserId == null) memberUserId = "";
        // 标题按入口区分：有群上下文=从群成员进来的；无群上下文=从联系人/好友进来的
        TextView titleView = findViewById(R.id.miTitle);
        if (titleView != null) {
            titleView.setText(groupId.isEmpty() ? "个人信息" : "社群成员");
        }
        android.util.Log.i("MemberClick", "MemberInfoActivity onCreate gid=" + groupId
            + " uid=" + memberUserId);
        bindViews();
        findViewById(R.id.miBack).setOnClickListener(v -> finish());
        startShimmer();
        armRevealTimeout();
        // 入口模式先给个默认值（无 groupId 直接判定为好友，不用等异步），群属性回来后再修正
        if (groupId.isEmpty()) {
            mode = MODE_FRIEND;
            modeReady = true;
        } else {
            mode = MODE_GROUP;
            resolveMode();
        }
        load();
    }

    // ── 骨架屏（流光） ──

    /** 收集骨架色块并启动横向位移动画（渐变 centerX 来回扫 → 视觉上是流光） */
    private void startShimmer() {
        int[] ids = {
            R.id.skAvatar, R.id.skName,
            R.id.skL1, R.id.skR1, R.id.skL2, R.id.skR2, R.id.skL3, R.id.skR3
        };
        for (int id : ids) {
            View v = findViewById(id);
            if (v == null) {
                continue;
            }
            try {
                android.graphics.drawable.Drawable bg =
                    androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_skeleton_shimmer);
                if (bg != null) {
                    v.setBackground(bg.mutate().getConstantState() == null ? bg : bg.mutate());
                }
            } catch (Exception ignored) {
            }
            skeletonBlocks.add(v);
        }
        for (View v : skeletonBlocks) {
            android.graphics.drawable.Drawable bg = v.getBackground();
            if (!(bg instanceof android.graphics.drawable.GradientDrawable)) {
                continue;
            }
            android.graphics.drawable.GradientDrawable gd = (android.graphics.drawable.GradientDrawable) bg;
            android.animation.ObjectAnimator anim =
                android.animation.ObjectAnimator.ofFloat(gd, "centerX", 0.05f, 0.95f);
            anim.setDuration(1100);
            anim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            anim.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            anim.setInterpolator(new android.view.animation.LinearInterpolator());
            anim.start();
            shimmerAnimators.add(anim);
        }
    }

    private void stopShimmer() {
        for (android.animation.ObjectAnimator a : shimmerAnimators) {
            try {
                a.cancel();
            } catch (Exception ignored) {
            }
        }
        shimmerAnimators.clear();
    }

    /** 标记某条异步链就绪；两条都到齐才一次性切到真内容 */
    private void markReady(boolean isProfile) {
        if (isProfile) {
            profileReady = true;
        } else {
            modeReady = true;
        }
        if (profileReady && modeReady) {
            uiThread(this::revealContent);
        }
    }

    /** 兜底放行：任一异步链超时未回也切内容（骨架屏不能变成"永久 loading"） */
    private void armRevealTimeout() {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!(profileReady && modeReady)) {
                android.util.Log.w("MemberInfo", "reveal timeout, force show content"
                    + " profileReady=" + profileReady + " modeReady=" + modeReady);
                profileReady = true;
                modeReady = true;
                revealContent();
            }
        }, 3500);
    }

    private void revealContent() {
        stopShimmer();
        if (skeleton != null) {
            skeleton.setVisibility(View.GONE);
        }
        if (content != null) {
            content.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 推导入口模式：无 groupId → 好友；有 groupId → 查一次 bizType，
     * {@code community} = 社群频道，否则 = 普通群聊。
     * 三条原生入口（聊天设置成员宫格 / 群成员列表 / 成员选择器）都只传 groupId，
     * 社群与群聊的差别只体现在群属性上，所以在这里统一判定。
     */
    private void resolveMode() {
        if (groupId == null || groupId.isEmpty()) {
            return;
        }
        try {
            com.zegocloud.zimkit.services.internal.ZIMKitCore.getInstance().zim()
                .queryGroupAllAttributes(groupId, (gid, attrs, err) -> {
                    boolean community = attrs != null && "community".equals(attrs.get("bizType"));
                    uiThread(() -> {
                        mode = community ? MODE_COMMUNITY : MODE_GROUP;
                        android.util.Log.i("MemberInfo", "mode=" + mode + " gid=" + groupId);
                        applyModeVisibility();
                        markReady(false);
                    });
                });
        } catch (Exception e) {
            android.util.Log.w("MemberInfo", "resolveMode fail: " + e.getMessage());
        }
    }

    /**
     * 按入口模式控制各块显隐。规则（产品确认）：
     * <ul>
     *   <li>社群频道成员：去掉「拥有社群 / 底部按钮」，保留入群时间、身份、**他的店铺**；
     *       禁言/移出照常按角色显示（群主/管理员可管理）</li>
     *   <li>群聊成员：去掉「禁言 / 移出群聊」，保留拥有社群、店铺、底部按钮</li>
     *   <li>好友：保留拥有社群、店铺、底部按钮（发消息 + 加/删好友）；无入群时间/身份/禁言/移出</li>
     * </ul>
     *
     * <p>注意：这里**不决定**禁言/移出的显示，那两个只由角色决定（见 fill）。
     */
    private void applyModeVisibility() {
        boolean community = mode == MODE_COMMUNITY;
        boolean friend = mode == MODE_FRIEND;
        if (communitiesCard != null) {
            communitiesCard.setVisibility(community ? View.GONE : View.VISIBLE);
        }
        // 底部按钮区：社群频道成员页不显示；**看自己时也不显示**（发消息/加好友/删好友对自己都没意义）
        if (actions != null) {
            actions.setVisibility((community || viewingSelf) ? View.GONE : View.VISIBLE);
        }
        // 他的店铺：三个入口都显示（社群频道成员也要能看店铺；可点性由 storeId 决定）
        // 禁言/移出：社群频道按角色显示（群主/管理员可管理），群聊成员与好友都不显示。
        // 这三块的最终显隐由 fill() 统一裁定，这里只负责"入口一票否决"。
        if (mode != MODE_COMMUNITY) {
            if (muteRow != null) {
                muteRow.setVisibility(View.GONE);
            }
            if (kickBtn != null) {
                kickBtn.setVisibility(View.GONE);
            }
        }
        if (friend) {
            if (identityRow != null) {
                identityRow.setVisibility(View.GONE);
            }
            if (join != null) {
                join.setText("");
                View row = (View) join.getParent();
                if (row != null) {
                    row.setVisibility(View.GONE);
                }
            }
        }
    }

    private void bindViews() {
        title = findViewById(R.id.miTitle);
        avatar = findViewById(R.id.miAvatar);
        avatarFallback = findViewById(R.id.miAvatarFallback);
        nick = findViewById(R.id.miNickValue);
        phone = findViewById(R.id.miPhoneValue);
        region = findViewById(R.id.miRegionValue);
        level = findViewById(R.id.miLevelValue);
        join = findViewById(R.id.miJoinValue);
        memberIdValue = findViewById(R.id.miMemberIdValue);
        muteRow = findViewById(R.id.miMuteRow);
        muteGroup = findViewById(R.id.miMuteGroup);
        storeRow = findViewById(R.id.miStoreRow);
        storeValue = findViewById(R.id.miStoreValue);
        identityRow = findViewById(R.id.miIdentityRow);
        identityValue = findViewById(R.id.miIdentityValue);
        kickBtn = findViewById(R.id.miKickBtn);
        levelChip = findViewById(R.id.miLevelChip);
        communitiesCard = findViewById(R.id.miCommunitiesCard);
        communitiesRow = findViewById(R.id.miCommunitiesRow);
        actions = findViewById(R.id.miActions);
        chatBtn = findViewById(R.id.miChatBtn);
        friendBtn = findViewById(R.id.miFriendBtn);
        skeleton = findViewById(R.id.miSkeleton);
        content = findViewById(R.id.miContent);

        // 防抖：这几个按钮都有真实副作用（建群 / 跳转+多次 finish / 好友申请 / 踢人），
        // 连点会重复建群、重复请求、连环跳转。用 ClickGuard 按 key 独立拦 800ms。
        ClickGuard.bind(kickBtn, "MemberInfo_kick", this::confirmKick);

        muteGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (updating) return;
            boolean mute = checkedId == R.id.miMuteYes;
            // 禁言：连点开关会连发请求；这里按目标状态加同一把锁
            if (!ClickGuard.allow("MemberInfo_mute", 600)) {
                return;
            }
            applyMute(mute);
        });
        ClickGuard.bind(storeRow, "MemberInfo_store", this::openStore);
        ClickGuard.bind(chatBtn, "MemberInfo_chat", this::startChat);
        ClickGuard.bind(friendBtn, "MemberInfo_friend", this::onFriendAction);
    }

    private void load() {
        if (memberUserId.isEmpty()) {
            Toast.makeText(this, "缺少成员ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // 无群上下文（从「联系人/好友」进入）→ 不做群成员查询，直接按通用资料渲染，
        // 群相关行（身份/加入时间/禁言/踢出）本来就没有数据，fill 里会各自隐藏。
        if (groupId == null || groupId.isEmpty()) {
            fill(null, null);
            return;
        }
        final ArrayList<ZIMGroupMemberInfo> all = new ArrayList<>();
        fetchAll(groupId, 0, all, () -> {
            ZIMGroupMemberInfo target = null;
            ZIMGroupMemberInfo self = null;
            for (ZIMGroupMemberInfo info : all) {
                if (info == null || info.userID == null) continue;
                if (info.userID.equals(memberUserId)) {
                    target = info;
                }
            }
            String selfId = null;
            try {
                selfId = com.zegocloud.zimkit.services.internal.ZIMKitCore.getInstance().getLocalUser() == null
                    ? null : com.zegocloud.zimkit.services.internal.ZIMKitCore.getInstance().getLocalUser().getId();
            } catch (Exception ignored) {
            }
            for (ZIMGroupMemberInfo info : all) {
                if (info != null && selfId != null && selfId.equals(info.userID)) {
                    self = info;
                    break;
                }
            }
            final ZIMGroupMemberInfo targetF = target;
            final ZIMGroupMemberInfo selfF = self;
            uiThread(() -> fill(targetF, selfF));
        });
    }

    private void fill(ZIMGroupMemberInfo target, ZIMGroupMemberInfo self) {
        if (target == null) {
            if (mode == MODE_FRIEND) {
                // 好友入口没有群成员记录 —— 这是正常的，不是"成员不存在"。
                // 昵称/头像/等级等一律由 /social/contact/{memberId} 提供（applyProfile）。
                android.util.Log.i("MemberInfo", "fill: friend entry, defer to profile");
                loadProfile(memberUserId);
                return;
            }
            // 有群上下文却查不到成员：可能是刚被移出群或本地成员表未同步，仍然让接口资料兜底渲染
            android.util.Log.w("MemberInfo", "fill: member not found in group " + groupId
                + ", fallback to profile");
            loadProfile(memberUserId);
            return;
        }
        String name = target.memberNickname;
        if (name == null || name.isEmpty()) {
            name = target.userName;
        }
        if (name == null || name.isEmpty()) {
            name = Member.stripZimPrefix(target.userID);
        }
        title.setText(name);
        nick.setText(name);
        String av = target.memberAvatarUrl;
        if (av == null || av.isEmpty()) {
            av = target.userAvatarUrl;
        }
        if (av != null && !av.isEmpty()) {
            avatar.setVisibility(View.VISIBLE);
            avatarFallback.setVisibility(View.GONE);
            com.bumptech.glide.Glide.with(this).load(av).circleCrop().into(avatar);
        } else {
            avatar.setVisibility(View.GONE);
            avatarFallback.setVisibility(View.VISIBLE);
            avatarFallback.setText(name.length() > 0 ? name.substring(0, 1) : "?");
        }

        long enter = target.groupEnterInfo == null ? 0 : target.groupEnterInfo.enterTime;
        join.setText(enter <= 0 ? "" : formatTime(enter));
        long mutedUntil = target.muteExpiredTime;
        boolean isMuted = mutedUntil > 0;

        // 角色统一以 ZIM memberRole 为唯一口径（后端 /social/contact 不返回 role/viewerIsOwner）
        boolean selfOwner = self != null && self.memberRole == ROLE_OWNER;
        selfIsAdmin = self != null && self.memberRole == ROLE_ADMIN;
        targetRole = target.memberRole;
        boolean targetSelf = self != null && target.userID != null && self.userID.equals(target.userID);
        // 兜底：self 拿不到时（本地成员表未同步）用本地登录 userId 直接比对裸 memberId
        if (!targetSelf) {
            String localId = TestModule.getLocalUserId();
            String targetMid = Member.stripZimPrefix(target.userID);
            targetSelf = localId != null && !localId.isEmpty() && localId.equals(targetMid);
        }
        viewingSelf = targetSelf;
        boolean iAmAdmin = selfOwner || selfIsAdmin;
        boolean targetPrivileged = targetRole == ROLE_OWNER || targetRole == ROLE_ADMIN;

        // 是否禁言：**仅社群频道**（产品确认：群聊成员页不显示禁言）+ 我是群主/管理员
        // + 目标不是自己 + 目标是普通成员（群主/管理员不可被禁言）
        boolean showMute = mode == MODE_COMMUNITY && iAmAdmin && !targetSelf && !targetPrivileged;
        if (showMute) {
            muteRow.setVisibility(View.VISIBLE);
            updating = true;
            muteGroup.check(isMuted ? R.id.miMuteYes : R.id.miMuteNo);
            updating = false;
        } else {
            muteRow.setVisibility(View.GONE);
        }

        // 身份：目标是群主/管理员（仅群入口显示）
        if (mode != MODE_FRIEND && targetRole == ROLE_OWNER) {
            identityRow.setVisibility(View.VISIBLE);
            identityValue.setText("群主");
        } else if (mode != MODE_FRIEND && targetRole == ROLE_ADMIN) {
            identityRow.setVisibility(View.VISIBLE);
            identityValue.setText("管理员");
        } else {
            identityRow.setVisibility(View.GONE);
        }

        // 移出：**仅社群频道**（产品确认：群聊成员页不显示移出）+ 我是群主/管理员
        // + 目标不是自己 + 目标是普通成员
        updateKickVisibility(mode == MODE_COMMUNITY && !targetSelf && !targetPrivileged && iAmAdmin);

        // 他的店铺：后端 profile 未到之前先按「无」并隐藏箭头
        storeValue.setText("无");
        setStoreClickable(false);
        // 补充手机/地区/等级
        loadProfile(target.userID);
    }

    /** 有店铺才可点、才显示右箭头（无店铺时整行不可点，避免点了没反应） */
    private void setStoreClickable(boolean clickable) {
        storeRow.setClickable(clickable);
        View arrow = findViewById(R.id.miStoreArrow);
        if (arrow != null) {
            arrow.setVisibility(clickable ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * 拉通用个人资料：{@code GET /buyer/social/contact/{memberId}}。
     *
     * <p>为什么换接口：这个页面现在有三个入口 —— 社群频道成员、群聊成员（含 2 人群聊）、好友联系人。
     * 旧接口 {@code /social/group/member/profile} 只在有群上下文时可用，且不返回好友关系/拥有社群；
     * 而 {@code /social/contact/{memberId}} 一次就给齐：friend / communities / level(Label) /
     * region(Name) / storeName / storeId / zimUserId —— 三个入口共用同一份数据。
     * 群相关的东西（身份/加入时间/禁言/踢出）继续用本地 ZIM 成员信息，不依赖后端。
     */
    private void loadProfile(String zimUserId) {
        String memberId = Member.stripZimPrefix(zimUserId);
        if (memberId.isEmpty()) {
            markReady(true);
            return;
        }
        // 请求去重：这个接口被多个入口触发（群成员填充 / 好友入口），
        // 重入会并发多次请求、并且回调打到已销毁页面上（历史崩溃点）。同一 memberId 只发一次。
        if (!loadingProfileIds.add(memberId)) {
            android.util.Log.i(TAG_MI, "loadProfile skipped (already loading) memberId=" + memberId);
            return;
        }
        try {
            RedPacketApi.get(TestModule.getBusinessBaseUrl() + "/social/contact/" + memberId,
                new JSONObject(), new RedPacketApi.Callback() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        // 三个出口都要解锁，否则失败后无法重试（页面重进就再也拉不到资料）
                        loadingProfileIds.remove(memberId);
                        if (result == null) {
                            markReady(true);
                            return;
                        }
                        uiThread(() -> {
                            applyProfile(result);
                            markReady(true);
                        });
                    }

                    @Override
                    public void onError(int code, String message) {
                        loadingProfileIds.remove(memberId);
                        // 接口失败也要放行骨架屏，否则页面会一直卡在骨架
                        android.util.Log.w(TAG_MI, "contact profile fail code=" + code
                            + " msg=" + message);
                        markReady(true);
                    }
                });
        } catch (Exception e) {
            loadingProfileIds.remove(memberId);
            markReady(true);
        }
    }

    private void applyProfile(JSONObject p) {
        // 头像/昵称
        String face = p.getString("face");
        if (face != null && !face.isEmpty()) {
            avatar.setVisibility(View.VISIBLE);
            avatarFallback.setVisibility(View.GONE);
            com.bumptech.glide.Glide.with(this).load(face).circleCrop().into(avatar);
        }
        String nickName = p.getString("nickName");
        if (nickName != null && !nickName.isEmpty()) {
            nick.setText(nickName);
            title.setText(nickName);
        }
        // 身份（群主/管理员/成员）
        String identity = p.getString("identity");
        if (identity != null && !identity.isEmpty()) {
            identityRow.setVisibility(View.VISIBLE);
            identityValue.setText(identity);
        }
        // 会员ID
        String mid = p.getString("memberId");
        if (mid != null && !mid.isEmpty()) {
            memberIdValue.setText(mid);
        }
        // 手机（脱敏）/地区/等级
        String mobile = p.getString("mobile");
        if (mobile != null && !mobile.isEmpty()) {
            phone.setText(maskMobile(mobile));
        }
        String regionTxt = p.getString("region");
        if (regionTxt == null || regionTxt.isEmpty()) {
            regionTxt = p.getString("regionName");
        }
        region.setText(regionTxt == null ? "" : regionTxt);
        // 等级：后端给了就显示（0 也显示）；null / 缺字段才不显示
        // 展示成 top card 上的 LV 徽章（对齐 uniapp profile.vue），资料行里的「等级」同步保留
        if (p.containsKey("level") && p.get("level") != null) {
            String label = p.getString("levelLabel");
            String levelText = label != null && !label.isEmpty()
                ? label : ("LV" + p.getIntValue("level"));
            level.setText(levelText);
            levelChip.setText(levelText);
            levelChip.setVisibility(View.VISIBLE);
        } else {
            level.setText("");
            levelChip.setVisibility(View.GONE);
        }
        // 开单聊用的 ZIM userId（接口优先，缺了用 memberId 拼 user_ 前缀）
        String zim = p.getString("zimUserId");
        zimUserId = (zim == null || zim.isEmpty()) ? "" : zim;
        // 好友关系：决定底部第二个按钮是「添加好友」还是「删除好友」。
        // 接口没返回（缺字段）→ 按钮保持隐藏，宁可不显示也不猜错。
        if (p.containsKey("friend")) {
            isFriend = Boolean.TRUE.equals(p.getBoolean("friend"));
        } else {
            isFriend = null;
        }
        renderFriendButton();
        renderCommunities(p.getJSONArray("communities"));
        // 好友入口没有群成员表，只能在这里用登录 userId 判"是不是自己"
        refreshViewingSelf();
        long joinTime = p.getLongValue("joinTime");
        if (joinTime > 0 && mode != MODE_FRIEND) {
            join.setText(formatTime(joinTime));
        }
        String storeName = p.getString("storeName");
        // 店铺 ID 兜底：兼容后端可能的其他字段命名（storeId / store_id / shopId）
        String sid = p.getString("storeId");
        if (sid == null || sid.isEmpty()) {
            sid = p.getString("store_id");
        }
        if (sid == null || sid.isEmpty()) {
            sid = p.getString("shopId");
        }
        storeId = sid == null ? "" : sid;
        boolean hasStore = !storeId.isEmpty();
        String shown = storeName;
        if ((shown == null || shown.isEmpty()) && hasStore) {
            shown = "查看店铺";
        }
        storeValue.setText(shown == null || shown.isEmpty() ? "无" : shown);
        setStoreClickable(hasStore);
        // 角色/禁言/移出：**只信 ZIM memberRole**（fill 里已算好）。
        // 不再读后端的 role / viewerIsOwner —— /social/contact/{memberId} 不返回这两个字段，
        // 之前 getIntValue("role") 拿到 0 会把"目标是否普通成员"判错，导致按钮显隐不准。
        // 这里只按入口模式收口一次，避免和 fill 的结论互相覆盖。
        applyModeVisibility();
    }

    /**
     * 目标是不是自己（本地登录 userId vs 目标 memberId，两边都剥 user_ 前缀比较）。
     * <p>两条路都要判：有群上下文时 {@code fill()} 能从 ZIM 成员表判；好友入口没群成员记录，
     * 只能在这里用登录 userId 判。判完统一收口到 {@link #applyModeVisibility()}。
     */
    private void refreshViewingSelf() {
        if (viewingSelf) {
            return;
        }
        try {
            String localId = TestModule.getLocalUserId();
            String targetMid = Member.stripZimPrefix(memberUserId);
            if (localId != null && !localId.isEmpty() && localId.equals(targetMid)) {
                viewingSelf = true;
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 底部第二个按钮：是好友 → 删除好友；非好友 → 添加好友。
     * <p>规则（产品确认）：
     * <ul>
     *   <li>看自己 → 整个按钮区隐藏（在 applyModeVisibility 里收口）</li>
     *   <li>**群聊成员入口**：是好友时"加好友"不显示、"删好友"也不显示（删好友只在好友页做）；
     *       非好友时显示"添加好友"</li>
     *   <li>好友入口：非好友 → 添加好友；是好友 → 删除好友</li>
     *   <li>{@code isFriend == null}（接口没回/没给字段）→ 整体隐藏，避免先"添加"后跳"删除"</li>
     * </ul>
     */
    private void renderFriendButton() {
        if (isFriend == null) {
            friendBtn.setVisibility(View.GONE);
            return;
        }
        boolean groupEntry = mode == MODE_GROUP;
        if (groupEntry) {
            // 群聊成员页：只在"非好友"时给「添加好友」；好友不显示任何好友操作
            if (isFriend) {
                friendBtn.setVisibility(View.GONE);
                return;
            }
            friendBtn.setVisibility(View.VISIBLE);
            friendBtn.setText("添加好友");
            return;
        }
        friendBtn.setVisibility(View.VISIBLE);
        friendBtn.setText(isFriend ? "删除好友" : "添加好友");
    }

    /** 「拥有社群」横滑卡（接口返回创建或加入的社群，不含临时群） */
    private void renderCommunities(com.alibaba.fastjson.JSONArray list) {
        communitiesRow.removeAllViews();
        if (list == null || list.isEmpty()) {
            communitiesCard.setVisibility(View.GONE);
            return;
        }
        communitiesCard.setVisibility(View.VISIBLE);
        for (int i = 0; i < list.size(); i++) {
            JSONObject c = list.getJSONObject(i);
            if (c == null) {
                continue;
            }
            String groupIdV = c.getString("groupId");
            String name = c.getString("groupName");
            String logo = c.getString("groupLogo");
            communitiesRow.addView(buildCommunityItem(
                groupIdV == null ? "" : groupIdV,
                name == null || name.isEmpty() ? "社群" : name,
                logo == null ? "" : logo));
        }
    }

    /** 单个社群项：120dp 圆角头像 + 两行名称（对齐 uniapp profile.vue 的横滑卡） */
    private View buildCommunityItem(final String groupIdV, String name, String logo) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(dp(72), LinearLayout.LayoutParams.WRAP_CONTENT);
        itemLp.rightMargin = dp(12);
        item.setLayoutParams(itemLp);

        android.widget.ImageView iv = new android.widget.ImageView(this);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(dp(60), dp(60));
        iv.setLayoutParams(ivLp);
        iv.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        iv.setBackgroundColor(0xFFECECF2);
        try {
            iv.setClipToOutline(true);
            android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
            bg.setColor(0xFFECECF2);
            bg.setCornerRadius(dp(8));
            iv.setBackground(bg);
        } catch (Exception ignored) {
        }
        if (logo.isEmpty()) {
            iv.setImageDrawable(null);
        } else {
            com.bumptech.glide.Glide.with(this).load(logo).into(iv);
        }
        item.addView(iv);

        TextView tv = new TextView(this);
        tv.setText(name);
        tv.setTextSize(12);
        tv.setTextColor(0xFF111111);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setMaxLines(2);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tvLp.topMargin = dp(5);
        tv.setLayoutParams(tvLp);
        item.addView(tv);

        if (!groupIdV.isEmpty()) {
            item.setOnClickListener(v -> toast("社群：" + name));
        }
        return item;
    }

    /**
     * 发消息：普通用户私聊 = 2 人 ZIM 群（自动创建/复用，无需加好友）；
     * 客服（merchant_/customer_）走 PEER 直连。与 uniapp profile.vue 的 startChat 同口径。
     */
    private void startChat() {
        String zimId = zimUserId;
        if (zimId == null || zimId.isEmpty()) {
            String mid = Member.stripZimPrefix(memberUserId);
            zimId = mid.isEmpty() ? "" : ("user_" + mid);
        }
        if (zimId.isEmpty()) {
            toast("缺少用户ID");
            return;
        }
        try {
            // 统一交给 TestModule：客服走 PEER，普通用户走 2 人群聊（与 uniapp profile.vue 同口径）
            memberInfoCallback.onClick(zimId);
           // TestModule.openMemberChat(zimId);
//            TestModule mk = new TestModule();
//            mk.openPeerChat(zimId);
//            TestModule.openPeerChat(zimId);
        } catch (Exception e) {
            toast("打开聊天失败：" + e.getMessage());
        }
    }

    /** 好友按钮：是好友 → 二次确认后删除；非好友 → 发好友申请 */
    private void onFriendAction() {
        if (isFriend == null) {
            return;
        }
        final String memberId = Member.stripZimPrefix(memberUserId);
        if (memberId.isEmpty()) {
            return;
        }
        if (!isFriend) {
            sendFriendApplication(memberId);
            return;
        }
        new android.app.AlertDialog.Builder(this)
            .setTitle("删除好友")
            .setMessage("确定将该好友从联系人中删除？")
            .setPositiveButton("确定", (d, w) -> deleteFriend(memberId))
            .setNegativeButton("取消", null)
            .show();
    }

    /** 添加好友：走原生 ZIM 好友申请（与联系人页「好友申请」同一桥） */
    private void sendFriendApplication(final String memberId) {
        try {
            TestModule.sendFriendApplicationFromNative(memberId, new com.alibaba.fastjson.JSONObject(),
                new io.dcloud.feature.uniapp.bridge.UniJSCallback() {
                    @Override
                    public void invoke(Object data) {
                        onFriendApplyResult(data, true);
                    }

                    @Override
                    public void invokeAndKeepAlive(Object data) {
                        onFriendApplyResult(data, false);
                    }
                });
        } catch (Exception e) {
            toast("发送失败：" + e.getMessage());
        }
    }

    /** 好友申请结果回主线程提示 */
    private void onFriendApplyResult(Object data, boolean oneShot) {
        final com.alibaba.fastjson.JSONObject r =
            data instanceof com.alibaba.fastjson.JSONObject ? (com.alibaba.fastjson.JSONObject) data : null;
        uiThread(() -> {
            boolean ok = r != null && Boolean.TRUE.equals(r.getBoolean("success"));
            if (ok) {
                toast("申请已发送");
            } else {
                String msg = r == null ? null : r.getString("message");
                toast(msg == null || msg.isEmpty() ? "发送失败" : msg);
            }
        });
    }

    /** 删除好友：DELETE /social/contact/{memberId}（与 uniapp profile.vue 同一接口） */
    private void deleteFriend(final String memberId) {
        try {
            RedPacketApi.delete(TestModule.getBusinessBaseUrl() + "/social/contact/" + memberId,
                new JSONObject(), new RedPacketApi.Callback() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        uiThread(() -> {
                            toast("已删除");
                            isFriend = false;
                            renderFriendButton();
                        });
                    }

                    @Override
                    public void onError(int code, String message) {
                        uiThread(() -> toast(message == null || message.isEmpty()
                            ? ("删除失败：错误码 " + code) : message));
                    }
                });
        } catch (Exception e) {
            toast("删除失败：" + e.getMessage());
        }
    }

    private int dp(int v) {
        return (int) android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private void toast(String message) {
        if (message != null && !message.isEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 移出按钮显隐。调用方（fill）已经把「有群聊上下文 + 我是群主/管理员 + 目标普通成员 + 非自己」
     * 全部算完了 —— 这里不再自己判权限，避免出现第二套口径（旧代码里 viewerIsOwner 依赖
     * 后端字段，而新接口不返回它，会让按钮永远不显示）。
     */
    private void updateKickVisibility(boolean show) {
        kickBtn.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void confirmKick() {
        String memberId = Member.stripZimPrefix(memberUserId);
        if (memberId.isEmpty()) return;
        new android.app.AlertDialog.Builder(this)
            .setTitle("踢出社群")
            .setMessage("确定将该成员移出社群？")
            .setPositiveButton("确定", (d, w) -> doKick(memberId))
            .setNegativeButton("取消", null)
            .show();
    }

    private void doKick(String memberId) {
        try {
            JSONObject body = new JSONObject();
            body.put("groupId", groupId);
            com.alibaba.fastjson.JSONArray ids = new com.alibaba.fastjson.JSONArray();
            ids.add(memberId);
            body.put("userIds", ids);
            RedPacketApi.post(TestModule.getBusinessBaseUrl() + "/social/group/member/remove",
                body, new RedPacketApi.Callback() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        uiThread(() -> {
                            Toast.makeText(MemberInfoActivity.this, "已踢出社群", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }

                    @Override
                    public void onError(int code, String message) {
                        uiThread(() -> Toast.makeText(MemberInfoActivity.this,
                            "操作失败：" + (message == null ? ("错误码 " + code) : message),
                            Toast.LENGTH_SHORT).show());
                    }
                });
        } catch (Exception e) {
            Toast.makeText(this, "操作失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /** 他的店铺 → uniapp 店铺首页 */
    private void openStore() {
        if (storeId == null || storeId.isEmpty()) {
            Toast.makeText(this, "该成员暂无店铺", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject data = new JSONObject();
            data.put("storeId", storeId);
            // 先让 uniapp 收到事件并开始 navigateTo（店铺卡片路径就是这么做的）
            TestModule.emitGlobalEvent("OPEN_MEMBER_STORE", data);
            // 关键：本页是"原生页盖在 uniapp 之上"。不关掉原生页的话，uniapp 的 navigateTo 虽然执行了，
            // 但页面被挡着 → 用户看到的是"点了没反应"。
            //
            // ⚠️ 必须关**所有**原生页，不能只关 ZIMKit 的：
            // 成员资料页有两条入口 —— ① ZIMKit 群成员列表（ZIMKitGroupMembersActivity，com.zegocloud.zimkit）
            // ② uniapp 调 openGroupMembers 打开的 io.dcloud.uniplugin.memberpicker.GroupMembersActivity。
            // 只关 ① 的话，从 ② 进来时会停在群成员页，用户得再手动返回一次才看到店铺首页。
            // finishNativePages 按「com.zegocloud.zimkit + io.dcloud.uniplugin」两个前缀一起关，并排除 uniapp 容器。
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    int closed = com.zegocloud.zimkit.common.utils.ZIMKitActivityUtils
                        .finishNativePages(MemberInfoActivity.class);
                    android.util.Log.i("MemberInfo", "store jump: closed " + closed
                        + " native page(s); alive="
                        + com.zegocloud.zimkit.common.utils.ZIMKitActivityUtils.describeAliveActivities());
                    finish();
                } catch (Exception e) {
                    android.util.Log.w("MemberInfo", "close native pages fail: " + e);
                    finish();
                }
            }, 150);
        } catch (Exception e) {
            Toast.makeText(this, "打开店铺失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void applyMute(boolean mute) {
        // 群主/管理员不受禁言影响：开关已隐藏，这里再兜一层，避免误调接口
        if (targetRole == ROLE_OWNER || targetRole == ROLE_ADMIN) {
            Toast.makeText(this, "群主/管理员不受禁言限制", Toast.LENGTH_SHORT).show();
            return;
        }
        // 禁言/解除走后端（后端调 ZIM）：POST /social/group/member/mute
        //
        // ⚠️ 契约不一致的兼容处理：接口文档写的是「userId（单数 string）」，
        // 但 uniapp 侧的批量解除（manage_group.vue）传的是「userIds（数组）」，
        // 两边口径不同。这里**两个字段都带上**：后端取它认识的那个，多余字段会被忽略，
        // 避免因为猜错字段名导致「点了没反应/报操作失败」。
        try {
            String memberId = Member.stripZimPrefix(memberUserId);
            JSONObject body = new JSONObject();
            body.put("groupId", groupId);
            body.put("mute", mute ? 1 : 0);   // 文档：1=禁言，0=不禁言
            body.put("userId", memberId);     // 文档口径（单数）
            com.alibaba.fastjson.JSONArray ids = new com.alibaba.fastjson.JSONArray();
            ids.add(memberId);
            body.put("userIds", ids);         // uniapp 批量口径（数组），兼容保留
            android.util.Log.i("MemberMute", "request body=" + body.toJSONString());
            RedPacketApi.post(TestModule.getBusinessBaseUrl() + "/social/group/member/mute",
                body, new RedPacketApi.Callback() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        android.util.Log.i("MemberMute", "ok mute=" + mute + " memberId=" + memberId);
                        uiThread(() -> Toast.makeText(MemberInfoActivity.this,
                            mute ? "已禁言" : "已解除禁言", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onError(int code, String message) {
                        android.util.Log.w("MemberMute", "fail mute=" + mute + " memberId=" + memberId
                            + " code=" + code + " msg=" + message);
                        uiThread(() -> Toast.makeText(MemberInfoActivity.this,
                            "操作失败：" + (message == null ? ("错误码 " + code) : message),
                            Toast.LENGTH_SHORT).show());
                    }
                });
        } catch (Exception e) {
            Toast.makeText(this, "操作失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 7) return mobile == null ? "" : mobile;
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }

    private String formatTime(long time) {
        try {
            Calendar c = Calendar.getInstance();
            if (time > 1000000000000L) {
                c.setTimeInMillis(time);
            } else {
                c.setTimeInMillis(time * 1000L);
            }
            return String.format("%04d.%02d.%02d %02d:%02d:%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
        } catch (Exception e) {
            return "";
        }
    }

    private void fetchAll(final String gid, final int flag, final ArrayList<ZIMGroupMemberInfo> all,
        final Runnable done) {
        ZIMGroupMemberQueryConfig config = new ZIMGroupMemberQueryConfig();
        config.count = 100;
        config.nextFlag = flag;
        try {
            com.zegocloud.zimkit.services.internal.ZIMKitCore.getInstance().zim().queryGroupMemberList(
                gid, config, new ZIMGroupMemberListQueriedCallback() {
                    @Override
                    public void onGroupMemberListQueried(String groupId,
                        ArrayList<ZIMGroupMemberInfo> memberList, int nextFlag, ZIMError error) {
                        if (error != null && error.code == ZIMErrorCode.SUCCESS && memberList != null) {
                            all.addAll(memberList);
                        }
                        if (memberList != null && nextFlag != 0 && all.size() < 1000) {
                            fetchAll(gid, nextFlag, all, done);
                        } else {
                            done.run();
                        }
                    }
                });
        } catch (Exception e) {
            done.run();
        }
    }

    @Override
    protected void onDestroy() {
        // 骨架流光是 INFINITE 动画，页面销毁必须停，否则泄漏 View
        stopShimmer();
        destroyed = true;
        super.onDestroy();
    }

    // ── 异步回调守卫 ──

    /**
     * 页面是否还活着。**所有网络回调都必须先过这道闸**。
     *
     * <p>修的真实崩溃（2026-09-12 14:04 抓到 FATAL）：
     * 点成员 → 进本页 → 资料接口还没回来用户就返回了 → 回调仍然执行
     * → {@code applyProfile()} 里 {@code Glide.with(this)} 抛
     * {@code IllegalArgumentException: You cannot start a load for a destroyed activity} → 闪退重启。
     * Glide 对已销毁 Activity 是**直接抛异常**，不是静默失败，所以必须提前拦住。
     */
    private boolean isAlive() {
        if (destroyed) {
            return false;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return !isFinishing() && !isDestroyed();
        }
        return !isFinishing();
    }

    /** runOnUiThread 的守卫版：页面已销毁就丢弃这次回调 */
    private void uiThread(Runnable r) {
        if (r == null) {
            return;
        }
        runOnUiThread(() -> {
            if (isAlive()) {
                r.run();
            }
        });
    }
}
