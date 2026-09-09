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

    private TextView title;
    private ImageView avatar;
    private TextView avatarFallback;
    private TextView nick;
    private TextView phone;
    private TextView region;
    private TextView level;
    private TextView join;
    private LinearLayout muteRow;
    private RadioGroup muteGroup;
    private LinearLayout storeRow;
    private TextView storeValue;
    private LinearLayout identityRow;
    private TextView identityValue;

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
        muteRow = findViewById(R.id.miMuteRow);
        muteGroup = findViewById(R.id.miMuteGroup);
        storeRow = findViewById(R.id.miStoreRow);
        storeValue = findViewById(R.id.miStoreValue);
        identityRow = findViewById(R.id.miIdentityRow);
        identityValue = findViewById(R.id.miIdentityValue);

        muteGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (updating) return;
            boolean mute = checkedId == R.id.miMuteYes;
            applyMute(mute);
        });
        storeRow.setOnClickListener(v -> Toast.makeText(this, "店铺跳转待后端字段", Toast.LENGTH_SHORT).show());
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
            RedPacketApi.get(TestModule.getBusinessBaseUrl() + "/social/contact/" + memberId,
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
        String mobile = p.getString("mobile");
        if (mobile != null && !mobile.isEmpty()) {
            phone.setText(maskMobile(mobile));
        }
        String regionTxt = p.getString("region");
        if (regionTxt != null && !regionTxt.isEmpty()) {
            region.setText(regionTxt);
        }
        String levelStr = p.getString("level");
        if (levelStr != null && !levelStr.isEmpty()) {
            try {
                int lv = Integer.parseInt(levelStr);
                level.setText(lv + "星");
            } catch (Exception e) {
                level.setText(levelStr);
            }
        }
        String storeName = p.getString("storeName");
        if (storeName != null && !storeName.isEmpty()) {
            storeValue.setText(storeName);
        }
        String nickName = p.getString("nickName");
        if (nickName != null && !nickName.isEmpty()) {
            nick.setText(nickName);
            title.setText(nickName);
        }
    }

    private void applyMute(boolean mute) {
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
