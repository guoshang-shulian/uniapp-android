package io.dcloud.uniplugin.activity;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.content.ContextWrapper;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.bumptech.glide.Glide;

import java.util.Locale;

import io.dcloud.uniplugin.TestModule;
import io.dcloud.uniplugin.others.RedPacketApi;
import uni.dcloud.io.uniplugin_module.R;

/**
 * 红包沉浸式弹窗（全屏 Dialog；与项目内 MemberPickerBottomSheet 同一套可靠做法）。
 * 状态：可领取（開）/ 已领取（金额）/ 已被领完 / 已过期 / 专属（仅 XX 可领取）。
 *
 * 日志 tag：RedPacketOpen —— adb logcat -s RedPacketOpen:V RedPacketApi:V
 */
public class RedPacketOpenDialog extends Dialog {

    private static final String TAG = "RedPacketOpen";

    private final String redPacketId;
    private final String conversationId;
    private final String conversationType;
    private final String senderUserId;
    private final String senderName;
    private final String senderAvatar;
    private JSONObject detail;
    private ObjectAnimator spinAnimator;

    public RedPacketOpenDialog(Context context, String redPacketId, String conversationId,
        String conversationType, String senderUserId, String senderName, String senderAvatar) {
        super(themeContext(context));
        this.redPacketId = sanitize(redPacketId);
        this.conversationId = conversationId == null ? "" : conversationId;
        this.conversationType = TextUtils.isEmpty(conversationType) ? "group" : conversationType;
        this.senderUserId = senderUserId == null ? "" : senderUserId;
        this.senderName = senderName == null ? "" : senderName;
        this.senderAvatar = senderAvatar == null ? "" : senderAvatar;
    }

