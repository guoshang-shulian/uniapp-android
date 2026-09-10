package io.dcloud.uniplugin.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.dcloud.uniplugin.TestModule;
import io.dcloud.uniplugin.others.RedPacketApi;
import uni.dcloud.io.uniplugin_module.R;

/**
 * 红包领取记录（Figma 5339:6845 / 5339:6862）：
 * 红色顶部区 + 发件人行 + 祝福语 + 概览文案 + 领取明细列表。
 */
public class RedPacketRecordsActivity extends Activity {

    private String redPacketId = "";
    private String conversationId = "";
    private String senderName = "";
    private String senderAvatar = "";

    public static void start(Context context, String redPacketId, String conversationId,
        String senderName, String senderAvatar) {
        Intent intent = new Intent(context, RedPacketRecordsActivity.class);
        intent.putExtra("redPacketId", redPacketId == null ? "" : redPacketId);
        intent.putExtra("conversationId", conversationId == null ? "" : conversationId);
        intent.putExtra("senderName", senderName == null ? "" : senderName);
        intent.putExtra("senderAvatar", senderAvatar == null ? "" : senderAvatar);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_red_packet_records);

        redPacketId = getIntent().getStringExtra("redPacketId");
        conversationId = getIntent().getStringExtra("conversationId");
        senderName = getIntent().getStringExtra("senderName");
        senderAvatar = getIntent().getStringExtra("senderAvatar");
        if (redPacketId == null) redPacketId = "";
        if (conversationId == null) conversationId = "";
        if (senderName == null) senderName = "";
        if (senderAvatar == null) senderAvatar = "";

        ((TextView) findViewById(R.id.rrTitle)).setText("领取记录");
        ((TextView) findViewById(R.id.rrSenderName)).setText(
            TextUtils.isEmpty(senderName) ? "朋友" : senderName);
        ImageView avatar = findViewById(R.id.rrSenderAvatar);
        if (!TextUtils.isEmpty(senderAvatar)) {
            Glide.with(this).load(senderAvatar).circleCrop().into(avatar);
        }
        findViewById(R.id.rrBack).setOnClickListener(v -> finish());

        load();
    }

    private void load() {
        RedPacketApi.detail(TestModule.getBusinessToken(), redPacketId, conversationId,
            new RedPacketApi.Callback() {
                @Override
                public void onSuccess(JSONObject result) {
                    runOnUiThread(() -> render(result == null ? new JSONObject() : result));
                }

                @Override
                public void onError(int code, String message) {
                    runOnUiThread(() -> toast(message == null ? ("错误码 " + code) : message));
                }
            });
    }

    private void render(JSONObject detail) {
        String remark = detail.getString("remark");
        ((TextView) findViewById(R.id.rrRemark)).setText(
            TextUtils.isEmpty(remark) ? "恭喜发财，大吉大利" : remark);

        int count = detail.getIntValue("count");
        int drawCount = detail.getIntValue("drawCount");
        String total = trimZero(detail.getDoubleValue("totalPoints"));
        String status = detail.getString("status");
        String summary;
        if ("ended".equals(status) || (count > 0 && drawCount >= count)) {
            summary = count + "个红包共" + total + "积分，已被领完";
        } else if ("expired".equals(status) || "refunded".equals(status)) {
            summary = count + "个红包共" + total + "积分，红包已过期";
        } else if (drawCount > 0) {
            summary = count + "个红包共" + total + "积分，已领取" + drawCount + "/" + count + "个";
        } else {
            summary = count + "个红包共" + total + "积分，等待被领取";
        }
        ((TextView) findViewById(R.id.rrSummary)).setText(summary);

        LinearLayout list = findViewById(R.id.rrList);
        list.removeAllViews();
        JSONArray drawList = detail.getJSONArray("drawList");
        if (drawList == null || drawList.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("暂无领取记录");
            empty.setTextColor(0xFF999999);
            empty.setTextSize(14);
            empty.setPadding(dp(24), dp(40), dp(24), dp(40));
            empty.setGravity(android.view.Gravity.CENTER);
            list.addView(empty);
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < drawList.size(); i++) {
            JSONObject item = drawList.getJSONObject(i);
            View row = inflater.inflate(R.layout.item_red_packet_record, list, false);
            String name = item.getString("userName");
            if (TextUtils.isEmpty(name)) {
                name = "用户";
            }
            ((TextView) row.findViewById(R.id.recordName)).setText(name);
            ((TextView) row.findViewById(R.id.recordPoints)).setText(
                trimZero(item.getDoubleValue("points")) + "积分");
            long ts = item.getLongValue("createTime");
            ((TextView) row.findViewById(R.id.recordTime)).setText(
                ts > 0 ? new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(ts)) : "");
            ImageView avatarView = row.findViewById(R.id.recordAvatar);
            String face = item.getString("avatarUrl");
            if (TextUtils.isEmpty(face)) {
                face = item.getString("face");
            }
            if (!TextUtils.isEmpty(face)) {
                Glide.with(this).load(face).circleCrop().into(avatarView);
            }
            list.addView(row);
            if (i < drawList.size() - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(0x0D000000);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
                lp.leftMargin = dp(12);
                lp.rightMargin = dp(12);
                divider.setLayoutParams(lp);
                list.addView(divider);
            }
        }
    }

    private String trimZero(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String message) {
        Toast.makeText(this, message == null ? "" : message, Toast.LENGTH_SHORT).show();
    }
}
