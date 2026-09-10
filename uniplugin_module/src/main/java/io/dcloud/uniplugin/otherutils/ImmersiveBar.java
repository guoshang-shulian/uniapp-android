package io.dcloud.uniplugin.otherutils;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * 状态栏工具（红包系原生页面统一使用）。
 *
 * 两种模式：
 *  · immersive = true  → 沉浸式：内容绘制到状态栏之下（状态栏透明），用于页面顶部是彩色/红色块（红包记录页）
 *  · immersive = false → 非沉浸式：状态栏单独取色，用于顶部是浅灰底的页面（发红包页 #EDEDED）
 *
 * 用法：
 *   ImmersiveBar.apply(activity, true, 0x00000000, false);              // 红顶区铺到状态栏 + 白色图标
 *   ImmersiveBar.apply(activity, false, 0xFFEDEDED, true);              // 状态栏 #EDEDED + 深色图标
 *
 * 兼容：API 30+ 用 WindowInsetsController；API 21~29 用 SYSTEM_UI_FLAG 分支。
 */
public final class ImmersiveBar {

    private ImmersiveBar() {
    }

    /** 沉浸式：状态栏透明，内容延伸到状态栏之下 */
    public static void immersive(Activity activity, boolean lightIcons) {
        setStatusBar(activity, true, Color.TRANSPARENT, lightIcons);
    }

    /** 非沉浸式：状态栏指定色（内容不进入状态栏） */
    public static void colorBar(Activity activity, int color, boolean lightIcons) {
        setStatusBar(activity, false, color, lightIcons);
    }

    public static void setStatusBar(Activity activity, boolean immersive, int color, boolean lightIcons) {
        if (activity == null) {
            return;
        }
        try {
            Window window = activity.getWindow();
            if (window == null) {
                return;
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(color);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(!immersive);
                android.view.WindowInsetsController controller = window.getInsetsController();
                if (controller != null) {
                    controller.setSystemBarsAppearance(
                        lightIcons ? android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
                }
            } else {
                View decor = window.getDecorView();
                int flags = decor.getSystemUiVisibility();
                if (immersive) {
                    flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                } else {
                    flags &= ~(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                }
                if (lightIcons && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                } else {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                }
                decor.setSystemUiVisibility(flags);
            }
        } catch (Exception ignored) {
        }
    }
}
