package com.zegocloud.zimkit.common.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;

/** 状态栏白底黑字工具（聊天/群设置等界面统一） */
public class ZimkitStatusBar {

    public static void setWhite(Activity activity) {
        try {
            Window window = activity.getWindow();
            window.setStatusBarColor(Color.WHITE);
            if (Build.VERSION.SDK_INT >= 23) {
                window.getDecorView().setSystemUiVisibility(
                    window.getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else if (Build.VERSION.SDK_INT >= 21) {
                window.getDecorView().setSystemUiVisibility(
                    window.getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }
        } catch (Exception ignored) {
        }
    }

    /** 供其他模块（如 uniplugin）使用 */
    public static void setWhite(Context context) {
        if (context instanceof Activity) {
            setWhite((Activity) context);
        }
    }
}
