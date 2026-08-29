package io.dcloud.uniplugin.activity.reward;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import uni.dcloud.io.uniplugin_module.R;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class CheckCallBackView extends LinearLayout {

    private static Config config;

    private final HashMap<AdEvent, ItemView> items = new HashMap<>();

    public CheckCallBackView(Context context) {
        this(context, null);
    }

    public CheckCallBackView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CheckCallBackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(LinearLayout.VERTICAL);
        if (config == null) {
            initData();
        }
        LayoutParams lp = new LayoutParams(-1, -2);
        lp.bottomMargin = 24;
        TextView child = new TextView(context);
        child.setTextSize(16);
        child.setText("广告回调信息：");
        addView(child, lp);
    }

    public void setAdType(AdType adType) {
        for (Name name : config.names) {
            if (name.adType == adType.ordinal()) {
                LayoutParams lp = new LayoutParams(-1, -2);
                lp.bottomMargin = 10;
                LinkedList<EventData> events = new LinkedList<>();
                events.add(config.descriptions.get(0));
                events.add(config.descriptions.get(1));
                for (EventData event : name.events) {
                    event.desc = config.descriptions.get(event.eventId).desc;
                    events.add(event);
                }
                for (EventData event : events) {
                    ItemView child = new ItemView(getContext(), event.name, event.desc);
                    items.put(AdEvent.values()[event.eventId], child);
                    addView(child, lp);
                }
                return;
            }
        }
        throw new IllegalArgumentException("Invalid adType: " + adType);
    }

    public void onAdLoadError() {
        onItemChecked(AdEvent.LoadError);
    }

    public void onAdLoaded() {
        onItemChecked(AdEvent.Loaded);
    }

    public void onAdShowError() {
        onItemChecked(AdEvent.ShowError);
    }

    public void onAdShow() {
        onItemChecked(AdEvent.Show);
    }

    public void onAdClick() {
        onItemChecked(AdEvent.Click);
    }

    public void onAdRewardVerify() {
        onItemChecked(AdEvent.RewardVerify);
    }

    public void onAdComplete() {
        onItemChecked(AdEvent.Complete);
    }

    public void onAdClose() {
        onItemChecked(AdEvent.Close);
    }

    private void onItemChecked(AdEvent event) {
        ItemView itemView = items.get(event);
        if (itemView != null) {
            itemView.setEnabled(true);
        }
    }

    public void reset() {
        for (ItemView itemView : items.values()) {
            itemView.setEnabled(false);
        }
    }

    @SuppressLint("ViewConstructor")
    private static class ItemView extends ConstraintLayout {

        private final TextView nameTV;
        private final TextView descTV;

        @SuppressLint("ResourceType")
        private ItemView(Context context, String name, String desc) {
            super(context);
//            LayoutInflater.from(context).inflate(R.layout.item_ad_event, this, true);
            nameTV = null;
//            nameTV.setText(name);
            descTV = null;
//            descTV.setText(desc);
        }

        @Override
        public void setEnabled(boolean enabled) {
            nameTV.setEnabled(enabled);
            descTV.setEnabled(enabled);
        }

    }

    private void initData() {
//        try {
//            InputStream inputStream = getResources().openRawResource(R.raw.ad_events);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//            config = new Gson().fromJson(reader, Config.class);
//            reader.close();
//            inputStream.close();
//        } catch (IOException | JsonSyntaxException ignore) {
//
//        }
    }

    public enum AdType {
        SplashAd,
        InterstitialAd,
        RewardedAd,
        MovieAd,
        VoiceAd
    }

    private enum AdEvent {
        LoadError,
        Loaded,
        ShowError,
        Show,
        Click,
        RewardVerify,
        Complete,
        Close
    }

    private static class Config {
        private List<EventData> descriptions;
        private List<Name> names;
    }

    private static class EventData {
        @SerializedName("id")
        private int eventId;
        private String desc;
        private String name;
    }

    private static class Name {
        @SerializedName("type")
        private int adType;
        private List<EventData> events;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.items.clear();
    }

}