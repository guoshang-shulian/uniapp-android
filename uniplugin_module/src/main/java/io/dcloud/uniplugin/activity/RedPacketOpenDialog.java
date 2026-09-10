package io.dcloud.uniplugin.activity;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.bumptech.glide.Glide;

import java.util.Locale;

import io.dcloud.uniplugin.TestModule;
import io.dcloud.uniplugin.others.RedPacketApi;
import uni.dcloud.io.uniplugin_module.R;

/**
 * 红包沉浸式弹窗（全屏 Dialog；与项目内 MemberPickerBottomSheet 同一套可靠做法）。
 * 状态：可领取（開）/ 已领取（金额）/ 已被领完 / 已过期 / 专属（仅 XX 可领取）。
 *
 * 日志 tag：RedPacketOpen —— adb logcat -s RedPacketOpen:V RedPacketApi:V
 */
public class RedPacketOpenDialog extends Dialog {

    private static final String TAG = "RedPacketOpen";

    private final String redPacketId;
    private final String conversationId;
    private final String conversationType;
    private final String senderUserId;
    private final String senderName;
    private final String senderAvatar;
    private JSONObject detail;

    public RedPacketOpenDialog(Context context, String redPacketId, String conversationId,
        String conversationType, String senderUserId, String senderName, String senderAvatar) {
        super(context);
        this.redPacketId = sanitize(redPacketId);
        this.conversationId = conversationId == null ? "" : conversationId;
        this.conversationType = TextUtils.isEmpty(conversationType) ? "group" : conversationType;
        this.senderUserId = senderUserId == null ? "" : senderUserId;
        this.senderName = senderName == null ? "" : senderName;
        this.senderAvatar = senderAvatar == null ? "" : senderAvatar;
    }

