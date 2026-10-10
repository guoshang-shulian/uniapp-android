package io.dcloud.uniplugin;

import static io.dcloud.uniplugin.AppConfig.BASE_URL;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.google.gson.Gson;
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.utils.ToastX;
import com.netease.yunxin.kit.entertainment.common.RoomConstants;
import com.netease.yunxin.kit.entertainment.common.model.RoomModel;
import com.netease.yunxin.kit.voiceroomkit.api.model.NEVoiceRoomInfo;
import com.netease.yunxin.kit.voiceroomkit.ui.AppUtils;
import com.netease.yunxin.kit.voiceroomkit.ui.LoginUtil;
import com.netease.yunxin.kit.entertainment.common.http.ECHttpService;
import com.netease.yunxin.kit.entertainment.common.model.ECModelResponse;
import com.netease.yunxin.kit.entertainment.common.model.NemoAccount;
import com.netease.yunxin.kit.voiceroomkit.ui.activity.VoiceRoomCreateActivity;
import com.netease.yunxin.kit.voiceroomkit.ui.activity.VoiceRoomListActivity;
import com.netease.yunxin.kit.voiceroomkit.ui.base.utils.FloatPlayManager;
import com.netease.yunxin.kit.voiceroomkit.ui.utils.NavUtils;
import com.zegocloud.uikit.plugin.signaling.ZegoSignalingPlugin;
//import com.zegocloud.uikit.prebuilt.liveaudioroom.internal.service.LiveAudioRoomManager;
import com.zegocloud.zimkit.common.ZIMKitRouter;
import com.zegocloud.zimkit.common.enums.ZIMKitConversationType;
import com.zegocloud.zimkit.components.message.ui.BackToUniappCallback;
import com.zegocloud.zimkit.components.message.ui.ZIMKitMessageActivity;
import com.zegocloud.zimkit.components.message.ui.ZIMKitMessageFragment;
import com.zegocloud.zimkit.services.ZIMKit;
import com.zegocloud.zimkit.services.ZIMKitDelegate;
import com.zegocloud.zimkit.services.callback.CreateGroupCallback;
import com.zegocloud.zimkit.services.callback.JoinGroupCallback;
import com.zegocloud.zimkit.services.model.ZIMKitConversation;
import com.zegocloud.zimkit.services.callback.QueryGroupInfoCallback;
import com.zegocloud.zimkit.services.model.ZIMKitGroupInfo;
import com.zegocloud.zimkit.services.internal.ZIMKitCore;
//import  io.dcloud.uniplugin.
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import im.zego.zim.ZIM;
import im.zego.zim.callback.ZIMFriendApplicationAcceptedCallback;
import im.zego.zim.callback.ZIMFriendApplicationListQueriedCallback;
import im.zego.zim.callback.ZIMFriendApplicationRejectedCallback;
import im.zego.zim.callback.ZIMFriendApplicationSentCallback;
import im.zego.zim.callback.ZIMFriendListQueriedCallback;
import im.zego.zim.callback.ZIMFriendsDeletedCallback;
import im.zego.zim.callback.ZIMFriendsInfoQueriedCallback;
import im.zego.zim.callback.ZIMFriendsSearchedCallback;
import im.zego.zim.callback.ZIMGroupMemberKickedCallback;
import im.zego.zim.callback.ZIMGroupMembersMutedCallback;
import im.zego.zim.callback.ZIMGroupUsersInvitedCallback;
import im.zego.zim.callback.ZIMGroupAttributesOperatedCallback;
import im.zego.zim.callback.ZIMGroupAttributesQueriedCallback;
import com.zegocloud.zimkit.services.callback.MessageSentCallback;
import im.zego.zim.callback.ZIMGroupMemberListQueriedCallback;
import im.zego.zim.callback.ZIMGroupDismissedCallback;
import im.zego.zim.callback.ZIMGroupOwnerTransferredCallback;
import im.zego.zim.entity.ZIMGroupMemberQueryConfig;
import com.zegocloud.zimkit.services.callback.LeaveGroupCallback;
import im.zego.zim.callback.ZIMConversationDeletedCallback;
import im.zego.zim.entity.ZIMConversationDeleteConfig;
import im.zego.zim.callback.ZIMUsersInfoQueriedCallback;
import im.zego.zim.entity.ZIMConversationFilterOption;
import im.zego.zim.entity.ZIMConversation;
import im.zego.zim.entity.ZIMGroupMuteConfig;
import im.zego.zim.entity.ZIMConversationQueryConfig;
import im.zego.zim.entity.ZIMError;
import im.zego.zim.entity.ZIMErrorUserInfo;
import im.zego.zim.entity.ZIMFriendAddConfig;
import im.zego.zim.entity.ZIMFriendApplicationAcceptConfig;
import im.zego.zim.entity.ZIMFriendApplicationInfo;
import im.zego.zim.entity.ZIMFriendApplicationListQueryConfig;
import im.zego.zim.entity.ZIMFriendApplicationRejectConfig;
import im.zego.zim.entity.ZIMFriendApplicationSendConfig;
import im.zego.zim.entity.ZIMFriendDeleteConfig;
import im.zego.zim.entity.ZIMFriendInfo;
import im.zego.zim.entity.ZIMFriendListQueryConfig;
import im.zego.zim.entity.ZIMFriendSearchConfig;
import im.zego.zim.entity.ZIMGroupMemberInfo;
import im.zego.zim.entity.ZIMGroupOperatedInfo;
import im.zego.zim.entity.ZIMGroupMemberMuteConfig;
import im.zego.zim.entity.ZIMUserFullInfo;
import im.zego.zim.entity.ZIMUserInfo;
import im.zego.zim.entity.ZIMUsersInfoQueryConfig;
import im.zego.zim.enums.ZIMConnectionEvent;
import im.zego.zim.enums.ZIMConnectionState;
import im.zego.zim.enums.ZIMConversationType;
import im.zego.zim.enums.ZIMErrorCode;
import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;
import io.dcloud.uniplugin.activity.AudioRoomActivity;
import io.dcloud.uniplugin.activity.ConversationActivity;
import io.dcloud.uniplugin.activity.LastRoomLeave;
import io.dcloud.uniplugin.activity.LiveActivity;
import io.dcloud.uniplugin.activity.NativePageActivity;
import io.dcloud.uniplugin.activity.MemberInfoActivity;
import io.dcloud.uniplugin.activity.RedPacketDetailActivity;
import io.dcloud.uniplugin.activity.RedPacketSendActivity;
import io.dcloud.uniplugin.friend.FriendEventBridge;
import io.dcloud.uniplugin.memberpicker.GroupMembersActivity;
import io.dcloud.uniplugin.memberpicker.Member;
import io.dcloud.uniplugin.memberpicker.MemberPickerActivity;
import io.dcloud.uniplugin.memberpicker.MemberPickerBottomSheet;
import io.dcloud.uniplugin.memberpicker.MemberPickerOptions;
//import io.dcloud.uniplugin.activity.TubeActivity;
//import io.dcloud.uniplugin.activity.reward.RewardedAds;
//import io.dcloud.uniplugin.activity.task.ContentTaskActivity;
import io.dcloud.uniplugin.others.OkHttpRequest;
import io.dcloud.uniplugin.others.RandomString;
import io.dcloud.uniplugin.others.SPUtil;
import io.dcloud.uniplugin.others.UniImageResponse;
import retrofit2.Call;
import retrofit2.Callback;
// Add these imports at the top of your file if they are missing
import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONObject;
//import com.zj.zjsdk.ZJConfig;
//import com.zj.zjsdk.ZjSdk;


public class TestModule extends UniModule {

    String TAG = "TestModule";
    public static int REQUEST_CODE = 1000;
    private ScheduledExecutorService syncScheduler;

    String user_id;
    String userName;
    String avatar;

    int logged = 0;

    private Context mainApplication;

    /**
     * 最近一次进入的群会话 ID（聊天页上下文）。
     * 改成 static：原生静态桥（如 openMemberChat / MemberInfoActivity 的「发消息」）也要重置它，
     * 原来是非静态字段，静态方法里碰不到。
     */
    static String groupId = "";

    private String productJson = "{"
            + "\"goodsId\": \"2099859778271653890\","
            + "\"skuId\": \"2099859778301014018\","
            + "\"productName\": \"赣南御龙鲜有机富硒红茶礼盒 200g*2/盒 400克\","
            + "\"productImage\": \"https://try.shanxunsw.com/uploads/_37ce4efaa8041b2afc852cc68b49f3f27e050b2eb74b5c6642.jpg?x-oss-process=style/400X400\","
            + "\"price\": 980,"
            + "\"pointsPrice\": \"\","
            + "\"merchantId\": \"2099498853458944002\","
            + "\"merchantName\": \"众慧优选\","
            + "\"merchantLogo\": \"\","
            + "\"sales\": 0,"
            + "\"stock\": 817,"
            + "\"remark\": \"高端礼盒赣南御龙鲜有机富硒红茶\""
            + "}";


    public void neteaseLogin(){
        System.out.println("netease-login info");
        System.out.println("netease-login info");
        createAccount(
                2,
                new Callback<ECModelResponse<NemoAccount>>() {
                    @Override
                    public void onResponse(
                            @NonNull Call<ECModelResponse<NemoAccount>> call,
                            @NonNull retrofit2.Response<ECModelResponse<NemoAccount>> response) {
                        if (response.body() != null) {
                            NemoAccount nemoAccount = response.body().data;
                            if (nemoAccount != null) {
                                login(nemoAccount);
                            } else {
                                ToastX.showShortToast("createAccountThenLogin failed,account is null");
                                ALog.e(TAG, "createAccountThenLogin failed,account is null");
                            }
                        }
                    }

                    @Override
                    public void onFailure(
                            @NonNull Call<ECModelResponse<NemoAccount>> call, @NonNull Throwable t) {
                        ToastX.showShortToast("createAccountThenLogin failed,t:" + t);
                        ALog.e(TAG, "createAccountThenLogin failed,exception:" + t);
                    }
                });
    }

    @UniJSMethod(uiThread = false)
    public static boolean isHuaweiOS() {
        String manufacturer = android.os.Build.MANUFACTURER;
        String brand = android.os.Build.BRAND;

        return (manufacturer != null && manufacturer.equalsIgnoreCase("huawei"))
                || (brand != null && brand.equalsIgnoreCase("huawei"));
    }

    public void neteaseLogout(){
        LoginUtil.logout();
    }

    private void createAccount(int sceneType, Callback<ECModelResponse<NemoAccount>> callback) {
        ECHttpService.getInstance().initialize(mUniSDKInstance.getContext(),BASE_URL);
        ECHttpService.getInstance().addHeader("Appkey", "c92ce58a027d659e41ba62da6819fb92");
        ECHttpService.getInstance().addHeader("AppSecret", "22759cffdce7");
        System.out.println("living here");
        ECHttpService.getInstance().createAccount(sceneType,user_id,userName,avatar, callback);

   }
    public void login(NemoAccount nemoAccount) {
        LoginUtil.loginVoiceRoom(
                mUniSDKInstance.getContext(),
                nemoAccount,
                new LoginUtil.LoginVoiceRoomCallback() {
                    @Override
                    public void onSuccess() {
                        logged = 1;
                      //  ToastX.showShortToast("登录成功");
                        System.out.println("living here - finally in");
                    }

                    @Override
                    public void onError(int errorCode, String errorMsg) {
                        ALog.e(TAG,  "认证失败，token错误 error log 1...");

                        // ToastX.showShortToast(errorMsg);
                        if (errorMsg.equals("认证失败，token错误")){
                            ALog.e(TAG,  "认证失败，token错误 error log 2...");
                        neteaseLogin();
                        ALog.e(TAG,  "5 seconds passed! Now executing login...");
                            //loginVoiceRoomInner(
                            ///     context,  nemoAccount,  callback);
                        }
                    }
                });
    }

    /**
     * AUDIO_MEETINGS
     * @param options
     * @param callback
     */

    @UniJSMethod(uiThread = true)
    public void makeCall(JSONObject options, UniJSCallback callback) {
        Context context = mUniSDKInstance.getContext();
        Intent intent = new Intent(context, VoiceRoomCreateActivity.class);
        intent.putExtra(RoomConstants.INTENT_IS_OVERSEA, AppConfig.isOversea());
        intent.putExtra(RoomConstants.INTENT_KEY_CONFIG_ID, AppConfig.getVoiceRoomConfigId());
        intent.putExtra(RoomConstants.INTENT_USER_NAME, AppUtils.getUserName());
        intent.putExtra(RoomConstants.INTENT_AVATAR, AppUtils.getAvatar());
        context.startActivity(intent);
//        AudioRoomActivity.setRoomLeaveListener(() -> {
//            System.out.println("HERE BRO, LEAVE ROOM TRIGGERED = ");
//            System.out.println(user_id + " = " + userName + " = "+avatar);
//            if(avatar == null){
//                avatar = "https://test.ioevisa.com/pics/profile.png";
//            }
//            JSONObject event = new JSONObject();
//            event.put("event", "ROOM_LEFT");
//
//            if (globalJsCallback != null) {
//                globalJsCallback.invokeAndKeepAlive(event);
//            }
//
//        });
//        boolean isHost = true;
//        Context context = mUniSDKInstance.getContext();
//        Intent intent = new Intent(context, AudioRoomActivity.class);
//        intent.putExtra("userID", options.getString("userId"));
//        intent.putExtra("userName", options.getString("userName"));
//        intent.putExtra("roomID", options.getString("meetingId"));
//        intent.putExtra("isHost", isHost);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(intent);

        if (callback != null) {
            callback.invoke("success");
        }
    }

    public void triggerLeft(){
        System.out.println("HERE BRO, LEAVE ROOM TRIGGERED = ");
        System.out.println(user_id + " = " + userName + " = "+avatar);
        if(avatar == null){
            avatar = "https://test.ioevisa.com/pics/profile.png";
        }
        JSONObject event = new JSONObject();
        event.put("event", "ROOM_LEFT");

        if (globalJsCallback != null) {
            globalJsCallback.invokeAndKeepAlive(event);
        }
    }

    @UniJSMethod(uiThread = true)
    public boolean isMicrophoneGranted() {
        System.out.println("here bro, we reached 2");
        Context context = mUniSDKInstance.getContext();
        if (context == null) return false;

        int result = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    @UniJSMethod(uiThread = true)
    public void needSettings(UniJSCallback callback) {
        System.out.println("here bro, we reached 3");
        Activity activity = (Activity) mUniSDKInstance.getContext();
        if (activity == null) {
            sendResponse(callback, true, "0");
            return ;
        }
        if (isMicrophoneGranted()) {
            sendResponse(callback, true, "0");
            return ;
        }

        boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO);

        // We use a local SharedPreference to track if we have prompted them at least once before
        boolean hasPromptedBefore = activity.getSharedPreferences("ZegoPerms", Context.MODE_PRIVATE)
                .getBoolean("mic_prompted", false);

        if (!showRationale && hasPromptedBefore) {
            sendResponse(callback, true, "1");
            return  ;

        }
        sendResponse(callback, true, "0");
    }



    // 2. Active Request Method
    @UniJSMethod(uiThread = true)
    public void requestMicrophone(UniJSCallback callback) {
        System.out.println("here bro, we reached 3");
        Activity activity = (Activity) mUniSDKInstance.getContext();
        if (activity == null) {
            sendResponse(callback, false, "Activity context is null");
            return;
        }

        // 1. If already granted, exit early with success
        if (isMicrophoneGranted()) {
            sendResponse(callback, true, "Already granted");
            return;
        }

        // 2. Check if the user has explicitly and permanently blocked it ("Don't Ask Again")
        // If they haven't granted it, AND the system says we should NOT show a rationale,
        // AND they have been prompted at least once before, it means they permanently denied it.
        boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO);

        // We use a local SharedPreference to track if we have prompted them at least once before
        boolean hasPromptedBefore = activity.getSharedPreferences("ZegoPerms", Context.MODE_PRIVATE)
                .getBoolean("mic_prompted", false);

        if (!showRationale && hasPromptedBefore) {
            // They checked "Don't ask again" -> Force open settings
            openAppSettings(activity);
            sendResponse(callback, false, "Permanently denied. Opening settings.");
            return;
        }

