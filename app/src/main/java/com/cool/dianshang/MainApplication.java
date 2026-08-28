package com.cool.dianshang;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import io.dcloud.application.DCloudApplication;

import com.cool.dianshang.data.PrivacyData;
import com.zegocloud.zimkit.services.ZIMKit; // Adjust package path to match your imports
import com.zegocloud.zimkit.services.ZIMKitConfig;
import com.zj.zjsdk.ZJConfig;
import com.zj.zjsdk.ZjCustomController;
import com.zj.zjsdk.ZjSdk;
//import com.example.openim.OpenIMSDK;
import java.lang.reflect.Method;
import java.util.List;

public class MainApplication extends DCloudApplication {

    private static final String TAG = "ZegoNativeBoot";

    public static MainApplication app;
    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        ZIMKitConfig zimKitConfig = new ZIMKitConfig();
        long appId = 845800108;
        String appSign = "8857c37bde5ffc60a8cadebef546f51f57f01d7f5a8bfc0d2b0d192adaa33bd3";

        try {
            ZjSdk.initWithoutStart(this.getApplicationContext(),
                    new ZJConfig.Builder("Z9760773547")
                            .userId(SPUtil.getInstance().getUID())
                            .isDebug(false)
                            .build()
            );
            start(MainApplication.this);
            // Wrap the SDK initialization logic inside the try block
            ZIMKit.initWith(this, appId, appSign, zimKitConfig);
            ZIMKit.initNotifications();
            Log.d("ZIMKIT", "STARTED SUCCESSFULLY");
        } catch (Exception e) {
            // Handle initialization errors to prevent app crashes
            Log.d("ZIMKIT", "STARTED FAILED");
            e.printStackTrace();
        }
    }

    public void start(Context context) {
        initSdkPrivacyConfig();
        // 2.4.18版本初始化方法调整，需要先调用initWithoutStart方法初始化，并在用户同意隐私协议后调用start方法
        ZjSdk.start(new ZjSdk.OnStartListener() {

            @Override
            public void onStartSuccess() {
                // 需要在start成功后请求广告
                Toast.makeText(context, "ZJSDK初始化成功", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartFailed(int code, @Nullable String msg) {
                String text = "ZJSDK初始化失败[" + code + "-" + msg + "]";
                if (code == 999001 && msg != null && msg.contains("ignore init on process")) {
                    // SDK不支持在子进程中初始化，会返回错误
                    // 可以忽略，不影响主进程上的广告调用
                    Log.e("ZJSdk", "忽略子进程上的初始化");
                } else {
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
                }
            }

        });
    }


    private void initSdkPrivacyConfig() {
        PrivacyData privacy = SPUtil.getInstance().getPrivacy();
        ZjCustomController.getInstance().setCustomController(new ZjCustomController.Controller() {

            /**
             * 是否允许 SDK 主动使用地理位置信息
             *
             * @return true 可以获取，false 禁止获取。默认为 true
             */
            @Override
            public boolean canReadLocation() {
                return privacy.canReadLocation;
            }

            /**
             * 当 canReadLocation==false 时，可传入地理位置信息，
             * sdk 使用您传入的地理位置信息
             *
             * @return 地理位置参数
             */
            @Override
            public ZjCustomController.ZJLocation getLocation() {
                return new ZjCustomController.ZJLocation(privacy.longitude, privacy.latitude);
            }

            /**
             * 是否允许 SDK 主动使用手机硬件参数，如：imei, android_id, meid, imsi, iccid
             *
             * @return true 可以使用，false 禁止使用。默认为 true
             */
            @Override
            public boolean canUsePhoneState() {
                return privacy.canUsePhoneState;
            }

            /**
             * 当 canUsePhoneState==false 时，可传入原始的 imei 信息，
             * sdk 使用您传入的原始 imei 信息。
             */
            @Override
            public String getImei() {
                return privacy.imei;
            }

            /**
             * 是否允许主动获取AndroidID
             */
            @Override
            public boolean canUseAndroidId() {
                return privacy.canUseAndroidId;
            }

            /**
             * 当 canUsePhoneState==false 时，可传入 android_id 信息，
             * sdk 使用您传入的 android_id
             */
            @Override
            public String getAndroidId() {
                return privacy.androidId;
            }

            /**
             * 是否允许 SDK 主动使用 mac_address
             *
             * @return true 可以使用，false 禁止使用。默认为 true
             */
            @Override
            public boolean canUseMacAddress() {
                return privacy.canUseMacAddress;
            }

            /**
             * 当 canUseMacAddress==false 时，可传入 mac 地址信息，sdk 使用您传入的 mac 地址信息
             */
            @Override
            public String getMacAddress() {
                return privacy.mac;
            }

            /**
             * 是否允许 SDK 主动使用 oaid
             *
             * @return true 可以使用，false 禁止使用。默认为 true
             */
            @Override
            public boolean canUseOaid() {
                return privacy.canUseOaid;
            }

            /**
             * 当 canUseOaid==false 时，可传入 oaid 信息，sdk 使用您传入的 oaid 信息
             */
            @Override
            public String getOaid() {
                return privacy.oaid;
            }

            /**
             * 是否允许 SDK 主动使用 gaid
             *
             * @return true 可以使用，false 禁止使用。默认为 true
             */
            public boolean canUseGaid() {
                return privacy.canUseGaid;
            }

            /**
             * 当 canUseGaid==false 时，可传入 gaid 信息，sdk 使用您传入的 gaid 信息
             */
            public String getGaid() {
                return privacy.gaid;
            }

            /**
             * 是否允许 SDK 主动使用 ACCESS_NETWORK_STATE 权限
             *
             * @return true 可以使用，false 禁止使用。默认为 true
             */
            @Override
            public boolean canUseNetworkState() {
                return privacy.canUseNetworkState;
            }

            /**
             * 是否允许 SDK 主动使用存储权限
             *
             * @return true 可以使用，false 禁止使用。默认为 true
             */
            @Override
            public boolean canUseStoragePermission() {
                return privacy.canUseStoragePermission;
            }

            /**
             * 是否允许 SDK 主动读取 app 安装列表
             *
             * @return true 可以使用，false 禁止使用。默认为 true
             */
            @Override
            public boolean canReadInstalledPackages() {
                return privacy.canReadInstalledPackages;
            }

            @Override
            public List<String> getInstalledPackages() {
                return privacy.installedPackages;
            }

            /**
             * 是否允许SDK在申明和授权了的情况下使用录音权限
             *
             * @return true 可以使用，false 禁止使用。默认为 true
             */
            @Override
            public boolean canRecordAudio() {
                return privacy.canRecordAudio;
            }

            /**
             * 是否允许 SDK 主动获取 BootID
             *
             * @return true 可以使用，false 禁止使用。默认为 true
             */
            @Override
            public boolean canReadBootId() {
                return privacy.canReadBootId;
            }

            /**
             * 是否允许获取附近的Wifi列表
             *
             * @return true 可以使用，false 禁止使用。默认为 true
             */
            @Override
            public boolean canReadNearbyWifiList() {
                return privacy.canReadNearbyWifiList;
            }

            /**
             * 是否允许获取传感器信息
             *
             * @return true 可以使用，false 禁止使用。默认为 true
             */
            @Override
            public boolean canUseSensor() {
                return privacy.canUseSensor;
            }

            /**
             * 是否允许 SDK 主动获取运营商信息
             *
             * @return true 可以使用，false 禁止使用。默认为 true
             */
            @Override
            public boolean canUseSimOperator() {
                return privacy.canUseSimOperator;
            }

            /**
             * 当 canUseSimOperator==false 时，可传入运营商编码，例如：46000
             *
             * @return 运营商编码
             */
            @Override
            public String getSimOperatorCode() {
                return privacy.simOperatorCode;
            }

            /**
             * 当 canUseSimOperator==false 时，可传入运营商名称，例如：中国移动
             *
             * @return 运营商名称
             */
            @Override
            public String getSimOperatorName() {
                return privacy.simOperatorName;
            }

        });
    }

}
