//package io.dcloud.uniplugin;
//
//import android.app.Activity;
//import android.util.Log;
//import com.taobao.weex.annotation.JSMethod;
//import com.taobao.weex.bridge.JSCallback;
//import io.dcloud.feature.uniapp.common.UniModule;
//
//// Import the correct package path as found in your AAR file
//import com.zj.zjsdk.api.v2.contentad.ZJContentAd;
//import com.zj.zjsdk.api.v2.contentad.ZJContentAdLoadListener;
//import com.zj.zjsdk.api.v2.draw.ZJDrawAd;
//import com.zj.zjsdk.api.v2.draw.ZJDrawAdLoadListener;
//import com.zj.zjsdk.api.v2.interstitial.ZJInterstitialAd;
//import com.zj.zjsdk.api.v2.interstitial.ZJInterstitialAdLoadListener;
//import com.zj.zjsdk.api.v2.mini.ZJMiniProgramAd;
//import com.zj.zjsdk.api.v2.mini.ZJMiniProgramAdLoadListener;
//import com.zj.zjsdk.api.v2.movie.ZJMovieAd;
//import com.zj.zjsdk.api.v2.movie.ZJMovieAdLoadListener;
//import com.zj.zjsdk.api.v2.rewarded.ZJRewardedAd;
//import com.zj.zjsdk.api.v2.rewarded.ZJRewardedAdLoadListener;
//import com.zj.zjsdk.api.v2.splash.ZJSplashAd;
//import com.zj.zjsdk.api.v2.splash.ZJSplashAdLoadListener;
//import com.zj.zjsdk.api.v2.tube.ZJTubeAd;
//import com.zj.zjsdk.api.v2.tube.ZJTubeAdLoadListener;
//
//import java.util.HashMap;
//import java.util.Map;
//
//public class MyAdModule extends UniModule {
//    private static final String TAG = "ZJSDK_Plugin";
//    private ZJRewardedAd rewardedAd;
//
//    private void loadAD(String posId) {
//        // 加载广告
//        ZJSplashAd.loadAd(this, posId, new ZJSplashAdLoadListener() {
//
//            @Override
//            public void onError(int code, @NonNull String msg) {
//                Log.e("ZJSplashAd", "开屏广告加载出错" + code + "-" + msg);
//            }
//
//            @Override
//            public void onAdLoaded(@NonNull ZJSplashAd splashAd) {
//                // 广告加载成功，配置交互回调
//                splashAd.setAdInteractionListener(SplashActivity.this);
//                splashAd.show(this.container);
//            }
//
//        });
//
//        // 加载广告 userId -> 必须传入
//        ZJRewardedAd.loadAd(this, posId, userId, new ZJRewardedAdLoadListener() {
//
//            @Override
//            public void onError(int code, @NonNull String msg) {
//                Log.e("ZJRewardAd", "激励广告加载出错" + code + "-" + msg);
//            }
//
//            @Override
//            public void onAdLoaded(@NonNull ZJRewardedAd rewardedAd) {
//                // 激励广告加载成功，配置交互回调
//                rewardedAd.setAdInteractionListener(MainActivity.this);
//                rewardedAd.show(MainActivity.this);
//            }
//
//        });
//
//        // 加载广告
//        ZJInterstitialAd.loadAd(this, posId, new ZJInterstitialAdLoadListener() {
//
//            @Override
//            public void onError(int code, @NonNull String msg) {
//                Log.e("ZJInterstitialAd", "插屏广告加载出错" + code + "-" + msg);
//            }
//
//            @Override
//            public void onAdLoaded(@NonNull ZJInterstitialAd interstitialAd) {
//                // 插屏广告加载成功，配置交互回调
//                interstitialAd.setAdInteractionListener(MainActivity.this);
//                interstitialAd.show(MainActivity.this);
//            }
//
//        });
//
//        // 加载广告
//        ZJDrawAd.loadAd(this, posId, 1, new ZJDrawAdLoadListener() {
//
//            @Override
//            public void onError(int code, @NonNull String msg) {
//                Log.e("ZJDrawAd", "视频加载出错" + code + "-" + msg);
//            }
//
//            @Override
//            public void onAdLoaded(@NonNull List<ZJDrawAd> adList) {
//                // 视频加载成功，配置交互回调
//                adList.get(0).setInteractionListener(new ZJDrawAdInteractionListener() {
//
//                                                         //...
//
//                                                         public void onDrawAdRenderSuccess(@NonNull View adView) {
//                                                             // 广告渲染视图，返回adView视图
//                                                             this.container.addView(adView);
//                                                         }
//
//                                                         //...
//
//                                                     }
//                        // 视频流渲染
//                        adList.get(0).render(DrawAdActivity.this);
//            }
//
//        });
//
//        // 加载广告
//        ZJMovieAd.loadAd(posId, true, new ZJMovieAdLoadListener() {
//
//            /**
//             * 加载出错
//             *
//             * @param code 错误码
//             * @param msg  错误信息
//             */
//            @Override
//            public void onError(int code, @NonNull String msg) {
//                Log.e("ZJMovieAd", "视频贴片加载出错" + code + "-" + msg);
//            }
//
//            /**
//             * 加载成功
//             */
//            @Override
//            public void onAdLoaded(@NonNull ZJMovieAd movieAd) {
//                // 广告加载成功，配置交互回调
//                movieAd.setInteractionListener(this);
//                movieAd.show(this, container);
//            }
//
//        });
//
//// 加载广告
//        ZJContentAd.loadAd(posId, new ZJContentAdLoadListener() {
//
//            @Override
//            public void onError(int code, @NonNull String msg) {
//                Log.e("ZJContentAd", "视频内容加载出错" + code + "-" + msg);
//            }
//
//            @Override
//            public void onAdLoaded(@NonNull ZJContentAd contentAd) {
//                // 视频内容加载成功，配置交互回调
//                contentAd.setInteractionListener(ContentActivity.this);
//                // 视频内容展示
//                contentAd.show(ContentActivity.this, R.id.container);
//            }
//
//        });
//// 加载广告
//        ZJMiniProgramAd.loadAd(posId, new ZJMiniProgramAdLoadListener() {
//
//            @Override
//            public void onError(int code, @NonNull String msg) {
//                Log.e("ZJMiniProgramAd", "小程序加载出错" + code + "-" + msg);
//            }
//
//            @Override
//            public void onAdLoaded() {
//                // 广告加载成功
//            }
//
//        });
//
//
//
//    }
//
//    public void load(){
//        // 加载广告
//        ZJTubeAd.loadAd("posId", generateTubeAdConfig(), new ZJTubeAdLoadListener() {
//
//            /**
//             * 加载出错
//             *
//             * @param code 错误码
//             * @param msg  错误信息
//             */
//            @Override
//            public void onError(int code, @NonNull String msg) {
//                Toast.makeText(requireContext(), "加载出错: " + code + "-" + msg, Toast.LENGTH_SHORT).show();
//            }
//
//            /**
//             * 加载成功
//             *
//             * @param ad 广告
//             */
//            @Override
//            public void onAdLoaded(@NonNull ZJTubeAd ad) {
//                Toast.makeText(requireContext(), "加载成功", Toast.LENGTH_SHORT).show();
//                TubeFragment.this.ad = ad;
//
//                ad.setInteractionListener(new ZJTubeAdInteractionListener() {
//
//                    /**
//                     * 配置短剧解锁⼴告交互回调
//                     * 如禁止了SDK内置的解锁对话框，需要在此回调中展示自定义解锁对话框
//                     * 必须调用UnlockCallback的callback方法返回用户是否同意解锁
//                     */
//                    @Override
//                    public void showCustomUnlockDialog(Activity activity, @NonNull ZJTubeAdItem item, @NonNull ZJTubeAdUnlockCallback callback) {
//                        unlockTipDialog = new CustomUnlockTipDialog(item);
//                        unlockTipDialog.setListener(new CustomUnlockTipDialog.DialogListener() {
//
//                            @Override
//                            public void confirm() {
//                                callback.callback(true);
//                                unlockTipDialog = null;
//                            }
//
//                            @Override
//                            public void cancel() {
//                                callback.callback(false);
//                                unlockTipDialog = null;
//                            }
//                        });
//                        unlockTipDialog.show(((FragmentActivity) activity).getSupportFragmentManager(), TAG);
//                    }
//
//                    /**
//                     * 获取解锁广告的广告位ID
//                     * 仅支持激励广告和插全屏，需要返回正确的广告位ID，返回无效的广告位ID时会无法解锁
//                     */
//                    @Override
//                    public String getUnlockAdPosId(@NonNull ZJTubeAdItem zjTubeAdItem) {
//                        String posId;
//                        String tip;
//                        if (new Random().nextBoolean()) {
//                            posId = DataCenter.PosId.REWARDED_AD;
//                            tip = "加载激励广告";
//                        } else {
//                            posId = DataCenter.PosId.INTERSTITIAL;
//                            tip = "加载插屏广告";
//                        }
//                        Toast.makeText(getContext(), tip, Toast.LENGTH_SHORT).show();
//                        return posId;
//                    }
//
//                    /**
//                     * 观看广告解锁短剧成功
//                     */
//                    @Override
//                    public void onUnlockSuccess() {
//                        Toast.makeText(getContext(), "观看广告解锁短剧成功", Toast.LENGTH_SHORT).show();
//                    }
//
//                    /**
//                     * 观看广告解锁短剧失败
//                     *
//                     * @param code 错误码
//                     * @param msg  错误信息
//                     */
//                    @Override
//                    public void onUnlockError(int code, @NonNull String msg) {
//                        Toast.makeText(getContext(), "观看广告解锁短剧失败:[" + code + "-" + msg + "]", Toast.LENGTH_SHORT).show();
//                    }
//
//                });
//
//                // 展示 Fragment
//                getChildFragmentManager()
//                        .beginTransaction()
//                        .add(R.id.container, ad.getFragmentObject())
//                        .show(ad.getFragmentObject())
//                        .commit();
//            }
//
//        });
//
//    }
//
//
//    @JSMethod(uiThread = true)
//    public void loadAndShowVideoAd(String adUnitId, String userId, JSCallback jsCallback) {
//        Activity activity = mUniSDKInstance.getContext() instanceof Activity ? (Activity) mUniSDKInstance.getContext() : null;
//
//        if (activity == null) {
//            if (jsCallback != null) jsCallback.invoke(createResponse(500, "Invalid Activity Context"));
//            return;
//        }
//
//        // Initialize using the modern V2 API structure
//        rewardedAd = new ZJRewardedAd(activity, adUnitId);
//        rewardedAd.setUserId(userId);
//
//        // Wire up the V2 listeners
//        rewardedAd.setAdListener(new ZJRewardedAdListener() {
//            @Override
//            public void onAdLoaded() {
//                Log.d(TAG, "V2 Video Ad Loaded successfully.");
//                if (rewardedAd != null) {
//                    rewardedAd.showAd(); // Displays full screen
//                }
//            }
//
//            @Override
//            public void onAdVideoComplete() {
//                Log.d(TAG, "Video playthrough complete.");
//                if (jsCallback != null) jsCallback.invokeAndKeepAlive(createResponse(200, "VideoCompleted"));
//            }
//
//            @Override
//            public void onAdReward() {
//                Log.d(TAG, "Reward threshold met! Unlocking ZimKit content.");
//                if (jsCallback != null) jsCallback.invokeAndKeepAlive(createResponse(200, "RewardGranted"));
//            }
//
//            @Override
//            public void onAdError(ZJAdError error) {
//                // If ZJAdError requires different getter methods, adjust error.getMsg() or error.getErrorCode() accordingly
//                Log.e(TAG, "Ad failed: " + error.getErrorMessage());
//                if (jsCallback != null) jsCallback.invoke(createResponse(400, error.getErrorMessage()));
//            }
//
//            @Override
//            public void onAdClose() {
//                Log.d(TAG, "Ad overlay closed by client.");
//                if (jsCallback != null) jsCallback.invoke(createResponse(200, "AdClosed"));
//                rewardedAd = null; // Memory management cleanup
//            }
//
//            // Note: If the interface prompts you to implement missing methods,
//            // press Alt+Enter in Android Studio to auto-generate missing overrides like onAdClick() or onAdShow()
//        });
//
//        // Fire the load trigger
//        rewardedAd.loadAd();
//    }
//
//    private Map<String, Object> createResponse(int code, String message) {
//        Map<String, Object> result = new HashMap<>();
//        result.put("code", code);
//        result.put("msg", message);
//        return result;
//    }
//}
