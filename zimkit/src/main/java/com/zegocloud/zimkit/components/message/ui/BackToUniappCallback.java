package com.zegocloud.zimkit.components.message.ui;

public interface  BackToUniappCallback {
    void onDataReceived(String data);

    void onStartCall(String id);

    /**
     * 自定义卡片点击（商品/店铺/文章/红包等）。
     * data 为卡片完整 payload JSON（含 cardType/conversationId/detail）。
     */
    default void onCardAction(String action, String data) {
    }
}
