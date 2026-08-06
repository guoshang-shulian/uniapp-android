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
import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.utils.ToastX;
import com.netease.yunxin.kit.entertainment.common.RoomConstants;
import com.netease.yunxin.kit.voiceroomkit.ui.AppUtils;
import com.netease.yunxin.kit.voiceroomkit.ui.LoginUtil;
import com.netease.yunxin.kit.entertainment.common.http.ECHttpService;
import com.netease.yunxin.kit.entertainment.common.model.ECModelResponse;
import com.netease.yunxin.kit.entertainment.common.model.NemoAccount;
import com.netease.yunxin.kit.voiceroomkit.ui.activity.VoiceRoomCreateActivity;
import com.netease.yunxin.kit.voiceroomkit.ui.activity.VoiceRoomListActivity;
import com.zegocloud.uikit.plugin.signaling.ZegoSignalingPlugin;
//import com.zegocloud.uikit.prebuilt.liveaudioroom.internal.service.LiveAudioRoomManager;
import com.zegocloud.zimkit.common.ZIMKitRouter;
import com.zegocloud.zimkit.common.enums.ZIMKitConversationType;
import com.zegocloud.zimkit.services.ZIMKit;
import com.zegocloud.zimkit.services.ZIMKitDelegate;
import com.zegocloud.zimkit.services.callback.CreateGroupCallback;
import com.zegocloud.zimkit.services.callback.JoinGroupCallback;
import com.zegocloud.zimkit.services.model.ZIMKitConversation;
import com.zegocloud.zimkit.services.model.ZIMKitGroupInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import im.zego.zim.entity.ZIMConversationFilterOption;
import im.zego.zim.entity.ZIMConversationQueryConfig;
import im.zego.zim.entity.ZIMError;
import im.zego.zim.entity.ZIMErrorUserInfo;
import im.zego.zim.enums.ZIMConnectionEvent;
import im.zego.zim.enums.ZIMConnectionState;
import im.zego.zim.enums.ZIMConversationType;
import im.zego.zim.enums.ZIMErrorCode;
import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;
import io.dcloud.uniplugin.activity.AudioRoomActivity;
import io.dcloud.uniplugin.activity.ConversationActivity;
import io.dcloud.uniplugin.activity.LiveActivity;
import io.dcloud.uniplugin.activity.NativePageActivity;
import retrofit2.Call;
import retrofit2.Callback;


public class TestModule extends UniModule {

    String TAG = "TestModule";
    public static int REQUEST_CODE = 1000;
    private ScheduledExecutorService syncScheduler;

    String user_id;
    String userName;
    String avatar;

    int logged = 0;


