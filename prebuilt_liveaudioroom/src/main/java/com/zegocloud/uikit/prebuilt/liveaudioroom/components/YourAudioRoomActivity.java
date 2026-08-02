package com.zegocloud.uikit.prebuilt.liveaudioroom.components;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.zegocloud.uikit.components.message.ZegoInRoomMessageView;
import com.zegocloud.uikit.components.message.ZegoInRoomMessageItemViewProvider;
import com.zegocloud.uikit.prebuilt.liveaudioroom.R;
import com.zegocloud.uikit.prebuilt.liveaudioroom.internal.BottomActionDialog;
import com.zegocloud.uikit.service.defines.ZegoInRoomMessage;

import java.util.List;

public class YourAudioRoomActivity extends BottomActionDialog {

    private static final String TAG = "ZEGO_LOG";

    public YourAudioRoomActivity(@NonNull Context context, List<String> stringList) {
        super(context, stringList);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Double check: Ensure this string exactly matches the lowercase filename of your XML code!
        setContentView(R.layout.liveaudioroom_fragment_liveaudioroom);

        // 2. Safely find the view from your XML
        ZegoInRoomMessageView liveMessageView = findViewById(R.id.live_message_view);

        if (liveMessageView != null) {
            Log.d(TAG, "Layout successfully matched. Waiting for Zego to boot up...");

            // 3. Use .post() to delay the hook binding until the message window is active on screen
            liveMessageView.post(new Runnable() {
                @Override
                public void run() {
                    liveMessageView.setItemViewProvider(new ZegoInRoomMessageItemViewProvider() {
                        @Override
                        public View onCreateView(ViewGroup parent) {
                            // THIS IS THE ECHO YOU ARE LOOKING FOR
                            Log.d(TAG, "We reached here");

                            // Return a basic transparent TextView placeholder so it safely loads
                            return new TextView(parent.getContext());
                        }

                        @Override
                        public void onBindView(View view, ZegoInRoomMessage inRoomMessage, int position) {
                            // Leave empty as requested
                        }
                    });
                    Log.d(TAG, "Echo hook successfully attached to Zego Message View!");
                }
            });

        } else {
            Log.e(TAG, "CRITICAL ERROR: Could not find R.id.live_message_view. Check if your layout file name is identical to your setContentView parameter.");
        }
    }
}
