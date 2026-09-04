package io.dcloud.uniplugin.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Outline;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bumptech.glide.Glide;
import com.zegocloud.zimkit.services.ZIMKit;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import im.zego.zim.enums.ZIMConversationType;
import im.zego.zim.enums.ZIMErrorCode;
import uni.dcloud.io.uniplugin_module.R;
import io.dcloud.uniplugin.TestModule;
import io.dcloud.uniplugin.others.RedPacketApi;

/**
 * 原生红包详情/开红包页（方案A）。
 * UI 按旧 pages/pagesGoEasy/envelope_receive/index 还原：
 * 大椭圆封面 + 发送信息 + 金额 + 已领列表（头像/昵称/积分/时间/手气最佳）。
 */
public class RedPacketDetailActivity extends android.app.Activity {

    private static final boolean NATIVE_MOCK = false;
    private static final String RED_PACKET_BG =
        "https://app.gdmlmh.cn/static/images/red/red_bg.jpg"; // 与 uniapp envelope_receive 同源封面

    private String redPacketId = "";
    private String conversationId = "";
    private String senderUserId = "";
    private String senderName = "";
    private String senderAvatar = "";

    private ImageView topBg;
    private TextView drawBtn;
    private ImageView senderAvatarView;
    private TextView senderNameView;
    private TextView remarkText;
    private TextView amountText;
    private TextView statusText;
    private TextView recordsTitle;
    private LinearLayout recordsContainer;

    private JSONObject detail;

    public static void start(Context context, String redPacketId, String conversationId,
        String senderUserId, String senderName, String senderAvatar) {
        Intent intent = new Intent(context, RedPacketDetailActivity.class);
        intent.putExtra("redPacketId", redPacketId == null ? "" : redPacketId);
        intent.putExtra("conversationId", conversationId == null ? "" : conversationId);
        intent.putExtra("senderUserId", senderUserId == null ? "" : senderUserId);
        intent.putExtra("senderName", senderName == null ? "" : senderName);
        intent.putExtra("senderAvatar", senderAvatar == null ? "" : senderAvatar);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_red_packet_detail);

        redPacketId = getIntent().getStringExtra("redPacketId");
        conversationId = getIntent().getStringExtra("conversationId");
        senderUserId = getIntent().getStringExtra("senderUserId");
        senderName = getIntent().getStringExtra("senderName");
        senderAvatar = getIntent().getStringExtra("senderAvatar");
        if (redPacketId == null) redPacketId = "";
        if (conversationId == null) conversationId = "";
        if (senderName == null || senderName.isEmpty()) senderName = "朋友";

