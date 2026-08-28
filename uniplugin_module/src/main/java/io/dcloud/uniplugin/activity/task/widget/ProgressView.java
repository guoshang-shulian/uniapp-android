package io.dcloud.uniplugin.activity.task.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import com.bumptech.glide.Glide;

import java.util.Map;
import java.util.Objects;

import io.dcloud.uniplugin.activity.task.config.CTDConfig;

/**
 * 视频内容任务进度条组件
 */
@SuppressLint("ViewConstructor")
public class ProgressView extends FrameLayout {

    // 样式配置
    private final CTDConfig config;

    // 进度条背景
    private Paint bgPaint;
    // 进度条前景
    private Paint fgPaint;
    // 文字
    private Paint textPaint;
    // 进度条前景
    private RectF fgRect;
    // 文字
    private Rect textRect;
    // outerCircleRadius
    private float ocr;
    // innerCircleRadius
    private float icr;

    // 是否画背景
    private final boolean isDrawBg;
    // 是否画背景
    private final boolean isDrawText;

    // 样式为图片
    private AppCompatImageView image;

    // 当前进度
    private volatile float currProgress;
    // 当前文字
    private String currText;

    public ProgressView(@NonNull Context context, CTDConfig config) {
        super(context);
        setWillNotDraw(false);
        this.config = config;
        this.isDrawBg = config.style == 1 || config.style == 3;
        this.isDrawText = config.style < 2;

        // LP
        LayoutParams lp = new LayoutParams(this.config.width, this.config.height);
        lp.gravity = Gravity.END;
        int dp12 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, Resources.getSystem().getDisplayMetrics());
        lp.setMargins(dp12, dp12, dp12, dp12);
        setLayoutParams(lp);

        // Paint
        initPaint();

        // 图片
        if (config.style > 1) {
            initImage();
        }
    }

    /**
     * 更新进度和文字
     *
     * @param progress 当前进度
     * @param text     文字
     */
    public void updateText(float progress, String text) {
        if (isDrawText) {
            this.currProgress = progress;
            currText = text;
            postInvalidate();
        }
    }

    private String imageUrl = "def";

    /**
     * 更新进度和图片
     *
     * @param progress 当前进度
     */
    public void updateImage(final float progress) {
        this.currProgress = progress;
        postInvalidate();
        if (!isDrawText) {
            final String url = getUrlByProgress(progress);
            if (url == null || url.trim().isEmpty() || Objects.equals(imageUrl, url)) {
                return;
            }
            imageUrl = url;
            if (image != null) {
                image.post(() -> {
                    if (image != null) {
                        try {
                            Glide.with(image.getContext()).load(url).into(image);
                        } catch (Throwable tr) {
                            Log.w(ProgressView.class.getSimpleName(), tr);
                        }
                    }
                });
            }
        }
    }


    /**
     * 根据进度获取对应的图片
     *
     * @param progress 进度
     * @return 图片URL
     */
    private String getUrlByProgress(float progress) {
        if (config.imageUrl == null || config.imageUrl.isEmpty()) {
            return "";
        }
        float maxProgress = -1;
        String ret = "";
        for (Map.Entry<Float, String> entity : config.imageUrl.entrySet()) {
            float key;
            if ((key = entity.getKey()) > maxProgress && key <= progress) {
                maxProgress = key;
                ret = entity.getValue();
            }
        }
        return ret;
    }


    /**
     * 初始化画笔
     */
    private void initPaint() {
        // 2dp宽
        int stokeWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, Resources.getSystem().getDisplayMetrics());
        // 圆形进度条外半径
        ocr = config.width / 2f;
        // 圆形进度条内半径
        icr = ocr - stokeWidth;
        if (isDrawBg) {
            bgPaint = new Paint();
            bgPaint.setAntiAlias(true);
            bgPaint.setStyle(Paint.Style.STROKE);
            bgPaint.setStrokeWidth(stokeWidth);
            bgPaint.setColor(config.backgroundColor);

            fgPaint = new Paint();
            fgPaint.setAntiAlias(true);
            fgPaint.setColor(config.foregroundColor);
            fgPaint.setStyle(Paint.Style.STROKE);
            fgPaint.setStrokeWidth(stokeWidth);
            fgRect = new RectF();
            //noinspection SuspiciousNameCombination
            fgRect.set(stokeWidth, stokeWidth, config.width - stokeWidth, config.width - stokeWidth);
        }

        if (isDrawText) {
            // 样式包含文字
            textPaint = new Paint();
            textPaint.setAntiAlias(true);
            textPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20, Resources.getSystem().getDisplayMetrics()));
            textPaint.setColor(config.textColor);
            textRect = new Rect();
        }
    }

    /**
     * 初始化图片样式
     */
    private void initImage() {
        image = new AppCompatImageView(getContext());
        LayoutParams lp = new LayoutParams(-1, -1);
        lp.gravity = Gravity.CENTER;
        if (config.style == 4) {
            int dp4 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, Resources.getSystem().getDisplayMetrics());
            lp.setMargins(dp4, dp4, dp4, dp4);
        }
        image.setScaleType(ImageView.ScaleType.FIT_XY);
        addView(image, lp);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        // 进度<0
        if (currProgress < 0) {
            return;
        }
        // 样式需要背景
        if (isDrawBg) {
            // 背景色圆环
            canvas.drawCircle(ocr, ocr, icr, bgPaint);
            // 进度条圆弧
            canvas.drawArc(fgRect, -90, currProgress * 360, false, fgPaint);
        }
        // 样式包含文字
        if (isDrawText) {
            if (TextUtils.isEmpty(currText)) {
                return;
            }
            textPaint.getTextBounds(currText, 0, currText.length(), textRect);
            canvas.drawText(currText, ocr - textRect.width() / 2f, ocr + textRect.height() / 2f, textPaint);
        }
    }

}
