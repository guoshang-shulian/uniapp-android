package com.cool.dianshang.data;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.zj.zjsdk.api.v2.tube.ZJTubeAd;
import com.zj.zjsdk.api.v2.tube.ZJTubeAdConfig;
import com.zj.zjsdk.api.v2.tube.ZJTubeAdInteractionListener;
import com.zj.zjsdk.api.v2.tube.ZJTubeAdItem;
import com.zj.zjsdk.api.v2.tube.ZJTubeAdLoadListener;
import com.zj.zjsdk.api.v2.tube.ZJTubeAdPageListener;
import com.zj.zjsdk.api.v2.tube.ZJTubeAdUnlockCallback;
import com.zj.zjsdk.api.v2.tube.ZJTubeAdVideoListener;

import io.dcloud.uniplugin.others.CustomUnlockTipDialog;
import io.dcloud.uniplugin.others.DataCenter;
import uni.dcloud.io.uniplugin_module.R;

/**
 * 短剧嵌套Fragment演示
 */
public class TubeFragment extends Fragment {

    private static final String TAG = TubeFragment.class.getSimpleName();

    private ZJTubeAd ad;
    private ProgressDialog loadingDialog;


    private CustomUnlockTipDialog unlockTipDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_news_main, container, false);
        root.findViewById(R.id.btn).setVisibility(View.GONE);
        return root;
    }

    private ZJTubeAdConfig generateTubeAdConfig() {
        // 是否关闭SDK的解锁提示对话框，默认值false。true时可以在showAdIfNeeded回调中自定义提示对话框
        return new ZJTubeAdConfig.Builder()
                .isNewUser(false)        // 是否为新用户
                .isOnlyICPNumber(false)  // 是否需要只有备案号内容，默认值false，穿山甲使用
                .setUserId("87876867876876")   // 用户ID
                .setFreeEpisodeCount(10) // 每个剧集前N集免费，默认为3
                .setUnlockEpisodeCount(10)   // 每次解锁X集，默认为2
                .isHideTitleBar(true)   // 是否隐藏剧集主页的TitleBar，默认值false
                .isDisableUnlockTipDialog(false) // 是否关闭SDK的解锁提示对话框，默认值false。true时可以在showAdIfNeeded回调中自定义提示对话框
                .isDisableShowTubePanelEntry(false) // 是否关闭短剧播放页的选集入口，默认值false
                .isHideDetailTitleBar(false)    // 是否隐藏播放页的titleBar，默认值false
                .isHideDetailBottomTitle(false) // 是否隐藏播放页底部的title文案，默认值false
                .isHideDetailBottomDesc(false)  // 是否隐藏播放页底部的内容描述文案，默认值false
                .isHideDetailPlaySeekbar(false) // 是否隐藏播放页底部的进度条，默认值false
                .setProfileName("SDK_Setting_5859213.json")     // 穿山甲参数配置文件，assets目录的相对路径
                .setPageStyle(ZJTubeAdConfig.TubePageStyle.STYLE_DEFAULT) // DEFAULT->默认样式(推荐+剧场) | RECOMMEND_CHANNEL_ONLY->仅推荐频道 | CHANNEL_ONLY->仅剧场频道
                .setTopTubeId(-1)       // 设置买量剧集id, 将在热门短剧最左位置展示该短剧
                .isHideChangeBtn(false) // 是否隐藏换一换按钮，默认为false
                .isHideMoreBtn(false)   // 设置是否隐藏更多按钮
                .bottomOffset(0)        // 设置底部标题文案、进度条、评论按钮底部偏移，单位 DP
                .titleTopMargin(0)      // 设置标题栏距离顶部间距，单位 DP
                .titleLeftMargin(0)     // 设置标题栏距离左间距.单位 DP
                .titleRightMargin(0)    // 设置标题栏距离右间距，单位 DP
                .build();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // 请求广告
        System.out.println("reachered ppp xxx2");
        loadingDialog = new ProgressDialog(requireContext());
        loadingDialog.setMessage("加载中...");
        loadingDialog.setCancelable(false); // Prevents user from canceling it by tapping outside
        loadingDialog.show();
        ZJTubeAd.loadAd(DataCenter.PosId.TUBE_AD, generateTubeAdConfig(), new ZJTubeAdLoadListener() {

            /**
             * 加载出错
             *
             * @param code 错误码
             * @param msg  错误信息
             */
            @Override
            public void onError(int code, @NonNull String msg) {
                System.out.println("reachered ppp xxx3");
                System.out.println(msg);
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
                Toast.makeText(requireContext(), "加载出错: " + code + "-" + msg, Toast.LENGTH_SHORT).show();
            }

            /**
             * 加载成功
             *
             * @param ad 广告
             */
            @Override
            public void onAdLoaded(@NonNull ZJTubeAd ad) {
                System.out.println("reachered ppp xxx4");
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
                System.out.println(ad);
                System.out.println("reachered ppp xxx5");
                //Toast.makeText(requireContext(), "加载成功", Toast.LENGTH_SHORT).show();
                TubeFragment.this.ad = ad;
//                // 配置回调
                setListeners();
//                // 展示短剧
                showAd();
            }

        });
    }

    /**
     * 配置回调
     */
    private void setListeners() {
        // 短剧页面回调
        // 可以不配置
        ad.setPageListener(new ZJTubeAdPageListener() {

            /**
             * 进入页面
             */
            @Override
            public void onPageEnter(@NonNull ZJTubeAdItem adItem) {
                Log.i(TAG, "onPageEnter: " + adItem);
            }

            /**
             * 页面恢复
             */
            @Override
            public void onPageResume(@NonNull ZJTubeAdItem adItem) {
                Log.i(TAG, "onPageResume: " + adItem);
            }

            /**
             * 页面暂停
             */
            @Override
            public void onPagePause(@NonNull ZJTubeAdItem adItem) {
                Log.i(TAG, "onPagePause: " + adItem);
            }

            /**
             * 离开页面
             */
            @Override
            public void onPageLeave(@NonNull ZJTubeAdItem adItem) {
                Log.i(TAG, "onPageLeave: " + adItem);
            }

        });

        // 短剧视频回调
        // 可以不配置
        ad.setVideoListener(new ZJTubeAdVideoListener() {

            /**
             * 播放开始
             */
            @Override
            public void onVideoPlayStart(@NonNull ZJTubeAdItem adItem) {
                Log.i(TAG, "onVideoPlayStart: " + adItem);
            }

            /**
             * 播放暂停
             */
            @Override
            public void onVideoPlayPaused(@NonNull ZJTubeAdItem adItem) {
                Log.i(TAG, "onVideoPlayPaused: " + adItem);
            }

            /**
             * 播放恢复
             */
            @Override
            public void onVideoPlayResume(@NonNull ZJTubeAdItem adItem) {
                Log.i(TAG, "onVideoPlayResume: " + adItem);
            }

            /**
             * 播放完成
             */
            @Override
            public void onVideoPlayCompleted(@NonNull ZJTubeAdItem adItem) {
                Log.i(TAG, "onVideoPlayCompleted: " + adItem);
            }

            /**
             * 播放错误
             */
            @Override
            public void onVideoPlayError(@NonNull ZJTubeAdItem adItem, int what, int extra) {
                Log.i(TAG, "onVideoPlayError: " + adItem + " | " + what + "&" + extra);
            }

        });

        // 配置短剧解锁⼴告交互回调
        ad.setInteractionListener(new ZJTubeAdInteractionListener() {

            /**
             * 如禁止了SDK内置的解锁对话框，需要在此回调中展示自定义解锁对话框
             * 必须调用UnlockCallback的callback方法返回用户是否同意解锁
             */
            @Override
            public void showCustomUnlockDialog(Activity activity, @NonNull ZJTubeAdItem item, @NonNull ZJTubeAdUnlockCallback callback) {
                unlockTipDialog = new CustomUnlockTipDialog(item);
                unlockTipDialog.setListener(new CustomUnlockTipDialog.DialogListener() {
                    @Override
                    public void confirm() {
                        callback.callback(true);
                        unlockTipDialog = null;
                    }

                    @Override
                    public void cancel() {
                        callback.callback(false);
                        unlockTipDialog = null;
                    }
                });
                unlockTipDialog.show(((FragmentActivity) activity).getSupportFragmentManager(), TAG);
            }

            /**
             * 获取解锁广告的广告位ID
             * 仅支持激励视频和插全屏，返回无效的广告位ID时会无法解锁
             */
            @Override
            public String getUnlockAdPosId(@NonNull ZJTubeAdItem zjTubeAdItem) {
                String posId;
                String tip;
//                if (new Random().nextBoolean()) {
                posId = DataCenter.PosId.REWARDED_AD;
                tip = "加载激励广告";
//                } else {
//                    posId = DataCenter.PosId.INTERSTITIAL;
//                    tip = "加载插屏广告";
//                }
                Toast.makeText(getContext(), tip, Toast.LENGTH_SHORT).show();
                return posId;
            }

            /**
             * 观看广告解锁短剧成功
             */
            @Override
            public void onUnlockSuccess() {
                Toast.makeText(getContext(), "观看广告解锁短剧成功", Toast.LENGTH_SHORT).show();
            }

            /**
             * 观看广告解锁短剧失败
             *
             * @param code 错误码
             * @param msg  错误信息
             */
            @Override
            public void onUnlockError(int code, @NonNull String msg) {
                Toast.makeText(getContext(), "观看广告解锁短剧失败:[" + code + "-" + msg + "]", Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void showAd() {
        // 1. Safety check to make sure the fragment is safely attached to the Activity
        if (!isAdded() || getActivity() == null || ad == null || ad.getFragmentObject() == null) {
            return;
        }
        System.out.println("reachered ppp xxx6");
        // 2. Clear any old video engine fragments and replace it cleanly with the new one
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.container, ad.getFragmentObject())
                .commitAllowingStateLoss();
    }


    @Override
    public void onResume() {
        super.onResume();
        if (ad != null) {
            // 从其他页面切换后需要调用
            ad.onResume();
        }
    }

    @Override
    public void onDestroy() {
        if (ad != null) {
            // 主动释放资源
            ad.onDestroy();
        }
        super.onDestroy();
    }

}
