package io.dcloud.uniplugin.memberpicker;

import android.content.Context;

/**
 * 资源查找兜底：按“资源名”从最终合并的应用资源表取 id（避免库模块 R 常量与
 * 主工程合并后 id 不一致导致 findViewById 取错视图 / layout 取错文件）。
 */
public final class MpRes {

    private MpRes() {
    }

    public static int id(Context c, String name) {
        return c.getResources().getIdentifier(name, "id", c.getPackageName());
    }

    public static int layout(Context c, String name) {
        return c.getResources().getIdentifier(name, "layout", c.getPackageName());
    }

    public static int drawable(Context c, String name) {
        return c.getResources().getIdentifier(name, "drawable", c.getPackageName());
    }

    public static int style(Context c, String name) {
        return c.getResources().getIdentifier(name, "style", c.getPackageName());
    }
}
