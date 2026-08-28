package io.dcloud.uniplugin.activity.task.widget;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewGroup;


import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import io.dcloud.uniplugin.activity.task.ContentTaskActivity;
import io.dcloud.uniplugin.activity.task.config.CTDConfig;
import io.dcloud.uniplugin.activity.task.config.TaskInfo;

@SuppressLint("ViewConstructor")
public class TaskCTDView extends ProgressView {

    // 更新频率
    private static final long period = 1000L;
    // 任务回调
    private TaskListener taskListener;
    // 目标（条数/时长）
    private final int target;
    // 倒计时的配置样式
    private final int ctdStyle;
    // 定时任务
    private CTDTask ctdTask;
    private Timer timer;

    public TaskCTDView(ContentTaskActivity activity, final TaskInfo taskInfo, final CTDConfig ctdConfig) {
        super(activity, ctdConfig);
        target = taskInfo.target;
        ctdStyle = ctdConfig.style;
        this.ctdTask = new CTDTask(taskInfo, new CTDTask.TaskListener() {
            @Override
            public void onProgress(float progress, int timeRemained) {
                // 文字或文字+进度条
                if (ctdStyle == 0 || ctdStyle == 1) {
                    if (taskInfo.style == 1) {
                        // 条数
                        updateText(progress, String.format(Locale.getDefault(), "%1$d/%2$d", timeRemained, target));
                    } else if (taskInfo.style == 2) {
                        // 倒数时长
                        updateText(progress, String.format(Locale.getDefault(), "%dS", timeRemained));
                    }
                }
                // 仅图片
                if (ctdStyle > 1) {
                    updateImage(progress);
                }
            }

            @Override
            public void onFinish() {
                if (taskListener != null) {
                    taskListener.onFinish();
                    taskListener = null;
                }
                // 回收资源
                onDestroy();
                TaskCTDView.this.post(() -> {
                    setVisibility(View.GONE);
                    try {
                        ((ViewGroup) getParent()).removeView(TaskCTDView.this);
                    } catch (Throwable ignore) {

                    }
                });
            }
        });
        this.timer = new Timer();
    }

    /**
     * 任务回调
     */
    public void setTaskListener(TaskListener taskListener) {
        this.taskListener = taskListener;
    }

    /**
     * 开始任务
     */
    public void start() {
        if (timer == null) {
            timer = new Timer();
        }
        timer.schedule(ctdTask, 0L, period);
        invalidate();
    }

    /**
     * 获取任务
     */
    public CTDTask getCtdTask() {
        return this.ctdTask;
    }

    /**
     * 销毁
     */
    public void onDestroy() {
        taskListener = null;
        if (this.timer != null) {
            try {
                timer.cancel();
                timer.purge();
                timer = null;
                ctdTask.cancel();
                ctdTask = null;
            } catch (Throwable ignore) {

            }
        }
    }

    /**
     * 任务回调
     */
    public interface TaskListener {

        /**
         * 任务完成
         */
        void onFinish();
    }

    /**
     * 定时任务
     */
    public static class CTDTask extends TimerTask {

        // 任务配置
        private final TaskInfo taskInfo;
        // 任务回调
        private final TaskListener taskListener;

        // 当前进度
        private int currVideoCount;
        // 当前播放的视频ID，用于暂停计时
        private volatile String currVideoId;
        // 上条完成计时的视频ID
        private String lastSuccessVideoId;
        // 当条视频的播放时长
        private int currPlayDuration;
        // 总播放时长
        private int totalPlayDuration;
        // 播放状态
        private boolean isVideoPlaying;

        private CTDTask(TaskInfo taskInfo, TaskListener listener) {
            this.taskInfo = taskInfo;
            this.taskListener = listener;
        }

        @Override
        public void run() {
            // 暂停播放
            if (!isVideoPlaying) {
                return;
            }
            // 当条视频播放时长达到限制
            if (Objects.equals(lastSuccessVideoId, currVideoId)) {
                currPlayDuration = 0;
                return;
            }
            switch (taskInfo.style) {
                // 条数
                case 1:
                    // 单条时长满足最低要求
                    if (currPlayDuration++ >= taskInfo.videoDuration) {
                        // 计数
                        currVideoCount += 1;
                        // 重置
                        currPlayDuration = 0;
                        // 记录
                        lastSuccessVideoId = currVideoId;
                    }
                    // 总条数满足要求
                    if (currVideoCount >= taskInfo.target) {
                        // 回调完成
                        taskListener.onFinish();
                        // 取消任务
                        cancel();
                    } else {
                        // 更新进度（当前条数满足任务需求的进度, 当前完成的条数）
                        taskListener.onProgress(currPlayDuration / (float) taskInfo.videoDuration, currVideoCount);
                    }
                    break;
                // 时长
                case 2:
                    // 单条时长超过最高限制
                    if (currPlayDuration++ >= taskInfo.videoDuration) {
                        return;
                    }
                    // 完成任务
                    if (totalPlayDuration++ >= taskInfo.target) {
                        // 重置
                        totalPlayDuration = 0;
                        // 回调完成
                        taskListener.onFinish();
                        // 取消任务
                        cancel();
                    } else {
                        // 更新进度（当前播放总时长进度，剩余时长）
                        taskListener.onProgress(totalPlayDuration / (float) taskInfo.target, taskInfo.target - totalPlayDuration);
                    }
                    break;
            }
        }

        /**
         * 播放暂停
         */
        public void onVideoPaused() {
            isVideoPlaying = false;
        }

        /**
         * 播放恢复
         */
        public void onVideoResume() {
            isVideoPlaying = true;
        }

        /**
         * 播放开始
         */
        public void onVideoStart(String id) {
            isVideoPlaying = true;
            currVideoId = id;
            currPlayDuration = 0;
        }

        /**
         * 播放完成
         */
        public void onVideoCompleted() {
            isVideoPlaying = false;
        }

        /**
         * 任务回调
         */
        private interface TaskListener {
            /**
             * 进度更新
             *
             * @param progress     百分比进度
             * @param timeRemained 已完成条数/剩余时长
             */
            void onProgress(float progress, int timeRemained);

            /**
             * 任务完成
             */
            void onFinish();
        }

    }

}