    /** 安全弹出（异常兜底，避免闪退/黑屏） */
    public static void show(Context context, String redPacketId, String conversationId,
        String conversationType, String senderUserId, String senderName, String senderAvatar) {
        if (context == null) {
            Log.e(TAG, "show skipped: context is null");
            return;
        }
        Context host = activityContext(context);
        Log.i(TAG, "show ctx=" + context.getClass().getName()
            + " host=" + (host == null ? "null" : host.getClass().getName()));
        if (host == null) {
            Log.e(TAG, "show skipped: no Window context (need Activity/ContextWrapper)");
            return;
        }
        try {
            RedPacketOpenDialog dialog = new RedPacketOpenDialog(host, redPacketId, conversationId,
                conversationType, senderUserId, senderName, senderAvatar);
            dialog.show();
            View decor = dialog.getWindow() == null ? null : dialog.getWindow().getDecorView();
            Log.i(TAG, "shown isShowing=" + dialog.isShowing()
                + " decorAttached=" + (decor != null && decor.isAttachedToWindow())
                + " decorVisible=" + (decor != null && decor.getVisibility() == View.VISIBLE)
                + " w=" + (decor == null ? -1 : decor.getWidth())
                + " h=" + (decor == null ? -1 : decor.getHeight()));
        } catch (Exception e) {
            Log.e(TAG, "show fail", e);
            Toast.makeText(host, "打开红包失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** 只能是能提供窗口令牌的上下文（Activity 或其 ContextWrapper）；Application/Service 不行 */
    private static Context activityContext(Context context) {
        if (context instanceof android.app.Activity) {
            return context;
        }
        if (context instanceof ContextWrapper) {
            Context base = ((ContextWrapper) context).getBaseContext();
            if (base instanceof android.app.Activity) {
                return base;
            }
            if (base != null && base != context) {
                return activityContext(base);
            }
        }
        return null;
    }

    /**
     * 主题兜底：宿主 Activity 主题若解析不到 selectableItemBackground（MIUI 深色模式会把应用主题
     * 强制成 Theme.DeviceDefault.Light.DarkActionBar，实测会少属性 → 布局 inflate 抛
     * UnsupportedOperationException: Failed to resolve attribute），则改用显式 Material 主题。
     * 这是本弹窗 2026-09-10 崩溃（黑屏+卡死）的根因之一，保留此兜底防止再次触发。
     */
    private static Context themeContext(Context context) {
        try {
            TypedValue value = new TypedValue();
            if (context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true)) {
                return context;
            }
            Log.w(TAG, "host theme misses selectableItemBackground -> fallback to Material dialog theme");
        } catch (Exception e) {
            Log.w(TAG, "theme check fail: " + e);
        }
        return new ContextThemeWrapper(context,
            com.google.android.material.R.style.Theme_MaterialComponents_Light_Dialog_Alert);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate rpId=" + redPacketId + " conv=" + conversationId);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_red_packet_open);
        Window window = getWindow();
        if (window != null) {
            // ── 让 Dialog 窗口真正铺满全屏（含状态栏），这样「遮罩」天然盖住状态栏 ──
            // 之前只设 FLAG_LAYOUT_NO_LIMITS / setDecorFitsSystemWindows(false)，但 Dialog 的
            // 窗口 bounds 仍被系统裁到状态栏之下（实测 decor bounds=[0,147]…），
            // 状态栏就露出没被遮罩的原始界面 → 没有沉浸感。
            // 关键补充：SOFT_INPUT_ADJUST_RESIZE（Dialog 默认 ADJUST_PAN 会把窗口压缩）+ 透明状态栏。
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false);
            } else {
                window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0f);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        // 注意：setCanceledOnTouchOutside 必须在 show() 之后调用（在 onCreate 里调用会抛
        // IllegalStateException: The dialog is not shown ...），因此这里只设置 setCancelable。
        setCancelable(true);

        View rootView = findViewById(R.id.rpCard);
        View contentRoot = rootView == null ? null : (View) rootView.getParent();
        final View statusMask = findViewById(R.id.rpStatusBarMask);
        if (statusMask != null) {
            // 用真实 inset 决定补多高：窗口若已铺到状态栏之下 → inset 为 0，色带自动隐藏；
            // 窗口被限制在状态栏之下（部分 ROM/Dialog 场景）→ 补一条同色遮罩，视觉上连成一片。
            final View attach = contentRoot == null ? statusMask : contentRoot;
            attach.setOnApplyWindowInsetsListener((v, insets) -> {
                int top;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    top = insets.getInsets(android.view.WindowInsets.Type.statusBars()).top;
                } else {
                    top = insets.getSystemWindowInsetTop();
                }
                if (top > 0) {
                    applyStatusBarMaskHeight(statusMask, top);
                }
                return insets;
            });
            // 兜底：attach 所在的窗口若不在状态栏之下，inset 恒为 0（实测 decor bounds 从状态栏下方开始），
            // 此时直接用量出来的系统状态栏高度补色带。
            attach.post(() -> {
                if (statusMask.getLayoutParams() != null && statusMask.getLayoutParams().height > 0) {
                    return;
                }
                applyStatusBarMaskHeight(statusMask, systemStatusBarHeight(attach));
            });
            attach.requestApplyInsets();
        }

        TextView nameView = findViewById(R.id.rpSenderName);
        nameView.setText(TextUtils.isEmpty(senderName) ? "朋友" : senderName);
        ImageView avatar = findViewById(R.id.rpSenderAvatar);
        if (!TextUtils.isEmpty(senderAvatar)) {
            Glide.with(avatar.getContext()).load(senderAvatar).circleCrop().into(avatar);
        }

        findViewById(R.id.rpCloseBtn).setOnClickListener(v -> dismiss());
        findViewById(R.id.rpOpenBtn).setOnClickListener(v -> draw());
        View recordsRow = findViewById(R.id.rpRecordsRow);
        applyRipple(recordsRow);
        recordsRow.setOnClickListener(v -> openRecords());
        loadDetail();
    }

