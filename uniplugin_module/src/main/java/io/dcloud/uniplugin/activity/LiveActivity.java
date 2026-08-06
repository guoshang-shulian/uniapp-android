package io.dcloud.uniplugin.activity;

import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
//import com.zegocloud.uikit.components.audiovideocontainer.ZegoLayout;
//import com.zegocloud.uikit.components.audiovideocontainer.ZegoLayoutGalleryConfig;
//import com.zegocloud.uikit.components.audiovideocontainer.ZegoLayoutMode;
//import com.zegocloud.uikit.components.common.ZegoShowFullscreenModeToggleButtonRules;
//import com.zegocloud.uikit.plugin.adapter.plugins.beauty.IBeautyEventHandler;
//import com.zegocloud.uikit.plugin.adapter.plugins.beauty.ZegoBeautyPluginConfig;
//import com.zegocloud.uikit.plugin.adapter.plugins.beauty.ZegoBeautyPluginEffectsType;
//import com.zegocloud.uikit.plugin.adapter.plugins.beauty.ZegoBeautyPluginFaceDetectionResult;
//import com.zegocloud.uikit.prebuilt.livestreaming.ZegoUIKitPrebuiltLiveStreamingConfig;
//import com.zegocloud.uikit.prebuilt.livestreaming.ZegoUIKitPrebuiltLiveStreamingFragment;
//import com.zegocloud.uikit.prebuilt.livestreaming.core.ZegoBottomMenuBarConfig;
//import com.zegocloud.uikit.prebuilt.livestreaming.core.ZegoMenuBarButtonName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import uni.dcloud.io.uniplugin_module.R;

public class LiveActivity extends AppCompatActivity {

    private static final String TAG = "LiveActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live);

        addFragment();
    }

    private void addFragment() {
        long appID = getIntent().getLongExtra("appID", 0L);
        String appSign = getIntent().getStringExtra("appSign");
        String userID = getIntent().getStringExtra("userID");
        String userName = getIntent().getStringExtra("userName");

        boolean isHost = getIntent().getBooleanExtra("host", false);
        String liveID = getIntent().getStringExtra("liveID");

//        ZegoUIKitPrebuiltLiveStreamingConfig config;
//        if (isHost) {
//            config = ZegoUIKitPrebuiltLiveStreamingConfig.host(true);
//        } else {
//            config = ZegoUIKitPrebuiltLiveStreamingConfig.audience(true);
//        }
//
//        beautyEffectsConfig(config);
//
//        ZegoUIKitPrebuiltLiveStreamingFragment fragment = ZegoUIKitPrebuiltLiveStreamingFragment.newInstance(appID,
//                appSign, userID, userName, liveID, config);
//        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commitNow();
    }

    //private void beautyEffectsConfig(ZegoUIKitPrebuiltLiveStreamingConfig config) {
        // need BeautyResources folder in assets to work

   //     config.bottomMenuBarConfig.hostButtons.add(ZegoMenuBarButtonName.BEAUTY_BUTTON);
  //      config.bottomMenuBarConfig.coHostButtons.add(ZegoMenuBarButtonName.BEAUTY_BUTTON);
//
//        List<ZegoBeautyPluginEffectsType> customTypes = new ArrayList<>();
//        customTypes.addAll(ZegoBeautyPluginConfig.basicTypes());
//        customTypes.addAll(ZegoBeautyPluginConfig.advancedTypes());
//        customTypes.addAll(ZegoBeautyPluginConfig.makeupTypes());
//        customTypes.addAll(ZegoBeautyPluginConfig.filterTypes());
//        config.beautyConfig.effectsTypes = customTypes;
//
//        config.beautyConfig.beautyEventHandler = new IBeautyEventHandler() {
//            @Override
//            public void onInitResult(int errorCode, String message) {
//                super.onInitResult(errorCode, message);
//                Log.d(TAG, "onInitResult() called with: errorCode = [" + errorCode + "], message = [" + message + "]");
//            }
//
//            @Override
//            public void onError(int errorCode, String message) {
//                super.onError(errorCode, message);
//                Log.d(TAG, "onError() called with: errorCode = [" + errorCode + "], message = [" + message + "]");
//            }
//
//            @Override
//            public void onFaceDetectionResult(ZegoBeautyPluginFaceDetectionResult[] results) {
//                super.onFaceDetectionResult(results);
//                Log.d(TAG, "onFaceDetectionResult() called with: results = [" + results + "]");
//            }
//        };
  //  }
}