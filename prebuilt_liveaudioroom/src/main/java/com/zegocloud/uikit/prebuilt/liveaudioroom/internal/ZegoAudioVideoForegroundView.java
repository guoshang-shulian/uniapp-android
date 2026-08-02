package com.zegocloud.uikit.prebuilt.liveaudioroom.internal;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zegocloud.uikit.prebuilt.liveaudioroom.core.ZegoLiveAudioRoomRole;
import com.zegocloud.uikit.prebuilt.liveaudioroom.databinding.LiveaudioroomLayoutSeatForegroundBinding;
import com.zegocloud.uikit.components.audiovideo.ZegoBaseAudioVideoForegroundView;
import com.zegocloud.uikit.service.defines.ZegoUIKitUser;
import java.util.HashMap;

public class ZegoAudioVideoForegroundView extends ZegoBaseAudioVideoForegroundView {

    private LiveaudioroomLayoutSeatForegroundBinding binding;

    public ZegoAudioVideoForegroundView(@NonNull Context context, String userID) {
        super(context, userID);
    }

    public ZegoAudioVideoForegroundView(@NonNull Context context, @Nullable AttributeSet attrs, String userID) {
        super(context, attrs, userID);
    }

    @Override
    protected void onForegroundViewCreated(ZegoUIKitUser uiKitUser) {
        super.onForegroundViewCreated(uiKitUser);

        binding = LiveaudioroomLayoutSeatForegroundBinding.inflate(LayoutInflater.from(getContext()), this, true);
        update(uiKitUser);
    }

    @Override
    protected void onMicrophoneStateChanged(boolean isMicrophoneOn) {
        super.onMicrophoneStateChanged(isMicrophoneOn);
        updateUserMicrophone(isMicrophoneOn);
    }

    @Override
    protected void onInRoomAttributesUpdated(HashMap<String, String> inRoomAttributes) {
        super.onInRoomAttributesUpdated(inRoomAttributes);
        updateUserInRoomAttributes(inRoomAttributes);
    }

    private void update(ZegoUIKitUser uiKitUser) {
        if (uiKitUser != null) {
            updateUserInRoomAttributes(uiKitUser.inRoomAttributes);
            updateUserMicrophone(uiKitUser.isMicrophoneOn);
            if (binding != null) {
                String[] profileData = decodeJsonToArray(uiKitUser.userName);
                String name = profileData[0];

                binding.foregroundUserName.setText(name);
            }
        }
    }

    public String[] decodeJsonToArray(String userName) {
        String baseCDN = "https://test.ioevisa.com";

        try {
            JsonObject jsonObj = JsonParser.parseString(userName).getAsJsonObject();

            String nickname = jsonObj.has("n") ? jsonObj.get("n").getAsString() : "用户";
            String profilePath = jsonObj.has("p") ? jsonObj.get("p").getAsString() : "";

            String fullAvatarUrl;
            if (profilePath.isEmpty()) {
                fullAvatarUrl = "";
            } else if (profilePath.startsWith("https://") || profilePath.startsWith("http://")) {
                fullAvatarUrl = profilePath;
            } else {
                fullAvatarUrl = baseCDN + profilePath;
            }

            return new String[]{nickname, fullAvatarUrl};

        } catch (Exception e) {
            // Fallback: Return raw string as name, empty string as image
            return new String[]{userName, ""};
        }
    }


    private void updateUserMicrophone(boolean isMicrophoneOn) {
        if (binding != null) {
            binding.foregroundAvatarContentMic.setVisibility(isMicrophoneOn ? GONE : VISIBLE);
        }
    }

    private void updateUserInRoomAttributes(HashMap<String, String> inRoomAttributes) {
        boolean isHost;
        if (inRoomAttributes != null) {
            isHost = ZegoLiveAudioRoomRole.get(inRoomAttributes.get("role")) == ZegoLiveAudioRoomRole.HOST;
        } else {
            isHost = false;
        }
        if (binding != null) {
            binding.foregroundIconHost.setVisibility(isHost ? View.VISIBLE : View.GONE);
        }
    }
}
