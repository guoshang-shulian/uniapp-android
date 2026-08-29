package io.dcloud.uniplugin.activity.reward;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.zj.zjsdk.api.v2.rewarded.ZJRewardedAd;
import com.zj.zjsdk.api.v2.rewarded.ZJRewardedAdInteractionListener;
import com.zj.zjsdk.api.v2.rewarded.ZJRewardedAdLoadListener;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 激励视频缓存Demo
 */
public class RewardedAdProvider {

    /**
     * 缓存的条目数
     */
    private final int cacheSize;

    /**
     * 广告位ID
     * 多广告位时需要改为其他数据结构存储
     */
    private final String posId;

    /**
     * 用户ID
     */
    private final String userId;

    /**
     * 广告对象
     */
    private LinkedBlockingQueue<ZJRewardedAd> cacheQueue;

    /**
     * 用户ID变化时需要重新构造
     */
    public RewardedAdProvider(String posId, String userId) {
        this(posId, userId, 1);
    }

    public RewardedAdProvider(String posId, String userId, int size) {
        this.posId = posId;
        this.userId = userId;
        this.cacheSize = size;
        cacheQueue = new LinkedBlockingQueue<>(size);
    }

    /**
     * 缓存
     */
    public void cache(Activity activity) {
        cache(activity, null);
    }

    public void cache(Activity activity, ZJRewardedAdLoadListener callback) {
        int size = cacheQueue.size();
        for (int i = size; i < cacheSize; i++) {
            ZJRewardedAd.loadAd(activity, posId, userId, new ZJRewardedAdLoadListener() {
                @Override
                public void onError(int code, @NonNull String msg) {
                    if (callback != null) {
                        callback.onError(code, msg);
                    }
                    Log.i("RewardedAdProvider", "激励视频缓存失败: " + code + "-" + msg + "\n已有缓存数: " + cacheQueue.size());
                }

                @Override
                public void onAdLoaded(@NonNull ZJRewardedAd rewardedAd) {
                    if (rewardedAd.isValid()) {
                        cacheQueue.offer(rewardedAd);
                        Log.i("RewardedAdProvider", "当前激励视频缓存成功，已有缓存数: " + cacheQueue.size());
                    } else {
                        Log.i("RewardedAdProvider", "当前激励视频缓存失败，已有缓存数: " + cacheQueue.size());
                    }
                    if (callback != null) {
                        callback.onAdLoaded(rewardedAd);
                    }
                }
            });
        }
    }

    /**
     * 判断是否有可用对象
     */
    public synchronized boolean isValid() {
        if (!cacheQueue.isEmpty()) {
            LinkedBlockingQueue<ZJRewardedAd> tmp = new LinkedBlockingQueue<>(cacheSize);
            while (!cacheQueue.isEmpty()) {
                ZJRewardedAd ad = cacheQueue.poll();
                if (ad != null && ad.isValid()) {
                    tmp.offer(ad);
                }
            }
            cacheQueue = tmp;
        }
        return !cacheQueue.isEmpty();
    }

    /**
     * 展示广告
     *
     * @param activity 当前的Activity
     */
    public void show(Activity activity, ZJRewardedAdInteractionListener rewardListener) {
        ZJRewardedAd ad = cacheQueue.poll();
        if (ad != null && ad.isValid()) {
            ad.setAdInteractionListener(rewardListener);
            ad.show(activity);
        } else {
            cache(activity);
            rewardListener.onRewardedAdShowError(-1, "当前无可用广告");
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        cacheQueue.clear();
        cacheQueue = null;
    }

}