        // 3. First time or standard retry -> Safe to trigger the native OS system popup
        // Save the flag that we have prompted them now
        activity.getSharedPreferences("ZegoPerms", Context.MODE_PRIVATE)
                .edit().putBoolean("mic_prompted", true).apply();

        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE);
        sendResponse(callback, false, "Prompting user");
    }

    @UniJSMethod(uiThread = true)
    public void requestLight(UniJSCallback callback) {
        System.out.println("here bro, we reached");
        Activity activity = (Activity) mUniSDKInstance.getContext();
        if (activity == null) {
            sendResponse(callback, false, "Activity context is null");
            return;
        }

        // 1. If already granted, exit early with success
        if (isMicrophoneGranted()) {
            sendResponse(callback, true, "Already granted");
            return;
        }

        // 2. Check if the user has explicitly and permanently blocked it ("Don't Ask Again")
        // If they haven't granted it, AND the system says we should NOT show a rationale,
        // AND they have been prompted at least once before, it means they permanently denied it.
        boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO);

        // We use a local SharedPreference to track if we have prompted them at least once before
        boolean hasPromptedBefore = activity.getSharedPreferences("ZegoPerms", Context.MODE_PRIVATE)
                .getBoolean("mic_prompted", false);

        activity.getSharedPreferences("ZegoPerms", Context.MODE_PRIVATE)
                .edit().putBoolean("mic_prompted", true).apply();

        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE);
        sendResponse(callback, false, "Prompting user");

        sendResponse(callback, false, "Prompting user");
    }


    private void sendResponse(UniJSCallback callback, boolean success, String msg) {
        if (callback != null) {
            JSONObject result = new JSONObject();
            result.put("success", success);
            result.put("message", msg);
            callback.invoke(result);
        }
    }

    // Helper to open system settings
    private void openAppSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }

    @UniJSMethod(uiThread = true)
    public void startDuanju(String conversationID) {
        Intent intent = new Intent();
        intent.setClassName(mUniSDKInstance.getContext(), "com.cool.dianshang.data.TubeActivity");
        intent.putExtra("key", "value");
        mUniSDKInstance.getContext().startActivity(intent);
    }

    String randomId = "";

    @UniJSMethod(uiThread = true)
    public void startCountVideo(String accessToken,String url,UniJSCallback callback) {
        try {

//           String accessToken = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VyQ29udGV4dCI6IntcInVzZXJuYW1lXCI6XCIxMzI1MDgyMDg0NlwiLFwibmlja05hbWVcIjpcIjgyMDg0NlFZXCIsXCJmYWNlXCI6XCJodHRwczovL3Rlc3QuaW9ldmlzYS5jb20vcGljcy9wcm9maWxlLnBuZ1wiLFwiaWRcIjpcIjE5OTcxMTE5NjUzODU5NTgyODlcIixcImxvbmdUZXJtXCI6dHJ1ZSxcInJvbGVcIjpcIk1FTUJFUlwifSIsInN1YiI6IjEzMjUwODIwODQ2IiwiZXhwIjoxOTQzNTEyMDQyfQ.8aHzNHRocp5SOZXgOAZv2b5o9qmQe9ekn5szYjp0v3k";
//            String url = "https://buyer-ceshi.shanxunsw.com/buyer/hashrate/package/task/ad/watch";

            SPUtil.getInstance().setContext(mUniSDKInstance.getContext());
            randomId = RandomString.generate40();
            OkHttpRequest.url = url;
            OkHttpRequest.accessToken = accessToken;
            OkHttpRequest.id = randomId;
//            RewardedAds.setRoomLeaveListener((new LastRoomLeave() {
//                @Override
//                public void onRoomLeft() {
//
//                }
//
//                @Override
//                public void triggerAd(String msg) {
//                    if (callback != null) {
//                        JSONObject result = new JSONObject();
//                        result.put("msg", msg);
//                        result.put("randomId", randomId);
//                        System.out.println("sent back to top");
//                        callback.invokeAndKeepAlive(result);
//                    }
//                   // System.out.println("triggered this one oo");
//
//                }
//            }));
//            RewardedAds mk = new RewardedAds();
//            mk.setContext((Activity) mUniSDKInstance.getContext());
//            mk.loadAd(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // mUniSDKInstance.getContext().startActivity(new Intent(mUniSDKInstance.getContext(), ContentTaskActivity.class));
    }

    @UniJSMethod(uiThread = true)
    public void startVideo(String conversationID) {
//         mUniSDKInstance.getContext().startActivity(new Intent(mUniSDKInstance.getContext(), ContentTaskActivity.class));
    }


    @UniJSMethod(uiThread = true)
    public void startChat(String conversationID) {
        try {
            openPeerChat(conversationID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 与好友“群聊”：严格 2 人复用——存在【仅我+对方】的 2 人群则进入；否则新建“群聊”（bizType=temp） */
    @UniJSMethod(uiThread = true)
    public void startPrivateGroupChat(String peerZimId, String peerName, UniJSCallback callback) {
        try {
            final String peer = peerZimId == null ? "" : peerZimId;
            android.util.Log.d("PrivateGroup", "start peer=" + peer + " name=" + peerName);
            if (peer.isEmpty()) {
                invokeFail(callback, new Exception("peerZimId empty"));
                return;
            }
            final String self = ZIMKitCore.getInstance().getLocalUser().getId();
            if (self == null || self.isEmpty()) {
                invokeFail(callback, new Exception("self id empty"));
                return;
            }
            // 群名统一「聊天」：ZIM 群名是双方共享的，写成任一方昵称都会让对方看到错的名字
            final String displayName = PRIVATE_CHAT_NAME;
            zimInstance().queryGroupList((groups, error) -> {
                if (error != null && error.code != ZIMErrorCode.SUCCESS) {
                    android.util.Log.d("PrivateGroup", "queryGroupList err=" + error.message);
                }
                final List<String> gids = new ArrayList<>();
                if (groups != null) {
                    for (im.zego.zim.entity.ZIMGroup g : groups) {
                        if (g != null && g.baseInfo != null && g.baseInfo.groupID != null) {
                            gids.add(g.baseInfo.groupID);
                        }
                    }
                }
                findTwoPersonGroupAndOpen(gids, 0, peer, self, displayName, callback);
            });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /**
     * 「2 人复用创建群聊」的静态入口（供原生页调用，如成员资料页的「发消息」）。
     *
     * <p>与 uniapp 桥 {@link #startPrivateGroupChat} **共用同一套逻辑**（
     * {@link #findTwoPersonGroupAndOpen}）：优先复用【仅我+对方】的 2 人群，
     * 没有就新建（群名「聊天」、bizType=temp）。**不要求双方是好友**。
     */
    public static void startTwoPersonChatStatic(String peerZimId, UniJSCallback callback) {
        try {
            final String peer = peerZimId == null ? "" : peerZimId;
            if (peer.isEmpty()) {
                invokeFailStatic(callback, new Exception("peerZimId empty"));
                return;
            }
            final String self = ZIMKitCore.getInstance().getLocalUser().getId();
            if (self == null || self.isEmpty()) {
                invokeFailStatic(callback, new Exception("self id empty"));
                return;
            }
            zimInstance().queryGroupList((groups, error) -> {
                final List<String> gids = new ArrayList<>();
                if (groups != null) {
                    for (im.zego.zim.entity.ZIMGroup g : groups) {
                        if (g != null && g.baseInfo != null && g.baseInfo.groupID != null) {
                            gids.add(g.baseInfo.groupID);
                        }
                    }
                }
                findTwoPersonGroupAndOpenStatic(gids, 0, peer, self, PRIVATE_CHAT_NAME, callback);
            });
        } catch (Exception e) {
            invokeFailStatic(callback, e);
        }
    }

    private static void invokeFailStatic(UniJSCallback callback, Exception e) {
        if (callback == null) {
            return;
        }
        JSONObject result = new JSONObject();
        result.put("success", false);
        result.put("message", e == null ? "" : e.getMessage());
        callback.invoke(result);
    }

    /** {@link #findTwoPersonGroupAndOpen} 的静态版（逻辑一致，供原生页复用） */
    private static void findTwoPersonGroupAndOpenStatic(final List<String> gids, final int index,
        final String peer, final String self, final String displayName, final UniJSCallback callback) {
        if (index >= gids.size()) {
            String newGid = createTempGroupId();
            android.util.Log.d("PrivateGroup", "create new two-person group gid=" + newGid
                + " name=" + displayName);
            ZIMKit.createGroup(displayName, newGid, new ArrayList<String>() {{ add(peer); }},
                new CreateGroupCallback() {
                    @Override
                    public void onCreateGroup(ZIMKitGroupInfo groupInfo, ArrayList<ZIMErrorUserInfo> inviteUserErrors,
                        ZIMError error) {
                        if (error != null && error.code == ZIMErrorCode.SUCCESS) {
                            markGroupBizTypeStatic(newGid, "temp");
                            // 后端登记：SDK 建的群后端不知道，不登记则二维码/扫码/详情都不可用
                            registerTempGroupToBackend(newGid,
                                java.util.Collections.singletonList(peer));
                            openGroupChatAndReplyStatic(newGid, callback);
                        } else {
                            invokeFailStatic(callback, new Exception(error == null ? "创建失败" : error.message));
                        }
                    }
                });
            return;
        }
        final String gid = gids.get(index);
        ZIMGroupMemberQueryConfig config = new ZIMGroupMemberQueryConfig();
        config.count = 100;
        config.nextFlag = 0;
        zimInstance().queryGroupMemberList(gid, config, new ZIMGroupMemberListQueriedCallback() {
            @Override
            public void onGroupMemberListQueried(String groupId, ArrayList<ZIMGroupMemberInfo> memberList,
                int flag, ZIMError errorInfo) {
                if (errorInfo != null && errorInfo.code == ZIMErrorCode.SUCCESS && memberList != null
                    && memberList.size() == 2 && containsUserStatic(memberList, self)
                    && containsUserStatic(memberList, peer)) {
                    zimInstance().queryGroupAllAttributes(gid, (g, attrs, e) -> {
                        boolean isCommunity = attrs != null && "community".equals(attrs.get("bizType"));
                        if (isCommunity) {
                            findTwoPersonGroupAndOpenStatic(gids, index + 1, peer, self, displayName, callback);
                        } else {
                            android.util.Log.d("PrivateGroup", "reuse two-person group gid=" + gid);
                            markGroupBizTypeStatic(gid, "temp");
                            openGroupChatAndReplyStatic(gid, callback);
                        }
                    });
                } else {
                    findTwoPersonGroupAndOpenStatic(gids, index + 1, peer, self, displayName, callback);
                }
            }
        });
    }

    private static boolean containsUserStatic(List<ZIMGroupMemberInfo> list, String userId) {
        if (list == null || userId == null) {
            return false;
        }
        for (ZIMGroupMemberInfo m : list) {
            if (m != null && userId.equals(m.userID)) {
                return true;
            }
        }
        return false;
    }

    /** 生成临时群 ID 复用实例版 {@link #nextTempGroupId()}（规则一致，避免两套 ID 生成器） */
    private static String createTempGroupId() {
        return nextTempGroupId();
    }

    /** 标记群业务类型（静态版，与实例版 markGroupBizType 行为一致） */
    private static void markGroupBizTypeStatic(String gid, String bizType) {
        try {
            HashMap<String, String> attrs = new HashMap<>();
            attrs.put("bizType", bizType);
            zimInstance().setGroupAttributes(attrs, gid,
                new im.zego.zim.callback.ZIMGroupAttributesOperatedCallback() {
                    @Override
                    public void onGroupAttributesOperated(String groupID, ArrayList<String> keys,
                        ZIMError errorInfo) {
                        if (errorInfo != null && errorInfo.code != ZIMErrorCode.SUCCESS) {
                            android.util.Log.w("GroupBizType", "setGroupAttributes fail gid=" + groupID
                                + " msg=" + errorInfo.message);
                        }
                    }
                });
        } catch (Exception e) {
            android.util.Log.w("GroupBizType", "mark fail: " + e.getMessage());
        }
    }

    /** 进群聊页并回执（静态版，与 openGroupChatAndReply 行为一致） */
    private static void openGroupChatAndReplyStatic(String gid, UniJSCallback callback) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            try {
                TestModule.groupId = gid;
                // 优先当前 Activity（原生资料页），兜底缓存的上下文 —— 与 openMemberInfoInternal 同口径
                Context ctx = null;
                try {
                    ctx = com.zegocloud.zimkit.common.utils.ZIMKitActivityUtils.getCurrentActivity();
                } catch (Exception ignored) {
                }
                if (ctx == null) {
                    ctx = sAppContext;
                }
                if (ctx != null) {
                    ZIMKitRouter.toMessageActivity(ctx, gid,
                        com.zegocloud.zimkit.common.enums.ZIMKitConversationType.ZIMKitConversationTypeGroup);
                } else {
                    android.util.Log.w("PrivateGroup", "open chat skipped: no context");
                }
            } catch (Exception e) {
                android.util.Log.w("PrivateGroup", "open chat fail: " + e.getMessage());
            }
        });
        if (callback != null) {
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("groupId", gid);
            callback.invoke(result);
        }
    }

    private void findTwoPersonGroupAndOpen(final List<String> gids, final int index,
        final String peer, final String self, final String displayName, final UniJSCallback callback) {
        if (index >= gids.size()) {
            // 没有“仅我+对方”的群 → 新建 2 人聊天群（群名统一「聊天」）
            // 群 ID 必须全局唯一：多选分享时会连续建群，同毫秒下随机数有概率撞 ID
            // （撞了第二次 createGroup 报“群已存在”，卡片就发不出去）
            String newGid = nextTempGroupId();
            android.util.Log.d("PrivateGroup", "create new two-person group gid=" + newGid
                + " name=" + displayName);
            ZIMKit.createGroup(displayName, newGid, new ArrayList<String>() {{ add(peer); }},
                new CreateGroupCallback() {
                    @Override
                    public void onCreateGroup(ZIMKitGroupInfo groupInfo, ArrayList<ZIMErrorUserInfo> inviteUserErrors,
                        ZIMError error) {
                        if (error != null && error.code == ZIMErrorCode.SUCCESS) {
                            android.util.Log.d("PrivateGroup", "createGroup ok gid=" + newGid);
                            markGroupBizType(newGid, "temp");
                            // 后端登记（2 人群聊也是临时群，必须登记才能扫码/详情/画码）
                            registerTempGroupToBackend(newGid,
                                java.util.Collections.singletonList(peer));
                            openGroupChatAndReply(newGid, callback);
                        } else {
                            android.util.Log.d("PrivateGroup", "create fail code="
                                + (error == null ? "" : error.code) + " msg=" + (error == null ? "" : error.message));
                            JSONObject result = new JSONObject();
                            result.put("success", false);
                            result.put("message", error == null ? "创建失败" : error.message);
                            if (callback != null) {
                                callback.invoke(result);
                            }
                        }
                    }
                });
            return;
        }
        final String gid = gids.get(index);
        ZIMGroupMemberQueryConfig config = new ZIMGroupMemberQueryConfig();
        config.count = 100;
        config.nextFlag = 0;
        zimInstance().queryGroupMemberList(gid, config, new ZIMGroupMemberListQueriedCallback() {
            @Override
            public void onGroupMemberListQueried(String groupId, ArrayList<ZIMGroupMemberInfo> memberList,
                int flag, ZIMError errorInfo) {
                if (errorInfo != null && errorInfo.code == ZIMErrorCode.SUCCESS && memberList != null
                    && memberList.size() == 2 && containsUser(memberList, self) && containsUser(memberList, peer)) {
                    // 2人群候选：确认不是社群频道后进入（旧名“群聊”→补成对方名，保留自定义改名）
                    zimInstance().queryGroupAllAttributes(gid, (g, attrs, e) -> {
                        boolean isCommunity = attrs != null && "community".equals(attrs.get("bizType"));
                        if (isCommunity) {
                            findTwoPersonGroupAndOpen(gids, index + 1, peer, self, displayName, callback);
                        } else {
                            android.util.Log.d("PrivateGroup", "reuse two-person group gid=" + gid);
                            markGroupBizType(gid, "temp");
                            openGroupChatAndReply(gid, callback);
                        }
                    });
                } else {
                    findTwoPersonGroupAndOpen(gids, index + 1, peer, self, displayName, callback);
                }
            }
        });
    }

    private boolean containsUser(ArrayList<ZIMGroupMemberInfo> list, String userId) {
        if (list == null || userId == null) {
            return false;
        }
        for (ZIMGroupMemberInfo info : list) {
            if (info != null && userId.equals(info.userID)) {
                return true;
            }
        }
        return false;
    }

    /** 创建/复用私聊群；遇到「群已销毁不可重建(108060)」自动换下一个确定性 id（双方一致），最多 3 次 */
    private void tryCreatePrivateGroup(final String gid, final String peer, final String name, final int attempt,
        final UniJSCallback callback) {
        if (attempt > 3) {
            JSONObject result = new JSONObject();
            result.put("success", false);
            result.put("message", "创建群聊失败（多次重试）");
            if (callback != null) {
                callback.invoke(result);
            }
            return;
        }
        android.util.Log.d("PrivateGroup", "tryCreate attempt=" + attempt + " gid=" + gid);
        ZIMKit.createGroup(name, gid, new ArrayList<String>() {{ add(peer); }},
            new CreateGroupCallback() {
                @Override
                public void onCreateGroup(ZIMKitGroupInfo groupInfo, ArrayList<ZIMErrorUserInfo> inviteUserErrors,
                    ZIMError error) {
                    if (error != null && error.code == ZIMErrorCode.SUCCESS) {
                        android.util.Log.d("PrivateGroup", "createGroup ok gid=" + gid);
                        markGroupBizType(gid, "private");
                        // 后端登记
                        registerTempGroupToBackend(gid,
                            java.util.Collections.singletonList(peer));
                        openGroupChatAndReply(gid, callback);
                    } else if (error != null && error.code == ZIMErrorCode.GROUP_ALREADY_EXISTS) {
                        android.util.Log.d("PrivateGroup", "already exists, join gid=" + gid);
                        ZIMKit.joinGroup(gid, new JoinGroupCallback() {
                            @Override
                            public void onJoinGroup(ZIMKitGroupInfo g, ZIMError e) {
                                if (e == null || e.code == ZIMErrorCode.SUCCESS
                                    || e.code == ZIMErrorCode.MEMBER_IS_ALREADY_IN_THE_GROUP) {
                                    android.util.Log.d("PrivateGroup", "join ok gid=" + gid);
                                    markGroupBizType(gid, "private");
                                    // 复用已存在的群同样要登记（幂等，已存在则补成员与资料）
                                    registerTempGroupToBackend(gid,
                                        java.util.Collections.singletonList(peer));
                                    openGroupChatAndReply(gid, callback);
                                } else {
                                    android.util.Log.d("PrivateGroup", "join fail gid=" + gid
                                        + " code=" + (e == null ? "" : e.code) + " -> retry new id");
                                    tryCreatePrivateGroup("pg_" + md5Hex(gid).substring(0, 24), peer, name,
                                        attempt + 1, callback);
                                }
                            }
                        });
                    } else {
                        android.util.Log.d("PrivateGroup", "create fail gid=" + gid + " code="
                            + (error == null ? "" : error.code) + " msg=" + (error == null ? "" : error.message));
                        // 108060：原群已销毁 → 换 id 链重建（双方可算出同一 id）
                        tryCreatePrivateGroup("pg_" + md5Hex(gid).substring(0, 24), peer, name, attempt + 1, callback);
                    }
                }
            });
    }

    private void markGroupBizType(String groupId, String bizType) {
        try {
            HashMap<String, String> attrs = new HashMap<>();
            attrs.put("bizType", bizType);
            ZIMKitCore.getInstance().zim().setGroupAttributes(attrs, groupId,
                new ZIMGroupAttributesOperatedCallback() {
                    @Override
                    public void onGroupAttributesOperated(String g, ArrayList<String> keys, ZIMError errorInfo) {
                    }
                });
        } catch (Exception ignored) {
        }
    }

    private void openGroupChatAndReply(String groupId, UniJSCallback callback) {
        ZIMKitMessageFragment.setOnNativeDataListener(new BackToUniappCallback() {
            @Override
            public void onDataReceived(String data) {
            }

            @Override
            public void onStartCall(String data) {
            }

            @Override
            public void onCardAction(String action, String data) {
                dispatchCardEvent(action, data);
            }
        });
        ZIMKitRouter.toMessageActivity(mUniSDKInstance.getContext(), groupId,
            ZIMKitConversationType.ZIMKitConversationTypeGroup);
        JSONObject result = new JSONObject();
        result.put("success", true);
        result.put("groupId", groupId);
        if (callback != null) {
            callback.invoke(result);
        }
    }

    /** 群聊解散（普通群聊/私聊群；仅群主可操作） */
    @UniJSMethod(uiThread = true)
    public void dismissGroupChat(String groupId, UniJSCallback callback) {
        try {
            dismissGroupChatWithReply(groupId, callback);
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /** 仅退出群聊/社群（不解散、不转让） */
    @UniJSMethod(uiThread = true)
    public void leaveGroupOnly(String groupId, UniJSCallback callback) {
        try {
            ZIMKit.leaveGroup(groupId, new LeaveGroupCallback() {
                @Override
                public void onLeaveGroup(ZIMError error) {
                    JSONObject result = new JSONObject();
                    if (error != null && error.code == ZIMErrorCode.SUCCESS) {
                        result.put("success", true);
                        result.put("message", "已退出");
                        // 退出成功：从本地会话列表移除该群
                        deleteConversationQuiet(groupId, ZIMConversationType.GROUP);
                    } else if (isGroupGone(error)) {
                        result.put("success", true);
                        result.put("alreadyGone", true);
                        result.put("message", "群聊已不存在，本地会话已清理");
                        clearLocalConversation(groupId);
                    } else {
                        result.put("success", false);
                        result.put("message", error == null ? "" : error.message);
                    }
                    if (callback != null) {
                        callback.invoke(result);
                    }
                }
            });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /** 社群频道：不可解散；群主退出→转让（管理员→最早加入→随机）并自动退群；仅剩自己才解散 */
    @UniJSMethod(uiThread = true)
    public void dissolveOrTransferCommunity(String groupId, UniJSCallback callback) {
        try {
            zimInstance().queryGroupAllAttributes(groupId, (gid, attributes, errorInfo) -> {
                if (errorInfo == null || errorInfo.code != ZIMErrorCode.SUCCESS || attributes == null) {
                    dismissGroupChatWithReply(groupId, callback);
                    return;
                }
                if (!"community".equals(attributes.get("bizType"))) {
                    // 普通群聊/私聊群 → 解散
                    dismissGroupChatWithReply(groupId, callback);
                    return;
                }
                // 社群：拉全量成员 → 找接班人
                fetchAllGroupMembers(groupId, new ArrayList<ZIMGroupMemberInfo>(), 0,
                    members -> transferToSuccessor(groupId, members, callback));
            });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    private void dismissGroupChatWithReply(String groupId, UniJSCallback callback) {
        zimInstance().dismissGroup(groupId, new ZIMGroupDismissedCallback() {
            @Override
            public void onGroupDismissed(String gid, ZIMError error) {
                JSONObject result = new JSONObject();
                if (error != null && error.code == ZIMErrorCode.SUCCESS) {
                    result.put("success", true);
                    result.put("message", "已解散");
                    deleteConversationQuiet(gid, ZIMConversationType.GROUP);
                } else if (isGroupGone(error)) {
                    result.put("success", true);
                    result.put("alreadyGone", true);
                    result.put("message", "群聊已不存在，本地会话已清理");
                    clearLocalConversation(groupId);
                } else {
                    result.put("success", false);
                    result.put("message", error == null ? "" : error.message);
                }
                if (callback != null) {
                    callback.invoke(result);
                }
            }
        });
    }

    /** 群已解散/不存在/不在其中 → 视为“已不存在”，清本地会话，不报错 */
    private boolean isGroupGone(ZIMError error) {
        if (error == null || error.code == ZIMErrorCode.SUCCESS) {
            return false;
        }
        return error.code == ZIMErrorCode.GROUP_WITH_DISMISSED
            || error.code == ZIMErrorCode.DOES_NOT_EXIST
            || error.code == ZIMErrorCode.TARGET_DOES_NOT_EXIST
            || error.code == ZIMErrorCode.USER_IS_NOT_IN_THE_GROUP
            || error.code == ZIMErrorCode.CONVERSATION_DOES_NOT_EXIST;
    }

    private void clearLocalConversation(String conversationId) {
        try {
            zimInstance().deleteConversation(conversationId, ZIMConversationType.GROUP,
                new ZIMConversationDeleteConfig(), new ZIMConversationDeletedCallback() {
                    @Override
                    public void onConversationDeleted(String id, ZIMConversationType type, ZIMError errorInfo) {
                    }
                });
        } catch (Exception ignored) {
        }
    }

    private void fetchAllGroupMembers(String groupId, ArrayList<ZIMGroupMemberInfo> all, int nextFlag,
        java.util.function.Consumer<ArrayList<ZIMGroupMemberInfo>> done) {
        ZIMGroupMemberQueryConfig config = new ZIMGroupMemberQueryConfig();
        config.count = 100;
        config.nextFlag = nextFlag;
        zimInstance().queryGroupMemberList(groupId, config, new ZIMGroupMemberListQueriedCallback() {
            @Override
            public void onGroupMemberListQueried(String gid, ArrayList<ZIMGroupMemberInfo> memberList,
                int nextFlag2, ZIMError errorInfo) {
                if (errorInfo != null || memberList == null) {
                    done.accept(all);
                    return;
                }
                all.addAll(memberList);
                if (nextFlag2 != 0 && all.size() < 500) {
                    fetchAllGroupMembers(groupId, all, nextFlag2, done);
                } else {
                    done.accept(all);
                }
            }
        });
    }

    private void transferToSuccessor(String groupId, ArrayList<ZIMGroupMemberInfo> members,
        UniJSCallback callback) {
        String self = ZIMKitCore.getInstance().getLocalUser().getId();
        ZIMGroupMemberInfo bestAdmin = null;
        ZIMGroupMemberInfo bestMember = null;
        long bestAdminTime = Long.MAX_VALUE;
        long bestMemberTime = Long.MAX_VALUE;
        if (members != null) {
            for (ZIMGroupMemberInfo info : members) {
                if (info == null || info.userID == null || info.userID.equals(self)) {
                    continue;
                }
                long t = info.groupEnterInfo == null ? Long.MAX_VALUE : info.groupEnterInfo.enterTime;
                if (info.memberRole == 2) {
                    if (t < bestAdminTime) {
                        bestAdminTime = t;
                        bestAdmin = info;
                    }
                } else if (t < bestMemberTime) {
                    bestMemberTime = t;
                    bestMember = info;
                }
            }
        }
        final String successor = bestAdmin != null ? bestAdmin.userID
            : (bestMember != null ? bestMember.userID : null);
        android.util.Log.i("ExitGroup", "transferToSuccessor gid=" + groupId
            + " successor=" + successor
            + " bestAdmin=" + (bestAdmin == null ? "null" : bestAdmin.userID)
            + " bestMember=" + (bestMember == null ? "null" : bestMember.userID)
            + " members=" + (members == null ? -1 : members.size()));
        if (successor == null) {
            android.util.Log.w("ExitGroup", "successor=null → 群内无人可转让，走解散 gid=" + groupId);
            // 只剩自己：唯一出口是解散
            dismissGroupChatWithReply(groupId, callback);
            return;
        }
        zimInstance().transferGroupOwner(groupId, successor, new ZIMGroupOwnerTransferredCallback() {
            @Override
            public void onGroupOwnerTransferred(String gid, String newOwner, ZIMError error) {
                android.util.Log.i("ExitGroup", "transferGroupOwner gid=" + gid + " newOwner=" + newOwner
                    + " code=" + (error == null ? "null" : error.code)
                    + " msg=" + (error == null ? "" : error.message));
                if (error != null && error.code == ZIMErrorCode.SUCCESS) {
                    ZIMKit.leaveGroup(groupId, new LeaveGroupCallback() {
                        @Override
                        public void onLeaveGroup(ZIMError e2) {
                            android.util.Log.i("ExitGroup", "leaveGroup(after transfer) code="
                                + (e2 == null ? "null" : e2.code));
                            JSONObject result = new JSONObject();
                            result.put("success", true);
                            result.put("transferred", true);
                            result.put("newOwner", newOwner);
                            result.put("message", "已转让并退出");
                            if (callback != null) {
                                callback.invoke(result);
                            }
                        }
                    });
                } else {
                    // ★ 转让失败绝不能直接 leaveGroup：群主直接退，ZIM 会**解散整个群**。
                    // 2 人群尤其致命（一退群就没了），所以这里中止退出、保持群不变。
                    android.util.Log.w("ExitGroup", "transfer FAILED → 中止退出，群保持不变 gid=" + groupId);
                    JSONObject result = new JSONObject();
                    result.put("success", false);
                    result.put("message", error == null
                        ? "转让群主失败，已取消退出" : ("转让群主失败：" + error.message));
                    if (callback != null) {
                        callback.invoke(result);
                    }
                }
            }
        });
    }

    /** 私聊直连（客服 merchant_/customer_ 等 PEER 会话；普通用户私聊走 startPrivateGroupChat） */
    @UniJSMethod(uiThread = true)
    public void startChatChecked(String conversationID, UniJSCallback callback) {
        try {
            openPeerChat(conversationID);
            JSONObject ok = new JSONObject();
            ok.put("success", true);
            if (callback != null) {
                callback.invoke(ok);
            }
        } catch (Exception e) {
            if (callback != null) {
                invokeFail(callback, e);
            }
        }
    }

    private  Context context;
    private void setContext(Context contex){
        context = contex;
    }

    public void openPeerChat(String conversationID) {
        groupId = "";
        ZIMKitMessageFragment.setOnNativeDataListener(new BackToUniappCallback() {
            @Override
            public void onDataReceived(String data) {
            }

            @Override
            public void onStartCall(String data) {
            }

            @Override
            public void onCardAction(String action, String data) {
                dispatchCardEvent(action, data);
            }
        });
        ZIMKitRouter.toMessageActivity(
            mUniSDKInstance.getContext(),
            conversationID,
            ZIMKitConversationType.ZIMKitConversationTypePeer,productJson
        );
    }

    @UniJSMethod(uiThread = true)
    public void openCustomerChat(String conversationID,String product) {
        groupId = "";
        ZIMKitMessageFragment.setOnNativeDataListener(new BackToUniappCallback() {
            @Override
            public void onDataReceived(String data) {
            }

            @Override
            public void onStartCall(String data) {
            }

            @Override
            public void onCardAction(String action, String data) {
                dispatchCardEvent(action, data);
            }
        });
        ZIMKitRouter.toMessageActivity(
                mUniSDKInstance.getContext(),
                conversationID,
                ZIMKitConversationType.ZIMKitConversationTypePeer,product
        );
    }
    public  RoomModel neVoiceRoomInfo2RoomInfo(NEVoiceRoomInfo voiceRoomInfo) {
        if (voiceRoomInfo == null) {
            return null;
        }
        RoomModel roomModel = new RoomModel();
        roomModel.setRoomUuid(voiceRoomInfo.getLiveModel().getRoomUuid());
        Integer audienceCount = voiceRoomInfo.getLiveModel().getAudienceCount();
        roomModel.setAudienceCount((audienceCount == null ? 0 : audienceCount) + 1);
        roomModel.setCover(voiceRoomInfo.getLiveModel().getCover());
        roomModel.setLiveRecordId(voiceRoomInfo.getLiveModel().getLiveRecordId());
        roomModel.setRoomName(voiceRoomInfo.getLiveModel().getLiveTopic());
        roomModel.setAnchorAvatar(voiceRoomInfo.getAnchor().getAvatar());
        roomModel.setAnchorNick(voiceRoomInfo.getAnchor().getNick());
        roomModel.setAnchorUserUuid(voiceRoomInfo.getAnchor().getAccount());
        roomModel.setGameName(voiceRoomInfo.getLiveModel().getGameName());
        return roomModel;
    }


    @UniJSMethod(uiThread = true)
    public void startGroupChat(String conversationID) {

        ZIMKitMessageActivity.setOnNativeDataListener(new BackToUniappCallback() {
            @Override
            public void onDataReceived(String data) {
                System.out.println("reached level 1");

                // Pass the raw data string directly
                JSONObject jsonObject = JSON.parseObject(data);

                System.out.println( data);
                System.out.println("JSON Object: " + jsonObject.toString());
                System.out.println("reached level 2");

                // 3. Extract just the "data" object string block
                String dataJson = jsonObject.getJSONObject("data").toString();
                // 4. Decode it directly into your NEVoiceRoomInfo model
                Gson gson = new Gson();
               NEVoiceRoomInfo voiceRoomInfo = gson.fromJson(dataJson, NEVoiceRoomInfo.class);
               System.out.println(voiceRoomInfo);
               System.out.println(voiceRoomInfo.getAnchor());
               System.out.println("reached level 3");
               RoomModel info = neVoiceRoomInfo2RoomInfo(voiceRoomInfo);
                System.out.println("reached level 4");
                System.out.println(info);
                LoginUtil.join( mUniSDKInstance.getContext(),info,avatar,userName);
            }

            @Override
            public void onStartCall(String data){

            }

            @Override
            public void onCardAction(String action, String data) {
                dispatchCardEvent(action, data);
            }
        });

        ZIMKitMessageFragment.setOnNativeDataListener(new BackToUniappCallback() {
            @Override
            public void onDataReceived(String data) {

            }

            @Override
            public void onStartCall(String data){
                Intent intent = new Intent(mUniSDKInstance.getContext(), VoiceRoomCreateActivity.class);
                intent.putExtra(RoomConstants.INTENT_IS_OVERSEA, AppConfig.isOversea());
                intent.putExtra("groupId", conversationID);
                intent.putExtra(RoomConstants.INTENT_KEY_CONFIG_ID, AppConfig.getVoiceRoomConfigId());
                intent.putExtra(RoomConstants.INTENT_USER_NAME, AppUtils.getUserName());
                intent.putExtra(RoomConstants.INTENT_AVATAR, AppUtils.getAvatar());
                mUniSDKInstance.getContext().startActivity(intent);
            }

            @Override
            public void onCardAction(String action, String data) {
                dispatchCardEvent(action, data);
            }
        });
        ZIMKitRouter.toMessageActivity(mUniSDKInstance.getContext(), conversationID, ZIMKitConversationType.ZIMKitConversationTypeGroup);
    }
    private static final int REQUEST_CODE_CHOOSE_IMAGE = 4221;
    private UniJSCallback jsCallback;

    @UniJSMethod(uiThread = true)
    public void chooseImage(UniJSCallback callback) {
        System.out.println("group chat reached here");
        this.jsCallback = callback;

        Activity activity = (Activity) mWXSDKInstance.getContext();

        // 2. Open standard Android Gallery picker without breaking context
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        activity.startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CODE_CHOOSE_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CHOOSE_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null && jsCallback != null) {

                // Convert the native Android URI into the exact uni-app temp file layout string
                String formattedUniAppPath = getUniAppFormattedPath(selectedImageUri);
                System.out.println(formattedUniAppPath);
                System.out.println("specialized url");
                if (formattedUniAppPath != null) {
                    // Structure the exact JSON response shape expected by uni-app
                    JSONObject response = new JSONObject();
                    List<String> tempFilePaths = new ArrayList<>();
                    tempFilePaths.add(formattedUniAppPath);
                    response.put("tempFilePaths", tempFilePaths);

                    // Send the structural payload back to the Vue code
                    jsCallback.invoke(response);
                }
            }
        }
    }

    private String getUniAppFormattedPath(Uri uri) {
        try {
            Context context = mWXSDKInstance.getContext();
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            // Target directory mimicking the log: sandbox/doc/uniapp_temp/
            File uniAppDocDir = new File(context.getExternalFilesDir(null), "apps/__UNI__F189B8A/doc/uniapp_temp");
            if (!uniAppDocDir.exists()) {
                uniAppDocDir.mkdirs();
            }

            // Replicate the randomized file name footprint
            String fileName = System.currentTimeMillis() + "_NATIVE_PICKED.jpg";
            File destinationFile = new File(uniAppDocDir, fileName);

            FileOutputStream outputStream = new FileOutputStream(destinationFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.close();
            inputStream.close();

            // Format exactly like uni-app logs: file:///storage/emulated/0/...
            return "file://" + destinationFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e("UniAppPlugin", "Failed copying to uni-app environment layout", e);
            return null;
        }
    }
    @UniJSMethod(uiThread = true)
    public void joinGroupChat(String conversationID) {

        ZIMKit.joinGroup(conversationID, new JoinGroupCallback() {
            @Override
            public void onJoinGroup(ZIMKitGroupInfo groupInfo, ZIMError error) {
                if (error.code == ZIMErrorCode.SUCCESS || error.code == ZIMErrorCode.MEMBER_IS_ALREADY_IN_THE_GROUP) {
                    groupId = conversationID;
                    ZIMKitRouter.toMessageActivity(mUniSDKInstance.getContext(), conversationID, ZIMKitConversationType.ZIMKitConversationTypeGroup);
                }
            }
        });
    }

    @UniJSMethod(uiThread = true)
    public void createGroup(String groupName, String groupID, List<String> userIDs, String avatarUrl) {
        ZIMKit.createGroup(groupName, groupID, userIDs, new CreateGroupCallback() {
            @Override
            public void onCreateGroup(ZIMKitGroupInfo groupInfo, ArrayList<ZIMErrorUserInfo> inviteUserErrors, ZIMError error) {
                if (error.code == ZIMErrorCode.SUCCESS) {

                    // 🚀 STEP 1: Update the Group Avatar immediately using the core ZIM Engine
                    im.zego.zim.ZIM.getInstance().updateGroupAvatarUrl(avatarUrl, groupInfo.getId(), new im.zego.zim.callback.ZIMGroupAvatarUrlUpdatedCallback() {
                        @Override
                        public void onGroupAvatarUrlUpdated(String groupID, String groupAvatarUrl, ZIMError errorInfo) {
                            // Optional: You can check if the avatar upload succeeded or failed here
                        }
                    });

                    // 🚀 STEP 1.5: 后端登记（勾选联系人建群走这条）：
                    // SDK 建的群后端不知道 → 不登记则二维码/扫码/群详情全失败。
                    // 成员 = 被邀请的人 + 自己（registerTempGroupToBackend 内部会补自己）
                    markGroupBizTypeStatic(groupID, "temp");
                    registerTempGroupToBackend(groupID, userIDs);

                    // 🏁 STEP 2: Smoothly forward the user straight into the target chat interface screen
                    ZIMKitRouter.toMessageActivity(mUniSDKInstance.getContext(), groupID, ZIMKitConversationType.ZIMKitConversationTypeGroup);

                }
            }
        });
    }

    /** 分享卡片到指定会话（通用：community_invite/product/store/article） */
    @UniJSMethod(uiThread = true)
    public void sendShareCard(String conversationId, String conversationType, int subType,
        String payloadJson, UniJSCallback callback) {
        try {
            ZIMConversationType type = "peer".equals(conversationType)
                ? ZIMConversationType.PEER : ZIMConversationType.GROUP;
            ZIMKit.sendCustomMessage(payloadJson == null ? "" : payloadJson, subType,
                conversationId, type, new MessageSentCallback() {
                    @Override
                    public void onMessageSent(ZIMError errorInfo) {
                        JSONObject result = new JSONObject();
                        result.put("success", errorInfo != null && errorInfo.code == ZIMErrorCode.SUCCESS);
                        result.put("message", errorInfo == null ? "" : errorInfo.message);
                        if (callback != null) {
                            callback.invoke(result);
                        }
                    }
                });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /** 邀请加入社群：2人群聊（严格复用）内自动发送社群邀请卡片 */
    @UniJSMethod(uiThread = true)
    public void inviteCommunityMember(String peerZimId, String cardJson, UniJSCallback callback) {
        try {
            final String peer = peerZimId == null ? "" : peerZimId;
            final String card = cardJson == null ? "" : cardJson;
            final String self = ZIMKitCore.getInstance().getLocalUser().getId();
            if (peer.isEmpty() || self == null || self.isEmpty()) {
                invokeFail(callback, new Exception("peer/self empty"));
                return;
            }
            zimInstance().queryGroupList((groups, error) -> {
                final List<String> gids = new ArrayList<>();
                if (groups != null) {
                    for (im.zego.zim.entity.ZIMGroup g : groups) {
                        if (g != null && g.baseInfo != null && g.baseInfo.groupID != null) {
                            gids.add(g.baseInfo.groupID);
                        }
                    }
                }
                findTwoPersonGroupForInvite(gids, 0, peer, self, card,
                    com.zegocloud.zimkit.services.model.ZIMKitMessageSubType.COMMUNITY_INVITE, callback);
            });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /** 分享卡片到指定好友：2人群聊（严格复用）内发送（通用：product/store/article/community_invite） */
    @UniJSMethod(uiThread = true)
    public void sendToMemberCard(String peerZimId, int subType, String cardJson, UniJSCallback callback) {
        try {
            final String peer = peerZimId == null ? "" : peerZimId;
            final String card = cardJson == null ? "" : cardJson;
            final String self = ZIMKitCore.getInstance().getLocalUser().getId();
            if (peer.isEmpty() || self == null || self.isEmpty()) {
                invokeFail(callback, new Exception("peer/self empty"));
                return;
            }
            zimInstance().queryGroupList((groups, error) -> {
                final List<String> gids = new ArrayList<>();
                if (groups != null) {
                    for (im.zego.zim.entity.ZIMGroup g : groups) {
                        if (g != null && g.baseInfo != null && g.baseInfo.groupID != null) {
                            gids.add(g.baseInfo.groupID);
                        }
                    }
                }
                findTwoPersonGroupForInvite(gids, 0, peer, self, card, subType, callback);
            });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /**
     * 临时群 ID 生成器：时间戳 + 进程内自增序号。
     * 用自增序号替代随机数，保证「同一毫秒内连续创建多个群」也绝不撞 ID。
     */
    private static final java.util.concurrent.atomic.AtomicLong TEMP_GROUP_SEQ =
        new java.util.concurrent.atomic.AtomicLong(0);

    private static String nextTempGroupId() {
        return "g_" + System.currentTimeMillis() + "_" + TEMP_GROUP_SEQ.incrementAndGet();
    }

    private void findTwoPersonGroupForInvite(final List<String> gids, final int index,
        final String peer, final String self, final String cardJson, final int subType,
        final UniJSCallback callback) {
        if (index >= gids.size()) {
            // 无“仅我+对方”2人群 → 新建（群名=群聊，bizType=temp）
            // ⚠️ 群 ID 必须全局唯一：多选分享时会给多个联系人连续建群，
            // 原来 "g_"+currentTimeMillis()+"_"+(int)(Math.random()*99999) 在同一毫秒内有概率撞 ID，
            // 撞了第二次 createGroup 就报“群已存在” → 那张卡片发不出去（且失败原因被 toast 吞掉）。
            String newGid = nextTempGroupId();
            ZIMKit.createGroup(PRIVATE_CHAT_NAME, newGid, new ArrayList<String>() {{ add(peer); }},
                new CreateGroupCallback() {
                    @Override
                    public void onCreateGroup(ZIMKitGroupInfo groupInfo, ArrayList<ZIMErrorUserInfo> inviteUserErrors,
                        ZIMError error) {
                        if (error != null && error.code == ZIMErrorCode.SUCCESS) {
                            markGroupBizType(newGid, "temp");
                            sendInviteCard(newGid, cardJson, subType, callback);
                        } else {
                            JSONObject result = new JSONObject();
                            result.put("success", false);
                            result.put("message", error == null ? "创建失败" : error.message);
                            if (callback != null) {
                                callback.invoke(result);
                            }
                        }
                    }
                });
            return;
        }
        final String gid = gids.get(index);
        ZIMGroupMemberQueryConfig config = new ZIMGroupMemberQueryConfig();
        config.count = 100;
        config.nextFlag = 0;
        zimInstance().queryGroupMemberList(gid, config, new ZIMGroupMemberListQueriedCallback() {
            @Override
            public void onGroupMemberListQueried(String groupId, ArrayList<ZIMGroupMemberInfo> memberList,
                int flag, ZIMError errorInfo) {
                if (errorInfo != null && errorInfo.code == ZIMErrorCode.SUCCESS && memberList != null
                    && memberList.size() == 2 && containsUser(memberList, self) && containsUser(memberList, peer)) {
                    zimInstance().queryGroupAllAttributes(gid, (g, attrs, e) -> {
                        boolean isCommunity = attrs != null && "community".equals(attrs.get("bizType"));
                        if (isCommunity) {
                            findTwoPersonGroupForInvite(gids, index + 1, peer, self, cardJson, subType, callback);
                        } else {
                            markGroupBizType(gid, "temp");
                            sendInviteCard(gid, cardJson, subType, callback);
                        }
                    });
                } else {
                    findTwoPersonGroupForInvite(gids, index + 1, peer, self, cardJson, subType, callback);
                }
            }
        });
    }

    private void sendInviteCard(String gid, String cardJson, int subType, UniJSCallback callback) {
        ZIMKit.sendCustomMessage(cardJson, subType,
            gid, ZIMConversationType.GROUP, new MessageSentCallback() {
                @Override
                public void onMessageSent(ZIMError errorInfo) {
                    JSONObject result = new JSONObject();
                    result.put("success", errorInfo != null && errorInfo.code == ZIMErrorCode.SUCCESS);
                    result.put("message", errorInfo == null ? "" : errorInfo.message);
                    if (callback != null) {
                        callback.invoke(result);
                    }
                }
            });
    }

    /** 给群打业务类型标记（ZIM 群属性，存 ZIM 服务器：卸载/换机不丢）bizType=community|temp */
    @UniJSMethod(uiThread = true)
    public void setGroupType(String groupId, String bizType, UniJSCallback callback) {
        try {
            HashMap<String, String> attrs = new HashMap<>();
            attrs.put("bizType", bizType == null ? "" : bizType);
            zimInstance().setGroupAttributes(attrs, groupId, new ZIMGroupAttributesOperatedCallback() {
                @Override
                public void onGroupAttributesOperated(String groupID, ArrayList<String> keys, ZIMError errorInfo) {
                    JSONObject result = new JSONObject();
                    boolean ok = errorInfo != null && errorInfo.code == ZIMErrorCode.SUCCESS;
                    result.put("success", ok);
                    result.put("groupId", groupID == null ? (groupId == null ? "" : groupId) : groupID);
                    result.put("message", errorInfo == null ? "" : errorInfo.message);
                    if (callback != null) {
                        callback.invoke(result);
                    }
                }
            });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /** 查询多个群的 bizType 属性：入参 JSON 数组 [groupId...]，返回 types: { groupId: bizType } */
    @UniJSMethod(uiThread = true)
    public void getGroupTypes(String groupIdsJson, UniJSCallback callback) {
        try {
            com.alibaba.fastjson.JSONArray ids =
                com.alibaba.fastjson.JSON.parseArray(groupIdsJson == null ? "[]" : groupIdsJson);
            JSONObject types = new JSONObject();
            if (ids == null || ids.isEmpty()) {
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("types", types);
                if (callback != null) {
                    callback.invoke(result);
                }
                return;
            }
            final int total = ids.size();
            final int[] remain = { total };
            for (int i = 0; i < total; i++) {
                final String gid = ids.getString(i);
                ArrayList<String> keys = new ArrayList<>();
                keys.add("bizType");
                zimInstance().queryGroupAttributes(keys, gid, new ZIMGroupAttributesQueriedCallback() {
                    @Override
                    public void onGroupAttributesQueried(String groupID, HashMap<String, String> attributes,
                        ZIMError errorInfo) {
                        if (errorInfo != null && errorInfo.code == ZIMErrorCode.SUCCESS && attributes != null
                            && attributes.containsKey("bizType")) {
                            types.put(groupID, attributes.get("bizType"));
                        }
                        synchronized (remain) {
                            remain[0]--;
                            if (remain[0] == 0) {
                                JSONObject result = new JSONObject();
                                result.put("success", true);
                                result.put("types", types);
                                if (callback != null) {
                                    callback.invoke(result);
                                }
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    @UniJSMethod(uiThread = true)
    public void createGroupChat(String conversationID,UniJSCallback callback) {

        ZIMKit.joinGroup(conversationID, new JoinGroupCallback() {
            @Override
            public void onJoinGroup(ZIMKitGroupInfo groupInfo, ZIMError error) {
                if (error.code == ZIMErrorCode.SUCCESS || error.code == ZIMErrorCode.MEMBER_IS_ALREADY_IN_THE_GROUP) {
                    ZIMKitRouter.toMessageActivity(mUniSDKInstance.getContext(), conversationID, ZIMKitConversationType.ZIMKitConversationTypeGroup);
                    if (callback != null) {
                        JSONObject result = new JSONObject();
                        result.put("message", "1");
                        callback.invoke(result);
                    }
                } else {
                    if (callback != null) {
                    JSONObject result = new JSONObject();
                    result.put("message", "0");
                    callback.invoke(result);
                    }
                }
            }
        });
    }


    @UniJSMethod(uiThread = true)
    public void joinCall(JSONObject options, UniJSCallback callback) {
        Context context = mUniSDKInstance.getContext();
        System.out.println("level here -->");
        System.out.println(options.getString("meetingId"));
//        Intent intent = new Intent(context, VoiceRoomListActivity.class);
//        intent.putExtra(RoomConstants.INTENT_IS_OVERSEA, AppConfig.isOversea());
//        intent.putExtra(RoomConstants.INTENT_KEY_CONFIG_ID, AppConfig.getVoiceRoomConfigId());
//        intent.putExtra(RoomConstants.INTENT_USER_NAME, AppUtils.getUserName());
//        intent.putExtra(RoomConstants.INTENT_AVATAR, AppUtils.getAvatar());
//        context.startActivity(intent);


//        Intent intent = new Intent(context, VoiceRoomCreateActivity.class);
//        intent.putExtra(RoomConstants.INTENT_IS_OVERSEA, AppConfig.isOversea());
//        intent.putExtra(RoomConstants.INTENT_KEY_CONFIG_ID, AppConfig.getVoiceRoomConfigId());
//        intent.putExtra(RoomConstants.INTENT_USER_NAME, AppUtils.getUserName());
//        intent.putExtra(RoomConstants.INTENT_AVATAR, AppUtils.getAvatar());
//        context.startActivity(intent);

//        AudioRoomActivity.setRoomLeaveListener(() -> {
//            System.out.println("HERE BRO, LEAVE ROOM TRIGGERED = ");
//            System.out.println(user_id + " = " + userName + " = "+avatar);
//            if(avatar == null){
//                avatar = "https://test.ioevisa.com/pics/profile.png";
//            }
//            JSONObject event = new JSONObject();
//            event.put("event", "ROOM_LEFT");
//
//            if (globalJsCallback != null) {
//                globalJsCallback.invokeAndKeepAlive(event);
//            }
//
//        });
//        boolean isHost = false;
//        Context context = mUniSDKInstance.getContext();
//        Intent intent = new Intent(context, AudioRoomActivity.class);
//        intent.putExtra("userID", options.getString("userId"));
//        intent.putExtra("userName", options.getString("userName"));
//        intent.putExtra("roomID", options.getString("meetingId"));
//        intent.putExtra("isHost", isHost);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(intent);
//        if (callback != null) {
//            callback.invoke("success");
//        }
    }

    @UniJSMethod(uiThread = true)
    public void joinCall3(){
        long appID = 845800108;
        String appSign = "8857c37bde5ffc60a8cadebef546f51f57f01d7f5a8bfc0d2b0d192adaa33bd3";
        Context context = mUniSDKInstance.getContext();

        String userID = "898";
        String userName = userID + "_Name";
        String liveID = "test_live_id";
        Intent intent = new Intent(context, LiveActivity.class);
        intent.putExtra("host", true);
        intent.putExtra("appID", appID);
        intent.putExtra("appSign", appSign);
        intent.putExtra("userID", userID);
        intent.putExtra("userName", userName);
        intent.putExtra("liveID", liveID);
        System.out.println("almost out there");
        context.startActivity(intent);
    }

//    @UniJSMethod(uiThread = true)
//    public void joinCall(){
//        loadMoreConversations();
//    }
    @UniJSMethod(uiThread = false)
    public void loginToZegoChat(JSONObject options,  UniJSCallback jsCallback) {
        System.out.println("Login in state oo"+ options.getString("userId")+" name "+options.getString("userName"));
        user_id = options.getString("userId");
        userName = options.getString("userName");
        String avatar = options.getString("avatarUrl") != null
                ? options.getString("avatarUrl")
                : "https://test.ioevisa.com/pics/profile.png";
        ZIMKit.connectUser(options.getString("userId"), options.getString("userName"),
                avatar, errorInfo -> {
            JSONObject result = new JSONObject();
            if (errorInfo.code == ZIMErrorCode.SUCCESS) {
                System.out.println("Zego standard login succeeded for tracking target: " + options.getString("userId"));
                result.put("success", true);
                result.put("msg", "Connected");
                registerGlobalListener();
                jsCallback.invoke(result); // Fire response back to JavaScript instantly
            } else {
                System.out.println("Zego login failed configuration fault: " + errorInfo.message);
                result.put("success", false);
                result.put("code", errorInfo.code.value());
                result.put("msg", errorInfo.message);
                jsCallback.invoke(result);
            }
        });
        //neteaseLogin();
    }

    /** 跨 Activity 向 uniapp 派发全局事件（静态入口） */
    private static UniJSCallback sGlobalJsCallback;

    public static void emitGlobalEvent(String event, JSONObject data) {
        emitGlobalEvent(event, (Object) data);
    }

    /**
     * 跨 Activity 向 uniapp 派发全局事件（宽松重载）。
     *
     * <p><b>为什么参数是 Object</b>：zimkit 侧用反射调这个方法，而 zimkit 手里的 JSON 类型
     * 可能与这里的 {@code org.json.JSONObject} 不是同一个类（工程里同时存在 org.json 与
     * com.alibaba.fastjson）→ 精确签名匹配会抛
     * {@code NoSuchMethodException: emitGlobalEvent [String, org.json.JSONObject]}，
     * 事件被静默吞掉（实测踩过：点悬浮球只关了原生页、没跳 uniapp）。
     * 放宽成 Object 后，两种 JSONObject 都能调进来，这里再按类型取用。
     */
    public static void emitGlobalEvent(String event, Object data) {
        try {
            if (sGlobalJsCallback == null) {
                android.util.Log.w("StoreEntry", "emitGlobalEvent skipped (no channel): " + event);
                return;
            }
            JSONObject payload = new JSONObject();
            payload.put("event", event);
            JSONObject body = new JSONObject();
            if (data instanceof JSONObject) {
                body = (JSONObject) data;
            } else if (data instanceof com.alibaba.fastjson.JSONObject) {
                // fastjson 侧传进来的：逐 key 复制成 org.json
                // （org.json.JSONObject 没有 String 构造器，new JSONObject(String) 会抛异常）
                for (java.util.Map.Entry<String, Object> entry
                    : ((com.alibaba.fastjson.JSONObject) data).entrySet()) {
                    body.put(entry.getKey(), entry.getValue());
                }
            }
            payload.put("data", body);
            sGlobalJsCallback.invokeAndKeepAlive(payload);
        } catch (Exception e) {
            android.util.Log.w("StoreEntry", "emitGlobalEvent fail: " + e);
        }
    }

    /**
     * uniapp 的事件通道是否就绪（{@code startSyncPipeline} 注册的回调）。
     *
     * <p>用途：原生页要"派发事件让 uniapp 跳页面"时，必须先确认通道存在 ——
     * 极冷启动（推送直接进聊天页）时通道还没注册，此时不能傻等，应改为轻提示而不是跳不相关页面。
     */
    public static boolean hasGlobalEventChannel() {
        return sGlobalJsCallback != null;
    }

    /**
     * uniapp 通道就绪后补发极冷启动期间暂存的事件（见 {@code UniappEventApi.emitOrQueue}），
     * 并收掉压在 uniapp 之上的原生聊天页 —— 否则 uniapp 的页面在下面，用户看不到跳转结果。
     */
    private static void flushPendingUniappEvents() {
        final boolean flushed;
        try {
            flushed = com.zegocloud.zimkit.common.utils.UniappEventApi.flushPending();
        } catch (Throwable t) {
            android.util.Log.w("StoreEntry", "flushPending 异常: " + t);
            return;
        }
        if (!flushed) {
            return;
        }
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            try {
                com.zegocloud.zimkit.components.message.ui.ZIMKitMessageActivity.finishCurrent();
            } catch (Throwable t) {
                android.util.Log.w("StoreEntry", "补发后收页失败: " + t);
            }
        }, 300);
    }

    @UniJSMethod(uiThread = false)
    public void startSyncPipeline(UniJSCallback callback) {
        globalJsCallback = callback;
        sGlobalJsCallback = callback;
        // uniapp 通道此刻就绪 → 补发极冷启动期间暂存的事件（点悬浮球/卡片时 uniapp 还没起来）
        flushPendingUniappEvents();
        // ⚠️ 幂等：uniapp 侧现在会在 onShow 也调一次（registerNativeEventChannel，为了让事件通道
        //    不依赖 ZIM 登录）。若这里每次都 stop + 重建，就会**每次回前台都把 3 个 ZIMKit 事件回调
        //    置空、重建线程池与 30 秒定时任务** → 聊天页明显卡顿（实测 3 分钟内被重启 10 次）。
        //    已注册过就只更新回调引用，不做任何拆卸/重建。
        if (syncScheduler != null && !syncScheduler.isShutdown()) {
            android.util.Log.i("StoreEntry", "事件通道已注册，仅更新回调（不重启管道）");
            return;
        }
        System.out.println("SaaS Data Engine: Sync Pipeline Activated.");
        android.util.Log.i("StoreEntry", "uniapp event channel registered (startSyncPipeline)");
        enableConversationLiveSync();
        syncScheduler = Executors.newSingleThreadScheduledExecutor();
        syncScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    loadPeerConversations();
                    loadGroupConversations();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 30, TimeUnit.SECONDS); // 0 seconds initial delay, runs every 30 seconds
    }

    /**
     * 群头像覆盖表：{@code groupId → groupAvatarUrl}，值是**服务端权威群头像**。
     *
     * <p>为什么需要：`updateGroupAvatarUrl` 成功只改服务端「群资料」，ZIM **不会**同步刷新
     * 会话对象上的 `conversationAvatarUrl`（uniapp 社群列表读的就是它），于是列表一直显示旧头像
     * 或退化成"群名首字"方块。而 `queryGroupList` 读的是**本地群缓存**，也补不到新值 ——
     * 只有 `queryGroupInfo(groupId)` 是单群服务端查询，能拿到权威值。
     *
     * <p>所以：群资料变更时用 `queryGroupInfo` 拉一次权威头像存这里，推送会话列表时优先用它覆盖。
     */
    private static final java.util.Map<String, String> groupAvatarOverride =
        new java.util.concurrent.ConcurrentHashMap<>();

    /** 拉取单个群的权威资料并写入覆盖表（群名/头像变更时调用） */
    private static void refreshGroupProfileOverride(String groupId) {
        // 诊断：确认这条链有没有被触发（adb logcat -s GroupProfile:V）
        android.util.Log.i("GroupProfile", "refresh requested gid=" + groupId);
        if (groupId == null || groupId.isEmpty()) {
            return;
        }
        try {
            zimInstance().queryGroupInfo(groupId, (info, err) -> {
                if (err != null && err.code != ZIMErrorCode.SUCCESS) {
                    android.util.Log.w("GroupProfile", "queryGroupInfo fail gid=" + groupId
                        + " code=" + err.code + " msg=" + err.message);
                    return;
                }
                if (info == null || info.baseInfo == null) {
                    return;
                }
                String avatar = info.baseInfo.groupAvatarUrl;
                if (avatar != null && !avatar.isEmpty()) {
                    groupAvatarOverride.put(groupId, avatar);
                    android.util.Log.i("GroupProfile", "override avatar gid=" + groupId
                        + " url=" + avatar);
                }
                String name = info.baseInfo.groupName;
                if (name != null && !name.isEmpty()) {
                    groupNameOverride.put(groupId, name);
                }
                // 覆盖表变了 → 立刻按新值重推一次列表
                loadGroupConversations();
            });
        } catch (Exception e) {
            android.util.Log.w("GroupProfile", "refreshGroupProfileOverride fail: " + e.getMessage());
        }
    }

    /** 群名覆盖表（同头像：会话对象上的 conversationName 也不会被 ZIM 自动刷新） */
    private static final java.util.Map<String, String> groupNameOverride =
        new java.util.concurrent.ConcurrentHashMap<>();

    private static void loadGroupConversations() {
        ZIMConversationQueryConfig config = new ZIMConversationQueryConfig();
        config.count = 100;

        ZIMConversationFilterOption filterOption = new ZIMConversationFilterOption();
        filterOption.conversationTypes = new ArrayList<>();
        filterOption.conversationTypes.add(ZIMConversationType.GROUP); // Zego server filters to 1-vs-1 only

        ZegoSignalingPlugin.getInstance().queryConversationList(config, filterOption, (conversationList, errorInfo) -> {
            if (errorInfo.code != ZIMErrorCode.SUCCESS) {
                System.out.println("SaaS Sync Fault: " + errorInfo.message);
                return;
            }
            if (conversationList == null) {
                return;
            }
            if (conversationList.isEmpty()) {
                System.out.println("SaaS Sync: group conversation list empty");
            }
            // 群名/群头像以覆盖表（服务端权威值）为准合并，覆盖表没有的再退回 queryGroupList
            // （queryGroupList 是本地缓存，只在"从未改过头像"时够用）
            zimInstance().queryGroupList((groups, groupError) -> {
                if (groupError != null && groupError.code != ZIMErrorCode.SUCCESS) {
                    android.util.Log.w("SaaS Sync", "queryGroupList fail: " + groupError.message);
                }
                pushGroupConversations(conversationList, groups);
            });
        });
    }

    /**
     * 把会话列表推给 uniapp（GROUP 事件）。
     *
     * <p>关键修复：`updateGroupName` / `updateGroupAvatarUrl` 成功只改**服务端群资料**，
     * ZIM **不会**自动刷新本地会话对象上的 `conversationName` / `conversationAvatarUrl`。
     * 于是 uniapp 列表（`sessionList` 用 `item.conversationAvatarUrl || item.groupLogo`，
     * 拿不到就退化成群名首字方块）永远显示旧头像/旧名字。
     * 这里用 {@code queryGroupList} 拿到的权威群资料去覆盖这两个字段后再推送，
     * 社群频道和普通群聊（含 2 人群聊）走的是同一套，所以两类头像都能生效。
     */
    private static void pushGroupConversations(List<im.zego.zim.entity.ZIMConversation> conversationList,
        ArrayList<im.zego.zim.entity.ZIMGroup> groups) {
        try {
            // 用 Gson 批量序列化，再转 fastjson 数组（保持原有字段名给 uniapp）
            com.alibaba.fastjson.JSONArray uniArray = com.alibaba.fastjson.JSON.parseArray(
                    new com.google.gson.Gson().toJson(conversationList)
            );
            for (int i = 0; i < uniArray.size() && i < conversationList.size(); i++) {
                JSONObject o = uniArray.getJSONObject(i);
                if (o == null) {
                    continue;
                }
                im.zego.zim.entity.ZIMConversation c = conversationList.get(i);
                // 免打扰标记供列表显示铃铛划线
                o.put("muted", c != null && c.notificationStatus
                    == im.zego.zim.enums.ZIMConversationNotificationStatus.DO_NOT_DISTURB);
                applyGroupProfileToConversations(o, c == null ? null : c.conversationID, groups);
            }
            JSONObject payload = new JSONObject();
            payload.put("list", uniArray);

            JSONObject eventPayload = new JSONObject();
            eventPayload.put("event", "GROUP");
            eventPayload.put("data", payload);

            UniJSCallback cb = globalJsCallback;
            if (cb == null) {
                return;
            }
            cb.invokeAndKeepAlive(eventPayload);
            System.out.println("SaaS Engine Sync Event broadcasted to uni-app workspace.");
        } catch (Exception e) {
            android.util.Log.w("SaaS Sync", "pushGroupConversations fail: " + e.getMessage());
        }
    }

    /** 用群资料覆盖会话上的群名/群头像（群资料查不到时保留原值，不写空覆盖） */
    private static void applyGroupProfileToConversations(JSONObject convJson, String groupId,
        ArrayList<im.zego.zim.entity.ZIMGroup> groups) {
        if (convJson == null || groupId == null) {
            return;
        }
        // 1) 优先用服务端权威覆盖表（改过头像/群名的群走这条，这是唯一可靠的来源）
        String overrideAvatar = groupAvatarOverride.get(groupId);
        String overrideName = groupNameOverride.get(groupId);
        if (overrideAvatar != null && !overrideAvatar.isEmpty()) {
            convJson.put("conversationAvatarUrl", overrideAvatar);
        }
        if (overrideName != null && !overrideName.isEmpty()) {
            convJson.put("conversationName", overrideName);
        }
        // 诊断：改过头像的群每次推送都打一行，方便确认覆盖是否真的落到 payload
        // adb logcat -s GroupProfile:V
        if ((overrideAvatar != null && !overrideAvatar.isEmpty())
            || (overrideName != null && !overrideName.isEmpty())) {
            android.util.Log.i("GroupProfile", "apply override gid=" + groupId
                + " avatarHit=" + (overrideAvatar != null && !overrideAvatar.isEmpty())
                + " nameHit=" + (overrideName != null && !overrideName.isEmpty()));
        }
        // 2) 覆盖表没有的，退回本地群列表（queryGroupList 是本地缓存）
        if (groups == null) {
            return;
        }
        for (im.zego.zim.entity.ZIMGroup g : groups) {
            im.zego.zim.entity.ZIMGroupInfo info = g == null ? null : g.baseInfo;
            if (info == null || !groupId.equals(info.groupID)) {
                continue;
            }
            if (overrideName == null || overrideName.isEmpty()) {
                if (info.groupName != null && !info.groupName.isEmpty()) {
                    convJson.put("conversationName", info.groupName);
                }
            }
            if (overrideAvatar == null || overrideAvatar.isEmpty()) {
                if (info.groupAvatarUrl != null && !info.groupAvatarUrl.isEmpty()) {
                    convJson.put("conversationAvatarUrl", info.groupAvatarUrl);
                }
            }
            return;
        }
    }

    private void loadPeerConversations() {
        ZIMConversationQueryConfig config = new ZIMConversationQueryConfig();
        config.count = 100;
        ZIMConversation conversation = new ZIMConversation();
//        conversation.marks.add(1);
//        conversation.marks.add(2);
//        config.nextConversation = conversation;


        ZIMConversationFilterOption filterOption = new ZIMConversationFilterOption();
        filterOption.conversationTypes = new ArrayList<>();
        ArrayList<Integer> marksList = new ArrayList<>(Arrays.asList(-1));
        filterOption.marks = marksList;
        filterOption.conversationTypes.add(ZIMConversationType.PEER); // Zego server filters to 1-vs-1 only

        ZegoSignalingPlugin.getInstance().queryConversationList(config, filterOption, (conversationList, errorInfo) -> {
            if (errorInfo.code != ZIMErrorCode.SUCCESS) {
                System.out.println("SaaS Sync Fault: " + errorInfo.message);
                return;
            }
            if (conversationList != null && globalJsCallback != null) {
                // Bulk serialize using your safe Gson trick
                com.alibaba.fastjson.JSONArray uniArray = com.alibaba.fastjson.JSON.parseArray(
                        new com.google.gson.Gson().toJson(conversationList)
                );
                System.out.println("list length :::" + conversationList.size());
                JSONObject payload = new JSONObject();
                payload.put("list", uniArray);

                // Package into an event channel payload box
                JSONObject eventPayload = new JSONObject();
                eventPayload.put("event", "PEER_LIST_SYNCED");
                eventPayload.put("data", payload);

                // 再取一次局部引用再调用：globalJsCallback 可能被 stopSyncPipeline 置空，
                // 直接调用会 NPE（2026-09-10 18:23 实测崩溃：invokeAndKeepAlive on a null object）
                UniJSCallback cb = globalJsCallback;
                if (cb == null) {
                    return;
                }
                try {
                    cb.invokeAndKeepAlive(eventPayload);
                    System.out.println("SaaS Engine Sync Event broadcasted to uni-app workspace.");
                } catch (Exception e) {
                    android.util.Log.w("TestModule", "PEER_LIST_SYNCED invoke fail: " + e);
                }
            }
        });
    }

    @UniJSMethod(uiThread = false)
    public void stopSyncPipeline() {
        if (syncScheduler != null && !syncScheduler.isShutdown()) {
            syncScheduler.shutdownNow();
            System.out.println("SaaS Data Engine: Sync Pipeline Deactivated cleanly.");
        }
        com.zegocloud.zimkit.services.internal.ZIMKitEventHandler.setConversationChangeCallback(null);
        com.zegocloud.zimkit.services.internal.ZIMKitEventHandler.setGroupApplicationCallback(null);
        com.zegocloud.zimkit.services.internal.ZIMKitEventHandler.setGroupProfileCallback(null);
    }

    /** 会话变化事件（ZIM 实时回调）→ 秒级重推列表 + 广而告之 uniapp 页面 */
    private void enableConversationLiveSync() {
        try {
            com.zegocloud.zimkit.services.internal.ZIMKitEventHandler.setConversationChangeCallback(
                () -> new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    try {
                        loadGroupConversations();
                        loadPeerConversations();
                        if (globalJsCallback != null) {
                            JSONObject eventPayload = new JSONObject();
                            eventPayload.put("event", "CONVERSATION_LIVE");
                            eventPayload.put("data", new JSONObject());
                            globalJsCallback.invokeAndKeepAlive(eventPayload);
                        }
                    } catch (Exception ignored) {
                    }
                }));
            // 群申请列表变化 → 角标/审核列表秒级
            com.zegocloud.zimkit.services.internal.ZIMKitEventHandler.setGroupApplicationCallback(
                () -> new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    try {
                        if (globalJsCallback != null) {
                            JSONObject eventPayload = new JSONObject();
                            eventPayload.put("event", "GROUP_APPLICATION_LIST_CHANGED");
                            eventPayload.put("data", new JSONObject());
                            globalJsCallback.invokeAndKeepAlive(eventPayload);
                        }
                    } catch (Exception ignored) {
                    }
                }));
            // 群名/群头像变更 → 用 queryGroupInfo 拉服务端权威值写入覆盖表，再重推会话列表
            // （不能只 loadGroupConversations：会话对象上的 conversationName/conversationAvatarUrl
            //   不会被 ZIM 自动刷新，queryGroupList 又只读本地缓存）
            com.zegocloud.zimkit.services.internal.ZIMKitEventHandler.setGroupProfileCallback(
                groupId -> new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    try {
                        android.util.Log.i("GroupProfile", "changed gid=" + groupId
                            + " → refresh authoritative profile");
                        refreshGroupProfileOverride(groupId);
                    } catch (Exception ignored) {
                    }
                }));
        } catch (Exception ignored) {
        }
    }

    @UniJSMethod(uiThread = false)
    public void logoutFromZegoChat(UniJSCallback jsCallback) {
        System.out.println("SaaS Security Engine: Direct manual logout command received.");
        JSONObject result = new JSONObject();
        System.out.println("SaaS Security Engine 1 ");
//        result.put("success", false);
//        jsCallback.invoke(result);


        try {
            // 1. Instantly kill your 30s background scheduling pipeline loops
            stopSyncPipeline();

            // 2. Trigger Zego's official socket disconnect clean routine
            ZIMKit.disconnectUser();
            //neteaseLogout();

            // 3. Clear your native class memory status tracking flags
            isDelegateRegistered = false;
            globalJsCallback = null;
            cardJsCallback = null;
            System.out.println("SaaS Security Engine 2 ");

            System.out.println("SaaS Security Engine: Zego socket destroyed and session cleared successfully.");
            result.put("success", true);
            result.put("msg", "Successfully logged out from chat system.");

        } catch (Exception e) {
            System.out.println("SaaS Security Engine Logout Fault: " + e.getMessage());
            result.put("success", false);
            result.put("msg", "Logout error: " + e.getMessage());
        }

        jsCallback.invoke(result);
    }

    @UniJSMethod(uiThread = true)
    public void joinCall4(){
        try {

            String userId = "7889";
            String userName = "Mercy";
            String avatarUrl = "https://storage.zego.im/IMKit/avatar/avatar-0.png";

            Log.d(TAG, "Initializing login for User ID: " + userId + " (" + userName + ")");

            Context context = mUniSDKInstance.getContext();
            if (context == null) {
                Log.e(TAG, "Failed to initialize: mUniSDKInstance.getContext() returned null.");
                return;
            }

            // Save user ID and username to local SharedPreferences
            Log.d(TAG, "Saving user credentials to SharedPreferences...");
            SharedPreferences.Editor editor = context.getSharedPreferences("myPrefs", Context.MODE_PRIVATE).edit();
            editor.putString("userID", userId);
            editor.putString("userName", userName);
            editor.apply();
            Log.i(TAG, "Preferences saved successfully.");

            // Attempting connection via ZIMKit SDK
            Log.d(TAG, "Calling ZIMKit.connectUser...");
            ZIMKit.connectUser(userId, userName, avatarUrl, error -> {
                try {
                    // Check for SDK connection errors
                    if (error.code != ZIMErrorCode.SUCCESS) {
                        String message = error.message + ": " + error.code.value();
                        Log.e(TAG, "ZIMKit connection failed. Error details: " + message);

                        Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Connection succeeded, routing to Activity
                    Log.i(TAG, "ZIMKit connection established successfully. Launching ConversationActivity...");
                    Intent intent = new Intent(context, ConversationActivity.class);

                    // Adding task flags if launching from outside a traditional Activity context
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    context.startActivity(intent);
                    Log.d(TAG, "ConversationActivity intent dispatched.");
                    loadPeerConversations();


                } catch (Exception callbackEx) {
                    Log.e(TAG, "Uncaught exception inside ZIMKit callback: " + callbackEx.getMessage(), callbackEx);
                }
            });

        } catch (NullPointerException npe) {
            Log.e(TAG, "NullPointerException occurred during setup (Check mUniSDKInstance initialization): " + npe.getMessage(), npe);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error occurred during user connection routing: " + e.getMessage(), e);
        }
    }

    private static UniJSCallback globalJsCallback;
    private static UniJSCallback cardJsCallback;
    /**
     * 冷启动点卡片时 uniapp 还没起来 → **暂存**事件，等 uniapp 注册回调的那一刻补发。
     *
     * <p>场景（实测）：进程被杀后从最近任务恢复到原生聊天页，Android 只重建栈顶那个原生 Activity，
     * uniapp 的 {@code PandoraEntryActivity} 还没被创建 → {@code App.vue} 的 onLaunch 没跑过 →
     * {@code registerCardEventCallback} 还没注册 → 点卡片时 {@code cardJsCallback == null}。
     * 旧代码在这里**静默 return**，用户看到的是"怎么点都没反应"（日志连续 `cardJsCallback=false`）。
     */
    private static volatile String sPendingCardAction;
    private static volatile String sPendingCardData;
    private static final Map<String, UniJSCallback> sMemberPickerCallbacks = new ConcurrentHashMap<>();
    private static UniJSCallback sGroupMembersCallback;
    private static boolean isDelegateRegistered = false;

    /** 2 人「聊天」与普通群聊的统一默认群名（ZIM 群名双方共享，不能写成任一方的昵称） */
    private static final String PRIVATE_CHAT_NAME = "聊天";

    /** 原生红包 Activity 调用业务接口所需的静态配置 */
    private static String businessBaseUrl = "https://buyer-ceshi.shanxunsw.com/buyer";
    private static String localUserId = "";
    private static String localUserName = "";
    private static String localUserAvatar = "";

    /**
     * 业务 token 统一由 {@link io.dcloud.uniplugin.others.BusinessSession} 持有与刷新
     * （原生自己判断 JWT 过期 + 自己调 /passport/member/refresh 刷新），
     * uniapp 只负责在这里初始化一次；不再需要 uniapp 的请求层反推 token 给原生。
     */
    @UniJSMethod(uiThread = true)
    public void setBusinessConfig(String baseUrl, String token, String userId, String userName, String avatarUrl) {
        setBusinessConfigWithRefresh(baseUrl, token, userId, userName, avatarUrl, "");
    }

    /** 带 refreshToken 的完整初始化：原生据此可独立刷新，不必回问 uniapp */
    @UniJSMethod(uiThread = true)
    public void setBusinessConfigWithRefresh(String baseUrl, String token, String userId, String userName,
        String avatarUrl, String refreshToken) {
        cacheContext(safeContext());
        if (baseUrl != null && !baseUrl.isEmpty()) {
            businessBaseUrl = baseUrl;
        }
        io.dcloud.uniplugin.others.BusinessSession.setConfig(businessBaseUrl, token, refreshToken);
        // zimkit 原生页（群聊设置改头像等）要上传图片，但它是最底层模块不能反向依赖这里 →
        // 由这里把 token 取用口子注入进去（zimkit 只管调，不认识业务登录态）
        com.zegocloud.zimkit.common.utils.MediaUploader.setTokenProvider(
            () -> io.dcloud.uniplugin.others.BusinessSession.ensureFreshTokenBlocking());
        // 群聊（含 2 人群聊）名称/头像改完要回写业务库：否则读业务后端的页面
        // （分享组件 share-picker 的 avatar/title）永远显示旧值。
        // 接口：PUT /buyer/social/temp-group/profile { groupId, groupLogo?, groupName? }
        com.zegocloud.zimkit.common.utils.GroupProfileApi.setBackendSync(
            (groupId, groupName, groupLogo, callback) -> {
                try {
                    JSONObject body = new JSONObject();
                    body.put("groupId", groupId);
                    // 接口约定：groupName、groupLogo 至少传一个；不传=不改
                    if (groupName != null && !groupName.isEmpty()) {
                        body.put("groupName", groupName);
                    }
                    if (groupLogo != null && !groupLogo.isEmpty()) {
                        body.put("groupLogo", groupLogo);
                    }
                    android.util.Log.i("GroupProfileApi", "PUT temp-group/profile body="
                        + body.toJSONString());
                    io.dcloud.uniplugin.others.RedPacketApi.put(
                        businessBaseUrl + "/social/temp-group/profile", body,
                        new io.dcloud.uniplugin.others.RedPacketApi.Callback() {
                            @Override
                            public void onSuccess(JSONObject result) {
                                if (callback != null) {
                                    callback.onDone(true, "");
                                }
                            }

                            @Override
                            public void onError(int code, String message) {
                                if (callback != null) {
                                    callback.onDone(false, "HTTP " + code + " " + message);
                                }
                            }
                        });
                } catch (Exception e) {
                    if (callback != null) {
                        callback.onDone(false, e.getMessage());
                    }
                }
            });
        localUserId = userId == null ? "" : userId;
        localUserName = userName == null ? "" : userName;
        localUserAvatar = avatarUrl == null ? "" : avatarUrl;
        // 原生页 → uniapp 的全局事件派发：zimkit 不能反向 import 本类，用注入接口对接。
        // （曾用反射调 emitGlobalEvent，运行时抛 NoSuchMethodException —— 两个模块持有的 JSONObject
        //   不是同一个类，精确签名匹配必然失败，事件被静默吞掉 → 点悬浮球只关原生页不跳转）
        com.zegocloud.zimkit.common.utils.UniappEventApi.setEmitter((event, dataClassName, keys, values) -> {
            if (sGlobalJsCallback == null) {
                return false;
            }
            JSONObject data = new JSONObject();
            if (keys != null && values != null) {
                for (int i = 0; i < keys.length && i < values.length; i++) {
                    try {
                        data.put(keys[i], values[i]);
                    } catch (Exception ignored) {
                    }
                }
            }
            emitGlobalEvent(event, (Object) data);
            return true;
        });
        // 通道"就绪"判据 = uniapp 真的注册了全局回调（startSyncPipeline），而不是"emitter 注册了"
        // （后者在 setBusinessConfig 时就有了，两者是两件事 —— 用错判据会让极冷启动提示语不对）
        com.zegocloud.zimkit.common.utils.UniappEventApi.setChannelProbe(TestModule::hasGlobalEventChannel);
        // 极冷启动（点悬浮球时 uniapp 还没起来）→ 由这里把 uniapp 拉起来（zimkit 不能反向依赖本模块）
        com.zegocloud.zimkit.common.utils.UniappEventApi.setLauncher(this::bringUniappToFront);
        // 聊天页「社群店铺」悬浮球：显隐由业务后端决定（群主开关 + 群主有 PASS 店铺，后端已综合）。
        // 轻量接口 GET /buyer/social/group/{id}/store-entry
        //   → { showStore, distributionId, groupId, storeLogo, storeName }
        // 其中 storeLogo 当悬浮球头像（铺满 58×58）、storeName 当底部横幅店名（最多 4 字 + 省略号）。
        // zimkit 不能反向依赖业务 token → 这里注入实现（与 GroupProfileApi 同一套路）。
        com.zegocloud.zimkit.common.utils.StoreEntryApi.setFetcher((groupId, callback) -> {
            try {
                io.dcloud.uniplugin.others.RedPacketApi.get(
                    businessBaseUrl + "/social/group/" + groupId + "/store-entry",
                    new JSONObject(),
                    new io.dcloud.uniplugin.others.RedPacketApi.Callback() {
                        @Override
                        public void onSuccess(JSONObject result) {
                            if (result == null) {
                                if (callback != null) {
                                    callback.onResult(null);
                                }
                                return;
                            }
                            boolean show = Boolean.TRUE.equals(result.getBoolean("showStore"));
                            String distributionId = result.getString("distributionId");
                            // 头像 / 店名：后端没给就为空串（前端据此回退成"白壳+橙圆+图标"、不显示店名横幅）
                            String storeLogo = result.getString("storeLogo");
                            String storeName = result.getString("storeName");
                            android.util.Log.i("StoreEntry", "gid=" + groupId + " show=" + show
                                + " distributionId=" + distributionId
                                + " storeName=" + storeName + " logoLen="
                                + (storeLogo == null ? 0 : storeLogo.length()));
                            if (callback != null) {
                                callback.onResult(new com.zegocloud.zimkit.common.utils.StoreEntryApi.Entry(
                                    show, distributionId, groupId, storeLogo, storeName));
                            }
                        }

                        @Override
                        public void onError(int code, String message) {
                            // 失败/超时 → 悬浮球不显示（只记日志，不打扰用户）
                            android.util.Log.w("StoreEntry", "gid=" + groupId + " fail code=" + code
                                + " msg=" + message + " → hide");
                            if (callback != null) {
                                callback.onResult(null);
                            }
                        }
                    });
            } catch (Exception e) {
                android.util.Log.w("StoreEntry", "gid=" + groupId + " exception: " + e.getMessage());
                if (callback != null) {
                    callback.onResult(null);
                }
            }
        });
        System.out.println("[BusinessConfig] baseUrl=" + businessBaseUrl
            + " tokenLen=" + io.dcloud.uniplugin.others.BusinessSession.getAccessToken().length()
            + " refreshLen=" + io.dcloud.uniplugin.others.BusinessSession.getRefreshToken().length()
            + " userId=" + localUserId + " userName=" + localUserName);
    }

    public static String getBusinessBaseUrl() {
        return businessBaseUrl;
    }

    public static String getLocalUserId() {
        return localUserId;
    }

    public static String getLocalUserName() {
        return localUserName;
    }

    public static String getLocalUserAvatar() {
        return localUserAvatar;
    }

    /**
     * uniapp 侧 token 刷新后主动推送最新 token（反向：不用等原生 403 才刷新）。
     * 只更新 token 字段，不动 baseUrl/userId/昵称头像，避免并发写坏配置。
     */
    @UniJSMethod(uiThread = false)
    public void notifyBusinessTokenChanged(String token) {
        String t = token == null ? "" : token;
        if (t.isEmpty()) {
            return;
        }
        // 可选同步通道：uniapp 若自己轮换了 token（走它自己的请求层），可主动同步给原生。
        // 但原生**不依赖**它——过期判断与刷新由 BusinessSession 独立完成。
        io.dcloud.uniplugin.others.BusinessSession.setConfig(null, t, null);
        android.util.Log.i("BusinessConfig", "token updated by push, tokenLen=" + t.length());
    }

    /**
     * 原生刷新了 token → 反向同步给 uniapp，更新其登录态存储。
     * 目的：避免"两边各持一份 token"互相踩（一侧刷新后另一侧仍用旧值 → 401 → 再刷 → 抖动）。
     * 只推"凭据变了"这一件事，不参与 uniapp 的请求层。
     */
    public static void notifyNativeTokenRefreshed(String accessToken, String refreshToken) {
        try {
            JSONObject data = new JSONObject();
            data.put("accessToken", accessToken == null ? "" : accessToken);
            data.put("refreshToken", refreshToken == null ? "" : refreshToken);
            emitGlobalEvent("BUSINESS_TOKEN_REFRESHED", data);
        } catch (Exception e) {
            android.util.Log.w("BusinessConfig", "notify token refreshed fail: " + e);
        }
    }

    /**
     * 原生发现凭据不可用（没登录 / 登录已过期）→ 通知 uniapp 走登录。
     * 场景：退出登录 → 冷启动（此时没有可用 token）→ 点红包等原生入口。
     * 不再拿空 token 去打接口换一个必然的 401，而是明确引导登录。
     */
    public static void notifyNeedLogin(String message) {
        try {
            JSONObject data = new JSONObject();
            data.put("message", message == null ? "" : message);
            emitGlobalEvent("BUSINESS_NEED_LOGIN", data);
        } catch (Exception e) {
            android.util.Log.w("BusinessConfig", "notify need login fail: " + e);
        }
    }

    @UniJSMethod(uiThread = true)
    public void registerCardEventCallback(UniJSCallback callback) {
        cardJsCallback = callback;
        // 冷启动点卡片时事件已被暂存 → uniapp 注册回调的这一刻补发（见 dispatchCardEvent）
        flushPendingCardEvent();
    }

    /**
     * 把 uniapp 主界面拉到前台（冷启动点卡片时 uniapp 可能还没被创建）。
     *
     * <p>⚠️ **只用 launcher intent 不够**（实测踩过）：任务栈已存在时，launcher intent 只是把该任务置前，
     * **不会真的创建 uniapp 宿主 Activity** —— 表现为日志打了 20+ 次「已拉起 uniapp 主界面」，
     * 界面却一直停在原生聊天页（`cardJsCallback` 始终 false）。
     * 所以这里**直接按类名启动 uniapp 宿主** `io.dcloud.PandoraEntryActivity`（debug 基座的
     * `PullDebugActivity` 只是 HBuilderX 的调试入口，不能当"主界面"用），失败才回退 launcher intent。
     */
    private void bringUniappToFront() {
        try {
            android.content.Context ctx = dialogContext();
            if (ctx == null && mUniSDKInstance != null) {
                ctx = mUniSDKInstance.getContext();
            }
            if (ctx == null) {
                android.util.Log.w("CardBridge", "bringUniappToFront: ctx 为空");
                return;
            }
            boolean started = false;
            // 按这个顺序试：① PandoraEntry = DCloud 的正式入口（release 的 launcher；debug 基座里也存在，
            // HBuilderX 的 PullDebugActivity 同步完就是跳它）→ ② PandoraEntryActivity = uniapp 宿主 Activity
            final String[] candidates = {"io.dcloud.PandoraEntry", "io.dcloud.PandoraEntryActivity"};
            for (String name : candidates) {
                try {
                    Class<?> host = Class.forName(name);
                    android.content.Intent direct = new android.content.Intent(ctx, host);
                    direct.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(direct);
                    started = true;
                    System.out.println("[CardBridge] 已直接启动 uniapp 入口 " + name);
                    break;
                } catch (Throwable t) {
                    android.util.Log.w("CardBridge", "启动 " + name + " 失败: " + t);
                }
            }
            if (!started) {
                android.content.Intent intent = ctx.getPackageManager()
                    .getLaunchIntentForPackage(ctx.getPackageName());
                if (intent == null) {
                    android.util.Log.w("CardBridge", "bringUniappToFront: launch intent 为空");
                    return;
                }
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
                System.out.println("[CardBridge] 已用 launcher intent 拉起 uniapp");
            }
            // 不再弹「正在打开…」：拉起成功时 uniapp 会立刻显示（用户反馈该提示多余）
        } catch (Exception e) {
            android.util.Log.w("CardBridge", "bringUniappToFront fail: " + e);
        }
    }

    /** uniapp 注册卡片回调后补发暂存事件（冷启动点卡片场景） */
    private static void flushPendingCardEvent() {
        final String action = sPendingCardAction;
        final String data = sPendingCardData;
        if (action == null) {
            return;
        }
        sPendingCardAction = null;
        sPendingCardData = null;
        System.out.println("[CardBridge] 补发暂存事件 action=" + action);
        // 注册发生在 App.vue onLaunch 期间，页面栈/路由此时才建好 → 略等一拍再发，避免跳转失败
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            try {
                final UniJSCallback cb = cardJsCallback;
                if (cb == null) {
                    System.out.println("[CardBridge] 补发放弃：回调又变 null");
                    return;
                }
                JSONObject payload = data == null ? new JSONObject() : JSON.parseObject(data);
                JSONObject event = new JSONObject();
                event.put("event", action);
                event.put("data", payload);
                cb.invokeAndKeepAlive(event);
                System.out.println("[CardBridge] 补发完成 action=" + action);
                // 与正常路径一致：等 uniapp 开始跳转后再收掉原生聊天页，否则它会压在 uniapp 之上
                ZIMKitMessageActivity.finishCurrent();
            } catch (Exception e) {
                android.util.Log.w("CardBridge", "补发失败: " + e);
            }
        }, 300);
    }

    @UniJSMethod(uiThread = true)
    public void registerFriendEventCallback(UniJSCallback callback) {
        FriendEventBridge.setCallback(callback);
    }

    /** 私聊群自动升级：bizType=private 的群拉人后 → temp（群聊） */
    @UniJSMethod(uiThread = true)
    public void registerGroupBizTypeBridge(UniJSCallback callback) {
        cacheContext(safeContext());
        io.dcloud.uniplugin.groupbridge.GroupBizTypeBridge.register();
        // 群设置页（ZIMKit 原生）动作桥：邀请/踢出/退出
        com.zegocloud.zimkit.services.internal.GroupSettingBridge.setListener(
            new com.zegocloud.zimkit.services.internal.GroupSettingBridge.Listener() {
                @Override
                public void onInvite(String groupId) {                    final String gid = groupId == null ? "" : groupId;
                    try {
                        zimInstance().queryGroupAllAttributes(gid, (g, attrs, e) -> {
                            boolean privateGroup = attrs != null && "private".equals(attrs.get("bizType"));
                            if (privateGroup) {
                                fetchAllGroupMembers(gid, new ArrayList<ZIMGroupMemberInfo>(), 0, members -> {
                                    String self = ZIMKitCore.getInstance().getLocalUser().getId();
                                    String peer = null;
                                    if (members != null) {
                                        for (ZIMGroupMemberInfo info : members) {
                                            if (info != null && info.userID != null
                                                && !info.userID.equals(self)) {
                                                peer = info.userID;
                                                break;
                                            }
                                        }
                                    }
                                    openNativeCreateGroupFromPrivate(peer);
                                });
                            } else {
                                openNativeInvitePicker(gid);
                            }
                        });
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                @Override
                public void onInvitePeer(String peerUserId) {
                    openNativeCreateGroupFromPrivate(peerUserId);
                }

                @Override
                public void onKick(String groupId) {
                    openNativeKickPicker(groupId);
                }

                @Override
                public void onExit(String groupId) {
                    exitGroupSmart(groupId, new UniJSCallback() {
                        @Override
                        public void invokeAndKeepAlive(Object data) {
                            JSONObject res = (JSONObject) data;
                            boolean ok = res != null && res.getBoolean("success");
                            String msg = res == null ? "" : res.getString("message");
                            if (ok) {
                                toast(msg == null || msg.isEmpty() ? "已退出群聊" : msg);
                                // 退出成功：关闭设置页+聊天页，回到社群首页（会话已删除，列表秒级刷新）
                                try {
                                    com.zegocloud.zimkit.components.message.ui.ZIMKitGroupChatSettingActivity
                                        .finishCurrent();
                                } catch (Exception ignored) {
                                }
                                try {
                                    ZIMKitMessageActivity.finishCurrent();
                                } catch (Exception ignored) {
                                }
                            } else {
                                toast("退出失败");
                            }
                        }

                        @Override
                        public void invoke(Object data) {
                            invokeAndKeepAlive(data);
                        }
                    });
                }

                @Override
                public void onMemberClick(String groupId, String memberUserId) {
                    openMemberInfoInternal(groupId, memberUserId);
                }

                @Override
                public void onQrcode(String groupId, String groupName) {
                    // 群设置「群二维码」栏 → 原生群二维码页（与 uniapp 页面同款样式）
                    try {
                        Context ctx = safeContext();
                        if (ctx == null) {
                            android.util.Log.w("GroupQrcode", "open failed: context null");
                            return;
                        }
                        io.dcloud.uniplugin.activity.GroupQrcodeActivity.start(ctx, groupId, groupName);
                    } catch (Exception e) {
                        android.util.Log.e("GroupQrcode", "open fail: " + e.getMessage());
                    }
                }
            });
        registerKickCleanupListener();
        // 社群邀请卡片：确认 → 直接申请/加入（不跳 uniapp 页面）
        com.zegocloud.zimkit.services.internal.CommunityInviteBridge.setListener(
            (groupId, cb) -> applyCommunityInvite(groupId, cb));
        if (callback != null) {
            JSONObject result = new JSONObject();
            result.put("success", true);
            callback.invoke(result);
        }
    }

    /** 社群邀请确认：直接走 ZIM 申请/直接加入（免审核时补后端成员同步） */
    private static void applyCommunityInvite(final String groupId,
        final com.zegocloud.zimkit.services.internal.CommunityInviteBridge.Callback cb) {
        try {
            im.zego.zim.entity.ZIMGroupJoinApplicationSendConfig cfg =
                new im.zego.zim.entity.ZIMGroupJoinApplicationSendConfig();
            cfg.wording = "";
            zimInstance().sendGroupJoinApplication(groupId, cfg,
                new im.zego.zim.callback.ZIMGroupJoinApplicationSentCallback() {
                    @Override
                    public void onGroupJoinApplicationSent(String gid, ZIMError e) {
                        if (e != null && e.code == ZIMErrorCode.SUCCESS) {
                            // 申请成功：判断是否已直接入群（ANY 群）→ 已入群=成功，否则=待审核
                            checkJoinedThenReply(groupId, cb);
                            return;
                        }
                        String msg = e == null ? "" : e.message;
                        if (msg != null && (msg.contains("108037") || msg.toLowerCase().contains("belong"))) {
                            reply(cb, true, "已加入");
                            return;
                        }
                        reply(cb, false, msg == null || msg.isEmpty() ? "申请失败" : msg);
                    }
                });
        } catch (Exception e) {
            reply(cb, false, "申请失败：" + e.getMessage());
        }
    }

    private static void checkJoinedThenReply(final String groupId,
        final com.zegocloud.zimkit.services.internal.CommunityInviteBridge.Callback cb) {
        zimInstance().queryGroupInfo(groupId, (info, e) -> {
            boolean joined = e != null && e.code == ZIMErrorCode.SUCCESS;
            if (joined) {
                // 免审核直接进：补后端业务成员同步（后端据此发「我加入了社群」）
                syncGroupMemberToBackend(groupId);
                reply(cb, true, "已加入");
            } else {
                reply(cb, false, "已提交申请");
            }
        });
    }

    /**
     * 登记 SDK 创建的临时群到后端本地库（**最小实现**）。
     *
     * <p>接口：`POST /buyer/social/temp-group/register`
     * 文档原文：`前端用 ZIM SDK createGroup 成功后立刻回调本接口，把 groupId/名称/头像/成员写入本地。
     * 幂等：已存在则补成员与资料。有本地记录后扫码解析、join、详情才可用。`
     *
     * <p><b>为什么必须调</b>：群是客户端用 ZIM SDK 建的（保留自造 groupId / 2 人复用逻辑），
     * 后端不知道这个群 → `li_group` 无记录 → 二维码/扫码解析/join/群详情全部失败。
     *
     * <p>只传 `groupId` + `memberIds`：
     * 群名/群头像按文档由**后端从 ZIM 补**（`不传则尝试从 ZIM 补，仍空则用「xxx的临时群」`），
     * 所以这里不传，避免和 ZIM 里的实际值打架。
     * 成员 id 统一剥掉 `user_` 前缀；自己不用传（后端会补）。
     *
     * @param memberIds 群内成员（除自己外；ZIM userId 或裸 memberId 都接受；可为 null 让后端自己拉）
     */
    private static void registerTempGroupToBackend(String groupId, java.util.List<String> memberIds) {
        try {
            if (groupId == null || groupId.isEmpty()) {
                return;
            }
            JSONObject body = new JSONObject();
            body.put("groupId", groupId);
            if (memberIds != null && !memberIds.isEmpty()) {
                com.alibaba.fastjson.JSONArray ids = new com.alibaba.fastjson.JSONArray();
                for (String raw : memberIds) {
                    String mid = io.dcloud.uniplugin.memberpicker.Member.stripZimPrefix(raw);
                    if (mid != null && !mid.isEmpty() && !ids.contains(mid)) {
                        ids.add(mid);
                    }
                }
                if (!ids.isEmpty()) {
                    body.put("memberIds", ids);
                }
            }
            final String gid = groupId;
            android.util.Log.i("GroupRegister", "register gid=" + gid + " body=" + body.toJSONString());
            io.dcloud.uniplugin.others.RedPacketApi.post(
                getBusinessBaseUrl() + "/social/temp-group/register", body,
                new io.dcloud.uniplugin.others.RedPacketApi.Callback() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        // 登记成功顺手把 qrContent 记下来（二维码页可直接用，省一次 GET）
                        String qr = result == null ? null : result.getString("qrContent");
                        android.util.Log.i("GroupRegister", "ok gid=" + gid + " qrContent=" + qr);
                        if (qr != null && !qr.isEmpty()) {
                            tempGroupQrCache.put(gid, qr);
                        }
                    }

                    @Override
                    public void onError(int code, String message) {
                        // 失败必须能定位：没登记成功 → 该群二维码/扫码/详情都会不可用
                        android.util.Log.w("GroupRegister", "FAIL gid=" + gid
                            + " code=" + code + " msg=" + message);
                    }
                });
        } catch (Exception e) {
            android.util.Log.w("GroupRegister", "exception gid=" + groupId + " err=" + e);
        }
    }

    /** 登记时后端返回的 qrContent 缓存：{groupId → TEMP_GROUP:xxx}，二维码页优先用它 */
    private static final java.util.Map<String, String> tempGroupQrCache =
        new java.util.concurrent.ConcurrentHashMap<>();

    /** 供原生页读取已缓存的二维码字符串（没登记过则为 null，调用方再走 GET 详情） */
    public static String getCachedTempGroupQr(String groupId) {
        return groupId == null ? null : tempGroupQrCache.get(groupId);
    }

    /**
     * 免审核加入后：POST /social/group/member/sync { groupId, userIds:[myMemberId] }
     *
     * 三条进群路径之一（原生邀请卡点「申请加入」且免审核直接进）必须同步，
     * 否则后端本地成员表（li_group_member）缺人 → 成员列表/人数/「我加入了社群」都不对。
     * 前缀：统一剥掉 user_（文档允许带或不带，统一不带）。
     *
     * <p>注意与 {@link #registerTempGroupToBackend} 的区别：
     * 这个是**用户入群后补成员**（ZIM 入群后调用）；那个是**建群后建档**。
     */
    private static void syncGroupMemberToBackend(String groupId) {
        try {
            String self = io.dcloud.uniplugin.memberpicker.Member.stripZimPrefix(getLocalUserId());
            if (groupId == null || groupId.isEmpty() || self == null || self.isEmpty()) {
                android.util.Log.w("MemberSync", "skip: gid=" + groupId + " memberId=" + self);
                return;
            }
            JSONObject body = new JSONObject();
            body.put("groupId", groupId);
            com.alibaba.fastjson.JSONArray ids = new com.alibaba.fastjson.JSONArray();
            ids.add(self);
            body.put("userIds", ids);
            io.dcloud.uniplugin.others.RedPacketApi.post(
                getBusinessBaseUrl() + "/social/group/member/sync", body,
                new io.dcloud.uniplugin.others.RedPacketApi.Callback() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        android.util.Log.i("MemberSync", "ok gid=" + groupId + " memberId=" + self);
                    }

                    @Override
                    public void onError(int code, String message) {
                        android.util.Log.w("MemberSync", "fail gid=" + groupId + " memberId=" + self
                            + " code=" + code + " msg=" + message);
                    }
                });
        } catch (Exception e) {
            android.util.Log.w("MemberSync", "exception gid=" + groupId + " err=" + e);
        }
    }

    private static void reply(com.zegocloud.zimkit.services.internal.CommunityInviteBridge.Callback cb,
        boolean joined, String message) {
        if (cb == null) {
            return;
        }
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> cb.onResult(joined, message));
    }

    /** 被踢出群：立即删除本地会话（列表不再显示该群） */
    private void registerKickCleanupListener() {
        try {
            com.zegocloud.zimkit.services.ZIMKit.registerZIMKitDelegate(
                new com.zegocloud.zimkit.services.ZIMKitDelegate() {
                    @Override
                    public void onGroupMemberStateChanged(im.zego.zim.enums.ZIMGroupMemberState state,
                        im.zego.zim.enums.ZIMGroupMemberEvent event, ArrayList<ZIMGroupMemberInfo> userList,
                        ZIMGroupOperatedInfo operatedInfo, String groupID) {
                        if (event == im.zego.zim.enums.ZIMGroupMemberEvent.KICKED_OUT && userList != null) {
                            String self = ZIMKitCore.getInstance().getLocalUser().getId();
                            for (ZIMGroupMemberInfo info : userList) {
                                if (info != null && self != null && self.equals(info.userID)) {
                                    android.util.Log.d("PrivateGroup", "kicked out, remove conv=" + groupID);
                                    deleteConversationQuiet(groupID, ZIMConversationType.GROUP);
                                    break;
                                }
                            }
                        }
                    }
                });
        } catch (Exception ignored) {
        }
    }

    private void openNativeInvitePicker(String groupId) {
        try {
            JSONObject opts = new JSONObject();
            opts.put("dataSource", "FRIENDS");
            opts.put("mode", "multi");
            opts.put("title", "邀请好友");
            opts.put("excludeGroupId", groupId == null ? "" : groupId);
            opts.put("scene", "invite");
            opts.put("action", "INVITE");
            launchNativePicker(opts, new UniJSCallback() {
                @Override
                public void invokeAndKeepAlive(Object data) {
                    JSONObject res = (JSONObject) data;
                    List<String> ids = parseMembersToIds(res);
                    if (!ids.isEmpty()) {
                        inviteUsersToGroup(groupId, quotesJson(ids), null);
                        toast("已邀请 " + ids.size() + " 人");
                    }
                }

                @Override
                public void invoke(Object data) {
                    invokeAndKeepAlive(data);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 私聊（private）拉人：新建群聊（包含原对端 + 选择的好友），原私聊保留 */
    private void openNativeCreateGroupFromPrivate(String peer) {
        try {
            JSONObject opts = new JSONObject();
            opts.put("dataSource", "FRIENDS");
            opts.put("mode", "multi");
            opts.put("title", "拉人建群");
            opts.put("excludeGroupId", "");
            opts.put("scene", "createGroup");
            opts.put("action", "CREATE_GROUP");
            launchNativePicker(opts, new UniJSCallback() {
                @Override
                public void invokeAndKeepAlive(Object data) {
                    JSONObject res = (JSONObject) data;
                    List<String> ids = parseMembersToIds(res);
                    List<String> members = new ArrayList<>();
                    if (peer != null && !peer.isEmpty()) {
                        members.add(peer);
                    }
                    members.addAll(ids);
                    if (members.isEmpty()) {
                        return;
                    }
                    createNewGroupFromPrivate(members, PRIVATE_CHAT_NAME, null);
                }

                @Override
                public void invoke(Object data) {
                    invokeAndKeepAlive(data);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 拉人进群：直接邀请进当前群（无“私聊/新建群”概念） */
    @UniJSMethod(uiThread = true)
    public void inviteUsersSmart(String groupId, String memberIdsJson, UniJSCallback callback) {
        inviteUsersToGroup(groupId, memberIdsJson, callback);
    }

    /** 创建“群聊”新群（bizType=temp），成功后进入聊天 */
    private void createNewGroupFromPrivate(List<String> members, String name, UniJSCallback callback) {
        try {
            String newGid = "g_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 99999);
            ZIMKit.createGroup(name == null || name.isEmpty() ? PRIVATE_CHAT_NAME : name, newGid,
                new ArrayList<String>(members), new CreateGroupCallback() {
                    @Override
                    public void onCreateGroup(ZIMKitGroupInfo groupInfo, ArrayList<ZIMErrorUserInfo> inviteUserErrors,
                        ZIMError error) {
                        if (error != null && error.code == ZIMErrorCode.SUCCESS) {
                            android.util.Log.d("PrivateGroup", "createGroupFromPrivate ok gid=" + newGid);
                            markGroupBizType(newGid, "temp");
                            // 后端登记（成员 = 被拉的人 + 自己）
                            registerTempGroupToBackend(newGid, members);
                            openGroupChatAndReply(newGid, callback);
                        } else {
                            android.util.Log.d("PrivateGroup", "createGroupFromPrivate fail code="
                                + (error == null ? "" : error.code) + " msg=" + (error == null ? "" : error.message));
                            JSONObject result = new JSONObject();
                            result.put("success", false);
                            result.put("message", error == null ? "创建失败" : error.message);
                            if (callback != null) {
                                callback.invoke(result);
                            }
                        }
                    }
                });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    private void openNativeKickPicker(String groupId) {
        try {
            String selfMemberId = io.dcloud.uniplugin.memberpicker.Member.stripZimPrefix(
                ZIMKitCore.getInstance().getLocalUser().getId());
            JSONObject opts = new JSONObject();
            opts.put("dataSource", "GROUP_MEMBERS");
            opts.put("mode", "multi");
            opts.put("title", "踢出成员");
            opts.put("conversationId", groupId == null ? "" : groupId);
            opts.put("scene", "kick");
            opts.put("action", "KICK");
            JSONArray selfArr = new JSONArray();
            if (selfMemberId != null && !selfMemberId.isEmpty()) {
                selfArr.add(selfMemberId);
            }
            opts.put("excludeIds", selfArr);
            JSONArray roleArr = new JSONArray();
            roleArr.add("OWNER");
            opts.put("excludeRoles", roleArr);
            launchNativePicker(opts, new UniJSCallback() {
                @Override
                public void invokeAndKeepAlive(Object data) {
                    JSONObject res = (JSONObject) data;
                    List<String> ids = parseMembersToIds(res);
                    if (!ids.isEmpty()) {
                        kickGroupMembers(groupId, quotesJson(ids), null);
                        toast("已移出 " + ids.size() + " 人");
                    }
                }

                @Override
                public void invoke(Object data) {
                    invokeAndKeepAlive(data);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void launchNativePicker(JSONObject opts, UniJSCallback onResult) {
        String requestId = "native_" + System.nanoTime();
        opts.put("requestId", requestId);
        opts.put("present", "page");
        if (onResult != null) {
            sMemberPickerCallbacks.put(requestId, onResult);
        }
        io.dcloud.uniplugin.memberpicker.MemberPickerActivity.start(
            mUniSDKInstance.getContext(), opts.toString());
    }

    private List<String> parseMembersToIds(JSONObject res) {
        List<String> ids = new ArrayList<>();
        if (res == null) {
            return ids;
        }
        com.alibaba.fastjson.JSONArray arr = res.getJSONArray("members");
        if (arr != null) {
            for (int i = 0; i < arr.size(); i++) {
                String id = arr.getJSONObject(i).getString("memberId");
                if (id != null && !id.isEmpty()) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private String quotesJson(List<String> ids) {
        JSONArray arr = new JSONArray();
        if (ids != null) {
            for (String id : ids) {
                arr.add(id);
            }
        }
        return arr.toString();
    }

    private void toast(String text) {
        try {
            android.widget.Toast.makeText(mUniSDKInstance.getContext(), text,
                android.widget.Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }

    /** 退出群聊：群主 → 转让（管理员→最早加入→随机）再退出；普通成员直接退出 */
    private void exitGroupSmart(String groupId, UniJSCallback callback) {
        try {
            final String gid = groupId;
            // 诊断 tag `ExitGroup`：退群分支很容易"看起来一样、结果不同"，这里把判据全打出来。
            // adb logcat -s ExitGroup:V
            android.util.Log.i("ExitGroup", "start gid=" + groupId);
            zimInstance().queryGroupAllAttributes(gid, (g, attrs, e) -> {
                boolean isCommunity = attrs != null && "community".equals(attrs.get("bizType"));
                String bizType = attrs == null ? "(attrs null)" : String.valueOf(attrs.get("bizType"));
                fetchAllGroupMembers(gid, new ArrayList<ZIMGroupMemberInfo>(), 0, members -> {
                    int count = members == null ? 0 : members.size();
                    // 先算「我是不是群主」——解散分支也要用它做保险（见下）
                    String selfId = ZIMKitCore.getInstance().getLocalUser().getId();
                    boolean owner = false;
                    boolean selfInList = false;
                    if (members != null) {
                        for (ZIMGroupMemberInfo info : members) {
                            if (info != null && selfId != null && selfId.equals(info.userID)) {
                                selfInList = true;
                                if (info.memberRole == 1) {
                                    owner = true;
                                    break;
                                }
                            }
                        }
                    }
                    android.util.Log.i("ExitGroup", "gid=" + gid + " bizType=" + bizType
                        + " isCommunity=" + isCommunity + " memberCount=" + count
                        + " owner=" + owner + " selfInList=" + selfInList);
                    if (count <= 1) {
                        // 最后一人：群聊→解散；社群频道→不可解散（与社群绑定）
                        if (isCommunity) {
                            JSONObject result = new JSONObject();
                            result.put("success", false);
                            result.put("message", "社群频道与社群绑定，不可解散");
                            if (callback != null) {
                                callback.invoke(result);
                            }
                            return;
                        }
                        // ★ 保险：解散必须**确实是群主**。成员/管理员调 dismissGroup 会被 ZIM 拒绝，
                        //   而回调原来不看错误码 → 会谎报"已解散群聊"并删本地会话（成员实际没退、
                        //   别人视角群还在）。这里提前拦掉，顺便避免误删会话。
                        if (!owner) {
                            android.util.Log.w("ExitGroup", "gid=" + gid
                                + " memberCount<=1 但我不是群主 → 不允许解散，走普通退群");
                            leaveGroupOnly(gid, callback);
                            return;
                        }
                        zimInstance().dismissGroup(gid, (gid2, e2) -> {
                            JSONObject result = new JSONObject();
                            boolean ok = e2 != null && e2.code == ZIMErrorCode.SUCCESS;
                            if (ok) {
                                // 只有真解散成功才删本地会话
                                deleteConversationQuiet(gid2, ZIMConversationType.GROUP);
                                result.put("success", true);
                                result.put("message", "已解散群聊");
                            } else if (isGroupGone(e2)) {
                                // 群本来就不存在：本地会话清掉即可，视为成功
                                clearLocalConversation(gid2);
                                result.put("success", true);
                                result.put("alreadyGone", true);
                                result.put("message", "群聊已不存在，本地会话已清理");
                            } else {
                                // ★ 解散失败必须如实上报，并且**不删本地会话**（否则会骗用户"退了"）
                                android.util.Log.w("ExitGroup", "dismissGroup FAILED gid=" + gid2
                                    + " code=" + (e2 == null ? "null" : e2.code)
                                    + " msg=" + (e2 == null ? "" : e2.message));
                                result.put("success", false);
                                result.put("message", e2 == null ? "解散失败" : ("解散失败：" + e2.message));
                            }
                            if (callback != null) {
                                callback.invoke(result);
                            }
                        });
                        return;
                    }
                    if (owner) {
                        android.util.Log.i("ExitGroup", "gid=" + gid + " → 走【转让并退出】(owner=true)");
                        transferToSuccessor(gid, members, new UniJSCallback() {
                            @Override
                            public void invokeAndKeepAlive(Object data) {
                                deleteConversationQuiet(gid, ZIMConversationType.GROUP);
                                if (callback != null) {
                                    callback.invokeAndKeepAlive(data);
                                }
                            }

                            @Override
                            public void invoke(Object data) {
                                deleteConversationQuiet(gid, ZIMConversationType.GROUP);
                                if (callback != null) {
                                    callback.invoke(data);
                                }
                            }
                        });
                    } else {
                        android.util.Log.i("ExitGroup", "gid=" + gid + " → 走【直接退群 leaveGroupOnly】(owner=false)");
                        leaveGroupOnly(gid, callback);
                    }
                });
            });
        } catch (Exception e) {
            if (callback != null) {
                invokeFail(callback, e);
            }
        }
    }

    /** 群成员摘要（九宫格头像用）：{count, members:[{memberId, userName, avatarUrl}]} */
    @UniJSMethod(uiThread = true)
    public void getGroupMembersSummary(String groupId, UniJSCallback callback) {
        try {
            fetchAllGroupMembers(groupId, new ArrayList<ZIMGroupMemberInfo>(), 0, members -> {
                JSONObject result = new JSONObject();
                result.put("success", true);
                JSONArray arr = new JSONArray();
                if (members != null) {
                    for (ZIMGroupMemberInfo info : members) {
                        if (info == null || info.userID == null) {
                            continue;
                        }
                        JSONObject m = new JSONObject();
                        m.put("memberId", Member.stripZimPrefix(info.userID));
                        m.put("zimUserId", info.userID);
                        String nick = info.memberNickname;
                        if (nick == null || nick.isEmpty()) {
                            nick = info.userName;
                        }
                        m.put("userName", nick == null ? "" : nick);
                        m.put("avatarUrl", info.memberAvatarUrl == null ? "" : info.memberAvatarUrl);
                        arr.add(m);
                    }
                }
                result.put("members", arr);
                result.put("count", arr.size());
                fillMemberProfiles(arr, () -> {
                    if (callback != null) {
                        callback.invoke(result);
                    }
                });
            });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /** 成员资料补全（memberAvatarUrl 常为空）：用 ZIM 用户资料填充头像/昵称 */
    @SuppressWarnings("unchecked")
    private void fillMemberProfiles(JSONArray arr, Runnable done) {
        try {
            ArrayList<String> ids = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                JSONObject m = arr.getJSONObject(i);
                String uid = m.getString("zimUserId");
                String av = m.getString("avatarUrl");
                String nn = m.getString("userName");
                if (uid != null && !uid.isEmpty() && ((av == null || av.isEmpty()) || (nn == null || nn.isEmpty()))) {
                    ids.add(uid);
                }
            }
            if (ids.isEmpty()) {
                done.run();
                return;
            }
            ZIMUsersInfoQueryConfig cfg = new ZIMUsersInfoQueryConfig();
            ZIMKitCore.getInstance().queryUserInfo(ids, cfg, new ZIMUsersInfoQueriedCallback() {
                @Override
                public void onUsersInfoQueried(ArrayList<ZIMUserFullInfo> userList,
                    ArrayList<ZIMErrorUserInfo> errorUserList, ZIMError error) {
                    if (error != null && error.code == ZIMErrorCode.SUCCESS && userList != null) {
                        for (ZIMUserFullInfo u : userList) {
                            if (u == null || u.baseInfo == null) {
                                continue;
                            }
                            for (int i = 0; i < arr.size(); i++) {
                                JSONObject m = arr.getJSONObject(i);
                                if (m.getString("zimUserId") != null
                                    && m.getString("zimUserId").equals(u.baseInfo.userID)) {
                                    m.put("avatarUrl", u.baseInfo.userAvatarUrl == null ? "" : u.baseInfo.userAvatarUrl);
                                    String nm = u.baseInfo.userName;
                                    if (nm == null || nm.isEmpty()) {
                                        m.put("userName", Member.stripZimPrefix(u.baseInfo.userID));
                                    } else {
                                        m.put("userName", nm);
                                    }
                                }
                            }
                        }
                    }
                    done.run();
                }
            });
        } catch (Exception e) {
            done.run();
        }
    }

    /** 立即重推群/单聊会话（加入群后保证聊天列表秒级出现；事件滞后时兜底） */
    @UniJSMethod(uiThread = true)
    public void refreshConversations(UniJSCallback callback) {
        try {
            loadGroupConversations();
            loadPeerConversations();
            if (callback != null) {
                JSONObject r = new JSONObject();
                r.put("success", true);
                callback.invoke(r);
            }
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /** 置顶/取消置顶会话（ZIM 服务端同步） */
    @UniJSMethod(uiThread = true)
    public void updateConversationPinnedState(String conversationId, String type, boolean pinned,
        UniJSCallback callback) {
        try {
            ZIMConversationType t = "peer".equalsIgnoreCase(type == null ? "" : type)
                ? ZIMConversationType.PEER : ZIMConversationType.GROUP;
            zimInstance().updateConversationPinnedState(pinned, conversationId, t,
                new im.zego.zim.callback.ZIMConversationPinnedStateUpdatedCallback() {
                    @Override
                    public void onConversationPinnedStateUpdated(String cid, ZIMConversationType ct, ZIMError e) {
                        JSONObject result = new JSONObject();
                        result.put("success", e != null && e.code == ZIMErrorCode.SUCCESS);
                        result.put("message", e == null ? "" : e.message);
                        if (callback != null) {
                            callback.invoke(result);
                        }
                    }
                });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /** 删除本地会话（聊天列表删除） */
    @UniJSMethod(uiThread = true)
    public void deleteConversationLocal(String conversationId, String type, UniJSCallback callback) {
        try {
            ZIMConversationType t = "peer".equalsIgnoreCase(type == null ? "" : type)
                ? ZIMConversationType.PEER : ZIMConversationType.GROUP;
            zimInstance().deleteConversation(conversationId, t, new ZIMConversationDeleteConfig(),
                new ZIMConversationDeletedCallback() {
                    @Override
                    public void onConversationDeleted(String cid, ZIMConversationType ct, ZIMError e) {
                        JSONObject result = new JSONObject();
                        result.put("success", e != null && e.code == ZIMErrorCode.SUCCESS);
                        result.put("message", e == null ? "" : e.message);
                        if (callback != null) {
                            callback.invoke(result);
                        }
                        // 删除完成后立即重推会话（等 ZIM 事件可能延迟）
                        if (e != null && e.code == ZIMErrorCode.SUCCESS) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                try {
                                    loadGroupConversations();
                                    loadPeerConversations();
                                } catch (Exception ignored) {
                                }
                            });
                        }
                    }
                });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /** 管理社群页：退出社群（群主→转让给 管理员→最早加入→随机，再退出） */
    @UniJSMethod(uiThread = true)
    public void exitGroupFromManage(String groupId, UniJSCallback callback) {
        try {
            exitGroupSmart(groupId, callback);
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /** 打开社群成员资料页（任意原生页面可调用：使用缓存的上下文） */
    public static void openMemberProfile(String groupId, String memberUserId) {
        openMemberProfileWith(sAppContext, groupId, memberUserId);
    }

    /**
     * 打开社群成员资料页。
     * idOrMemberId：ZIM userId（user_xxx）或裸 memberId 都接受，统一归一化成 ZIM userId —— 避免
     * 资料页拿裸 memberId 去 ZIM 成员列表里比对不上而提示「成员不存在」。
     * 调用方直接传上下文最稳（原生 Activity 走这个重载）。
     */
    public static void openMemberProfileWith(Context callerContext, String groupId, String idOrMemberId) {
        android.util.Log.i("MemberClick", "openMemberProfile gid=" + groupId
            + " id=" + idOrMemberId + " ctx=" + (callerContext != null));
        if (groupId == null || groupId.isEmpty() || idOrMemberId == null || idOrMemberId.isEmpty()) {
            android.util.Log.w("MemberInfo", "open skipped: groupId=" + groupId + " id=" + idOrMemberId);
            return;
        }
        try {
            Context ctx = callerContext != null ? callerContext : sAppContext;
            if (ctx == null) {
                android.util.Log.w("MemberInfo", "open failed: context null");
                return;
            }
            MemberInfoActivity.start(ctx, groupId, toZimUserId(idOrMemberId));
        } catch (Exception e) {
            android.util.Log.e("MemberInfo", "open member profile fail: " + e.getMessage());
        }
    }

    /** 打开社群成员信息页（图3/图4：群主看用户 / 用户看群主） */
    @UniJSMethod(uiThread = true)
    public void openMemberInfo(String groupId, String memberUserId, UniJSCallback callback) {
        try {
            openMemberInfoInternal(groupId, memberUserId);
            JSONObject result = new JSONObject();
            result.put("success", true);
            if (callback != null) {
                callback.invoke(result);
            }
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /**
     * 打开「个人信息」原生页（三个入口共用：社群频道成员 / 群聊成员 / 好友联系人）。
     *
     * @param memberUserId ZIM userId（user_xxx）或裸 memberId 都接受
     * @param groupId      群上下文，可空。空 = 从联系人/好友进入 → 不显示身份/加入时间/禁言/踢出
     */
    @UniJSMethod(uiThread = true)
    public void openMemberInfoByZim(String memberUserId, String groupId, UniJSCallback callback) {
        try {
            setContext(mUniSDKInstance.getContext());

            Context ctx = safeContext();
            if (ctx == null) {
                invokeFail(callback, new Exception("context null"));
                return;
            }
            MemberInfoActivity.setMemberInfoCallback((zimid) -> {
                System.out.println("making cash oo");
                System.out.println(zimid);
                openPeerChat(zimid);
            });
            MemberInfoActivity.start(ctx,
                groupId == null ? "" : groupId,
                toZimUserId(memberUserId));
            JSONObject result = new JSONObject();
            result.put("success", true);
            if (callback != null) {
                callback.invoke(result);
            }
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    private void openMemberInfoInternal(String groupId, String memberUserId) {
        android.util.Log.i("MemberClick", "openMemberInfoInternal gid=" + groupId
            + " uid=" + memberUserId + " ctx=" + (sAppContext != null));
        if (groupId == null || groupId.isEmpty() || memberUserId == null || memberUserId.isEmpty()) {
            android.util.Log.w("MemberInfo", "open skipped: groupId=" + groupId + " memberId=" + memberUserId);
            return;
        }
        try {
            Context ctx = safeContext();
            if (ctx == null) {
                android.util.Log.w("MemberInfo", "open failed: context null");
                return;
            }
            MemberInfoActivity.start(ctx, groupId, memberUserId);
        } catch (Exception e) {
            android.util.Log.e("MemberInfo", "open member info fail: " + e.getMessage());
        }
    }

    /**
     * 原生页发起单聊（供 MemberInfoActivity 的「发消息」按钮调用）。
     * 与 uniapp profile.vue 的 startChat 同口径：
     * 客服（merchant_/customer_）走 PEER 直连；普通用户走「2 人群聊」（自动创建/复用，无需加好友）。
     */
    /**
     * 原生页发起聊天（供 MemberInfoActivity 的「发消息」按钮调用）。
     *
     * <p>与 uniapp profile.vue 的 startChat 同口径：
     * <ul>
     *   <li>客服（merchant_/customer_）→ PEER 直连</li>
     *   <li>普通用户 → **走「2 人复用创建群聊」**（{@link #startTwoPersonChatStatic}）：
     *       优先复用【仅我+对方】的 2 人群，没有就新建「聊天」群。**不要求是好友**。</li>
     * </ul>
     */
    public static void openMemberChat(String zimUserId) {

//        return;
        String peer = zimUserId == null ? "" : zimUserId;

        if (peer.isEmpty()) {
            return;
        }
        try {
            boolean peerChat = peer.startsWith("merchant_") || peer.startsWith("customer_");
            android.util.Log.i("MemberInfo", "openMemberChat peer=" + peer + " peerChat=" + peerChat);
            if (peerChat) {
                openPeerChatStatic(peer);
                return;
            }
            // 复用与 uniapp 桥完全相同的「2 人复用/新建」逻辑
            startTwoPersonChatStatic(peer, null);
        } catch (Exception e) {
            android.util.Log.w("MemberInfo", "openMemberChat fail: " + e.getMessage());
        }
    }

    private static void openPeerChatStatic(String conversationID) {
        TestModule.groupId = "";
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            Context ctx = null;
            try {
                ctx = com.zegocloud.zimkit.common.utils.ZIMKitActivityUtils.getCurrentActivity();
            } catch (Exception ignored) {
            }
            if (ctx == null) {
                ctx = sAppContext;
            }
            if (ctx == null) {
                android.util.Log.w("MemberInfo", "openPeerChat skipped: no context");
                return;
            }
            ZIMKitRouter.toMessageActivity(ctx, conversationID,
                com.zegocloud.zimkit.common.enums.ZIMKitConversationType.ZIMKitConversationTypePeer);
        });
    }

    /**
     * 原生页发好友申请（供 MemberInfoActivity 的「添加好友」按钮调用）。
     * 与 uniapp 的 {@code sendFriendApplication} 同一实现（走 ZIM 好友申请）。
     */
    public static void sendFriendApplicationFromNative(String memberId, JSONObject ignored,
        UniJSCallback callback) {
        try {
            im.zego.zim.entity.ZIMFriendApplicationSendConfig config =
                new im.zego.zim.entity.ZIMFriendApplicationSendConfig();
            config.wording = "";
            im.zego.zim.ZIM.getInstance().sendFriendApplication(toZimUserId(memberId), config,
                (applicationInfo, errorInfo) -> {
                    JSONObject result = new JSONObject();
                    result.put("success", errorInfo == null
                        || errorInfo.code == ZIMErrorCode.SUCCESS);
                    result.put("message", errorInfo == null ? "" : errorInfo.message);
                    if (callback != null) {
                        callback.invoke(result);
                    }
                });
        } catch (Exception e) {
            if (callback != null) {
                JSONObject result = new JSONObject();
                result.put("success", false);
                result.put("message", e.getMessage());
                callback.invoke(result);
            }
        }
    }

    /** 安全上下文：优先 uniapp 实例上下文，兜底启动时缓存的 Application/Activity 上下文 */
    private static Context sAppContext;
    /**
     * 弹窗/原生页专用上下文：必须是**当前在屏幕顶部的 Activity**。
     * 历史 bug：用 safeContext() 拿到的是 uniapp 的 PandoraEntryActivity（即使当前在 ZIM 原生聊天页），
     * Dialog 会挂到那个窗口令牌上 → decorAttached=false、看不到，直到回到 uniapp 页才被合成显示。
     */
    public static Context dialogContext() {
        try {
            Activity top = com.zegocloud.zimkit.common.utils.ZIMKitActivityUtils.getCurrentActivity();
            if (top != null && !top.isFinishing()) {
                return top;
            }
        } catch (Exception e) {
            android.util.Log.w("RedPacketOpen", "dialogContext topActivity fail: " + e);
        }
        if (sAppContext == null) {
            android.util.Log.e("RedPacketOpen", "dialogContext: no cached context either");
        }
        return sAppContext;
    }

    public static void cacheContext(Context context) {
        if (context != null) {
            sAppContext = context;
        }
    }

    private Context safeContext() {
        try {
            if (mUniSDKInstance != null && mUniSDKInstance.getContext() != null) {
                sAppContext = mUniSDKInstance.getContext();
                return sAppContext;
            }
        } catch (Exception ignored) {
        }
        return sAppContext;
    }

    /** 申请加入社群频道群（ZIM 原生申请；需要验证的群走审核） */
    @UniJSMethod(uiThread = true)
    public void sendGroupJoinApplication(String groupId, String wording, UniJSCallback callback) {
        try {
            im.zego.zim.entity.ZIMGroupJoinApplicationSendConfig cfg =
                new im.zego.zim.entity.ZIMGroupJoinApplicationSendConfig();
            cfg.wording = wording == null ? "" : wording;
            zimInstance().sendGroupJoinApplication(groupId, cfg,
                new im.zego.zim.callback.ZIMGroupJoinApplicationSentCallback() {
                    @Override
                    public void onGroupJoinApplicationSent(String gid, ZIMError e) {
                        JSONObject result = new JSONObject();
                        result.put("success", e != null && e.code == ZIMErrorCode.SUCCESS);
                        result.put("message", e == null ? "" : e.message);
                        if (callback != null) {
                            callback.invoke(result);
                        }
                    }
                });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /** 直接进群（无需验证的社群/群） */
    @UniJSMethod(uiThread = true)
    public void joinZimGroup(String groupId, UniJSCallback callback) {
        try {
            zimInstance().joinGroup(groupId, new im.zego.zim.callback.ZIMGroupJoinedCallback() {
                @Override
                public void onGroupJoined(im.zego.zim.entity.ZIMGroupFullInfo groupInfo, ZIMError e) {
                    JSONObject result = new JSONObject();
                    result.put("success", e != null && e.code == ZIMErrorCode.SUCCESS);
                    result.put("message", e == null ? "" : e.message);
                    if (callback != null) {
                        callback.invoke(result);
                    }
                }
            });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /** 同意入群申请（群主/管理员） */
    @UniJSMethod(uiThread = true)
    public void acceptGroupJoinApplication(String fromUserID, String groupId, UniJSCallback callback) {
        try {
            // ZIM 签名：acceptGroupJoinApplication(userID, groupID, ...) —— 注意顺序！
            zimInstance().acceptGroupJoinApplication(fromUserID, groupId,
                new im.zego.zim.entity.ZIMGroupJoinApplicationAcceptConfig(),
                new im.zego.zim.callback.ZIMGroupJoinApplicationAcceptedCallback() {
                    @Override
                    public void onGroupJoinApplicationAccepted(String gid, String uid, ZIMError e) {
                        JSONObject result = new JSONObject();
                        result.put("success", e != null && e.code == ZIMErrorCode.SUCCESS);
                        result.put("message", e == null ? "" : e.message);
                        if (callback != null) {
                            callback.invoke(result);
                        }
                    }
                });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /** 拒绝入群申请（群主/管理员） */
    @UniJSMethod(uiThread = true)
    public void rejectGroupJoinApplication(String fromUserID, String groupId, UniJSCallback callback) {
        try {
            // ZIM 签名：rejectGroupJoinApplication(userID, groupID, ...) —— 注意顺序！
            zimInstance().rejectGroupJoinApplication(fromUserID, groupId,
                new im.zego.zim.entity.ZIMGroupJoinApplicationRejectConfig(),
                new im.zego.zim.callback.ZIMGroupJoinApplicationRejectedCallback() {
                    @Override
                    public void onGroupJoinApplicationRejected(String gid, String uid, ZIMError e) {
                        JSONObject result = new JSONObject();
                        result.put("success", e != null && e.code == ZIMErrorCode.SUCCESS);
                        result.put("message", e == null ? "" : e.message);
                        if (callback != null) {
                            callback.invoke(result);
                        }
                    }
                });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /** 判断用户是否已经是该群成员（queryGroupInfo 成功=成员；108035=非成员） */
    @UniJSMethod(uiThread = true)
    public void isJoinedGroup(String groupId, UniJSCallback callback) {
        try {
            zimInstance().queryGroupInfo(groupId, new im.zego.zim.callback.ZIMGroupInfoQueriedCallback() {
                @Override
                public void onGroupInfoQueried(im.zego.zim.entity.ZIMGroupFullInfo info, ZIMError e) {
                    JSONObject result = new JSONObject();
                    boolean inGroup = e != null && e.code == ZIMErrorCode.SUCCESS;
                    result.put("success", true);
                    result.put("inGroup", inGroup);
                    result.put("message", e == null ? "" : e.message);
                    if (callback != null) {
                        callback.invoke(result);
                    }
                }
            });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /** 调试：查询群 joinMode/属性（确认后端是否开了 ZIM 审核 AUTH） */
    @UniJSMethod(uiThread = true)
    public void debugGroupInfo(String groupId, UniJSCallback callback) {
        try {
            zimInstance().queryGroupInfo(groupId, new im.zego.zim.callback.ZIMGroupInfoQueriedCallback() {
                @Override
                public void onGroupInfoQueried(im.zego.zim.entity.ZIMGroupFullInfo info, ZIMError e) {
                    JSONObject result = new JSONObject();
                    try {
                        if (e != null && e.code == ZIMErrorCode.SUCCESS && info != null) {
                            String mode = info.verifyInfo == null
                                || info.verifyInfo.joinMode == null ? "unknown" : info.verifyInfo.joinMode.name();
                            result.put("jointMode", mode);
                            result.put("groupName", info.baseInfo == null ? "" : info.baseInfo.groupName);
                            result.put("groupId", groupId);
                            if (info.groupAttributes != null) {
                                JSONObject attrs = new JSONObject();
                                for (String k : info.groupAttributes.keySet()) {
                                    attrs.put(k, info.groupAttributes.get(k));
                                }
                                result.put("attributes", attrs);
                            } else {
                                result.put("attributes", new JSONObject());
                            }
                            result.put("createTime", info.createTime);
                            android.util.Log.i("TestModule",
                                "[debugGroupInfo] group=" + groupId + " joinMode=" + mode);
                        } else {
                            result.put("joinMode", "error");
                            result.put("message", e == null ? "" : e.message);
                        }
                    } catch (Exception ex) {
                        android.util.Log.e("TestModule", "[debugGroupInfo] parse fail", ex);
                    }
                    result.put("success", true);
                    if (callback != null) {
                        callback.invoke(result);
                    }
                }
            });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /** 群申请列表（ZIM 全部申请：type/state 由前端过滤） */
    @UniJSMethod(uiThread = true)
    public void queryGroupApplications(UniJSCallback callback) {
        try {
            final JSONArray arr = new JSONArray();
            queryGroupApplicationsPage(0, arr, callback);
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    private void queryGroupApplicationsPage(final int nextFlag, final JSONArray arr,
        final UniJSCallback callback) {
        im.zego.zim.entity.ZIMGroupApplicationListQueryConfig cfg =
            new im.zego.zim.entity.ZIMGroupApplicationListQueryConfig();
        cfg.count = 100;
        cfg.nextFlag = nextFlag;
        zimInstance().queryGroupApplicationList(cfg,
            new im.zego.zim.callback.ZIMGroupApplicationListQueriedCallback() {
                @Override
                public void onGroupApplicationListQueried(
                    ArrayList<im.zego.zim.entity.ZIMGroupApplicationInfo> list, int nf, ZIMError e) {
                    if (e != null && e.code == ZIMErrorCode.SUCCESS && list != null) {
                        for (im.zego.zim.entity.ZIMGroupApplicationInfo app : list) {
                            if (app == null) continue;
                            JSONObject o = new JSONObject();
                            String fromUid = app.applyUser == null ? "" : app.applyUser.userID;
                            String fromName = app.applyUser == null ? "" : app.applyUser.userName;
                            String fromAv = app.applyUser == null ? "" : app.applyUser.userAvatarUrl;
                            o.put("fromUserID", fromUid == null ? "" : fromUid);
                            o.put("nickName", fromName == null ? "" : fromName);
                            o.put("avatar", fromAv == null ? "" : fromAv);
                            o.put("groupId", app.groupInfo == null ? "" : app.groupInfo.groupID);
                            o.put("groupName", app.groupInfo == null ? "" : app.groupInfo.groupName);
                            o.put("message", app.wording == null ? "" : app.wording);
                            o.put("createTime", app.createTime);
                            o.put("state", app.state == null ? "" : app.state.name());
                            o.put("type", app.type == null ? "" : app.type.name());
                            arr.add(o);
                        }
                    }
                    if (nf != 0 && arr.size() < 500) {
                        queryGroupApplicationsPage(nf, arr, callback);
                    } else {
                        JSONObject result = new JSONObject();
                        result.put("success", true);
                        result.put("list", arr);
                        result.put("count", arr.size());
                        if (callback != null) {
                            callback.invoke(result);
                        }
                    }
                }
            });
    }

    /** 群全员禁言/解除禁言 */
    @UniJSMethod(uiThread = true)
    public void setGroupMuteAll(String groupId, boolean mute, UniJSCallback callback) {
        try {
            // ZIM muteGroup 的 config.mode 必填（null 会在 SDK 内部 NPE：ZIMGroupMuteMode.value()）
            // duration 不能为 0（SDK 校验 "mute duration can not be 0"）；开启了给 30 天，接触用 None+0
            ZIMGroupMuteConfig cfg = new ZIMGroupMuteConfig();
            cfg.mode = mute ? im.zego.zim.enums.ZIMGroupMuteMode.All
                : im.zego.zim.enums.ZIMGroupMuteMode.None;
            cfg.duration = mute ? 30 * 24 * 60 * 60 : 0;
            zimInstance().muteGroup(mute, groupId, cfg,
                new im.zego.zim.callback.ZIMGroupMutedCallback() {
                    @Override
                    public void onGroupMuted(String gid, boolean muted,
                        im.zego.zim.entity.ZIMGroupMuteInfo info, ZIMError e) {
                        JSONObject result = new JSONObject();
                        result.put("success", e != null && e.code == ZIMErrorCode.SUCCESS);
                        result.put("message", e == null ? "" : e.message);
                        if (callback != null) {
                            callback.invoke(result);
                        }
                    }
                });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /** 被禁言成员列表（禁言用户人数/图5数据） */
    @UniJSMethod(uiThread = true)
    public void getMutedMemberIds(String groupId, UniJSCallback callback) {
        try {
            im.zego.zim.entity.ZIMGroupMemberMutedListQueryConfig config =
                new im.zego.zim.entity.ZIMGroupMemberMutedListQueryConfig();
            zimInstance().queryGroupMemberMutedList(groupId, config,
                new im.zego.zim.callback.ZIMGroupMemberMutedListQueriedCallback() {
                    @Override
                    public void onGroupMemberListQueried(String gid, long muteUntil,
                        ArrayList<ZIMGroupMemberInfo> memberList, ZIMError e) {
                        JSONObject result = new JSONObject();
                        result.put("success", true);
                        com.alibaba.fastjson.JSONArray arr = new com.alibaba.fastjson.JSONArray();
                        if (e != null && e.code == ZIMErrorCode.SUCCESS && memberList != null) {
                            for (ZIMGroupMemberInfo info : memberList) {
                                if (info != null && info.userID != null) {
                                    JSONObject m = new JSONObject();
                                    m.put("memberId", Member.stripZimPrefix(info.userID));
                                    m.put("zimUserId", info.userID);
                                    arr.add(m);
                                }
                            }
                        }
                        result.put("members", arr);
                        result.put("count", arr.size());
                        if (callback != null) {
                            callback.invoke(result);
                        }
                    }
                });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    private void deleteConversationQuiet(String id, ZIMConversationType type) {
        try {
            zimInstance().deleteConversation(id, type, new ZIMConversationDeleteConfig(),
                new ZIMConversationDeletedCallback() {
                    @Override
                    public void onConversationDeleted(String cid, ZIMConversationType t, ZIMError e) {
                    }
                });
        } catch (Exception ignored) {
        }
    }

    /** 置顶会话列表（ZIM 服务端，跨端同步） */
    @UniJSMethod(uiThread = true)
    public void getPinnedConversations(UniJSCallback callback) {
        try {
            ZIMConversationQueryConfig config = new ZIMConversationQueryConfig();
            config.count = 100;
            zimInstance().queryConversationPinnedList(config, (list, errorInfo) -> {
                JSONObject result = new JSONObject();
                result.put("success", errorInfo != null && errorInfo.code == ZIMErrorCode.SUCCESS);
                JSONArray arr = new JSONArray();
                if (errorInfo != null && errorInfo.code == ZIMErrorCode.SUCCESS && list != null) {
                    for (im.zego.zim.entity.ZIMConversation c : list) {
                        JSONObject o = new JSONObject();
                        o.put("conversationID", c.conversationID == null ? "" : c.conversationID);
                        o.put("conversationName", c.conversationName == null ? "" : c.conversationName);
                        o.put("conversationAvatarUrl", c.conversationAvatarUrl == null ? "" : c.conversationAvatarUrl);
                        o.put("conversationType", c.type == ZIMConversationType.GROUP ? "group" : "peer");
                        o.put("orderKey", c.orderKey);
                        o.put("updateTime", c.lastMessage != null ? c.lastMessage.getTimestamp() : 0L);
                        o.put("muted", c.notificationStatus
                            == im.zego.zim.enums.ZIMConversationNotificationStatus.DO_NOT_DISTURB);
                        o.put("unreadMessageCount", c.unreadMessageCount);
                        arr.add(o);
                    }
                }
                result.put("list", arr);
                result.put("message", errorInfo == null ? "" : errorInfo.message);
                if (callback != null) {
                    callback.invoke(result);
                }
            });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    /** 发送一条单聊文本（好友通过后打招呼用）：会话自动出现在双方聊天列表 */
    @UniJSMethod(uiThread = true)
    public void sendPeerTextMessage(String zimUserId, String text, UniJSCallback callback) {
        try {
            ZIMKit.sendTextMessage(text == null ? "" : text, zimUserId, "", ZIMConversationType.PEER,
                new MessageSentCallback() {
                    @Override
                    public void onMessageSent(ZIMError errorInfo) {
                        JSONObject result = new JSONObject();
                        result.put("success", errorInfo != null && errorInfo.code == ZIMErrorCode.SUCCESS);
                        result.put("message", errorInfo == null ? "" : errorInfo.message);
                        if (callback != null) {
                            callback.invoke(result);
                        }
                    }
                });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    @UniJSMethod(uiThread = true)
    public void openMemberPicker(String optionsJson, UniJSCallback callback) {
        try {
            MemberPickerOptions options = MemberPickerOptions.fromJson(optionsJson);
            if (options == null || options.requestId.isEmpty()) {
                options = MemberPickerOptions.fromJson("");
                options.requestId = "mp_" + System.currentTimeMillis();
            }
            sMemberPickerCallbacks.put(options.requestId, callback);

            if ("sheet".equals(options.present) && mUniSDKInstance.getContext() instanceof Activity) {
                final MemberPickerOptions finalOptions = options;
                MemberPickerBottomSheet.show((Activity) mUniSDKInstance.getContext(), options,
                    (success, canceled, members) -> {
                        JSONObject result = new JSONObject();
                        result.put("requestId", finalOptions.requestId);
                        result.put("success", success);
                        result.put("canceled", canceled);
                        com.alibaba.fastjson.JSONArray arr = new com.alibaba.fastjson.JSONArray();
                        if (members != null) {
                            for (Member m : members) {
                                arr.add(m.toJson());
                            }
                        }
                        result.put("members", arr);
                        UniJSCallback cb = sMemberPickerCallbacks.remove(finalOptions.requestId);
                        if (cb != null) {
                            cb.invokeAndKeepAlive(result);
                        }
                    });
                return;
            }

            // uniapp 默认整页（原生层级高、生命周期稳）
            MemberPickerActivity.start(mUniSDKInstance.getContext(), optionsJson);
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null) {
                JSONObject result = new JSONObject();
                result.put("success", false);
                result.put("canceled", true);
                result.put("members", new com.alibaba.fastjson.JSONArray());
                // 便于定位：整页选择器启动失败的异常类名
                result.put("message", "MemberPickerActivity.start failed: "
                    + (e == null ? "" : e.getClass().getName()));
                callback.invoke(result);
            }
        }
    }

    /** MemberPickerActivity 选中/取消后回传结果（requestId 匹配） */
    public static void deliverMemberPickResult(String requestId, String resultJson) {
        if (requestId == null) {
            return;
        }
        UniJSCallback cb = sMemberPickerCallbacks.remove(requestId);
        if (cb != null) {
            try {
                cb.invokeAndKeepAlive(JSON.parseObject(resultJson));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @UniJSMethod(uiThread = true)
    public void openGroupMembers(String groupId, String title, boolean isAdmin, UniJSCallback callback) {
        try {
            sGroupMembersCallback = callback;
            GroupMembersActivity.start(mUniSDKInstance.getContext(), groupId, title, isAdmin);
            JSONObject result = new JSONObject();
            result.put("success", true);
            if (callback != null) {
                callback.invoke(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (callback != null) {
                JSONObject result = new JSONObject();
                result.put("success", false);
                result.put("message", e.getMessage());
                callback.invoke(result);
            }
        }
    }

    /** 群成员宫格：点击成员头像 → 通知 uniapp 打开成员资料页 */
    public static void deliverGroupMemberClick(String memberId) {
        if (sGroupMembersCallback == null) {
            return;
        }
        JSONObject result = new JSONObject();
        result.put("event", "group_member_click");
        result.put("memberId", memberId == null ? "" : memberId);
        try {
            sGroupMembersCallback.invokeAndKeepAlive(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @UniJSMethod(uiThread = true)
    public void inviteUsersToGroup(String conversationId, String memberIdsJson, UniJSCallback callback) {
        try {
            ArrayList<String> ids = parseZimIds(memberIdsJson);
            zimInstance().inviteUsersIntoGroup(ids, conversationId, new ZIMGroupUsersInvitedCallback() {
                @Override
                public void onGroupUsersInvited(String groupID, ArrayList<ZIMGroupMemberInfo> userList,
                    ArrayList<ZIMErrorUserInfo> errorUserList, ZIMError errorInfo) {
                    JSONObject result = new JSONObject();
                    result.put("success", errorInfo == null || errorInfo.code == ZIMErrorCode.SUCCESS);
                    result.put("message", errorInfo == null ? "" : errorInfo.message);
                    if (userList != null) result.put("invitedCount", userList.size());
                    if (errorUserList != null) result.put("errorCount", errorUserList.size());
                    if (callback != null) callback.invoke(result);
                }
            });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    @UniJSMethod(uiThread = true)
    public void kickGroupMembers(String conversationId, String memberIdsJson, UniJSCallback callback) {
        try {
            ArrayList<String> ids = parseZimIds(memberIdsJson);
            zimInstance().kickGroupMembers(ids, conversationId, new ZIMGroupMemberKickedCallback() {
                @Override
                public void onGroupMemberKicked(String groupID, ArrayList<String> kickedUserIDs,
                    ArrayList<ZIMErrorUserInfo> errorUserList, ZIMError errorInfo) {
                    JSONObject result = new JSONObject();
                    result.put("success", errorInfo == null || errorInfo.code == ZIMErrorCode.SUCCESS);
                    result.put("message", errorInfo == null ? "" : errorInfo.message);
                    if (kickedUserIDs != null) result.put("kickedCount", kickedUserIDs.size());
                    if (errorUserList != null) result.put("errorCount", errorUserList.size());
                    if (callback != null) callback.invoke(result);
                }
            });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    @UniJSMethod(uiThread = true)
    public void muteGroupMembers(String conversationId, String memberIdsJson, boolean isMute, UniJSCallback callback) {
        try {
            ArrayList<String> ids = parseZimIds(memberIdsJson);
            zimInstance().muteGroupMembers(isMute, ids, conversationId, new ZIMGroupMemberMuteConfig(),
                new ZIMGroupMembersMutedCallback() {
                    @Override
                    public void onGroupMembersMuted(String groupID, boolean mute, int mode,
                        ArrayList<String> mutedUserIDs, ArrayList<ZIMErrorUserInfo> errorUserList,
                        ZIMError errorInfo) {
                        JSONObject result = new JSONObject();
                        result.put("success", errorInfo == null || errorInfo.code == ZIMErrorCode.SUCCESS);
                        result.put("message", errorInfo == null ? "" : errorInfo.message);
                        if (mutedUserIDs != null) result.put("mutedCount", mutedUserIDs.size());
                        if (callback != null) callback.invoke(result);
                    }
                });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    private static String md5Hex(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] d = md.digest((s == null ? "" : s).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf((s == null ? "" : s).hashCode());
        }
    }

    /** "[\"10001\",\"10002\"]" -> ["user_10001","user_10002"] */
    private static ArrayList<String> parseZimIds(String memberIdsJson) {        ArrayList<String> ids = new ArrayList<>();
        if (memberIdsJson == null || memberIdsJson.isEmpty()) {
            return ids;
        }
        com.alibaba.fastjson.JSONArray arr = com.alibaba.fastjson.JSONArray.parseArray(memberIdsJson);
        for (int i = 0; i < arr.size(); i++) {
            ids.add(toZimUserId(arr.getString(i)));
        }
        return ids;
    }

    @UniJSMethod(uiThread = true)
    public void sendCustomCard(String payload, int subType, String conversationID, String conversationType,
        UniJSCallback callback) {
        try {
            im.zego.zim.enums.ZIMConversationType type = "group".equals(conversationType)
                ? im.zego.zim.enums.ZIMConversationType.GROUP
                : im.zego.zim.enums.ZIMConversationType.PEER;
            ZIMKit.sendCustomMessage(payload, subType, conversationID, type, error -> {
                boolean success = error == null || error.code == im.zego.zim.enums.ZIMErrorCode.SUCCESS;
                System.out.println("[CardSend] subType=" + subType + " conversationId=" + conversationID
                    + " success=" + success
                    + " error=" + (error == null ? "" : (error.code + ":" + error.message)));
                JSONObject result = new JSONObject();
                result.put("success", success);
                result.put("message", error == null ? "" : error.message);
                if (callback != null) {
                    callback.invoke(result);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject result = new JSONObject();
            result.put("success", false);
            result.put("message", e.getMessage());
            if (callback != null) {
                callback.invoke(result);
            }
        }
    }

    @UniJSMethod(uiThread = true)
    public void updateRedPacketState(String redPacketId, String payloadJson, UniJSCallback callback) {
        try {
            ZIMKitMessageFragment.updateRedPacketState(redPacketId, payloadJson);
            JSONObject result = new JSONObject();
            result.put("success", true);
            if (callback != null) {
                callback.invoke(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject result = new JSONObject();
            result.put("success", false);
            result.put("message", e.getMessage());
            if (callback != null) {
                callback.invoke(result);
            }
        }
    }

    @UniJSMethod(uiThread = true)
    public void getConversationListForShare(UniJSCallback callback) {
        try {
            ZIMConversationQueryConfig config = new ZIMConversationQueryConfig();
            config.count = 100;
            ZegoSignalingPlugin.getInstance().queryConversationList(config, (conversationList, errorInfo) -> {
                com.alibaba.fastjson.JSONArray arr = new com.alibaba.fastjson.JSONArray();
                if (conversationList != null) {
                    for (im.zego.zim.entity.ZIMConversation conv : conversationList) {
                        com.alibaba.fastjson.JSONObject obj = new com.alibaba.fastjson.JSONObject();
                        obj.put("conversationID", conv.conversationID);
                        String name = conv.conversationName;
                        obj.put("conversationName", (name == null || name.isEmpty()) ? conv.conversationID : name);
                        obj.put("conversationAvatarUrl",
                            conv.conversationAvatarUrl == null ? "" : conv.conversationAvatarUrl);
                        obj.put("conversationType",
                            conv.type == im.zego.zim.enums.ZIMConversationType.PEER ? "peer" : "group");
                        arr.add(obj);
                    }
                }
                com.alibaba.fastjson.JSONObject result = new com.alibaba.fastjson.JSONObject();
                result.put("list", arr);
                if (callback != null) {
                    callback.invoke(result);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            com.alibaba.fastjson.JSONObject result = new com.alibaba.fastjson.JSONObject();
            result.put("list", new com.alibaba.fastjson.JSONArray());
            if (callback != null) {
                callback.invoke(result);
            }
        }
    }

    @UniJSMethod(uiThread = true)
    public void queryFriendList(UniJSCallback callback) {
        try {
            ZIMFriendListQueryConfig config = new ZIMFriendListQueryConfig();
            config.count = 100;
            zimInstance().queryFriendList(config, (friendList, nextFlag, errorInfo) -> {
                JSONObject result = new JSONObject();
                result.put("success", errorInfo.code == ZIMErrorCode.SUCCESS);
                result.put("list", toFriendJsonArray(friendList));
                result.put("nextFlag", nextFlag);
                result.put("message", errorInfo.message);
                if (callback != null) callback.invoke(result);
            });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    @UniJSMethod(uiThread = true)
    public void sendFriendApplication(String memberId, String applyMessage, UniJSCallback callback) {
        try {
            ZIMFriendApplicationSendConfig config = new ZIMFriendApplicationSendConfig();
            config.wording = applyMessage == null ? "" : applyMessage;
            zimInstance().sendFriendApplication(toZimUserId(memberId), config,
                (applicationInfo, errorInfo) -> {
                    JSONObject result = new JSONObject();
                    result.put("success", errorInfo.code == ZIMErrorCode.SUCCESS);
                    result.put("message", errorInfo.message);
                    if (applicationInfo != null) {
                        result.put("application", applicationInfoToJson(applicationInfo));
                    }
                    if (callback != null) callback.invoke(result);
                });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    @UniJSMethod(uiThread = true)
    public void queryFriendApplicationList(UniJSCallback callback) {
        try {
            ZIMFriendApplicationListQueryConfig config = new ZIMFriendApplicationListQueryConfig();
            config.count = 100;
            zimInstance().queryFriendApplicationList(config,
                (applicationList, nextFlag, errorInfo) -> {
                    JSONObject result = new JSONObject();
                    result.put("success", errorInfo.code == ZIMErrorCode.SUCCESS);
                    result.put("list", toApplicationJsonArray(applicationList));
                    result.put("nextFlag", nextFlag);
                    result.put("message", errorInfo.message);
                    if (callback != null) callback.invoke(result);
                });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    @UniJSMethod(uiThread = true)
    public void acceptFriendApplication(String memberId, UniJSCallback callback) {
        try {
            zimInstance().acceptFriendApplication(toZimUserId(memberId), new ZIMFriendApplicationAcceptConfig(),
                (friendInfo, errorInfo) -> {
                    JSONObject result = new JSONObject();
                    result.put("success", errorInfo.code == ZIMErrorCode.SUCCESS);
                    result.put("message", errorInfo.message);
                    if (friendInfo != null) {
                        result.put("friend", friendInfoToJson(friendInfo));
                    }
                    if (callback != null) callback.invoke(result);
                });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    @UniJSMethod(uiThread = true)
    public void rejectFriendApplication(String memberId, UniJSCallback callback) {
        try {
            zimInstance().rejectFriendApplication(toZimUserId(memberId), new ZIMFriendApplicationRejectConfig(),
                (userInfo, errorInfo) -> {
                    JSONObject result = new JSONObject();
                    result.put("success", errorInfo.code == ZIMErrorCode.SUCCESS);
                    result.put("message", errorInfo.message);
                    if (userInfo != null) {
                        result.put("user", userInfoToJson(userInfo));
                    }
                    if (callback != null) callback.invoke(result);
                });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    @UniJSMethod(uiThread = true)
    public void deleteFriend(String memberId, UniJSCallback callback) {
        try {
            ArrayList<String> ids = new ArrayList<>();
            ids.add(toZimUserId(memberId));
            zimInstance().deleteFriends(ids, new ZIMFriendDeleteConfig(),
                (errorUserList, errorInfo) -> {
                    JSONObject result = new JSONObject();
                    result.put("success", errorInfo.code == ZIMErrorCode.SUCCESS);
                    result.put("message", errorInfo.message);
                    if (callback != null) callback.invoke(result);
                });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    @UniJSMethod(uiThread = true)
    public void searchLocalFriends(String keyword, UniJSCallback callback) {
        try {
            ZIMFriendSearchConfig config = new ZIMFriendSearchConfig();
            config.count = 100;
            config.keywords = new ArrayList<>();
            config.keywords.add(keyword == null ? "" : keyword);
            config.isAlsoMatchFriendAlias = true;
            zimInstance().searchLocalFriends(config, (friendList, nextFlag, errorInfo) -> {
                JSONObject result = new JSONObject();
                result.put("success", errorInfo.code == ZIMErrorCode.SUCCESS);
                result.put("list", toFriendJsonArray(friendList));
                result.put("nextFlag", nextFlag);
                result.put("message", errorInfo.message);
                if (callback != null) callback.invoke(result);
            });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    @UniJSMethod(uiThread = true)
    public void queryUsersInfo(String memberIdsJson, UniJSCallback callback) {
        try {
            List<String> zimIds = new ArrayList<>();
            com.alibaba.fastjson.JSONArray arr = com.alibaba.fastjson.JSONArray.parseArray(memberIdsJson);
            for (int i = 0; i < arr.size(); i++) {
                zimIds.add(toZimUserId(arr.getString(i)));
            }
            ZIMUsersInfoQueryConfig config = new ZIMUsersInfoQueryConfig();
            config.isQueryFromServer = true;
            zimInstance().queryUsersInfo(zimIds, config, new ZIMUsersInfoQueriedCallback() {
                @Override
                public void onUsersInfoQueried(ArrayList<ZIMUserFullInfo> userList,
                    ArrayList<ZIMErrorUserInfo> errorUserList, ZIMError errorInfo) {
                    JSONObject result = new JSONObject();
                    result.put("success", errorInfo.code == ZIMErrorCode.SUCCESS);
                    result.put("list", toUserFullJsonArray(userList));
                    result.put("message", errorInfo.message);
                    if (callback != null) callback.invoke(result);
                }
            });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    // ==================== 好友桥接辅助 ====================

    private static ZIM zimInstance() {
        return ZIMKitCore.getInstance().zim();
    }

    private static String toZimUserId(String memberId) {
        if (memberId == null) return null;
        return memberId.startsWith("user_") ? memberId : ("user_" + memberId);
    }

    private static String toMemberId(String zimUserId) {
        if (zimUserId == null) return null;
        return zimUserId.startsWith("user_") ? zimUserId.substring("user_".length()) : zimUserId;
    }

    private static JSONObject friendInfoToJson(ZIMFriendInfo friend) {
        JSONObject obj = new JSONObject();
        if (friend == null) return obj;
        obj.put("zimUserId", friend.userID == null ? "" : friend.userID);
        obj.put("memberId", toMemberId(friend.userID));
        obj.put("userName", friend.userName == null ? "" : friend.userName);
        obj.put("avatarUrl", friend.userAvatarUrl == null ? "" : friend.userAvatarUrl);
        obj.put("remark", friend.friendAlias == null ? "" : friend.friendAlias);
        obj.put("createTime", friend.createTime);
        return obj;
    }

    private static com.alibaba.fastjson.JSONArray toFriendJsonArray(ArrayList<ZIMFriendInfo> list) {
        com.alibaba.fastjson.JSONArray result = new com.alibaba.fastjson.JSONArray();
        if (list == null) return result;
        for (ZIMFriendInfo friend : list) {
            result.add(friendInfoToJson(friend));
        }
        return result;
    }

    private static JSONObject applicationInfoToJson(ZIMFriendApplicationInfo info) {
        JSONObject obj = new JSONObject();
        if (info == null) return obj;
        ZIMUserInfo applyUser = info.applyUser;
        if (applyUser != null) {
            obj.put("zimUserId", applyUser.userID == null ? "" : applyUser.userID);
            obj.put("memberId", toMemberId(applyUser.userID));
            obj.put("userName", applyUser.userName == null ? "" : applyUser.userName);
            obj.put("avatarUrl", applyUser.userAvatarUrl == null ? "" : applyUser.userAvatarUrl);
        } else {
            obj.put("zimUserId", "");
            obj.put("memberId", "");
            obj.put("userName", "");
            obj.put("avatarUrl", "");
        }
        obj.put("wording", info.wording == null ? "" : info.wording);
        obj.put("createTime", info.createTime);
        obj.put("updateTime", info.updateTime);
        obj.put("state", info.state == null ? "" : info.state.name());
        obj.put("type", info.type == null ? "" : info.type.name());
        return obj;
    }

    private static com.alibaba.fastjson.JSONArray toApplicationJsonArray(ArrayList<ZIMFriendApplicationInfo> list) {
        com.alibaba.fastjson.JSONArray result = new com.alibaba.fastjson.JSONArray();
        if (list == null) return result;
        for (ZIMFriendApplicationInfo info : list) {
            result.add(applicationInfoToJson(info));
        }
        return result;
    }

    private static JSONObject userInfoToJson(ZIMUserInfo user) {
        JSONObject obj = new JSONObject();
        if (user == null) return obj;
        obj.put("zimUserId", user.userID == null ? "" : user.userID);
        obj.put("memberId", toMemberId(user.userID));
        obj.put("userName", user.userName == null ? "" : user.userName);
        obj.put("avatarUrl", user.userAvatarUrl == null ? "" : user.userAvatarUrl);
        return obj;
    }

    private static JSONObject userFullInfoToJson(ZIMUserFullInfo user) {
        JSONObject obj = new JSONObject();
        if (user == null) return obj;
        obj.put("zimUserId", user.baseInfo == null ? "" : user.baseInfo.userID);
        obj.put("memberId", user.baseInfo == null ? "" : toMemberId(user.baseInfo.userID));
        obj.put("userName", user.baseInfo == null ? "" : user.baseInfo.userName);
        obj.put("avatarUrl", user.userAvatarUrl == null ? "" : user.userAvatarUrl);
        obj.put("extendedData", user.extendedData == null ? "" : user.extendedData);
        return obj;
    }

    private static com.alibaba.fastjson.JSONArray toUserFullJsonArray(ArrayList<ZIMUserFullInfo> list) {
        com.alibaba.fastjson.JSONArray result = new com.alibaba.fastjson.JSONArray();
        if (list == null) return result;
        for (ZIMUserFullInfo user : list) {
            result.add(userFullInfoToJson(user));
        }
        return result;
    }

    private static void invokeFail(UniJSCallback callback, Exception e) {
        e.printStackTrace();
        JSONObject result = new JSONObject();
        result.put("success", false);
        result.put("message", e.getMessage());
        if (callback != null) {
            callback.invoke(result);
        }
    }

    private void dispatchCardEvent(String action, String data) {
        System.out.println("[CardBridge] dispatch action=" + action + ", cardJsCallback=" + (cardJsCallback != null));
        try {
            JSONObject payload = data == null ? new JSONObject() : JSON.parseObject(data);
            // 红包页改为原生 Activity：直接从聊天页 startActivity，返回自动回到原聊天
            if ("send_red_packet".equals(action)) {
                RedPacketSendActivity.start(mUniSDKInstance.getContext(),
                    payload.getString("conversationId"), payload.getString("conversationType"));
                return;
            }
            if ("open_card".equals(action) && "red_packet".equals(payload.getString("cardType"))) {
                JSONObject detail = payload.getJSONObject("detail");
                JSONObject sender = payload.getJSONObject("sender");
                String rpId = detail == null ? "" : detail.getString("redPacketId");
                // 容错：历史卡片里可能被拼上 "?nonce=..."（签名串），截断取纯 ID
                if (rpId != null && rpId.contains("?")) {
                    rpId = rpId.substring(0, rpId.indexOf('?'));
                }
                // 红包卡片：打开沉浸式弹窗（可领取/已领取/已被领完/专属 各状态）
                try {
                    final android.content.Context rpCtx = dialogContext();
                    final String fRpId = rpId;
                    final String fConvId = payload.getString("conversationId");
                    final String fConvType = payload.getString("conversationType");
                    final String fSenderId = sender == null ? "" : sender.getString("userId");
                    final String fSenderName = sender == null ? "" : sender.getString("userName");
                    final String fSenderAvatar = sender == null ? "" : sender.getString("avatarUrl");
                    android.util.Log.i("RedPacketOpen", "dispatch ctx="
                        + (rpCtx == null ? "null" : rpCtx.getClass().getName())
                        + " rpId=" + fRpId);
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        try {
                            // 全屏 Activity 版（取代原 RedPacketOpenDialog）：
                            // Dialog 的窗口被系统摆在状态栏下方，遮罩碰不到状态栏；Activity 铺满整屏才行。
                            // 旧 Dialog 类先保留（RedPacketOpenDialog.java），确认新版 OK 后再删。
                            io.dcloud.uniplugin.activity.RedPacketFullscreenActivity.show(rpCtx, fRpId,
                                fConvId, fConvType, fSenderId, fSenderName, fSenderAvatar);
                        } catch (Exception e) {
                            android.util.Log.e("RedPacket", "open fullscreen fail: " + e);
                            android.widget.Toast.makeText(rpCtx,
                                "打开红包失败：" + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    android.util.Log.e("RedPacket", "open modal dispatch fail: " + e);
                }
                return;
            }
            if (cardJsCallback == null) {
                // 冷启动（从最近任务恢复到原生聊天页）时 uniapp 还没创建 → 回调未注册。
                // **不能静默 return**：暂存事件 + 主动把 uniapp 拉起来，等它注册回调的瞬间补发。
                sPendingCardAction = action;
                sPendingCardData = data;
                System.out.println("[CardBridge] uniapp 未就绪 → 暂存 action=" + action + "，并拉起 uniapp");
                bringUniappToFront();
                return;
            }
            // 其他卡片（商品/店铺/文章）走 uniapp 页面：先通知 JS，再关闭聊天页（避免 finish 影响回调投递）
            JSONObject event = new JSONObject();
            event.put("event", action);
            event.put("data", payload);
            cardJsCallback.invokeAndKeepAlive(event);
            ZIMKitMessageActivity.finishCurrent();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @UniJSMethod(uiThread = false)
    public void registerGlobalListener() { // 1. ACCEPT THE CALLBACK PARAMETER
        System.out.println("Initializing global real-time event pipeline...");
//        globalJsCallback = callback;

        if (!isDelegateRegistered) {
            ZIMKit.registerZIMKitDelegate(new ZIMKitDelegate() {

                @Override
                public void onConversationListChanged(List<ZIMKitConversation> conversations) {
                    System.out.println("final data here oo");
//                    loadPeerConversations();
////                    if (conversations != null ) {
                        // 2. CONVERT MIXED LIST TO CLEAN JSON STRINGS
//                        com.alibaba.fastjson.JSONArray uniArray = com.alibaba.fastjson.JSONArray.parseArray(
//                                new com.google.gson.Gson().toJson(conversations)
//                        );
//                        System.out.println(uniArray);
//                        System.out.println("final data here oo");
//
//                        com.alibaba.fastjson.JSONObject payload = new com.alibaba.fastjson.JSONObject();
//                        payload.put("list", uniArray);

                        // 3. BLAST MIXED LIST OVER THE BRIDGE TO UNI-APP
//                        com.alibaba.fastjson.JSONObject response = new com.alibaba.fastjson.JSONObject();
//                        response.put("event", "CONVERSATION_CHANGED");
//                        response.put("data", payload);

                        //globalJsCallback.invokeAndKeepAlive(response);
//                    }
                }

                @Override
                public void newChange() {
                    System.out.println("final data here oo");
                    loadPeerConversations();
                    loadGroupConversations();


//
//                    if (conversations != null ) {
//                        // 2. CONVERT MIXED LIST TO CLEAN JSON STRINGS
//                        com.alibaba.fastjson.JSONArray uniArray = com.alibaba.fastjson.JSONArray.parseArray(
//                                new com.google.gson.Gson().toJson(conversations)
//                        );
//                        System.out.println(uniArray);
//                        System.out.println("final data here oo");
//
//                        com.alibaba.fastjson.JSONObject payload = new com.alibaba.fastjson.JSONObject();
//                        payload.put("list", uniArray);
//
//                        // 3. BLAST MIXED LIST OVER THE BRIDGE TO UNI-APP
//                        com.alibaba.fastjson.JSONObject response = new com.alibaba.fastjson.JSONObject();
//                        response.put("event", "CONVERSATION_CHANGED");
//                        response.put("data", payload);
//
//                        //globalJsCallback.invokeAndKeepAlive(response);
//                    }
                }

                @Override
                public void onConnectionStateChange(ZIMConnectionState state, ZIMConnectionEvent event) {
                    System.out.println("SaaS Security Audit -> Connection State: " + state.name() + " | Event: " + event.name());

                    // 1. Initialize our cross-bridge reporting object
                    com.alibaba.fastjson.JSONObject payload = new com.alibaba.fastjson.JSONObject();
                    String actionType = "STATE_UPDATE";

                    // 2. Main Logic Execution Router
                    if (state == ZIMConnectionState.CONNECTED) {
                        // Safe and authenticated active session
                        payload.put("isLoggedIn", true);
                        payload.put("sessionStatus", "LOGGED_IN");

                    } else if (state == ZIMConnectionState.CONNECTING || state == ZIMConnectionState.RECONNECTING) {
                        // App is actively trying to regain socket connection
                        payload.put("isLoggedIn", true);
                        payload.put("sessionStatus", "CONNECTING");

                    } else if (state == ZIMConnectionState.DISCONNECTED) {
                        payload.put("isLoggedIn", false);

                        // Check the EXACT sub-event reason why we are disconnected
                        switch (event) {
                            case KICKED_OUT:
                                // Dual login enforcement breach!
                                actionType = "ACCOUNT_KICKED";
                                payload.put("sessionStatus", "LOGGED_OUT");
                                payload.put("reason", "DUAL_LOGIN");
                                payload.put("msg", "您的账号在其他设备登录，已被迫下线。");
                                break;

                            case TOKEN_EXPIRED:
                                // Security certificate timeout from Mall4j backend gateway
                                actionType = "TOKEN_EXPIRED";
                                payload.put("sessionStatus", "LOGGED_OUT");
                                payload.put("reason", "CERTIFICATE_EXPIRED");
                                payload.put("msg", "会话安全凭证已过期，正在重新获取安全链。");
                                break;

                            case LOGIN_TIMEOUT:
                            case LOGIN_INTERRUPTED:
                                // Network signal drops or firewall blocked connection
                                payload.put("sessionStatus", "NETWORK_ERROR");
                                payload.put("reason", "SIGNAL_FAULT");
                                payload.put("msg", "网络连接异常中断，正在自动排查故障链路。");
                                break;

                            case SUCCESS:
                            case UNREGISTERED:
                            default:
                                // Normal standard intentional user logout event
                                actionType = "USER_LOGOUT";
                                payload.put("sessionStatus", "LOGGED_OUT");
                                payload.put("reason", "MANUAL_SIGNOUT");
                                payload.put("msg", "已安全退出当前会话。");
                                break;
                        }
                    }

                    // 3. Clear CallKit Sessions synchronously if disconnected cleanly or kicked
                    if (state == ZIMConnectionState.DISCONNECTED && (event == ZIMConnectionEvent.SUCCESS || event == ZIMConnectionEvent.KICKED_OUT)) {
                        try {
                            Class<?> adapterClass = Class.forName("im.zego.integration.ZegoPluginAdapter");
                            java.lang.reflect.Method getCallkitMethod = adapterClass.getMethod("callkitPlugin");
                            Object callkitPlugin = getCallkitMethod.invoke(null);
                            if (callkitPlugin != null) {
                                java.lang.reflect.Method logoutMethod = callkitPlugin.getClass().getMethod("logoutUser");
                                logoutMethod.invoke(callkitPlugin);
                            }
                        } catch (Exception e) {
                            System.out.println("Optional CallKit engine detachment skipped.");
                        }
                    }

                    // 4. Send the structured data directly over your persistent uni-app JavaScript bridge
                    if (globalJsCallback != null) {
                        com.alibaba.fastjson.JSONObject finalResponse = new com.alibaba.fastjson.JSONObject();
                        finalResponse.put("event", actionType);
                        finalResponse.put("data", payload);
                        globalJsCallback.invokeAndKeepAlive(finalResponse);
                    }
                }

            });
            isDelegateRegistered = true;
        }
    }

//    @UniJSMethod(uiThread = false)
//    public void loadPeerConversations() {
//        ZIMConversationQueryConfig config = new ZIMConversationQueryConfig();
//        config.count = 100;
//        ZIMConversationFilterOption filterOption = new ZIMConversationFilterOption();
//        filterOption.conversationTypes.add(ZIMConversationType.PEER);
//
//        ZegoSignalingPlugin.getInstance().queryConversationList(config,filterOption, (conversationList, errorInfo) -> {
//            com.alibaba.fastjson.JSONArray uniArray = com.alibaba.fastjson.JSONArray.parseArray(
//                    new com.google.gson.Gson().toJson(conversationList)
//            );
//
//            com.alibaba.fastjson.JSONObject payload = new com.alibaba.fastjson.JSONObject();
//            payload.put("list", uniArray);
//            System.out.println(payload);
//            System.out.println("LOOP UP");
//        });
//    }


    //run ui thread



    @UniJSMethod(uiThread = true)
    public void testAsyncFunc(JSONObject options, UniJSCallback callback) {
        Log.e(TAG, "testAsyncFunc--"+options);
        if(callback != null) {
            JSONObject data = new JSONObject();
            data.put("code", "success");
            callback.invoke(data);
            //callback.invokeAndKeepAlive(data);
        }
    }

    //run JS thread
    @UniJSMethod (uiThread = false)
    public JSONObject testSyncFunc(){
        JSONObject data = new JSONObject();
        data.put("code", "success");
        return data;
    }

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if(requestCode == REQUEST_CODE && data.hasExtra("respond")) {
//            Log.e("TestModule", "原生页面返回----"+data.getStringExtra("respond"));
//        } else {
//            super.onActivityResult(requestCode, resultCode, data);
//        }
//    }

    @UniJSMethod (uiThread = true)
    public void gotoNativePage(){
        if(mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
            Intent intent = new Intent(mUniSDKInstance.getContext(), NativePageActivity.class);
            ((Activity)mUniSDKInstance.getContext()).startActivityForResult(intent, REQUEST_CODE);
        }
    }
}
