package io.dcloud.uniplugin.activity.task;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;



import io.dcloud.uniplugin.activity.task.config.CTDConfig;
import io.dcloud.uniplugin.activity.task.config.TaskInfo;
import io.dcloud.uniplugin.activity.task.widget.TaskCTDView;
import io.dcloud.uniplugin.others.DataCenter;
import com.zj.zjsdk.api.v2.contentad.ZJContentAd;
import com.zj.zjsdk.api.v2.contentad.ZJContentAdItem;
import com.zj.zjsdk.api.v2.contentad.ZJContentAdLoadListener;
import com.zj.zjsdk.api.v2.contentad.ZJContentAdVideoListener;
import uni.dcloud.io.uniplugin_module.R;
/**
 * 视频内容浏览任务场景模拟
 */
public class ContentTaskActivity extends AppCompatActivity {

    // 任务提示界面
    private TaskCTDView ctdView;

    // 视频内容对象
    private ZJContentAd ad;

    private ProgressDialog loadingDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_task);

        // 任务条件
        TaskInfo taskInfo = new TaskInfo.Builder()
                .setStyle(1)
                .setTarget(5)
                .setVideoDuration(10)
                .build();
        // 任务View的样式
        CTDConfig ctdConfig = CTDConfig.def;

        loadingDialog = new ProgressDialog(ContentTaskActivity.this);
        loadingDialog.setMessage("加载中...");
        loadingDialog.setCancelable(false); // Prevents user from canceling it by tapping outside
        loadingDialog.show();

        findViewById(R.id.backBtn).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        if (taskInfo.style != 0) {
            this.ctdView = new TaskCTDView(this, taskInfo, ctdConfig);
            this.ctdView.setTaskListener(() ->

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ContentTaskActivity.this, "完成任务", Toast.LENGTH_SHORT).show();
                        }
                    })


            //Toast.makeText(ContentTaskActivity.this, "完成任务", Toast.LENGTH_SHORT).show()
            );
            this.ctdView.setVisibility(View.GONE);
            ((ViewGroup) findViewById(R.id.root)).addView(ctdView);
        }
        loadAd();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (ad == null || !ad.onBackPressed()) {
                    if (ctdView != null) {
                        ctdView.onDestroy();
                        ctdView = null;
                    }
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void loadAd() {
        ZJContentAd.loadAd(DataCenter.PosId.CONTENT_AD, new ZJContentAdLoadListener() {

            /**
             * 加载出错
             *
             * @param code 错误码
             * @param msg  错误信息
             */
            @Override
            public void onError(int code, @NonNull String msg) {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
                Toast.makeText(ContentTaskActivity.this, "加载出错: " + code + "-" + msg, Toast.LENGTH_SHORT).show();
            }

            /**
             * 加载成功
             *
             * @param ad 广告
             */
            @Override
            public void onAdLoaded(@NonNull ZJContentAd ad) {
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
                ContentTaskActivity.this.ad = ad;
                ad.setVideoListener(new ZJContentAdVideoListener() {
                    @Override
                    public void onVideoPlayStart(@NonNull ZJContentAdItem item) {
                        TaskCTDView.CTDTask task;
                        if (ctdView != null && (task = ctdView.getCtdTask()) != null) {
                            task.onVideoStart(item.id);
                        }
                    }

                    @Override
                    public void onVideoPlayPaused(@NonNull ZJContentAdItem item) {
                        TaskCTDView.CTDTask task;
                        if (ctdView != null && (task = ctdView.getCtdTask()) != null) {
                            task.onVideoPaused();
                        }
                    }

                    @Override
                    public void onVideoPlayResume(@NonNull ZJContentAdItem item) {
                        TaskCTDView.CTDTask task;
                        if (ctdView != null && (task = ctdView.getCtdTask()) != null) {
                            task.onVideoResume();
                        }
                    }

                    @Override
                    public void onVideoPlayCompleted(@NonNull ZJContentAdItem item) {
                        TaskCTDView.CTDTask task;
                        if (ctdView != null && (task = ctdView.getCtdTask()) != null) {
                            task.onVideoCompleted();
                        }
                    }

                    @Override
                    public void onVideoPlayError(@NonNull ZJContentAdItem item, int i, int i1) {
                        // ignore
                    }
                });
                ad.showAd(ContentTaskActivity.this, R.id.container);
                if (ctdView != null) {
                    ctdView.setVisibility(View.VISIBLE);
                    ctdView.start();
                }
            }

        });

    }

}
