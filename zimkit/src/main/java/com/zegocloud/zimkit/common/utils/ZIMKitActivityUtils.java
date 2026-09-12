package com.zegocloud.zimkit.common.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.zegocloud.zimkit.services.internal.ZIMKitCore;

public class ZIMKitActivityUtils {

    private static List<Activity> activityList = new ArrayList<>();
    private static Activity currentActivity;

    public static void recreateAll() {
        Iterator<Activity> iterator = activityList.iterator();
        //The activity at the top of the stack is destroyed first, then started, and the rest goes to rebuild
        while (iterator.hasNext()) {
            Activity activity = iterator.next();
            activity.recreate();
            iterator.remove();
        }
    }

    private static Application context;

    public static void init(Application application) {
        if (application == null) {
            return;
        }
        context = application;
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                removeDestroyActivity(activity, savedInstanceState);
                if (activityList != null) {
                    activityList.add(activity);
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
                currentActivity = activity;
            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (activityList != null) {
                    activityList.remove(activity);
                }
                currentActivity = currentActivity == activity ? null : currentActivity;
            }
        });
    }

    /**
     * Get the current Activity
     *
     * @return
     */
    public static Activity getCurrentActivity() {
        return isActivityAlive(currentActivity) ? currentActivity : null;
    }

    /**
     * Whether Activity is alive or not
     *
     * @param activity
     * @return
     */
    public static boolean isActivityAlive(final Activity activity) {
        return activity != null && !activity.isFinishing()
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !activity.isDestroyed());
    }

    /**
     * Remove the system from destroying the rebuilt activity due to lack of memory
     *
     * @param activity
     * @param savedInstanceState
     */
    private static void removeDestroyActivity(Activity activity, Bundle savedInstanceState) {
        if (activity != null && savedInstanceState != null) {//System Recovery Create Activity
            Iterator<Activity> iterator = activityList.iterator();
            while (iterator.hasNext()) {
                Activity exitActivity = iterator.next();
                if (exitActivity == null || activity.getComponentName().getClassName().equals(exitActivity.getComponentName().getClassName())) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Whether the process is running in the background
     *
     * @return
     */
    public static boolean isBackstage() {
        return activityList.isEmpty();
    }

    /**
     * 原生页所在的包前缀。**必须两套都列**，这是「少关一层」的根因所在：
     * <ul>
     *   <li>{@code com.zegocloud.zimkit} —— ZIMKit 聊天页 / 群设置页 / 群成员列表页；</li>
     *   <li>{@code io.dcloud.uniplugin} —— 本项目自己的原生页。成员资料页的两条入口里，
     *       {@code memberpicker.GroupMembersActivity}（uniapp 调 openGroupMembers 打开）就属于这一包。
     *       只关 ZIMKit 的话，用户从「群成员页 → 成员资料」走，点完店铺会停在群成员页，
     *       必须再手动返回一次才能看到 uniapp 的店铺首页。</li>
     * </ul>
     */
    private static final String[] NATIVE_PAGE_PREFIXES = {
        "com.zegocloud.zimkit",
        "io.dcloud.uniplugin",
    };

    /** uniapp 容器（**绝不能 finish**，关掉它整个 App 就退出了） */
    private static final String[] CONTAINER_CLASSES = {
        "io.dcloud.PandoraEntry",
        "io.dcloud.PandoraEntryActivity",
    };

    /**
     * 关闭压在 uniapp 之上的**所有原生页**（用于"原生页 → uniapp 页面"的跳转）。
     *
     * <p>场景：任何"原生页压着 uniapp"的时候（群设置页 / 群成员页 / 聊天页 / 成员资料页），
     * uniapp 侧即使执行了 navigateTo，用户也**看不到**跳转结果 —— 必须先把原生页**全部**关掉，
     * uniapp 才会露出来。否则用户感受就是"点了没反应"或"还要手动返回一次"。
     *
     * <p>安全性：按 {@link #NATIVE_PAGE_PREFIXES} 前缀匹配 + 排除 uniapp 容器，
     * **绝不动** {@code io.dcloud.PandoraEntry*} 以及其它第三方页面。
     *
     * @param exclude 额外排除的类（例如调用方自己，想最后再 finish）
     * @return 实际关闭的数量
     */
    public static int finishNativePages(Class<?>... exclude) {
        if (activityList == null || activityList.isEmpty()) {
            return 0;
        }
        int count = 0;
        // 拷贝一份再遍历：finish() 会触发 onActivityDestroyed 回调改动 activityList
        List<Activity> snapshot = new ArrayList<>(activityList);
        for (Activity activity : snapshot) {
            if (!isActivityAlive(activity)) {
                continue;
            }
            ComponentName cn = activity.getComponentName();
            if (cn == null || cn.getClassName() == null) {
                continue;
            }
            String name = cn.getClassName();
            if (!isNativePage(name)) {
                continue;   // 容器 / 第三方页面 → 不碰
            }
            if (isExcluded(name, exclude)) {
                continue;
            }
            activity.finish();
            count++;
        }
        return count;
    }

    /** 是否属于"我们的原生页"（前缀命中且不是 uniapp 容器） */
    private static boolean isNativePage(String className) {
        for (String container : CONTAINER_CLASSES) {
            if (container.equals(className)) {
                return false;
            }
        }
        for (String prefix : NATIVE_PAGE_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 当前存活的原生页类名（排查"关不干净"用：日志里能看到到底还剩哪一层）。
     * 仅用于诊断，不影响行为。
     */
    public static String describeAliveActivities() {
        if (activityList == null || activityList.isEmpty()) {
            return "(activityList empty)";
        }
        StringBuilder sb = new StringBuilder();
        for (Activity activity : new ArrayList<>(activityList)) {
            if (!isActivityAlive(activity) || activity.getComponentName() == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(activity.getComponentName().getClassName());
        }
        return sb.length() == 0 ? "(none alive)" : sb.toString();
    }

    private static boolean isExcluded(String activityName, Class<?>[] exclude) {
        if (exclude == null) {
            return false;
        }
        for (Class<?> c : exclude) {
            if (c != null && c.getName().equals(activityName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Destroy all activities
     */
    public static void finishAll() {
        Iterator<Activity> activityIterator = activityList.iterator();
        while (activityIterator.hasNext()) {
            Activity activity = activityIterator.next();
            if (activity != null) {
                activity.finish();
            }
            activityIterator.remove();
        }
    }

    public static boolean isExitActivity(Class<?> mClass) {
        return isExitActivity(mClass.getName());
    }

    /**
     * Keep only the specified Activity
     *
     * @param mClass
     */
    public static void onlyExitActivity(Class<? extends Activity> mClass) {
        Iterator<Activity> activityIterator = activityList.iterator();
        String activityName = mClass.getName();
        while (activityIterator.hasNext()) {
            Activity activity = activityIterator.next();
            ComponentName componentName = activity.getComponentName();
            if (componentName != null) {
                if (!componentName.getClassName().equals(activityName)) {
                    activity.finish();
                }
            }
        }
    }

    /**
     * Message notifications are retained on the login page, the session page and the current message page
     */
    public static void finishActivityForMessage(String activityNam) {
        ArrayList<Integer> index = new ArrayList<>();
        for (int i = 0; i < activityList.size(); i++) {
            Activity activity = activityList.get(i);
            ComponentName componentName = activity.getComponentName();
            if (componentName != null) {
                if (componentName.getClassName().equals(activityNam)) {
                    index.add(i);
                }
            }
        }

        if (index.size() > 1) {
            int maxIndex = index.get(index.size() - 1);
            int minIndex = index.get(0);
            for (int j = minIndex; j < maxIndex; j++) {
                if (j < maxIndex) {
                    activityList.get(j).finish();
                }
            }
        }
        index.clear();
    }

    /**
     * Keep only the specified Activity
     *
     * @param activityName
     */
    public static void onlyExitActivity(String activityName) {
        Iterator<Activity> activityIterator = activityList.iterator();
        while (activityIterator.hasNext()) {
            Activity activity = activityIterator.next();
            ComponentName componentName = activity.getComponentName();
            if (componentName != null) {
                if (!componentName.getClassName().equals(activityName)) {
                    activity.finish();
                }
            }
        }
    }

    /**
     * finish the specified activity
     *
     * @param mClass
     */
    public static void onlyFinishActivity(Class<? extends Activity> mClass) {
        Iterator<Activity> activityIterator = activityList.iterator();
        String activityName = mClass.getName();
        while (activityIterator.hasNext()) {
            Activity activity = activityIterator.next();
            ComponentName componentName = activity.getComponentName();
            if (componentName != null) {
                if (componentName.getClassName().equals(activityName)) {
                    activity.finish();
                }
            }
        }
    }

    /**
     * Whether an Activity exists or not
     *
     * @param activityName ( The name of the complete package path)
     * @return
     */
    public static boolean isExitActivity(String activityName) {
        boolean isExit = false;
        for (Activity activity : activityList) {
            ComponentName componentName = activity.getComponentName();
            if (componentName != null) {
                if (componentName.getClassName().equals(activityName)) {
                    isExit = true;
                    break;
                }
            }
        }
        return isExit;
    }

    /**
     * Exit the specified page, and the page above it,
     * If the specified page does not exist no action is taken
     *
     * @param firstPushEnterActivity
     */
    public static void back2Activity(Activity firstPushEnterActivity) {
        boolean isExistActivity = false;
        List<Activity> tmpList = new ArrayList<>();
        Iterator<Activity> activityIterator = activityList.iterator();
        while (activityIterator.hasNext()) {
            Activity activity = activityIterator.next();
            if (activity != null) {
                if (activity == firstPushEnterActivity) {
                    isExistActivity = true;
                }
                if (isExistActivity) {
                    tmpList.add(activity);
                }
            }
        }
        if (isExistActivity) {
            for (Activity activity : tmpList) {
                if (isActivityAlive(activity)) {
                    activity.finish();
                }
            }
        }
        tmpList.clear();
    }

    /**
     * Whether at the front desk
     *
     * @return
     */
    public static boolean isBackground() {
        Context context = ZIMKitCore.getInstance().getApplication();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(context.getPackageName())) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    return false;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Clear all message notifications
     */
    public static void clearAllNotifications() {
        if (context != null) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
        }
    }

}