    /** 安全弹出（异常兜底，避免闪退/黑屏） */
    public static void show(Context context, String redPacketId, String conversationId,
        String conversationType, String senderUserId, String senderName, String senderAvatar) {
        try {
            RedPacketOpenDialog dialog = new RedPacketOpenDialog(context, redPacketId, conversationId,
                conversationType, senderUserId, senderName, senderAvatar);
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "show fail", e);
            Toast.makeText(context, "打开红包失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate rpId=" + redPacketId + " conv=" + conversationId);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_red_packet_open);
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setDimAmount(0f);
        }
        setCanceledOnTouchOutside(false);

        TextView nameView = findViewById(R.id.rpSenderName);
        nameView.setText(TextUtils.isEmpty(senderName) ? "朋友" : senderName);
        ImageView avatar = findViewById(R.id.rpSenderAvatar);
        if (!TextUtils.isEmpty(senderAvatar)) {
            Glide.with(avatar.getContext()).load(senderAvatar).circleCrop().into(avatar);
        }

        findViewById(R.id.rpCloseBtn).setOnClickListener(v -> dismiss());
        findViewById(R.id.rpOpenBtn).setOnClickListener(v -> draw());
        findViewById(R.id.rpRecordsRow).setOnClickListener(v -> openRecords());
        loadDetail();
    }

    private void loadDetail() {
        if (redPacketId.isEmpty()) {
            toast("红包信息缺失");
            return;
        }
        RedPacketApi.detail(TestModule.getBusinessToken(), redPacketId, conversationId,
            new RedPacketApi.Callback() {
                @Override
                public void onSuccess(JSONObject result) {
                    Log.i(TAG, "detail ok");
                    post(() -> applyState(result));
                }

                @Override
                public void onError(int code, String message) {
                    Log.w(TAG, "detail fail code=" + code + " msg=" + message);
                }
            });
    }

    private void post(Runnable r) {
        View root = findViewById(R.id.rpCard);
        if (root != null) {
            root.post(r);
        } else {
            r.run();
        }
    }

    private void applyState(JSONObject result) {
        detail = result == null ? new JSONObject() : result;
        TextView amountView = findViewById(R.id.rpAmount);
        TextView remarkView = findViewById(R.id.rpRemark);
        TextView stateView = findViewById(R.id.rpStateText);
        View openBtn = findViewById(R.id.rpOpenBtn);

        String remark = detail.getString("remark");
        remarkView.setText(TextUtils.isEmpty(remark) ? "恭喜发财，大吉大利" : remark);

        String status = detail.getString("status");
        double myPoints = detail.getDoubleValue("myDrawPoints");
        String type = detail.getString("type");
        String toUserId = detail.getString("toUserId");
        String toUserName = detail.getString("toUserName");

        boolean expired = "expired".equals(status) || "refunded".equals(status);
        boolean ended = "ended".equals(status);
        boolean exclusiveToOther = "exclusive".equals(type) && !TextUtils.isEmpty(toUserId)
            && !toUserId.equals(TestModule.getLocalUserId());

        if (myPoints > 0) {
            openBtn.setVisibility(View.GONE);
            amountView.setText(String.format(Locale.CHINA, "%.2f 积分", myPoints));
            amountView.setVisibility(View.VISIBLE);
            stateView.setText("已存入积分账户");
            stateView.setVisibility(View.VISIBLE);
        } else if (ended) {
            openBtn.setVisibility(View.GONE);
            stateView.setText("已被领完");
            stateView.setVisibility(View.VISIBLE);
        } else if (expired) {
            openBtn.setVisibility(View.GONE);
            stateView.setText("红包已过期");
            stateView.setVisibility(View.VISIBLE);
        } else if (exclusiveToOther) {
            openBtn.setVisibility(View.GONE);
            stateView.setText("仅" + (TextUtils.isEmpty(toUserName) ? "对方" : toUserName) + "可领取");
            stateView.setVisibility(View.VISIBLE);
        } else {
            openBtn.setVisibility(View.VISIBLE);
            stateView.setVisibility(View.GONE);
        }
    }

    private void draw() {
        if (redPacketId.isEmpty()) {
            return;
        }
        findViewById(R.id.rpOpenBtn).setEnabled(false);
        JSONObject params = new JSONObject();
        params.put("redPacketId", redPacketId);
        params.put("groupId", conversationId);
        params.put("clientRequestId", "rp_draw_" + System.currentTimeMillis());
        Log.i(TAG, "draw start rpId=" + redPacketId);
        RedPacketApi.draw(TestModule.getBusinessToken(), params, new RedPacketApi.Callback() {
            @Override
            public void onSuccess(JSONObject result) {
                post(() -> {
                    findViewById(R.id.rpOpenBtn).setEnabled(true);
                    if (result == null) {
                        toast("未获取到领取结果");
                        return;
                    }
                    double points = result.getDoubleValue("points");
                    Log.i(TAG, "draw ok points=" + points);
                    JSONObject merged = detail == null ? new JSONObject() : detail;
                    merged.put("myDrawPoints", points);
                    applyState(merged);
                    RedPacketClaimAction.sendDrawSync(conversationId, conversationType,
                        redPacketId, result,
                        senderUserId.startsWith("user_") ? senderUserId : ("user_" + senderUserId),
                        senderName);
                });
            }

            @Override
            public void onError(int code, String message) {
                post(() -> {
                    findViewById(R.id.rpOpenBtn).setEnabled(true);
                    Log.w(TAG, "draw fail code=" + code + " msg=" + message);
                    toast(message == null ? ("错误码 " + code) : message);
                });
            }
        });
    }

    private void openRecords() {
        dismiss();
        RedPacketRecordsActivity.start(getContext(), redPacketId, conversationId, senderName,
            senderAvatar);
    }

    private void toast(String message) {
        Toast.makeText(getContext(), message == null ? "" : message, Toast.LENGTH_SHORT).show();
    }

    private static String sanitize(String id) {
        String value = id == null ? "" : id;
        int idx = value.indexOf('?');
        return idx > 0 ? value.substring(0, idx) : value;
    }
}
