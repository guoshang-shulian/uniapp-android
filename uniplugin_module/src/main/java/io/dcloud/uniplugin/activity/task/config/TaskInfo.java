package io.dcloud.uniplugin.activity.task.config;

import java.io.Serializable;

/**
 * 视频内容任务配置
 * 无任务时不展示任务视图
 * 仅视频数量时，需要配置target为总条数，低于minVideoDuration的播放不会计次
 * 仅总时长时，需要配置target为总的播放时长，单条高于minVideoDuration后才会开始计时，单条高于maxVideoDuration的时长不会增加
 */
public class TaskInfo implements Serializable {

    public static final TaskInfo def = new Builder().setStyle(0).build();

    // 任务类型，默认为0：0->无任务 | 1->仅视频数量 | 2->仅总时长
    public final int style;

    // 目标限制，默认10条/120s
    public final int target;

    // 单次的有效性时长，默认15s
    public final int videoDuration;


    private TaskInfo(Builder builder) {
        this.style = builder.style;
        this.target = builder.target;
        this.videoDuration = builder.videoDuration;
    }

    public static class Builder {

        // 任务类型，默认为0：0->无任务 | 1->仅视频数量 | 2->仅总时长
        private int style;

        // 目标限制，默认10条/120s
        private int target;

        // 单次的有效性时长，条数时为完成条件，时长时为单条最高限制
        private int videoDuration;

        public Builder setStyle(int style) {
            this.style = style;
            return this;
        }

        public Builder setTarget(int target) {
            this.target = target;
            return this;
        }

        public Builder setVideoDuration(int videoDuration) {
            this.videoDuration = videoDuration;
            return this;
        }

        public TaskInfo build() {
            return new TaskInfo(this);
        }

    }

}
