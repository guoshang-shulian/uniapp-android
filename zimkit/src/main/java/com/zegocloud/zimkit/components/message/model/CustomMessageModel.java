package com.zegocloud.zimkit.components.message.model;

import androidx.databinding.Bindable;
import com.zegocloud.zimkit.services.model.CardMessageContent;
import com.zegocloud.zimkit.services.model.ZIMKitMessageSubType;
import im.zego.zim.entity.ZIMCustomMessage;
import im.zego.zim.entity.ZIMMessage;
import im.zego.zim.enums.ZIMMessageType;

public class CustomMessageModel extends ZIMKitMessageModel {

    private String mContent;

    private int cardSubType = ZIMKitMessageSubType.TEXT;

    private CardMessageContent cardContent;

    @Override
    public void onProcessMessage(ZIMMessage message) {
        if (message.getType() == ZIMMessageType.CUSTOM) {
            ZIMCustomMessage customMessage = (ZIMCustomMessage) message;
            if (customMessage.subType == ZIMKitMessageSubType.TEXT) {
                mContent = customMessage.message;
            } else {
                cardSubType = customMessage.subType;
                cardContent = CardMessageContent.parse(customMessage.subType, customMessage);
                mContent = cardContent.getSummary();
            }
        }
    }

    @Bindable
    public String getContent() {
        return mContent;
    }

    public void setContent(String content) {
        this.mContent = content;
    }

    public int getCardSubType() {
        return cardSubType;
    }

    public void setCardSubType(int cardSubType) {
        this.cardSubType = cardSubType;
    }

    public CardMessageContent getCardContent() {
        return cardContent;
    }

    public void setCardContent(CardMessageContent cardContent) {
        this.cardContent = cardContent;
    }
}
