package com.zegocloud.zimkit.common;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.zegocloud.zimkit.common.enums.ZIMKitConversationType;
import com.zegocloud.zimkit.components.message.ui.ZIMKitMessageActivity;
import com.zegocloud.zimkit.services.ZIMKit;
import com.zegocloud.zimkit.services.callback.MessageSentCallback;

import im.zego.zim.entity.ZIMError;
import im.zego.zim.enums.ZIMConversationType;
import im.zego.zim.enums.ZIMErrorCode;

public class ZIMKitRouter {


    /**
     * Jump to the chat page via session
     * No avatar required as parameter
     *
     * @param context
     * @param conversationId
     * @param type           Session type single chat or group chat
     */
    public static void toMessageActivity(Context context, String conversationId, ZIMKitConversationType type) {
        Bundle data = new Bundle();
        if (type == ZIMKitConversationType.ZIMKitConversationTypeGroup) {
            data.putString(ZIMKitConstant.MessagePageConstant.KEY_TYPE, ZIMKitConstant.MessagePageConstant.TYPE_GROUP_MESSAGE);
        } else if (type == ZIMKitConversationType.ZIMKitConversationTypePeer) {
            data.putString(ZIMKitConstant.MessagePageConstant.KEY_TYPE, ZIMKitConstant.MessagePageConstant.TYPE_SINGLE_MESSAGE);
        }
        data.putString(ZIMKitConstant.MessagePageConstant.KEY_ID, conversationId);
        data.putBoolean(ZIMKitConstant.MessagePageConstant.KEY_PUSH, false);

        Intent intent = new Intent(context, ZIMKitMessageActivity.class);
        intent.putExtra(ZIMKitConstant.RouterConstant.KEY_BUNDLE, data);
        context.startActivity(intent, data);
    }


    public static void toMessageActivity1(Context context, String conversationId, ZIMKitConversationType type, String name) {
        Bundle data = new Bundle();
        if (type == ZIMKitConversationType.ZIMKitConversationTypeGroup) {
            data.putString(ZIMKitConstant.MessagePageConstant.KEY_TYPE, ZIMKitConstant.MessagePageConstant.TYPE_GROUP_MESSAGE);
        } else if (type == ZIMKitConversationType.ZIMKitConversationTypePeer) {
            data.putString(ZIMKitConstant.MessagePageConstant.KEY_TYPE, ZIMKitConstant.MessagePageConstant.TYPE_SINGLE_MESSAGE);
        }
        data.putString(ZIMKitConstant.MessagePageConstant.KEY_ID, conversationId);
        data.putBoolean(ZIMKitConstant.MessagePageConstant.KEY_PUSH, false);

        Intent intent = new Intent(context, ZIMKitMessageActivity.class);
        Bundle productB = new Bundle();
        productB.putString("name",name);
        intent.putExtra(ZIMKitConstant.RouterConstant.KEY_BUNDLE, data);
        intent.putExtra("name",productB);
        context.startActivity(intent, data);
    }

    public static void toMessageActivity1(Context context, String conversationId, ZIMKitConversationType type, String product, String name) {
        Bundle data = new Bundle();
        if (type == ZIMKitConversationType.ZIMKitConversationTypeGroup) {
            data.putString(ZIMKitConstant.MessagePageConstant.KEY_TYPE, ZIMKitConstant.MessagePageConstant.TYPE_GROUP_MESSAGE);
        } else if (type == ZIMKitConversationType.ZIMKitConversationTypePeer) {
            data.putString(ZIMKitConstant.MessagePageConstant.KEY_TYPE, ZIMKitConstant.MessagePageConstant.TYPE_SINGLE_MESSAGE);
        }
        data.putString(ZIMKitConstant.MessagePageConstant.KEY_ID, conversationId);
        data.putBoolean(ZIMKitConstant.MessagePageConstant.KEY_PUSH, false);

        Intent intent = new Intent(context, ZIMKitMessageActivity.class);
        Bundle productA = new Bundle();
        productA.putString("name",name);
        Bundle productB = new Bundle();
        productB.putString("product",product);
        intent.putExtra(ZIMKitConstant.RouterConstant.KEY_BUNDLE, data);
        intent.putExtra("name",productA);
        intent.putExtra("product",productB);
        context.startActivity(intent, data);
    }

    public static void toMessageActivity(Context context, String conversationId, ZIMKitConversationType type, String product) {
        Bundle data = new Bundle();
        if (type == ZIMKitConversationType.ZIMKitConversationTypeGroup) {
            data.putString(ZIMKitConstant.MessagePageConstant.KEY_TYPE, ZIMKitConstant.MessagePageConstant.TYPE_GROUP_MESSAGE);
        } else if (type == ZIMKitConversationType.ZIMKitConversationTypePeer) {
            data.putString(ZIMKitConstant.MessagePageConstant.KEY_TYPE, ZIMKitConstant.MessagePageConstant.TYPE_SINGLE_MESSAGE);
        }
        data.putString(ZIMKitConstant.MessagePageConstant.KEY_ID, conversationId);
        data.putBoolean(ZIMKitConstant.MessagePageConstant.KEY_PUSH, false);

        Intent intent = new Intent(context, ZIMKitMessageActivity.class);
        Bundle productB = new Bundle();
        productB.putString("product",product);
        intent.putExtra(ZIMKitConstant.RouterConstant.KEY_BUNDLE, data);
        intent.putExtra("product",productB);
        context.startActivity(intent, data);
    }


    /**
     * Jump to the chat page via session
     *
     * @param context
     * @param conversationId
     * @param name           Session Title
     * @param avatar
     * @param type           Session type single chat or group chat
     */
    public static void toMessageActivity(Context context, String conversationId, String name, String avatar, ZIMKitConversationType type) {
        Bundle bundle = new Bundle();
        if (type == ZIMKitConversationType.ZIMKitConversationTypeGroup) {
            bundle.putString(ZIMKitConstant.MessagePageConstant.KEY_TYPE, ZIMKitConstant.MessagePageConstant.TYPE_GROUP_MESSAGE);
        } else if (type == ZIMKitConversationType.ZIMKitConversationTypePeer) {
            bundle.putString(ZIMKitConstant.MessagePageConstant.KEY_TYPE, ZIMKitConstant.MessagePageConstant.TYPE_SINGLE_MESSAGE);
            bundle.putString(ZIMKitConstant.MessagePageConstant.KEY_AVATAR, avatar);
        }
        bundle.putString(ZIMKitConstant.MessagePageConstant.KEY_ID, conversationId);
        bundle.putString(ZIMKitConstant.MessagePageConstant.KEY_TITLE, name);
        bundle.putBoolean(ZIMKitConstant.MessagePageConstant.KEY_PUSH, false);

        Intent intent = new Intent(context, ZIMKitMessageActivity.class);
        intent.putExtra(ZIMKitConstant.RouterConstant.KEY_BUNDLE, bundle);
        context.startActivity(intent, bundle);
    }

}
