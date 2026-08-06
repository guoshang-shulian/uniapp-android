package io.dcloud.uniplugin.activity;

import android.Manifest;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Rational;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.gson.JsonObject;
//import com.zegocloud.uikit.prebuilt.liveaudioroom.ZegoUIKitPrebuiltLiveAudioRoomConfig;
//import com.zegocloud.uikit.prebuilt.liveaudioroom.ZegoUIKitPrebuiltLiveAudioRoomFragment;
//import com.zegocloud.uikit.prebuilt.liveaudioroom.core.ZegoMenuBarButtonName;
//import com.zegocloud.uikit.prebuilt.liveaudioroom.internal.service.RoomLeaveListener;

import io.dcloud.uniplugin.TestModule;
import uni.dcloud.io.uniplugin_module.R;
//import com.zegocloud.uikit.prebuilt.liveaudioroom.ZegoMenuBarButtonName;

public class AudioRoomActivity extends AppCompatActivity {

    private static final long APP_ID = 845800108;
    private static final String APP_SIGN = "8857c37bde5ffc60a8cadebef546f51f57f01d7f5a8bfc0d2b0d192adaa33bd3";
    long appID = 845800108;
    String appSign = "8857c37bde5ffc60a8cadebef546f51f57f01d7f5a8bfc0d2b0d192adaa33bd3";
    private String roomID = "test_room_id";
    private String userID = "1231";
    private String userName = "12312";

    private Boolean isHost = false;

    private ImageView ivLogo;


//    private  ZegoUIKitPrebuiltLiveAudioRoomFragment fragment;
    private static LastRoomLeave roomLeaveListener;

    public static void setRoomLeaveListener(LastRoomLeave listener) {
        roomLeaveListener = listener;
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        System.out.println("activity started here 1xxx");
        if(!isMicrophoneGranted()){
            return;
        }
//        if (fragment != null) {
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .hide(fragment)
//                    .commitNow(); // Apply immediately
//           // ivLogo.setVisibility(View.VISIBLE);
//        }

        if(roomLeaveListener != null){
            roomLeaveListener.onRoomLeft();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !isInPictureInPictureMode()) {

            enterPictureInPictureMode(
                    new PictureInPictureParams.Builder()
                            .setAspectRatio(new Rational(3, 4))
                            .build()
            );
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("activity stopped here 1xxx");
//        TestModule.ACTION_ACTIVITY_RESULT.
        if(roomLeaveListener != null){
            roomLeaveListener.onRoomLeft();
        }

        // If the activity is stopping, finishing, and was in PiP mode,
        // the user closed it using the "X" button or dragged it to close.
        if (isFinishing() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            System.out.println("activity stopped here");
            if (isInPictureInPictureMode()) {
                // User clicked the 'X' button while it was still small
                //handlePipClosedByUser();
            }
        }
    }
    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode,
                                              Configuration newConfig) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        }
//        if(getLifecycle().getCurrentState() == lif)

//        if (!isInPictureInPictureMode && fragment != null) {
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .show(fragment)
//                    .commitAllowingStateLoss();
//            ivLogo.setVisibility(View.GONE);
//        }
    }

    public boolean isMicrophoneGranted() {
        System.out.println("here bro, we reached 2");
        Context context = this;
        if (context == null) return false;

        int result = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() != null) {
            roomID = getIntent().getStringExtra("roomID");
            userID = getIntent().getStringExtra("userID");
            userName = getIntent().getStringExtra("userName");
            isHost = getIntent().getBooleanExtra("isHost",false);
        }




       // roomID = "test_room_id";

        //String userName = json.toString();

//        setContentView(new android.widget.FrameLayout(this));
        setContentView(R.layout.activity_live_audio_room);
        ivLogo = findViewById(R.id.ivLogo);
       // ivLogo.setVisibility(View.VISIBLE);

        View root = findViewById(android.R.id.content);

        if (root == null) {
            Log.e("AudioRoom", "Activity layout root is NULL");
        } else {
            Log.d("AudioRoom", "Activity layout loaded OK");
        }

        ivLogo = findViewById(R.id.ivLogo);

        if (ivLogo == null) {
            Log.e("AudioRoom", "ivLogo is NULL ❌ - ImageView not found in layout");
        } else {
            Log.d("AudioRoom", "ivLogo loaded successfully ✅");
        }
//
//        ZegoUIKitPrebuiltLiveAudioRoomConfig config =
//                ZegoUIKitPrebuiltLiveAudioRoomConfig.audience();
//        if(isHost) {
//            config = ZegoUIKitPrebuiltLiveAudioRoomConfig.host();
//        }
//
//        config.bottomMenuBarConfig.hostButtons.add(ZegoMenuBarButtonName.SWITCH_AUDIO_OUTPUT_BUTTON);
//        fragment  =
//                ZegoUIKitPrebuiltLiveAudioRoomFragment.newInstance(
//                        APP_ID,
//                        APP_SIGN,
//                        userID,
//                        userName,
//                        roomID,
//                        config
//                );
//
//        getSupportFragmentManager()
//                .beginTransaction()
//                .replace(R.id.fragment_container, fragment)
//                .commit();
    }
}
