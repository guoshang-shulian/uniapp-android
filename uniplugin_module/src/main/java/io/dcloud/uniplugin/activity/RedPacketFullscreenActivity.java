package io.dcloud.uniplugin.activity;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
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
 * 红包沉浸式全屏页（**取代原来的全屏 Dialog**）。
 *
 * <p><b>为什么从 Dialog 改成 Activity</b>：Dialog 的窗口被系统摆在"状态栏正下方"
 * （实测 {@code decorTopOnScreen=147 = 状态栏高度}），窗口本身从未覆盖状态栏那 147px，
 * 而状态栏是由宿主 Activity 的窗口画的不透明白色 → 弹窗的遮罩**在物理上够不到状态栏**，
 * 试过 inset 补色带、按偏移反向平移都没用（色带只会落在 y=147 变成一条多余的深色条）。
 * 全屏 Activity 的窗口天然铺满整屏（y=0），遮罩直接生效。
 *
 * <p><b>UI 完全不变</b>：仍然使用 {@code activity_red_packet_open.xml}，尺寸/圆角/渐变/
 * 内阴影/铜钱旋转/状态渲染全部照搬；只是承载方式从 Dialog 换成 Activity。
 *
 * <p><b>行为对齐 dialog</b>：点遮罩空白处关闭、返回键关闭、屏蔽宿主触摸、
 * 关闭后回到宿主聊天页（本页从聊天页启动，finish 即回）。
 *
 * <p>日志 tag：RedPacketOpen —— adb logcat -s RedPacketOpen:V RedPacketApi:V
 */
public class RedPacketFullscreenActivity extends Activity {

    private static final String TAG = "RedPacketOpen";

    private static final String EXTRA_RP_ID = "redPacketId";
    private static final String EXTRA_CONV_ID = "conversationId";
    private static final String EXTRA_CONV_TYPE = "conversationType";
    private static final String EXTRA_SENDER_ID = "senderUserId";
    private static final String EXTRA_SENDER_NAME = "senderName";
    private static final String EXTRA_SENDER_AVATAR = "senderAvatar";

    private String redPacketId = "";
    private String conversationId = "";
    private String conversationType = "group";
    private String senderUserId = "";
    private String senderName = "";
    private String senderAvatar = "";
    private JSONObject detail;
    private ObjectAnimator spinAnimator;
    private boolean closing = false;