        bindViews();
        setupTopBg();
        loadDetail();
    }

    private void bindViews() {
        topBg = findViewById(R.id.topBg);
        drawBtn = findViewById(R.id.drawBtn);
        senderAvatarView = findViewById(R.id.senderAvatar);
        senderNameView = findViewById(R.id.senderName);
        remarkText = findViewById(R.id.remarkText);
        amountText = findViewById(R.id.amountText);
        statusText = findViewById(R.id.statusText);
        recordsTitle = findViewById(R.id.recordsTitle);
        recordsContainer = findViewById(R.id.recordsContainer);

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        drawBtn.setOnClickListener(v -> draw());

        senderNameView.setText(senderName + " 发出的红包");
        if (!TextUtils.isEmpty(senderAvatar)) {
            Glide.with(this).load(senderAvatar).circleCrop().into(senderAvatarView);
        }
    }

    /** 大椭圆封面：与 uniapp 一致，使用同源背景图 + 椭圆裁剪 + 金边 */
    private void setupTopBg() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && topBg != null) {
            topBg.setClipToOutline(true);
            topBg.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
            topBg.post(new Runnable() {
                @Override
                public void run() {
                    topBg.invalidateOutline();
                }
            });
        }
        if (topBg != null) {
            Glide.with(this).load(RED_PACKET_BG).centerCrop().into(topBg);
        }
    }

    private void loadDetail() {
        if (NATIVE_MOCK) {
            JSONObject mock = new JSONObject();
            mock.put("redPacketId", redPacketId);
            mock.put("type", "fortune");
            mock.put("totalPoints", 100);
            mock.put("unitPoints", 33.33);
            mock.put("count", 3);
            mock.put("toUserId", "");
            mock.put("toUserName", "");
            mock.put("remark", "恭喜发财，大吉大利");
            mock.put("status", "active");
            mock.put("drawCount", 1);
            mock.put("myDrawPoints", 0.0);
            mock.put("expireAt", System.currentTimeMillis() + 86400L * 1000);

            JSONArray drawList = new JSONArray();
            JSONObject u1 = new JSONObject();
            u1.put("userId", "105974");
            u1.put("userName", "明天会更好");
            u1.put("avatarUrl", "https://tse4-mm.cn.bing.net/th/id/OIP-C.NimIzUOhgk2QHjPwRE0Q8gHaE5?rs=1&pid=ImgDetMain");
            u1.put("points", 0.01);
            u1.put("createTime", "2024-07-29 14:01:04");
            JSONObject u2 = new JSONObject();
            u2.put("userId", "87253");
            u2.put("userName", "复兴中华");
            u2.put("avatarUrl", "https://img.zcool.cn/community/014cdd5a96ba16a801219586209ded.png@1280w_1l_2o_100sh.png");
            u2.put("points", 99.99);
            u2.put("createTime", "2024-07-29 14:01:05");
            drawList.add(u1);
            drawList.add(u2);
            mock.put("drawList", drawList);

            renderDetail(mock);
            return;
        }
        RedPacketApi.detail(TestModule.getBusinessToken(), redPacketId, conversationId,
            new RedPacketApi.Callback() {
                @Override
                public void onSuccess(JSONObject result) {
                    runOnUiThread(() -> renderDetail(result));
                }

                @Override
                public void onError(int code, String message) {
                    runOnUiThread(() -> toast(message == null ? ("错误码 " + code) : message));
                }
            });
    }

    private void renderDetail(JSONObject result) {
        detail = result == null ? new JSONObject() : result;
        String type = detail.getString("type");
        remarkText.setText(detail.getString("remark") == null || detail.getString("remark").isEmpty()
            ? "恭喜发财，大吉大利" : detail.getString("remark"));

        double myPoints = detail.getDoubleValue("myDrawPoints");
        double display = myPoints > 0 ? myPoints : detail.getDoubleValue("totalPoints");
        amountText.setText(String.format("%.2f", display));

        int drawCount = detail.getIntValue("drawCount");
        int count = detail.getIntValue("count");
        String status = detail.getString("status");

        if ("ended".equals(status)) {
            statusText.setText("已抢完");
            recordsTitle.setText("已抢光");
        } else if ("expired".equals(status) || "refunded".equals(status)) {
            statusText.setText("已过期");
            recordsTitle.setText("红包已过期");
        } else if (myPoints > 0) {
            statusText.setText("已领取");
            recordsTitle.setText("已领取");
        } else {
            statusText.setText(drawCount + "/" + count + " 份已领");
            recordsTitle.setText("领取" + drawCount + "/" + count + "个");
        }

        boolean exclusiveToOther = "exclusive".equals(type)
            && !TextUtils.isEmpty(detail.getString("toUserId"))
            && !detail.getString("toUserId").equals(TestModule.getLocalUserId());

        boolean canDraw = !("ended".equals(status) || "expired".equals(status) || "refunded".equals(status))
            && myPoints <= 0 && !exclusiveToOther;
        if (canDraw) {
            drawBtn.setVisibility(View.VISIBLE);
            drawBtn.setText("开");
            drawBtn.setEnabled(true);
        } else {
            drawBtn.setVisibility(View.GONE);
            if ("ended".equals(status)) {
                statusText.setText("手慢了，已抢完");
            } else if ("expired".equals(status) || "refunded".equals(status)) {
                statusText.setText("红包已过期");
            } else if (myPoints > 0) {
                statusText.setText("已领取");
            } else if (exclusiveToOther) {
                statusText.setText("这是别人的专属红包");
            }
        }

        renderRecords(detail.getJSONArray("drawList"));
    }

    /** 领取记录：头像 + 昵称 + 积分 + 时间 + 手气最佳（参照 envelope_receive .list） */
    private void renderRecords(JSONArray drawList) {
        recordsContainer.removeAllViews();
        if (drawList == null || drawList.isEmpty()) {
            recordsTitle.setText(normalizeTitle(recordsTitle.getText().toString()));
            TextView empty = new TextView(this);
            empty.setText("暂无领取记录");
            empty.setTextColor(0xFFB2B2B2);
            empty.setTextSize(13);
            empty.setPadding(dp(16), dp(16), dp(16), dp(16));
            recordsContainer.addView(empty);
            return;
        }

        double maxPoints = 0;
        for (int i = 0; i < drawList.size(); i++) {
            JSONObject item = drawList.getJSONObject(i);
            double points = itemDouble(item, "points", itemDouble(item, "red", itemDouble(item, "amount", 0)));
            if (points > maxPoints) maxPoints = points;
        }

        for (int i = 0; i < drawList.size(); i++) {
            JSONObject item = drawList.getJSONObject(i);
            View row = getLayoutInflater().inflate(R.layout.item_red_packet_record, recordsContainer, false);

            String name = itemString(item, "userName");
            if (name.isEmpty()) name = itemString(item, "nickname");
            if (name.isEmpty()) {
                JSONObject memberInfo = item.getJSONObject("member_info");
                if (memberInfo != null) name = itemString(memberInfo, "name");
            }
            if (name.isEmpty()) name = "用户";

            double points = itemDouble(item, "points", itemDouble(item, "red", itemDouble(item, "amount", 0)));
            String avatarUrl = itemString(item, "avatarUrl");
            if (avatarUrl.isEmpty()) avatarUrl = itemString(item, "avatar");
            if (avatarUrl.isEmpty()) {
                JSONObject memberInfo = item.getJSONObject("member_info");
                if (memberInfo != null) avatarUrl = itemString(memberInfo, "avatar");
            }

            String time = itemString(item, "createTime");
            if (time.isEmpty()) {
                long ts = item.getLongValue("createTime");
                if (ts > 0) time = formatTime(ts);
            }
            if (time.isEmpty()) time = "";

            ((TextView) row.findViewById(R.id.recordName)).setText(name);
            ((TextView) row.findViewById(R.id.recordPoints)).setText(String.format("%.2f", points));
            ((TextView) row.findViewById(R.id.recordTime)).setText(time);

            ImageView avatar = row.findViewById(R.id.recordAvatar);
            if (!avatarUrl.isEmpty()) {
                Glide.with(this).load(avatarUrl).circleCrop().into(avatar);
            }

            boolean best = maxPoints > 0 && points >= maxPoints;
            if (best) {
                row.findViewById(R.id.bestIcon).setVisibility(View.VISIBLE);
                row.findViewById(R.id.bestLabel).setVisibility(View.VISIBLE);
            }

            final JSONObject record = item;
            row.setOnClickListener(v -> {
                // 与 uniapp 一致：点击可查看成员信息（当前项目无原生成员页，保留跳转口）
                String memberId = record.getString("userId");
                if (memberId != null && memberId.startsWith("user_")) {
                    memberId = memberId.substring(5);
                } else if (memberId == null) {
                    memberId = "";
                }
                if (!memberId.isEmpty()) {
                    toast("成员ID: " + memberId);
                }
            });

            recordsContainer.addView(row);

            if (i < drawList.size() - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(0xFFE5E5E5);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
                recordsContainer.addView(divider);
            }
        }
    }

    private String normalizeTitle(String title) {
        if (TextUtils.isEmpty(title)) return "领取记录";
        return title;
    }

    private String itemString(JSONObject item, String key) {
        if (item == null || !item.containsKey(key) || item.getString(key) == null) return "";
        return item.getString(key);
    }

    private double itemDouble(JSONObject item, String key, double defaultValue) {
        if (item == null || !item.containsKey(key)) return defaultValue;
        try {
            return item.getDoubleValue(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String formatTime(long millis) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                .format(new Date(millis));
        } catch (Exception e) {
            return "";
        }
    }

    private void draw() {
        if (detail == null || !isDrawable()) {
            return;
        }
        if (NATIVE_MOCK) {
            JSONObject result = new JSONObject();
            result.put("redPacketId", redPacketId);
            result.put("points", 33.33);
            result.put("isLast", false);
            result.put("status", "active");
            result.put("remainCount", 1);
            result.put("remainPoints", 66.67);
            onDrawn(result);
            return;
        }
        JSONObject params = new JSONObject();
        params.put("redPacketId", redPacketId);
        params.put("groupId", conversationId);
        params.put("clientRequestId", "rp_draw_" + System.currentTimeMillis());
        RedPacketApi.draw(TestModule.getBusinessToken(), params, new RedPacketApi.Callback() {
            @Override
            public void onSuccess(JSONObject result) {
                runOnUiThread(() -> onDrawn(result));
            }

            @Override
            public void onError(int code, String message) {
                runOnUiThread(() -> toast(message == null ? ("错误码 " + code) : message));
            }
        });
    }

    private boolean isDrawable() {
        String status = detail.getString("status");
        if ("ended".equals(status) || "expired".equals(status) || "refunded".equals(status)) {
            return false;
        }
        return detail.getDoubleValue("myDrawPoints") <= 0;
    }

    private void onDrawn(JSONObject result) {
        double points = result.getDoubleValue("points");
        detail.put("myDrawPoints", points);
        int oldDraw = detail.getIntValue("drawCount");
        detail.put("drawCount", oldDraw + 1);
        boolean isLast = Boolean.TRUE.equals(result.getBoolean("isLast"));
        if (isLast) {
            detail.put("status", "ended");
        }
        renderDetail(detail);
        toast("领取成功 " + String.format("%.2f", points) + " 积分");
        sendDrawSync(result);
    }

    private void sendDrawSync(JSONObject drawResult) {
        JSONObject payload = new JSONObject();
        payload.put("version", 1);
        payload.put("cardType", "red_packet");
        payload.put("conversationType", "group");
        payload.put("conversationId", conversationId);
        JSONObject sender = new JSONObject();
        sender.put("userId", TestModule.getLocalUserId());
        sender.put("userName", TestModule.getLocalUserName());
        sender.put("avatarUrl", TestModule.getLocalUserAvatar());
        payload.put("sender", sender);
        payload.put("createdAt", System.currentTimeMillis());

        JSONObject syncDetail = new JSONObject();
        syncDetail.put("redPacketId", redPacketId);
        syncDetail.put("action", "draw");
        syncDetail.put("userId", TestModule.getLocalUserId());
        syncDetail.put("userName", TestModule.getLocalUserName());
        syncDetail.put("points", drawResult.getDoubleValue("points"));
        syncDetail.put("isLast", Boolean.TRUE.equals(drawResult.getBoolean("isLast")));
        syncDetail.put("remainCount", drawResult.getIntValue("remainCount"));
        syncDetail.put("remainPoints", drawResult.getDoubleValue("remainPoints"));
        payload.put("detail", syncDetail);

        ZIMKit.sendCustomMessage(payload.toJSONString(), 5, conversationId,
            ZIMConversationType.GROUP, error -> { /* 同步失败不阻塞 */ });
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String message) {
        if (message == null) return;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
