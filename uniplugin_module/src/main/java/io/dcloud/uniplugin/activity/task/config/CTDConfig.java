package io.dcloud.uniplugin.activity.task.config;

import android.graphics.Color;

import java.io.Serializable;
import java.util.Map;

/**
 * 倒计时组件的配置
 */
@SuppressWarnings("unused")
public class CTDConfig implements Serializable {

    public static final CTDConfig def = new Builder()
            .setStyle(1)
            .setWidth(140)
            .setHeight(140)
            .setTextColor(Color.WHITE)
            .setForegroundColor(Color.BLACK)
            .setBackgroundColor(Color.WHITE)
            .build();

    // 视图宽
    public final int width;

    // 视图高
    public final int height;

    // 样式：0-> 仅文字 | 1-> 文字+进度条 | 2-> 仅图标 | 3-> 图标+进度条
    public final int style;

    // 文字颜色
    public final int textColor;

    // 进度条前景色
    public final int foregroundColor;

    // 进度条背景色
    public final int backgroundColor;

    // 进度与图片地址
    public final Map<Float, String> imageUrl;

    private CTDConfig(Builder builder) {
        this.width = builder.width;
        this.height = builder.height;
        this.style = builder.style;
        this.textColor = builder.textColor;
        this.foregroundColor = builder.foregroundColor;
        this.backgroundColor = builder.backgroundColor;
        this.imageUrl = builder.imageUrl;
    }

    public static class Builder {

        // 视图宽
        private int width;

        // 视图高
        private int height;

        // 样式：0-> 仅文字 | 1-> 文字+进度条 | 2-> 仅图标 | 3-> 图标+进度条
        private int style;

        // 文字颜色
        private int textColor;

        // 进度条前景色
        private int foregroundColor;

        // 进度条背景色
        private int backgroundColor;

        // 进度与图片地址
        private Map<Float, String> imageUrl;

        public Builder setWidth(int width) {
            this.width = width;
            return this;
        }

        public Builder setHeight(int height) {
            this.height = height;
            return this;
        }

        public Builder setStyle(int style) {
            this.style = style;
            return this;
        }

        public Builder setTextColor(int textColor) {
            this.textColor = textColor;
            return this;
        }

        public Builder setForegroundColor(int foregroundColor) {
            this.foregroundColor = foregroundColor;
            return this;
        }

        public Builder setBackgroundColor(int backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder setImageUrlMap(Map<Float, String> map) {
            this.imageUrl = map;
            return this;
        }

        public CTDConfig build() {
            return new CTDConfig(this);
        }

    }

}
