package io.dcloud.uniplugin.activity.reward;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.zj.zjsdk.api.v2.rewarded.ZJRewardedAd;
import com.zj.zjsdk.api.v2.rewarded.ZJRewardedAdInteractionListener;
import com.zj.zjsdk.api.v2.rewarded.ZJRewardedAdLoadListener;

import io.dcloud.uniplugin.activity.LastRoomLeave;
import io.dcloud.uniplugin.others.DataCenter;
import io.dcloud.uniplugin.others.SPUtil;

public class RewardedAds implements ZJRewardedAdInteractionListener {

    private ZJRewardedAd ad;
    private boolean isRewardCache;
    private CheckCallBackView checkCallBackView;

    private Activity context;

    private ProgressDialog loadingDialog;

    public static RewardedAdProvider rewardedAdProvider;

    public void setContext(Activity activity){
        context = activity;
    }

    private static LastRoomLeave roomLeaveListener;

    public static void setRoomLeaveListener(LastRoomLeave roomLeaveListener) {
        RewardedAds.roomLeaveListener = roomLeaveListener;
    }

    public void loadAd(boolean isShowAd) {
        System.out.println("here ---oh");
        loadingDialog = new ProgressDialog(context);
        loadingDialog.setMessage("加载中...");
        loadingDialog.setCancelable(false); // Prevents user from canceling it by tapping outside
        loadingDialog.show();
        if (isRewardCache) {
            // 预加载已开启时
            if (rewardedAdProvider.isValid()) {
                // 存在可用广告
                if (isShowAd) {
                    // 自动展示
                    if (loadingDialog != null && loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                    showAd();
                } else {
                    // 提示加载成功
                    Toast.makeText(context, "广告加载成功", Toast.LENGTH_SHORT).show();
                }
            } else {
                // 无可用广告，缓存新的并监听加载回调
                rewardedAdProvider.cache(context, new ZJRewardedAdLoadListener() {

                    /**
                     * 加载出错
                     *
                     * @param code 错误码
                     * @param msg  错误信息
                     */
                    @Override
                    public void onError(int code, @NonNull String msg) {
//                        checkCallBackView.onAdLoadError();
                        if (loadingDialog != null && loadingDialog.isShowing()) {
                            loadingDialog.dismiss();
                        }
                        Toast.makeText(context, "广告请求失败: " + code + "-" + msg, Toast.LENGTH_SHORT).show();
                    }

                    /**
                     * 加载成功
                     *
                     * @param rewardedAd 广告对象
                     */
                    @Override
                    public void onAdLoaded(@NonNull ZJRewardedAd rewardedAd) {
                       // checkCallBackView.onAdLoaded();
                        if (loadingDialog != null && loadingDialog.isShowing()) {
                            loadingDialog.dismiss();
                        }
                        if (isShowAd) {
                            // 自动展示
                            showAd();
                        }
                    }

                });
            }
        } else {
            // 实时请求
            ZJRewardedAd.loadAd(context, DataCenter.PosId.REWARDED_AD, SPUtil.getInstance().getUID(), new ZJRewardedAdLoadListener() {

                /**
                 * 加载出错
                 *
                 * @param code 错误码
                 * @param msg  错误信息
                 */
                @Override
                public void onError(int code, @NonNull String msg) {
                    //checkCallBackView.onAdLoadError();
                    if (loadingDialog != null && loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                    Toast.makeText(context, "广告请求失败: " + code + "-" + msg, Toast.LENGTH_SHORT).show();
                }

                /**
                 * 加载成功
                 *
                 * @param rewardedAd 广告对象
                 */
                @Override
                public void onAdLoaded(@NonNull ZJRewardedAd rewardedAd) {
                   // checkCallBackView.onAdLoaded();
                    if (loadingDialog != null && loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                    RewardedAds.this.ad = rewardedAd;
                    if (isShowAd) {
                        // 自动展示
                        showAd();
                    }
                }

            });
        }
    }
    private boolean isValid() {
        if (isRewardCache) {
            // 预加载已开启时，判断是否已缓存
            return rewardedAdProvider.isValid();
        } else {
            // 实时请求时。判断当前加载状态
            return ad != null && ad.isValid();
        }
    }
    private void showAd() {
        // 先确认广告有效
        if (isValid()) {
            if (isRewardCache) {
                rewardedAdProvider.show(context, this);
            } else {
                // 配置交互回调
                ad.setAdInteractionListener(this);
                // 展示
                ad.show(context);
            }
        } else {
            Toast.makeText(context, "广告无效，请重新加载", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRewardedAdShow() {

        roomLeaveListener.triggerAd("onshow");
    }

    @Override
    public void onRewardedAdClick() {
        // 点击不等于观看完成，不再当 success 回调，避免误上报
        System.out.println("Reward clicked");
    }

    @Override
    public void onRewardedAdShowError(int i, @NonNull String s) {
        System.out.println("onReward show clicked");
        roomLeaveListener.triggerAd("fail");
    }

    @Override
    public void onRewardVerify(@NonNull String s) {

        roomLeaveListener.triggerAd("success");
        System.out.println("verify clicked");
    }

    @Override
    public void onRewardedAdClose() {
        roomLeaveListener.triggerAd("close");
        System.out.println("Reward Closed clicked");
    }
}
