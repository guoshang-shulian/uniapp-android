package io.dcloud.uniplugin.activity.reward;

import com.alibaba.fastjson.JSONObject;

/**
 * 激励视频广告事件回调（原生 -> uniapp）
 * event: show | success | close | error
 */
public interface AdEventListener {

    void onAdEvent(JSONObject event);
}
