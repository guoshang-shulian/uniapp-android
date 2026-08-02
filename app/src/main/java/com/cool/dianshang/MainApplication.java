package com.cool.dianshang;

import android.util.Log;
import io.dcloud.application.DCloudApplication;
import com.zegocloud.zimkit.services.ZIMKit; // Adjust package path to match your imports
import com.zegocloud.zimkit.services.ZIMKitConfig;
//import com.example.openim.OpenIMSDK;
import java.lang.reflect.Method;

public class MainApplication extends DCloudApplication {

    private static final String TAG = "ZegoNativeBoot";

    @Override
    public void onCreate() {
        super.onCreate();
        ZIMKitConfig zimKitConfig = new ZIMKitConfig();
        long appId = 845800108;
        String appSign = "8857c37bde5ffc60a8cadebef546f51f57f01d7f5a8bfc0d2b0d192adaa33bd3";

        try {
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

}
