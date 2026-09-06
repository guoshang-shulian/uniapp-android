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
import com.zegocloud.zimkit.services.model.ZIMKitGroupInfo;
import com.zegocloud.zimkit.services.internal.ZIMKitCore;
//import  io.dcloud.uniplugin.
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
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
import im.zego.zim.callback.ZIMUsersInfoQueriedCallback;
import im.zego.zim.entity.ZIMConversationFilterOption;
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

    String groupId = "";

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
                 ZIMKitConversationType.ZIMKitConversationTypePeer
             );
        } catch (Exception e) {
            e.printStackTrace();
        }
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

                    // 🏁 STEP 2: Smoothly forward the user straight into the target chat interface screen
                    ZIMKitRouter.toMessageActivity(mUniSDKInstance.getContext(), groupID, ZIMKitConversationType.ZIMKitConversationTypeGroup);

                }
            }
        });
    }

    /** 创建社群：后端建记录后，用 ZIM SDK 建群（不自动进聊天页，由 uniapp 决定） */
    @UniJSMethod(uiThread = true)
    public void createZimGroup(String groupID, String groupName, String avatarUrl, UniJSCallback callback) {
        try {
            ZIMKit.createGroup(groupName == null ? "" : groupName, groupID, new ArrayList<String>(),
                new CreateGroupCallback() {
                    @Override
                    public void onCreateGroup(ZIMKitGroupInfo groupInfo, ArrayList<ZIMErrorUserInfo> inviteUserErrors,
                        ZIMError error) {
                        JSONObject result = new JSONObject();
                        boolean ok = error != null && error.code == ZIMErrorCode.SUCCESS;
                        result.put("success", ok);
                        result.put("message", error == null ? "" : error.message);
                        if (ok && groupInfo != null) {
                            result.put("groupId", groupInfo.getId());
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

    @UniJSMethod(uiThread = false)
    public void startSyncPipeline(UniJSCallback callback) {
        globalJsCallback = callback;
//        neteaseLogin();
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
                System.out.println(uniArray);
                System.out.println("list is above");
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
    private static final Map<String, UniJSCallback> sMemberPickerCallbacks = new ConcurrentHashMap<>();
    private static UniJSCallback sGroupMembersCallback;
    private static boolean isDelegateRegistered = false;

    /** 原生红包 Activity 调用业务接口所需的静态配置 */
    private static String businessBaseUrl = "https://buyer-ceshi.shanxunsw.com/buyer";
    private static String businessToken = "";
    private static String localUserId = "";
    private static String localUserName = "";
    private static String localUserAvatar = "";

    @UniJSMethod(uiThread = true)
    public void setBusinessConfig(String baseUrl, String token, String userId, String userName, String avatarUrl) {
        if (baseUrl != null && !baseUrl.isEmpty()) {
            businessBaseUrl = baseUrl;
        }
        businessToken = token == null ? "" : token;
        localUserId = userId == null ? "" : userId;
        localUserName = userName == null ? "" : userName;
        localUserAvatar = avatarUrl == null ? "" : avatarUrl;
        System.out.println("[BusinessConfig] baseUrl=" + businessBaseUrl
            + " tokenLen=" + businessToken.length()
            + " userId=" + localUserId + " userName=" + localUserName);
    }

    public static String getBusinessBaseUrl() {
        return businessBaseUrl;
    }

    public static String getBusinessToken() {
        return businessToken;
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

    @UniJSMethod(uiThread = true)
    public void registerCardEventCallback(UniJSCallback callback) {
        cardJsCallback = callback;
    }

    @UniJSMethod(uiThread = true)
    public void registerFriendEventCallback(UniJSCallback callback) {
        FriendEventBridge.setCallback(callback);
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

    /** "[\"10001\",\"10002\"]" -> ["user_10001","user_10002"] */
    private static ArrayList<String> parseZimIds(String memberIdsJson) {
        ArrayList<String> ids = new ArrayList<>();
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
                RedPacketDetailActivity.start(mUniSDKInstance.getContext(),
                    detail == null ? "" : detail.getString("redPacketId"),
                    payload.getString("conversationId"),
                    sender == null ? "" : sender.getString("userId"),
                    sender == null ? "" : sender.getString("userName"),
                    sender == null ? "" : sender.getString("avatarUrl"));
                return;
            }
            if (cardJsCallback == null) {
                System.out.println("[CardBridge] cardJsCallback is null, action=" + action);
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
