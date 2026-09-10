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
            // 2人“群聊”：群名固定“群聊”（共享名会导致对方看到自己名字），显示名按“对方”本地计算
            final String displayName = "群聊";
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

    private void findTwoPersonGroupAndOpen(final List<String> gids, final int index,
        final String peer, final String self, final String displayName, final UniJSCallback callback) {
        if (index >= gids.size()) {
            // 没有“仅我+对方”的群 → 新建 2人“群聊”（群名=对方显示名）
            String newGid = "g_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 99999);
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

    /** 对方显示名：JS 没传/传了“私聊/群聊”时，用 ZIM 用户资料兜底 */
    private void resolvePeerDisplayName(String peer, String rawName, java.util.function.Consumer<String> onDone) {
        if (rawName != null && !rawName.isEmpty() && !"私聊".equals(rawName) && !"群聊".equals(rawName)) {
            onDone.accept(rawName);
            return;
        }
        try {
            ArrayList<String> ids = new ArrayList<>();
            ids.add(peer);
            ZIMUsersInfoQueryConfig cfg = new ZIMUsersInfoQueryConfig();
            ZIMKitCore.getInstance().queryUserInfo(ids, cfg, new ZIMUsersInfoQueriedCallback() {
                @Override
                public void onUsersInfoQueried(ArrayList<ZIMUserFullInfo> userList,
                    ArrayList<ZIMErrorUserInfo> errorUserList, ZIMError error) {
                    String name = "群聊";
                    if (error != null && error.code == ZIMErrorCode.SUCCESS && userList != null
                        && !userList.isEmpty() && userList.get(0) != null && userList.get(0).baseInfo != null) {
                        String n = userList.get(0).baseInfo.userName;
                        if (n != null && !n.isEmpty()) {
                            name = n;
                        } else {
                            name = Member.stripZimPrefix(peer);
                        }
                    }
                    onDone.accept(name);
                }
            });
        } catch (Exception e) {
            onDone.accept(rawName == null || rawName.isEmpty() ? "群聊" : rawName);
        }
    }

    /** 2人“群聊”旧名=“群聊”时补成对方显示名（用户自定义改名不动） */
    private void ensureTwoPersonName(String gid, String displayName) {
        if (displayName == null || displayName.isEmpty() || "群聊".equals(displayName)
            || "私聊".equals(displayName)) {
            return;
        }
        try {
            ZIMKit.queryGroupInfo(gid, new QueryGroupInfoCallback() {
                @Override
                public void onQueryGroupInfo(ZIMKitGroupInfo info, ZIMError error) {
                    if (error != null && error.code == ZIMErrorCode.SUCCESS && info != null) {
                        String cur = info.getName();
                        if (cur != null && "群聊".equals(cur)) {
                            zimInstance().updateGroupName(displayName, gid, (g, n, e2) -> {
                            });
                        }
                    }
                }
            });
        } catch (Exception ignored) {
        }
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
        if (successor == null) {
            // 只剩自己：唯一出口是解散
            dismissGroupChatWithReply(groupId, callback);
            return;
        }
        zimInstance().transferGroupOwner(groupId, successor, new ZIMGroupOwnerTransferredCallback() {
            @Override
            public void onGroupOwnerTransferred(String gid, String newOwner, ZIMError error) {
                if (error != null && error.code == ZIMErrorCode.SUCCESS) {
                    ZIMKit.leaveGroup(groupId, new LeaveGroupCallback() {
                        @Override
                        public void onLeaveGroup(ZIMError e2) {
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
                    JSONObject result = new JSONObject();
                    result.put("success", false);
                    result.put("message", error == null ? "转让失败" : error.message);
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

    private void openPeerChat(String conversationID) {
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

    private void findTwoPersonGroupForInvite(final List<String> gids, final int index,
        final String peer, final String self, final String cardJson, final int subType,
        final UniJSCallback callback) {
        if (index >= gids.size()) {
            // 无“仅我+对方”2人群 → 新建（群名=群聊，bizType=temp）
            String newGid = "g_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 99999);
            ZIMKit.createGroup("群聊", newGid, new ArrayList<String>() {{ add(peer); }},
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
        try {
            if (sGlobalJsCallback != null) {
                JSONObject payload = new JSONObject();
                payload.put("event", event);
                payload.put("data", data == null ? new JSONObject() : data);
                sGlobalJsCallback.invokeAndKeepAlive(payload);
            }
        } catch (Exception ignored) {
        }
    }

    @UniJSMethod(uiThread = false)
    public void startSyncPipeline(UniJSCallback callback) {
        globalJsCallback = callback;
        sGlobalJsCallback = callback;
//        neteaseLogin();
        System.out.println("SaaS Data Engine: Sync Pipeline Activated.");
        stopSyncPipeline();
        enableConversationLiveSync();
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
                // 补 muted 标记（免打扰）供列表显示铃铛划线
                try {
                    for (int i = 0; i < uniArray.size() && i < conversationList.size(); i++) {
                        JSONObject o = uniArray.getJSONObject(i);
                        im.zego.zim.entity.ZIMConversation c = conversationList.get(i);
                        o.put("muted", c != null && c.notificationStatus
                            == im.zego.zim.enums.ZIMConversationNotificationStatus.DO_NOT_DISTURB);
                    }
                } catch (Exception ignored) {
                }
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
        cacheContext(safeContext());
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

    /** 请求 uniapp 重新下发 businessToken（原生接口 401/403 时自动触发，刷新后自动重试一次） */
    public static void requestBusinessConfigRefresh() {
        emitGlobalEvent("REFRESH_BUSINESS_TOKEN", new JSONObject());
    }

    /**
     * uniapp 侧 token 刷新后主动推送最新 token（反向：不用等原生 403 才刷新）。
     * 只更新 token 字段，不动 baseUrl/userId/昵称头像，避免并发写坏配置。
     */
    @UniJSMethod(uiThread = false)
    public void notifyBusinessTokenChanged(String token) {
        String t = token == null ? "" : token;
        if (t.isEmpty() || t.equals(businessToken)) {
            return;
        }
        businessToken = t;
        android.util.Log.i("BusinessConfig", "token updated by push, tokenLen=" + t.length());
    }

    @UniJSMethod(uiThread = true)
    public void registerCardEventCallback(UniJSCallback callback) {
        cardJsCallback = callback;
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

    /** 免审核加入后：POST /social/group/member/sync { groupId, userIds:[myMemberId] } */
    private static void syncGroupMemberToBackend(String groupId) {
        try {
            String memberId = io.dcloud.uniplugin.memberpicker.Member.stripZimPrefix(
                getLocalUserId());
            if (memberId == null || memberId.isEmpty()) {
                return;
            }
            JSONObject body = new JSONObject();
            body.put("groupId", groupId);
            com.alibaba.fastjson.JSONArray ids = new com.alibaba.fastjson.JSONArray();
            ids.add(memberId);
            body.put("userIds", ids);
            io.dcloud.uniplugin.others.RedPacketApi.post(
                getBusinessBaseUrl() + "/social/group/member/sync", getBusinessToken(), body,
                new io.dcloud.uniplugin.others.RedPacketApi.Callback() {
                    @Override
                    public void onSuccess(JSONObject result) {
                    }

                    @Override
                    public void onError(int code, String message) {
                    }
                });
        } catch (Exception ignored) {
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
                    createNewGroupFromPrivate(members, "群聊", null);
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
            ZIMKit.createGroup(name == null || name.isEmpty() ? "群聊" : name, newGid,
                new ArrayList<String>(members), new CreateGroupCallback() {
                    @Override
                    public void onCreateGroup(ZIMKitGroupInfo groupInfo, ArrayList<ZIMErrorUserInfo> inviteUserErrors,
                        ZIMError error) {
                        if (error != null && error.code == ZIMErrorCode.SUCCESS) {
                            android.util.Log.d("PrivateGroup", "createGroupFromPrivate ok gid=" + newGid);
                            markGroupBizType(newGid, "temp");
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
            zimInstance().queryGroupAllAttributes(gid, (g, attrs, e) -> {
                boolean isCommunity = attrs != null && "community".equals(attrs.get("bizType"));
                fetchAllGroupMembers(gid, new ArrayList<ZIMGroupMemberInfo>(), 0, members -> {
                    int count = members == null ? 0 : members.size();
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
                        zimInstance().dismissGroup(gid, (gid2, e2) -> {
                            deleteConversationQuiet(gid2, ZIMConversationType.GROUP);
                            JSONObject result = new JSONObject();
                            result.put("success", true);
                            result.put("message", "已解散群聊");
                            if (callback != null) {
                                callback.invoke(result);
                            }
                        });
                        return;
                    }
                    String selfId = ZIMKitCore.getInstance().getLocalUser().getId();
                    boolean owner = false;
                    if (members != null) {
                        for (ZIMGroupMemberInfo info : members) {
                            if (info != null && selfId != null && selfId.equals(info.userID)
                                && info.memberRole == 1) {
                                owner = true;
                                break;
                            }
                        }
                    }
                    if (owner) {
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

    /** 打开群成员信息页（图3/图4：群主看用户 / 用户看群主） */
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

    /** 清理老数据：解散/退出所有 bizType=private 群 + 删除老旧 user_ PEER 本地会话（正式版清场可复用） */
    @UniJSMethod(uiThread = true)
    public void cleanupLegacyPrivateChats(UniJSCallback callback) {
        try {
            zimInstance().queryGroupList((groups, error) -> {
                final List<String> gids = new ArrayList<>();
                if (groups != null) {
                    for (im.zego.zim.entity.ZIMGroup g : groups) {
                        if (g != null && g.baseInfo != null && g.baseInfo.groupID != null) {
                            gids.add(g.baseInfo.groupID);
                        }
                    }
                }
                cleanupLegacyStep(gids, 0, callback);
            });
        } catch (Exception e) {
            invokeFail(callback, e);
        }
    }

    private void cleanupLegacyStep(final List<String> gids, final int index, final UniJSCallback callback) {
        if (index >= gids.size()) {
            final int[] cleaned = {0};
            ZIMConversationQueryConfig config = new ZIMConversationQueryConfig();
            config.count = 100;
            zimInstance().queryConversationList(config, (list, error) -> {
                if ((error == null || error.code == ZIMErrorCode.SUCCESS) && list != null) {
                    for (ZIMConversation c : list) {
                        if (c != null && c.conversationID != null && c.conversationID.startsWith("user_")) {
                            cleaned[0]++;
                            deleteConversationQuiet(c.conversationID, ZIMConversationType.PEER);
                        }
                    }
                }
                JSONObject result = new JSONObject();
                result.put("success", true);
                result.put("cleanedPeerConversations", cleaned[0]);
                if (callback != null) {
                    callback.invoke(result);
                }
            });
            return;
        }
        final String gid = gids.get(index);
        zimInstance().queryGroupAllAttributes(gid, (g, attrs, e) -> {
            if (attrs != null && "private".equals(attrs.get("bizType"))) {
                ZIMGroupMemberQueryConfig config = new ZIMGroupMemberQueryConfig();
                config.count = 100;
                config.nextFlag = 0;
                zimInstance().queryGroupMemberList(gid, config, (gid2, memberList, flag, err) -> {
                    String self = ZIMKitCore.getInstance().getLocalUser().getId();
                    int selfRole = 3;
                    if (memberList != null) {
                        for (ZIMGroupMemberInfo info : memberList) {
                            if (info != null && self != null && self.equals(info.userID)) {
                                selfRole = info.memberRole;
                            }
                        }
                    }
                    if (selfRole == 1) {
                        zimInstance().dismissGroup(gid, (gid3, e2) -> {
                            deleteConversationQuiet(gid, ZIMConversationType.GROUP);
                            cleanupLegacyStep(gids, index + 1, callback);
                        });
                    } else {
                        ZIMKit.leaveGroup(gid, new LeaveGroupCallback() {
                            @Override
                            public void onLeaveGroup(ZIMError e3) {
                                deleteConversationQuiet(gid, ZIMConversationType.GROUP);
                                cleanupLegacyStep(gids, index + 1, callback);
                            }
                        });
                    }
                });
            } else {
                cleanupLegacyStep(gids, index + 1, callback);
            }
        });
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
                            io.dcloud.uniplugin.activity.RedPacketOpenDialog.show(rpCtx, fRpId,
                                fConvId, fConvType, fSenderId, fSenderName, fSenderAvatar);
                        } catch (Exception e) {
                            android.util.Log.e("RedPacket", "open modal fail: " + e);
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
