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
        // 沉浸式：红顶区延伸到状态栏之下，状态栏图标/时间用白色（Figma 5339:6845 原样）
        io.dcloud.uniplugin.otherutils.ImmersiveBar.immersive(this, false);

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
        // 返回
        findViewById(R.id.rrBack).setOnClickListener(v -> finish());
        // 点「已存入"我的积分"」→ 我的积分页；水波纹安全设置（不用 ?selectableItemBackground，见布局注释）
        View mineRow = findViewById(R.id.rrMineRow);
        applyRipple(mineRow);
        mineRow.setOnClickListener(v -> openMyPoints());

        load();
    }

    /**
     * 安全设置点击水波纹：
     * 布局里不写 ?selectableItemBackground（MIUI 强制主题时解析不到 → inflate 直接崩），
     * 改为代码 resolveAttribute，失败就退化成浅灰按压色，绝不崩。
     */
    private void applyRipple(View view) {
        if (view == null) {
            return;
        }
        try {
            android.util.TypedValue out = new android.util.TypedValue();
            boolean ok = getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground, out, true);
            if (ok && out.resourceId != 0) {
                view.setBackgroundResource(out.resourceId);
            } else {
                view.setBackgroundColor(0x14000000);
            }
        } catch (Exception e) {
            android.util.Log.w("RedPacketRecords", "applyRipple fallback: " + e);
            try {
                view.setBackgroundColor(0x14000000);
            } catch (Exception ignored) {
            }
        }
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
        double myPoints = detail.getDoubleValue("myDrawPoints");
        JSONArray drawList = detail.getJSONArray("drawList");

        // 已领取状态（Figma 5339:6729）：大金额 40sp #111 + 「已存入"我的积分"」
        View amountBlock = findViewById(R.id.rrAmountBlock);
        View mineRow = findViewById(R.id.rrMineRow);
        if (myPoints > 0) {
            ((TextView) findViewById(R.id.rrAmountValue)).setText(trimZero(myPoints));
            amountBlock.setVisibility(View.VISIBLE);
            mineRow.setVisibility(View.VISIBLE);
        } else {
            amountBlock.setVisibility(View.GONE);
            mineRow.setVisibility(View.GONE);
        }

        // 概览文案：抢光时补「，N秒被抢光」（Figma 5339:6739）
        boolean ended = "ended".equals(status) || (count > 0 && drawCount >= count);
        String summary;
        if (ended) {
            summary = count + "个红包共" + total + "积分" + grabbedSeconds(detail);
        } else if ("expired".equals(status) || "refunded".equals(status)) {
            summary = count + "个红包共" + total + "积分，红包已过期";
        } else if (drawCount > 0) {
            summary = count + "个红包共" + total + "积分，已领取" + drawCount + "/" + count + "个";
        } else {
            summary = count + "个红包共" + total + "积分，等待被领取";
        }
        ((TextView) findViewById(R.id.rrSummary)).setText(summary);

        // 退款提示：还有未领取份额时显示（后端 24 小时后按剩余份额退款）
        //   · 已领完（ended）→ 无可退，隐藏
        //   · 已过期/已退款（expired/refunded）→ 退款已发生，隐藏
        View refundTip = findViewById(R.id.rrRefundTip);
        boolean expiredStatus = "expired".equals(status) || "refunded".equals(status);
        boolean hasRemain = count <= 0 || drawCount < count;
        refundTip.setVisibility(!ended && !expiredStatus && hasRemain ? View.VISIBLE : View.GONE);

        LinearLayout list = findViewById(R.id.rrList);
        list.removeAllViews();
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

        // 手气最佳：金额最大者（Figma 5339:6755，仅多人时标）
        double maxPoints = 0;
        for (int i = 0; i < drawList.size(); i++) {
            double p = drawList.getJSONObject(i).getDoubleValue("points");
            if (p > maxPoints) {
                maxPoints = p;
            }
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < drawList.size(); i++) {
            JSONObject item = drawList.getJSONObject(i);
            View row = inflater.inflate(R.layout.item_red_packet_record, list, false);
            String name = item.getString("userName");
            if (TextUtils.isEmpty(name)) {
                name = "用户";
            }
            double points = item.getDoubleValue("points");
            ((TextView) row.findViewById(R.id.recordName)).setText(name);
            ((TextView) row.findViewById(R.id.recordPoints)).setText(
                trimZero(points) + "积分");
            if (drawList.size() > 1 && maxPoints > 0 && points >= maxPoints) {
                row.findViewById(R.id.bestLabel).setVisibility(View.VISIBLE);
            }
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

    /** 「，N秒被抢光」：最后一条领取时间 − 红包创建时间（后端未返回创建时间则为空） */
    private String grabbedSeconds(JSONObject detail) {
        long created = detail.getLongValue("createTime");
        if (created <= 0) {
            created = detail.getLongValue("createdAt");
        }
        JSONArray drawList = detail.getJSONArray("drawList");
        if (created <= 0 || drawList == null || drawList.isEmpty()) {
            return "";
        }
        long last = 0;
        for (int i = 0; i < drawList.size(); i++) {
            long ts = drawList.getJSONObject(i).getLongValue("createTime");
            if (ts > last) {
                last = ts;
            }
        }
        if (last <= created) {
            return "";
        }
        return "，" + Math.max(1, (last - created) / 1000) + "秒被抢光";
    }

    /** 点「已存入"我的积分"」→ 通知 uniapp 跳我的积分页 */
    private void openMyPoints() {
        try {
            io.dcloud.uniplugin.TestModule.emitGlobalEvent("OPEN_MY_POINTS", new JSONObject());
        } catch (Exception e) {
            Toast.makeText(this, "打开我的积分失败", Toast.LENGTH_SHORT).show();
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