    public void neteaseLogin(){
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

    private void createAccount(int sceneType, Callback<ECModelResponse<NemoAccount>> callback) {
        ECHttpService.getInstance().initialize(mUniSDKInstance.getContext(),BASE_URL);
        ECHttpService.getInstance().addHeader("Appkey", "c92ce58a027d659e41ba62da6819fb92");
        ECHttpService.getInstance().addHeader("AppSecret", "22759cffdce7");
        System.out.println("living here");
        ECHttpService.getInstance().createAccount(sceneType, callback);
    }

    public void login(NemoAccount nemoAccount) {
        LoginUtil.loginVoiceRoom(
                mUniSDKInstance.getContext(),
                nemoAccount,
                new LoginUtil.LoginVoiceRoomCallback() {
                    @Override
                    public void onSuccess() {
                        logged = 1;
                        System.out.println("living here - finally in");
                    }

                    @Override
                    public void onError(int errorCode, String errorMsg) {
                        ToastX.showShortToast(errorMsg);
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
    public void startChat(String conversationID) {
        ZIMKitRouter.toMessageActivity(mUniSDKInstance.getContext(), conversationID, ZIMKitConversationType.ZIMKitConversationTypePeer);
    }

    @UniJSMethod(uiThread = true)
    public void startGroupChat(String conversationID) {
        ZIMKitRouter.toMessageActivity(mUniSDKInstance.getContext(), conversationID, ZIMKitConversationType.ZIMKitConversationTypeGroup);
    }

    @UniJSMethod(uiThread = true)
    public void joinGroupChat(String conversationID) {

        ZIMKit.joinGroup(conversationID, new JoinGroupCallback() {
            @Override
            public void onJoinGroup(ZIMKitGroupInfo groupInfo, ZIMError error) {
                if (error.code == ZIMErrorCode.SUCCESS || error.code == ZIMErrorCode.MEMBER_IS_ALREADY_IN_THE_GROUP) {
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

                    // 🏁 STEP 2: Smoothly forward the user straight into the target chat interface screen
                    ZIMKitRouter.toMessageActivity(mUniSDKInstance.getContext(), groupID, ZIMKitConversationType.ZIMKitConversationTypeGroup);

                }
            }
        });
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
        Intent intent = new Intent(context, VoiceRoomListActivity.class);
        intent.putExtra(RoomConstants.INTENT_IS_OVERSEA, AppConfig.isOversea());
        intent.putExtra(RoomConstants.INTENT_KEY_CONFIG_ID, AppConfig.getVoiceRoomConfigId());
        intent.putExtra(RoomConstants.INTENT_USER_NAME, AppUtils.getUserName());
        intent.putExtra(RoomConstants.INTENT_AVATAR, AppUtils.getAvatar());
        context.startActivity(intent);


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
    }

    @UniJSMethod(uiThread = false)
    public void startSyncPipeline(UniJSCallback callback) {
        globalJsCallback = callback;
        neteaseLogin();
        System.out.println("SaaS Data Engine: Sync Pipeline Activated.");
        stopSyncPipeline();
        syncScheduler = Executors.newSingleThreadScheduledExecutor();
        syncScheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("SaaS Auto-Sync: Refreshing peer conversations...");
                    loadPeerConversations();
                    loadGroupConversations();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 30, TimeUnit.SECONDS); // 0 seconds initial delay, runs every 30 seconds
    }

    private void loadGroupConversations() {
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
            if (conversationList != null && globalJsCallback != null) {
                // Bulk serialize using your safe Gson trick
                com.alibaba.fastjson.JSONArray uniArray = com.alibaba.fastjson.JSON.parseArray(
                        new com.google.gson.Gson().toJson(conversationList)
                );
                JSONObject payload = new JSONObject();
                payload.put("list", uniArray);

                // Package into an event channel payload box
                JSONObject eventPayload = new JSONObject();
                eventPayload.put("event", "GROUP");
                eventPayload.put("data", payload);

                // Push straight downstream over your persistent uni-app JavaScript bridge
                globalJsCallback.invokeAndKeepAlive(eventPayload);
                System.out.println(uniArray);
                System.out.println("SaaS Engine Sync Event broadcasted to uni-app workspace.");
            }
        });
    }

    private void loadPeerConversations() {
        ZIMConversationQueryConfig config = new ZIMConversationQueryConfig();
        config.count = 100;

        ZIMConversationFilterOption filterOption = new ZIMConversationFilterOption();
        filterOption.conversationTypes = new ArrayList<>();
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
                JSONObject payload = new JSONObject();
                payload.put("list", uniArray);

                // Package into an event channel payload box
                JSONObject eventPayload = new JSONObject();
                eventPayload.put("event", "PEER_LIST_SYNCED");
                eventPayload.put("data", payload);

                // Push straight downstream over your persistent uni-app JavaScript bridge
                globalJsCallback.invokeAndKeepAlive(eventPayload);
                System.out.println("SaaS Engine Sync Event broadcasted to uni-app workspace.");
            }
        });
    }

    @UniJSMethod(uiThread = false)
    public void stopSyncPipeline() {
        if (syncScheduler != null && !syncScheduler.isShutdown()) {
            syncScheduler.shutdownNow();
            System.out.println("SaaS Data Engine: Sync Pipeline Deactivated cleanly.");
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

            // 3. Clear your native class memory status tracking flags
            isDelegateRegistered = false;
            globalJsCallback = null;
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
    private static boolean isDelegateRegistered = false;
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CODE && data.hasExtra("respond")) {
            Log.e("TestModule", "原生页面返回----"+data.getStringExtra("respond"));
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @UniJSMethod (uiThread = true)
    public void gotoNativePage(){
        if(mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
            Intent intent = new Intent(mUniSDKInstance.getContext(), NativePageActivity.class);
            ((Activity)mUniSDKInstance.getContext()).startActivityForResult(intent, REQUEST_CODE);
        }
    }
}