    /**
     * 安全设置点击水波纹：布局里不写 ?selectableItemBackground
     * （MIUI 强制主题时该属性解析不到 → inflate 直接崩），改为代码 resolveAttribute，失败退化浅灰。
     */
    private void applyRipple(View view) {
        if (view == null) {
            return;
        }
        try {
            TypedValue out = new TypedValue();
            boolean ok = getContext().getTheme().resolveAttribute(
                android.R.attr.selectableItemBackground, out, true);
            if (ok && out.resourceId != 0) {
                view.setBackgroundResource(out.resourceId);
            } else {
                view.setBackgroundColor(0x14000000);
            }
        } catch (Exception e) {
            Log.w(TAG, "applyRipple fallback: " + e);
            try {
                view.setBackgroundColor(0x14000000);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void show() {
        super.show();
        // 显示之后再禁止“点击外部关闭”（窗口已 addView，不会触发 IllegalStateException）
        try {
            setCanceledOnTouchOutside(false);
        } catch (Exception e) {
            Log.w(TAG, "setCanceledOnTouchOutside fail: " + e);
        }
    }

    private void loadDetail() {
        if (redPacketId.isEmpty()) {
            toast("红包信息缺失");
            return;
        }
        // 先给一个"加载中"骨架：详情接口可能 1s+，避免用户以为没反应（历史反馈：以为要点两次）
        TextView stateView = findViewById(R.id.rpStateText);
        if (stateView != null) {
            stateView.setText("加载中...");
            stateView.setVisibility(View.VISIBLE);
        }
        View openBtn = findViewById(R.id.rpOpenBtn);
        if (openBtn != null) {
            openBtn.setVisibility(View.GONE);
        }
        RedPacketApi.detail(redPacketId, conversationId,
            new RedPacketApi.Callback() {
                @Override
                public void onSuccess(JSONObject result) {
                    Log.i(TAG, "detail ok");
                    post(() -> applyState(result));
                }

                @Override
                public void onError(int code, String message) {
                    Log.w(TAG, "detail fail code=" + code + " msg=" + message);
                    post(() -> {
                        TextView tv = findViewById(R.id.rpStateText);
                        if (tv != null) {
                            tv.setText(TextUtils.isEmpty(message) ? ("错误码 " + code) : message);
                            tv.setVisibility(View.VISIBLE);
                        }
                    });
                }
            });
    }

    /** 设置状态栏补色带的高度 */
    private static void applyStatusBarMaskHeight(View mask, int height) {
        if (mask == null || height <= 0) {
            return;
        }
        ViewGroup.LayoutParams lp = mask.getLayoutParams();
        if (lp != null && lp.height != height) {
            lp.height = height;
            mask.setLayoutParams(lp);
            Log.i(TAG, "status bar mask height=" + height);
        }
    }

    /** 系统状态栏高度（不依赖窗口 inset：Dialog 窗口可能整体位于状态栏之下） */
    private static int systemStatusBarHeight(View view) {
        try {
            int id = view.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (id > 0) {
                return view.getResources().getDimensionPixelSize(id);
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    /** 网络回包在子线程 → 统一切主线程（不再用 findViewById 判断，避免子线程碰 View） */
    private void post(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            new Handler(Looper.getMainLooper()).post(r);
        }
    }

    private void applyState(JSONObject result) {
        detail = result == null ? new JSONObject() : result;
        TextView amountView = findViewById(R.id.rpAmount);
        TextView remarkView = findViewById(R.id.rpRemark);
        TextView stateView = findViewById(R.id.rpStateText);
        View openBtn = findViewById(R.id.rpOpenBtn);

        String remark = detail.getString("remark");
        remarkView.setText(TextUtils.isEmpty(remark) ? "恭喜发财，大吉大利" : remark);

        String status = detail.getString("status");
        double myPoints = detail.getDoubleValue("myDrawPoints");
        String type = detail.getString("type");
        String toUserId = detail.getString("toUserId");
        String toUserName = detail.getString("toUserName");

        boolean expired = "expired".equals(status) || "refunded".equals(status);
        boolean ended = "ended".equals(status);
        // 专属判定：后端 detail.toUserId 是纯数字用户ID，本地是带 user_ 前缀的 ZIM ID
        // —— 必须剥前缀比较，否则「给自己的专属红包」也会被判成「仅他人可领取」
        String selfMemberId = io.dcloud.uniplugin.memberpicker.Member
            .stripZimPrefix(TestModule.getLocalUserId());
        boolean exclusiveToOther = "exclusive".equals(type) && !TextUtils.isEmpty(toUserId)
            && !toUserId.equals(selfMemberId)
            && !toUserId.equals(TestModule.getLocalUserId());

        if (myPoints > 0) {
            openBtn.setVisibility(View.GONE);
            amountView.setText(String.format(Locale.CHINA, "%.2f 积分", myPoints));
            amountView.setVisibility(View.VISIBLE);
            stateView.setText("已存入积分账户");
            stateView.setVisibility(View.VISIBLE);
        } else if (ended) {
            openBtn.setVisibility(View.GONE);
            stateView.setText("已被领完");
            stateView.setVisibility(View.VISIBLE);
        } else if (expired) {
            openBtn.setVisibility(View.GONE);
            stateView.setText("红包已过期");
            stateView.setVisibility(View.VISIBLE);
        } else if (exclusiveToOther) {
            openBtn.setVisibility(View.GONE);
            stateView.setText("仅" + (TextUtils.isEmpty(toUserName) ? "对方" : toUserName) + "可领取");
            stateView.setVisibility(View.VISIBLE);
        } else {
            openBtn.setVisibility(View.VISIBLE);
            stateView.setVisibility(View.GONE);
        }
    }

    /**
     * 点「開」→ 领取。
     * 铜钱原地 360° 旋转 = 请求 loading（ObjectAnimator，纯原生）；成功/失败都停止并回正。
     */
    private void draw() {
        if (redPacketId.isEmpty()) {
            return;
        }
        View openBtn = findViewById(R.id.rpOpenBtn);
        if (openBtn != null) {
            openBtn.setEnabled(false);
        }
        startCoinSpin();
        JSONObject params = new JSONObject();
        params.put("redPacketId", redPacketId);
        params.put("groupId", conversationId);
        params.put("clientRequestId", "rp_draw_" + System.currentTimeMillis());
        Log.i(TAG, "draw start rpId=" + redPacketId);
        RedPacketApi.draw(params, new RedPacketApi.Callback() {
            @Override
            public void onSuccess(JSONObject result) {
                post(() -> {
                    if (result == null) {
                        stopCoinSpin();
                        toast("未获取到领取结果");
                        return;
                    }
                    double points = result.getDoubleValue("points");
                    Log.i(TAG, "draw ok points=" + points);
                    RedPacketClaimAction.sendDrawSync(conversationId, conversationType,
                        redPacketId, result,
                        senderUserId.startsWith("user_") ? senderUserId : ("user_" + senderUserId),
                        senderName);
                    // 领取成功：不做"已存入账户"停留，直接进原生领取记录页
                    stopCoinSpin();
                    openRecords();
                });
            }

            @Override
            public void onError(int code, String message) {
                post(() -> {
                    stopCoinSpin();
                    Log.w(TAG, "draw fail code=" + code + " msg=" + message);
                    toast(message == null ? ("错误码 " + code) : message);
                });
            }
        });
    }

    /** 铜钱原地 360° 循环旋转（loading） */
    private void startCoinSpin() {
        View coin = findViewById(R.id.rpCoinRotator);
        if (coin == null) {
            return;
        }
        stopCoinSpin();
        spinAnimator = ObjectAnimator.ofFloat(coin, View.ROTATION, 0f, 360f);
        // 0→360 线性、360→720 线性：避免匀速转一圈后"回跳"的观感
        spinAnimator.setDuration(700);
        spinAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        spinAnimator.setRepeatMode(ObjectAnimator.RESTART);
        spinAnimator.setInterpolator(new LinearInterpolator());
        spinAnimator.start();
    }

    private void stopCoinSpin() {
        if (spinAnimator != null) {
            spinAnimator.cancel();
            spinAnimator = null;
        }
        View coin = findViewById(R.id.rpCoinRotator);
        if (coin != null) {
            coin.setRotation(0f);
        }
    }

    @Override
    public void dismiss() {
        stopCoinSpin();
        try {
            super.dismiss();
        } catch (Exception e) {
            Log.w(TAG, "dismiss fail: " + e);
        }
    }

    private void openRecords() {
        stopCoinSpin();
        dismiss();
        RedPacketRecordsActivity.start(getContext(), redPacketId, conversationId, senderName,
            senderAvatar);
    }

    private void toast(String message) {
        Toast.makeText(getContext(), message == null ? "" : message, Toast.LENGTH_SHORT).show();
    }

    private static String sanitize(String id) {
        String value = id == null ? "" : id;
        int idx = value.indexOf('?');
        return idx > 0 ? value.substring(0, idx) : value;
    }
}
