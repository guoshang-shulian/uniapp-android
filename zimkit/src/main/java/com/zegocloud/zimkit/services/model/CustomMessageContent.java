package com.zegocloud.zimkit.services.model;

import im.zego.zim.entity.ZIMCustomMessage;

public class CustomMessageContent {

    public ZIMCustomMessage customMessage;

    /** 自定义消息 subType（0=文本，1=商品卡，2=店铺卡，3=文章卡，4=红包卡，5=红包同步） */
    public int cardSubType;

    /** 已解析的卡片内容（商品/店铺/文章/红包），subType>0 时可用 */
    public CardMessageContent cardContent;
}