    /**
     * 安全弹出（异常兜底，避免闪退/黑屏）。
     * <p>方法签名与原来的 {@code RedPacketOpenDialog.show(...)} **完全一致**，
     * 所以调用方（TestModule）只需换类名，参数不用动。
     */
    public static void show(Context context, String redPacketId, String conversationId,
        String conversationType, String senderUserId, String senderName, String senderAvatar) {
        if (context == null) {
            Log.e(TAG, "show skipped: context is null");
            return;
        }
        // 必须用能起 Activity 的上下文；Dialog 时代要求的 Window 上下文这里放宽为 Activity
        Context host = activityContext(context);
        if (host == null) {
            Log.e(TAG, "show skipped: no Activity context");
            return;
        }
        try {
            Intent intent = new Intent(host, RedPacketFullscreenActivity.class);
            intent.putExtra(EXTRA_RP_ID, sanitize(redPacketId));
            intent.putExtra(EXTRA_CONV_ID, conversationId == null ? "" : conversationId);
            intent.putExtra(EXTRA_CONV_TYPE,
                TextUtils.isEmpty(conversationType) ? "group" : conversationType);
            intent.putExtra(EXTRA_SENDER_ID, senderUserId == null ? "" : senderUserId);
            intent.putExtra(EXTRA_SENDER_NAME, senderName == null ? "" : senderName);
            intent.putExtra(EXTRA_SENDER_AVATAR, senderAvatar == null ? "" : senderAvatar);
            // 非 Activity 上下文（Application）也能起
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            host.startActivity(intent);
            Log.i(TAG, "started fullscreen activity rpId=" + sanitize(redPacketId)
                + " host=" + host.getClass().getName());
        } catch (Exception e) {
            Log.e(TAG, "start fail", e);
            Toast.makeText(host, "打开红包失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /** 只能是 Activity 或其 ContextWrapper */
    private static Context activityContext(Context context) {
        if (context instanceof Activity) {
            return context;
        }
        if (context instanceof android.content.ContextWrapper) {
            Context base = ((android.content.ContextWrapper) context).getBaseContext();
            if (base instanceof Activity) {
                return base;
            }
            if (base != null && base != context) {
                return activityContext(base);
            }
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent it = getIntent();
        redPacketId = sanitize(it.getStringExtra(EXTRA_RP_ID));
        conversationId = it.getStringExtra(EXTRA_CONV_ID);
        if (conversationId == null) conversationId = "";
        conversationType = it.getStringExtra(EXTRA_CONV_TYPE);
        if (TextUtils.isEmpty(conversationType)) conversationType = "group";
        senderUserId = it.getStringExtra(EXTRA_SENDER_ID);
        if (senderUserId == null) senderUserId = "";
        senderName = it.getStringExtra(EXTRA_SENDER_NAME);
        if (senderName == null) senderName = "";
        senderAvatar = it.getStringExtra(EXTRA_SENDER_AVATAR);
        if (senderAvatar == null) senderAvatar = "";
        Log.i(TAG, "onCreate rpId=" + redPacketId + " conv=" + conversationId);

        setContentView(R.layout.activity_red_packet_open);
        configureWindow();

        TextView nameView = findViewById(R.id.rpSenderName);
        if (nameView != null) {
            nameView.setText(TextUtils.isEmpty(senderName) ? "朋友" : senderName);
        }
        ImageView avatar = findViewById(R.id.rpSenderAvatar);
        if (avatar != null && !TextUtils.isEmpty(senderAvatar)) {
            Glide.with(avatar.getContext()).load(senderAvatar).circleCrop().into(avatar);
        }

        // 关闭按钮
        View closeBtn = findViewById(R.id.rpCloseBtn);
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> closeSelf());
        }
        // 点「開」→ 领取
        View openBtn = findViewById(R.id.rpOpenBtn);
        if (openBtn != null) {
            openBtn.setOnClickListener(v -> draw());
        }
        // 领取记录
        View recordsRow = findViewById(R.id.rpRecordsRow);
        applyRipple(recordsRow);
        if (recordsRow != null) {
            recordsRow.setOnClickListener(v -> openRecords());
        }
        // 点遮罩空白处关闭（与原 Dialog 的"点外部关闭"对齐）
        View maskLayer = findViewById(R.id.rpMaskLayer);
        if (maskLayer != null) {
            maskLayer.setOnClickListener(v -> closeSelf());
        }
        // 关键：卡片自己是 maskLayer 的子 View，若不消费点击，点卡片空白处会冒泡到遮罩把弹窗关掉。
        // 卡片内可点元素（開/领取记录）本来就消费了事件，这里只补"卡片背景区域"。
        View card = findViewById(R.id.rpCard);
        if (card != null) {
            card.setClickable(true);
        }

        // 诊断：窗口是否真的铺满整屏（这次期望 decorTopOnScreen=0 / decorH=screenH）
        View decor = getWindow() == null ? null : getWindow().getDecorView();
        if (decor != null) {
            decor.post(() -> {
                int[] loc = new int[2];
                decor.getLocationOnScreen(loc);
                android.util.DisplayMetrics dm = decor.getResources().getDisplayMetrics();
                Log.i(TAG, "GEOM decorTopOnScreen=" + loc[1]
                    + " decorH=" + decor.getHeight()
                    + " screenH=" + dm.heightPixels + " screenW=" + dm.widthPixels);
            });
        }

        loadDetail();
    }

    /**
     * 全屏透明窗口：铺满整屏（含状态栏/导航栏）+ 透明底，遮罩由布局里的内容层负责画。
     * 注意**不要**设 FLAG_DIM_BEHIND —— 视觉上的"暗"来自布局里 {@code #80000000}，
     * 用系统 dim 会和它叠加变成双倍变暗。
     */
    private void configureWindow() {
        Window window = getWindow();
        if (window == null) {
            return;
        }
        try {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false);
            } else {
                window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // 状态栏/导航栏都交给本页的遮罩去盖：先设透明，避免露出宿主 Activity 的白色状态栏
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0f);
            window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT);
        } catch (Exception e) {
            Log.w(TAG, "configureWindow fail: " + e);
        }
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
            boolean ok = getTheme().resolveAttribute(
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

    private void post(Runnable r) {
        if (r == null) {
            return;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!isFinishing()) {
                r.run();
            }
        });
    }

    private void applyState(JSONObject result) {
        detail = result == null ? new JSONObject() : result;
        TextView amountView = findViewById(R.id.rpAmount);
        TextView remarkView = findViewById(R.id.rpRemark);
        TextView stateView = findViewById(R.id.rpStateText);
        View openBtn = findViewById(R.id.rpOpenBtn);

        String remark = detail.getString("remark");
        if (remarkView != null) {
            remarkView.setText(TextUtils.isEmpty(remark) ? "恭喜发财，大吉大利" : remark);
        }
        if (openBtn == null || stateView == null || amountView == null) {
            return;
        }

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

    /** 返回键 = 关闭弹窗（与原 Dialog 行为一致） */
    @Override
    public void onBackPressed() {
        closeSelf();
    }

    @Override
    protected void onDestroy() {
        stopCoinSpin();
        super.onDestroy();
    }

    /** 关闭本页（幂等，避免"点遮罩关闭"与"返回键"同时触发重复 finish） */
    private void closeSelf() {
        if (closing) {
            return;
        }
        closing = true;
        stopCoinSpin();
        try {
            finish();
        } catch (Exception e) {
            Log.w(TAG, "close fail: " + e);
        }
    }

    private void openRecords() {
        stopCoinSpin();
        RedPacketRecordsActivity.start(this, redPacketId, conversationId, senderName, senderAvatar);
        closeSelf();
    }

    private void toast(String message) {
        if (message != null && !message.isEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private static String sanitize(String id) {
        String value = id == null ? "" : id;
        int idx = value.indexOf('?');
        return idx > 0 ? value.substring(0, idx) : value;
    }
}
