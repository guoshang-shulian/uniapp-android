//package com.cool.dianshang;
//
//import android.app.Application;
//import android.util.Log;
//
//import io.dcloud.feature.uniapp.UniAppHookProxy;
//
//public class MySDKHookProxy implements UniAppHookProxy {
//    private static final String TAG = "ZegoOfflineHook";
//
//    @Override
//    public void onCreate(Application application) {
//        Log.d(TAG, "🚀 Android Studio Offline App Lifecycle Hook Fired!");
//
//        try {
//            Long appId = 123456789L; // Replace with your real AppID
//            String appSign = "your_actual_app_sign_here";
//
//            // Link your ZEGOCLOUD SDK directly to the Application instance
//          //  ZIMKit.initWith(application, appId, appSign);
//            Log.d(TAG, "✅ ZIMKit successfully initialized in offline build.");
//        } catch (Exception e) {
//            Log.e(TAG, "❌ Initialization crash: " + e.getMessage());
//        }
//    }
//
//    @Override
//    public void onSubProcessCreate(Application application) {
//        Log.d(TAG, "🚀 Android Studio Offline App Lifecycle Hook Fired! SUB-PROCESS");
//    }
//}
