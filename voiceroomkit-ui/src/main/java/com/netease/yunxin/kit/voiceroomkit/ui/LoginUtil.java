// Copyright (c) 2022 NetEase, Inc. All rights reserved.
// Use of this source code is governed by a MIT license that can be
// found in the LICENSE file.

package com.netease.yunxin.kit.voiceroomkit.ui;

import static kotlinx.coroutines.DelayKt.delay;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;

import androidx.annotation.Nullable;

import com.netease.yunxin.kit.alog.ALog;
import com.netease.yunxin.kit.common.ui.utils.ToastX;
import com.netease.yunxin.kit.entertainment.common.model.NemoAccount;
import com.netease.yunxin.kit.entertainment.common.model.RoomModel;
import com.netease.yunxin.kit.entertainment.common.utils.OneOnOneUtils;
import com.netease.yunxin.kit.entertainment.common.utils.UserInfoManager;
import com.netease.yunxin.kit.ordersong.core.NEOrderSongService;
import com.netease.yunxin.kit.voiceroomkit.api.NEVoiceRoomCallback;
import com.netease.yunxin.kit.voiceroomkit.api.NEVoiceRoomKit;
import com.netease.yunxin.kit.voiceroomkit.api.NEVoiceRoomKitConfig;
import com.netease.yunxin.kit.voiceroomkit.ui.base.utils.FloatPlayManager;
import com.netease.yunxin.kit.voiceroomkit.ui.utils.NavUtils;

import java.util.HashMap;
import java.util.Map;
import kotlin.Unit;

public class LoginUtil {
  private static final String TAG = "LoginUtil";

  public static void loginVoiceRoom(
      Context context, NemoAccount nemoAccount, LoginVoiceRoomCallback callback) {
    ALog.i(TAG, "initVoiceRoomKit");
    Map<String, String> extras = new HashMap<>();
    extras.put("serverUrl", AppConfig.getNERoomServerUrl());
    extras.put("baseUrl", AppConfig.getBaseUrl());
    NEVoiceRoomKit.getInstance()
        .initialize(
            context,
            new NEVoiceRoomKitConfig(AppConfig.getAppKey(), extras),
            new NEVoiceRoomCallback<Unit>() {
              @Override
              public void onSuccess(@Nullable Unit unit) {
                ALog.d(TAG, "NEVoiceRoomKit init success");
                loginVoiceRoomInner(context, nemoAccount, callback);
              }

              @Override
              public void onFailure(int code, @Nullable String msg) {
                if (callback != null) {
                  callback.onError(
                      code, "NEVoiceRoomKit initialize failed,code:" + code + "，msg:" + msg);
                }
              }
            });
  }

    public static void  logout(){
        NEVoiceRoomKit.getInstance()
                .logout(null);
        UserInfoManager.clearUserInfo();
    }

    public static  void join(Context context, RoomModel info,String avatar,String userName){
        if (FloatPlayManager.getInstance().isShowFloatView()) {
            if (FloatPlayManager.getInstance().getVoiceRoomInfo() != null
                    && FloatPlayManager.getInstance()
                    .getVoiceRoomInfo()
                    .getRoomUuid()
                    .equals(info.getRoomUuid())) {
                FloatPlayManager.getInstance().stopFloatPlay();
                NavUtils.toVoiceRoomAudiencePage(context, userName, avatar, info, false);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("提示");
                builder.setMessage("点击入");
                builder.setCancelable(true);
                builder.setPositiveButton("确认",
                        (dialog, which) -> {
                            NEVoiceRoomKit.getInstance()
                                    .leaveRoom(
                                            new NEVoiceRoomCallback<Unit>() {
                                                @Override
                                                public void onSuccess(@Nullable Unit unit) {
                                                    NavUtils.toVoiceRoomAudiencePage(
                                                            context, userName, avatar, info, true);
                                                }

                                                @Override
                                                public void onFailure(int code, @Nullable String msg) {}
                                            });
                            dialog.dismiss();
                        });
                builder.setNegativeButton( "取消"
                        , (dialog, which) -> dialog.dismiss());
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        } else if (OneOnOneUtils.isInTheCall()) {
            ToastX.showShortToast("你在会议中");
        } else {
            NavUtils.toVoiceRoomAudiencePage(context, userName, avatar, info, true);
        }
    }

  private static void loginVoiceRoomInner(
      Context context, NemoAccount nemoAccount, LoginVoiceRoomCallback callback) {

              System.out.println("userUuid ---> " +nemoAccount.userUuid);
      System.out.println("userToken ---> " +nemoAccount.userToken);
      System.out.println("userToken ---> " +nemoAccount.userName);
    NEVoiceRoomKit.getInstance()
        .login(
            nemoAccount.userUuid,
                nemoAccount.userToken,
            new NEVoiceRoomCallback<Unit>() {

              @Override
              public void onSuccess(@Nullable Unit unit) {
                ALog.d(TAG, "NEVoiceRoomKit login success");
                UserInfoManager.setUserInfo(
                    nemoAccount.userUuid,
                    nemoAccount.userToken,
                    nemoAccount.imToken,
                    nemoAccount.userName,
                    nemoAccount.icon,
                    nemoAccount.mobile);
                UserInfoManager.saveUserInfoToSp(nemoAccount);
                NEOrderSongService.INSTANCE.initialize(
                    context.getApplicationContext(),
                    AppConfig.getAppKey(),
                    AppConfig.getBaseUrl(),
                    AppConfig.getNERoomServerUrl(),
                    nemoAccount.userUuid);
                NEOrderSongService.INSTANCE.addHeader("user", nemoAccount.userUuid);
                NEOrderSongService.INSTANCE.addHeader("token", nemoAccount.userToken);
                if (callback != null) {
                  callback.onSuccess();
                }
              }

              @Override
              public void onFailure(int code, @Nullable String msg) {
                ALog.e(TAG, "NEVoiceRoomKit login failed code = " + code + ", msg = " + msg);
                UserInfoManager.clearUserInfo();
                if (callback != null) {
                  callback.onError(
                      code,  msg);
                }
                if(msg != null){
                    if (msg.equals("认证失败，token错误")){
                             ///     context,  nemoAccount,  callback);
                    }
                }
              }
            });
  }

  public interface LoginVoiceRoomCallback {
    void onSuccess();

    void onError(int errorCode, String errorMsg);
  }
}
