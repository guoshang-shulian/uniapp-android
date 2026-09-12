package com.zegocloud.zimkit.services.model;

import im.zego.zim.entity.ZIMCustomMessage;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 自定义卡片内容解析（商品/店铺/文章/红包）。
 *
 * <p>payload 是 ZIM 自定义消息的 {@code message} 字段（JSON 字符串），
 * 由后端 {@code /buyer/social/group/chat/goods-card} 给出结构（客户端发消息时用 payloadJson）。
 * 卡片类型由 {@code subType} 区分，各类型的字段直接从这个 json 里按 key 取。
 */
public class CardMessageContent {

    public int cardSubType;
    public String payloadJson;
    public JSONObject json;

    public static CardMessageContent parse(int subType, ZIMCustomMessage message) {
        CardMessageContent content = new CardMessageContent();
        content.cardSubType = subType;
        if (message == null) {
            return content;
        }
        content.payloadJson = message.message == null ? "" : message.message;
        try {
            content.json = new JSONObject(content.payloadJson);
        } catch (JSONException e) {
            content.json = new JSONObject();
        }
        return content;
    }

    public static CardMessageContent parse(int subType, String payload) {
        CardMessageContent content = new CardMessageContent();
        content.cardSubType = subType;
        content.payloadJson = payload == null ? "" : payload;
        try {
            content.json = new JSONObject(content.payloadJson);
        } catch (JSONException e) {
            content.json = new JSONObject();
        }
        return content;
    }

    public boolean isValid() {
        return cardSubType > 0 && json != null && json.length() > 0;
    }

    public String getString(String key) {
        if (json == null) {
            return "";
        }
        return json.optString(key, "");
    }

    public String getNestedString(String parent, String key) {
        if (json == null) {
            return "";
        }
        JSONObject obj = json.optJSONObject(parent);
        if (obj == null) {
            return "";
        }
        return obj.optString(key, "");
    }

    public double getNestedDouble(String parent, String key) {
        if (json == null) {
            return 0;
        }
        JSONObject obj = json.optJSONObject(parent);
        if (obj == null) {
            return 0;
        }
        return obj.optDouble(key, 0);
    }

    public int getNestedInt(String parent, String key) {
        if (json == null) {
            return 0;
        }
        JSONObject obj = json.optJSONObject(parent);
        if (obj == null) {
            return 0;
        }
        return obj.optInt(key, 0);
    }

    public double getDouble(String key) {
        if (json == null) {
            return 0;
        }
        return json.optDouble(key, 0);
    }

    public int getInt(String key) {
        if (json == null) {
            return 0;
        }
        return json.optInt(key, 0);
    }

    public long getLong(String key) {
        if (json == null) {
            return 0;
        }
        return json.optLong(key, 0);
    }

    /** 会话列表 / 通知 / 回复预览文案 */
    public String getSummary() {
        if (cardSubType == ZIMKitMessageSubType.RED_PACKET) {
            String type = getNestedString("detail", "type");
            if ("fortune".equals(type)) {
                return "[拼手气红包]";
            }
            if ("exclusive".equals(type)) {
                return "[专属红包]";
            }
            return "[红包]";
        }
        if (cardSubType == ZIMKitMessageSubType.RED_PACKET_SYNC) {
            return "[红包]";
        }
        if (cardSubType == ZIMKitMessageSubType.PRODUCT_CARD) {
            return "[商品]";
        }
        if (cardSubType == ZIMKitMessageSubType.SHOP_CARD) {
            return "[店铺]";
        }
        if (cardSubType == ZIMKitMessageSubType.ARTICLE_CARD) {
            return "[文章]";
        }
        if (cardSubType == ZIMKitMessageSubType.FUNCTION_MODULE) {
            return "[功能]";
        }
        if (cardSubType == ZIMKitMessageSubType.COMMUNITY_INVITE) {
            return "[社群邀请]";
        }
        return getNestedString("detail", "remark");
    }
}
