package io.dcloud.uniplugin.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;

import io.dcloud.uniplugin.TestModule;
import io.dcloud.uniplugin.others.RedPacketApi;

/**
 * 红包卡片点击 → 只做「领取」（不打开领取记录页）。
 * 流程：查详情 → 判断可领性 → 领取 → 成功弹窗 + 发送状态同步消息；任何异常/不可领 → 弹窗提示。
 */
public final class RedPacketClaimAction {

    private RedPacketClaimAction() {
    }

    public static void claim(Context context, String redPacketId, String conversationId,
        String conversationType, String senderUserId, String senderName, String senderAvatar) {
        final String rpId = sanitize(redPacketId);
        if (rpId.isEmpty()) {
            dialog(context, "无法领取", "红包信息缺失");
            return;
        }
        Toast.makeText(context, "领取中...", Toast.LENGTH_SHORT).show();
        // 直接调用【领取红包】接口：可领性由后端校验（已领过/抢完/过期/专属等都会返回 message）
        doDraw(context, rpId, conversationId, conversationType);
    }

    /** 返回不可领取原因；可领取返回 null（保留：如需前端预检可调用） */
    static String blockReason(JSONObject detail, String senderUserId) {
        String status = detail.getString("status");
        double myPoints = detail.getDoubleValue("myDrawPoints");
        if ("ended".equals(status)) {
            return "手慢了，红包已抢完";
        }
        if ("expired".equals(status) || "refunded".equals(status)) {
            return "红包已过期";
        }
        if (myPoints > 0) {
            return "你已领取过该红包";
        }
        String toUserId = detail.getString("toUserId");
        if ("exclusive".equals(detail.getString("type")) && !TextUtils.isEmpty(toUserId)
            && !toUserId.equals(TestModule.getLocalUserId())) {
            return "这是别人的专属红包";
        }
        String self = TestModule.getLocalUserId();
        if (!TextUtils.isEmpty(senderUserId) && senderUserId.equals(self)) {
            return "不能领取自己发出的红包";
        }
        return null;
    }

    private static void doDraw(Context context, String redPacketId, String conversationId,
        String conversationType) {
        JSONObject params = new JSONObject();
        params.put("redPacketId", redPacketId);
        params.put("groupId", conversationId);
        params.put("clientRequestId", "rp_draw_" + System.currentTimeMillis());
        RedPacketApi.draw(TestModule.getBusinessToken(), params, new RedPacketApi.Callback() {
            @Override
            public void onSuccess(JSONObject result) {
                if (result == null) {
                    dialog(context, "领取失败", "未获取到领取结果");
                    return;
                }
                if (Boolean.TRUE.equals(result.getBoolean("alreadyDrawn"))) {
                    dialog(context, "提示", "你已领取过该红包");
                    return;
                }
                double points = result.getDoubleValue("points");
                dialog(context, "领取成功", "已领取 " + String.format("%.2f", points) + " 积分");
                sendDrawSync(conversationId, conversationType, redPacketId, result, "", "");
            }

            @Override
            public void onError(int code, String message) {
                dialog(context, "领取失败", message == null ? ("错误码 " + code) : message);
            }
        });
    }

    /** 领取成功：发一条红包状态同步消息（卡片状态刷新 + 领取提示）——弹窗/详情页共用 */
    static void sendDrawSync(String conversationId, String conversationType,
        String redPacketId, JSONObject drawResult, String ownerId, String ownerName) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("version", 1);
            payload.put("cardType", "red_packet");
            payload.put("conversationType", TextUtils.isEmpty(conversationType) ? "group" : conversationType);
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
            String selfId = TestModule.getLocalUserId();
            syncDetail.put("userId", selfId != null && selfId.startsWith("user_")
                ? selfId : ("user_" + (selfId == null ? "" : selfId)));
            syncDetail.put("userName", TestModule.getLocalUserName());
            syncDetail.put("packetOwnerId", ownerId == null ? "" : ownerId);
            syncDetail.put("packetOwnerName", ownerName == null ? "" : ownerName);
            syncDetail.put("points", drawResult.getDoubleValue("points"));
            syncDetail.put("isLast", Boolean.TRUE.equals(drawResult.getBoolean("isLast")));
            syncDetail.put("remainCount", drawResult.getIntValue("remainCount"));
            syncDetail.put("remainPoints", drawResult.getDoubleValue("remainPoints"));
            payload.put("detail", syncDetail);

            com.zegocloud.zimkit.services.ZIMKit.sendCustomMessage(payload.toJSONString(), 5,
                conversationId,
                "peer".equalsIgnoreCase(conversationType)
                    ? im.zego.zim.enums.ZIMConversationType.PEER
                    : im.zego.zim.enums.ZIMConversationType.GROUP,
                error -> {
                });
        } catch (Exception ignored) {
        }
    }

    private static void dialog(Context context, String title, String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("确定", null)
                    .show();
            } catch (Exception e) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private static String sanitize(String id) {
        String value = id == null ? "" : id;
        int idx = value.indexOf('?');
        return idx > 0 ? value.substring(0, idx) : value;
    }
}
