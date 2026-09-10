package com.zegocloud.zimkit.services.model;

/**
 * IM 自定义消息 subType 常量（与后端确认契约一致）。
 */
public final class ZIMKitMessageSubType {

    private ZIMKitMessageSubType() {
    }

    /** 普通自定义文本（ZIMKit 原有逻辑） */
    public static final int TEXT = 0;

    /** 商品卡 */
    public static final int PRODUCT_CARD = 1;

    /** 店铺卡 */
    public static final int SHOP_CARD = 2;

    /** 文章卡 */
    public static final int ARTICLE_CARD = 3;

    /** 红包卡（普通 / 拼手气 / 专属） */
    public static final int RED_PACKET = 4;

    /** 红包领取 / 状态同步消息 */
    public static final int RED_PACKET_SYNC = 5;

    /** 功能模块卡（预留，本期不实现） */
    public static final int FUNCTION_MODULE = 6;

    /** 社群邀请卡（2人群聊内：邀请您加入社群） */
    public static final int COMMUNITY_INVITE = 7;

    public static String typeName(int subType) {
        switch (subType) {
            case PRODUCT_CARD:
                return "product";
            case SHOP_CARD:
                return "shop";
            case ARTICLE_CARD:
                return "article";
            case RED_PACKET:
                return "red_packet";
            case RED_PACKET_SYNC:
                return "red_packet_sync";
            case FUNCTION_MODULE:
                return "function_module";
            case COMMUNITY_INVITE:
                return "community_invite";
            default:
                return "text";
        }
    }
}
