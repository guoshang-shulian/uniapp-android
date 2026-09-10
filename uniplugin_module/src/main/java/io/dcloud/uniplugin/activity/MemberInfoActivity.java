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
    private String storeId = "";
    private boolean viewerIsOwner = false;
    private int targetRole = 3;
    private boolean selfIsAdmin = false;

    public static void start(Context context, String groupId, String memberUserId) {
        Intent intent = new Intent(context, MemberInfoActivity.class);
        intent.putExtra("groupId", groupId == null ? "" : groupId);
        intent.putExtra("memberUserId", memberUserId == null ? "" : memberUserId);
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
        android.util.Log.i("MemberClick", "MemberInfoActivity onCreate gid=" + groupId
            + " uid=" + memberUserId);

        bindViews();
        findViewById(R.id.miBack).setOnClickListener(v -> finish());
        load();
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

        kickBtn.setOnClickListener(v -> confirmKick());

        muteGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (updating) return;
            boolean mute = checkedId == R.id.miMuteYes;
            applyMute(mute);
        });
        storeRow.setOnClickListener(v -> openStore());
    }

    private void load() {
        if (memberUserId.isEmpty()) {
            Toast.makeText(this, "缺少成员ID", Toast.LENGTH_SHORT).show();
            finish();
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
            runOnUiThread(() -> fill(targetF, selfF));
        });
    }

    private void fill(ZIMGroupMemberInfo target, ZIMGroupMemberInfo self) {
        if (target == null) {
            Toast.makeText(this, "成员不存在", Toast.LENGTH_SHORT).show();
            finish();
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

        // 是否禁言：仅群主视角（且非自己）
        boolean selfOwner = self != null && self.memberRole == 1;
        selfIsAdmin = self != null && self.memberRole == 2;
        targetRole = target.memberRole;
        boolean targetSelf = self != null && self.userID != null && self.userID.equals(target.userID);
        if (selfOwner && !targetSelf) {
            muteRow.setVisibility(View.VISIBLE);
            updating = true;
            muteGroup.check(isMuted ? R.id.miMuteYes : R.id.miMuteNo);
            updating = false;
        } else {
            muteRow.setVisibility(View.GONE);
        }

        // 身份：目标是群主
        if (target.memberRole == 1) {
            identityRow.setVisibility(View.VISIBLE);
            identityValue.setText("群主");
        } else {
            identityRow.setVisibility(View.GONE);
        }

        // 他的店铺：默认“无”（后端补字段后展示）
        storeValue.setText("无");
        // 补充手机/地区/等级
        loadProfile(target.userID);
    }

    private void loadProfile(String zimUserId) {
        String memberId = Member.stripZimPrefix(zimUserId);
        if (memberId.isEmpty()) return;
        try {
            JSONObject query = new JSONObject();
            query.put("groupId", groupId);
            query.put("userId", memberId);
            RedPacketApi.get(TestModule.getBusinessBaseUrl() + "/social/group/member/profile",
                TestModule.getBusinessToken(), query, new RedPacketApi.Callback() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        if (result == null) return;
                        runOnUiThread(() -> applyProfile(result));
                    }

                    @Override
                    public void onError(int code, String message) {
                    }
                });
        } catch (Exception ignored) {
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
        if (p.containsKey("level") && p.get("level") != null) {
            String label = p.getString("levelLabel");
            level.setText(label != null && !label.isEmpty()
                ? label : (p.getIntValue("level") + "星"));
        } else {
            level.setText("");
        }
        long joinTime = p.getLongValue("joinTime");
        if (joinTime > 0) {
            join.setText(formatTime(joinTime));
        }
        String storeName = p.getString("storeName");
        storeValue.setText(storeName == null || storeName.isEmpty() ? "无" : storeName);
        storeId = p.getString("storeId") == null ? "" : p.getString("storeId");
        targetRole = p.getIntValue("role");
        // 禁言开关：viewerIsOwner 控制（且不是自己）；**群主(1)/管理员(2) 不可被禁言 → 不显示开关**
        viewerIsOwner = Boolean.TRUE.equals(p.getBoolean("viewerIsOwner"));
        boolean targetSelf = memberUserId != null && memberUserId.equals(TestModule.getLocalUserId());
        boolean targetPrivileged = targetRole == ROLE_OWNER || targetRole == ROLE_ADMIN;
        if (viewerIsOwner && !targetSelf && !targetPrivileged) {
            boolean muted = Boolean.TRUE.equals(p.getBoolean("muted"));
            muteRow.setVisibility(View.VISIBLE);
            updating = true;
            muteGroup.check(muted ? R.id.miMuteYes : R.id.miMuteNo);
            updating = false;
        } else {
            muteRow.setVisibility(View.GONE);
        }
        updateKickVisibility(!targetSelf);
    }

    /** 踢出按钮：群主/管理员 看【普通成员】才显示；群主本人不显示 */
    private void updateKickVisibility(boolean notSelf) {
        boolean canKick = notSelf && targetRole == 3 && (viewerIsOwner || selfIsAdmin);
        kickBtn.setVisibility(canKick ? View.VISIBLE : View.GONE);
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
                TestModule.getBusinessToken(), body, new RedPacketApi.Callback() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        runOnUiThread(() -> {
                            Toast.makeText(MemberInfoActivity.this, "已踢出社群", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }

                    @Override
                    public void onError(int code, String message) {
                        runOnUiThread(() -> Toast.makeText(MemberInfoActivity.this,
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
        JSONObject data = new JSONObject();
        data.put("storeId", storeId);
        TestModule.emitGlobalEvent("OPEN_MEMBER_STORE", data);
    }

    private void applyMute(boolean mute) {
        // 群主/管理员不受禁言影响：开关已隐藏，这里再兜一层，避免误调接口
        if (targetRole == ROLE_OWNER || targetRole == ROLE_ADMIN) {
            Toast.makeText(this, "群主/管理员不受禁言限制", Toast.LENGTH_SHORT).show();
            return;
        }
        // 禁言/解除走后端（后端调 ZIM）：POST /social/group/member/mute
        try {
            JSONObject body = new JSONObject();
            body.put("groupId", groupId);
            body.put("mute", mute ? 1 : 0);
            com.alibaba.fastjson.JSONArray ids = new com.alibaba.fastjson.JSONArray();
            ids.add(Member.stripZimPrefix(memberUserId));
            body.put("userIds", ids);
            RedPacketApi.post(TestModule.getBusinessBaseUrl() + "/social/group/member/mute",
                TestModule.getBusinessToken(), body, new RedPacketApi.Callback() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        runOnUiThread(() -> Toast.makeText(MemberInfoActivity.this,
                            mute ? "已禁言" : "已解除禁言", Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onError(int code, String message) {
                        runOnUiThread(() -> Toast.makeText(MemberInfoActivity.this,
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
}
