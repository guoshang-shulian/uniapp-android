package io.dcloud.uniplugin.activity.reward;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import com.zj.zjsdk.api.v2.rewarded.ZJRewardedAd;
import com.zj.zjsdk.api.v2.rewarded.ZJRewardedAdInteractionListener;
import com.zj.zjsdk.api.v2.rewarded.ZJRewardedAdLoadListener;

import io.dcloud.uniplugin.others.DataCenter;
import io.dcloud.uniplugin.others.SPUtil;
import uni.dcloud.io.uniplugin_module.R;
import io.dcloud.uniplugin.activity.E2EActivity;

public class RewardedAdActivity extends E2EActivity implements ZJRewardedAdInteractionListener {

    private ZJRewardedAd ad;
    private boolean isRewardCache;
    private CheckCallBackView checkCallBackView;

    public static RewardedAdProvider rewardedAdProvider;

    private ProgressDialog loadingDialog;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setHomeButtonEnabled(true);
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }
        setTitle(getIntent().getStringExtra("title"));
        //setContentView(R.layout.activity_rewarded_ad);
        //checkCallBackView = findViewById(R.id.checkCallBackView);

        checkCallBackView.setAdType(CheckCallBackView.AdType.RewardedAd);
        isRewardCache = SPUtil.getInstance().isRewardCache() && rewardedAdProvider != null;
    }

    public void onClick(View view) {
        int id = view.getId();
//        if (id == R.id.loadAndShow) {
//            loadAd(true);
//            checkCallBackView.reset();
//        } else if (id == R.id.loadOnly) {
//            loadAd(false);
//            checkCallBackView.reset();
//        } else if (id == R.id.isValid) {
//            Toast.makeText(this, isValid() ? "广告有效" : "广告无效，请重新加载", Toast.LENGTH_SHORT).show();
//        } else if (id == R.id.show) {
//            showAd();
//        }
    }

    /**
     * 加载广告
     *
     * @param isShowAd 是否自动展示
     */
    private void loadAd(boolean isShowAd) {
        loadingDialog = new ProgressDialog(RewardedAdActivity.this);
        loadingDialog.setMessage("加载中...");
        loadingDialog.setCancelable(false); // Prevents user from canceling it by tapping outside
        loadingDialog.show();
        if (isRewardCache) {
            // 预加载已开启时
            if (rewardedAdProvider.isValid()) {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
                // 存在可用广告
                if (isShowAd) {
                    // 自动展示
                    showAd();
                } else {
                    // 提示加载成功
                    Toast.makeText(this, "广告加载成功", Toast.LENGTH_SHORT).show();
                }

            } else {
                // 无可用广告，缓存新的并监听加载回调
                rewardedAdProvider.cache(this, new ZJRewardedAdLoadListener() {

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
                        Toast.makeText(RewardedAdActivity.this, "广告请求失败: " + code + "-" + msg, Toast.LENGTH_SHORT).show();
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
            ZJRewardedAd.loadAd(this, DataCenter.PosId.REWARDED_AD, SPUtil.getInstance().getUID(), new ZJRewardedAdLoadListener() {

                /**
                 * 加载出错
                 *
                 * @param code 错误码
                 * @param msg  错误信息
                 */
                @Override
                public void onError(int code, @NonNull String msg) {
                   // checkCallBackView.onAdLoadError();
                    if (loadingDialog != null && loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                    Toast.makeText(RewardedAdActivity.this, "广告请求失败: " + code + "-" + msg, Toast.LENGTH_SHORT).show();
                }

                /**
                 * 加载成功
                 *
                 * @param rewardedAd 广告对象
                 */
                @Override
                public void onAdLoaded(@NonNull ZJRewardedAd rewardedAd) {
                  //  checkCallBackView.onAdLoaded();
                    if (loadingDialog != null && loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }
                    RewardedAdActivity.this.ad = rewardedAd;
                    if (isShowAd) {
                        // 自动展示
                        showAd();
                    }
                }

            });
        }
    }

    /**
     * 判断广告有效性
     *
     * @return 广告是否有效
     */
    private boolean isValid() {
        if (isRewardCache) {
            // 预加载已开启时，判断是否已缓存
            return rewardedAdProvider.isValid();
        } else {
            // 实时请求时。判断当前加载状态
            return ad != null && ad.isValid();
        }
    }

    /**
     * 展示广告
     */
    private void showAd() {
        // 先确认广告有效
        if (isValid()) {
            if (isRewardCache) {
                rewardedAdProvider.show(this, this);
            } else {
                // 配置交互回调
                ad.setAdInteractionListener(this);
                // 展示
                ad.show(this);
            }
        } else {
            Toast.makeText(this, "广告无效，请重新加载", Toast.LENGTH_SHORT).show();
        }
    }

    //=========================================ZJRewardedAdInteractionListener

    /**
     * 展示失败
     *
     * @param code 错误码
     * @param msg  错误信息，前端友好
     */
    @Override
    public void onRewardedAdShowError(int code, @NonNull String msg) {
        checkCallBackView.onAdShowError();
    }

    /**
     * 激励展示
     */
    @Override
    public void onRewardedAdShow() {
        checkCallBackView.onAdShow();
    }

    /**
     * 激励点击
     */
    @Override
    public void onRewardedAdClick() {
        checkCallBackView.onAdClick();
    }

    /**
     * 奖励
     */
    @Override
    public void onRewardVerify(@NonNull String s) {
        checkCallBackView.onAdRewardVerify();
    }

    /**
     * 关闭
     */
    @Override
    public void onRewardedAdClose() {
        checkCallBackView.onAdClose();
    }

    //=========================================ZJRewardedAdInteractionListener

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

}
