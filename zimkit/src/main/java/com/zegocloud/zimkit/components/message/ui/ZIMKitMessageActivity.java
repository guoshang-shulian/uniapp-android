package com.zegocloud.zimkit.components.message.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModel;

import com.bumptech.glide.Glide;
import com.zegocloud.zimkit.R;
import com.zegocloud.zimkit.common.ZIMKitConstant;
import com.zegocloud.zimkit.common.base.BaseActivity;
import com.zegocloud.zimkit.common.components.widget.TitleBar;
import com.zegocloud.zimkit.common.utils.StoreEntryApi;
import com.zegocloud.zimkit.common.utils.ZIMKitActivityUtils;
import com.zegocloud.zimkit.components.message.interfaces.ZIMKitMessagesListListener;
import com.zegocloud.zimkit.components.message.model.ZIMKitHeaderBar;
import com.zegocloud.zimkit.databinding.ZimkitActivityMessageBinding;
import com.zegocloud.zimkit.services.ZIMKit;
import com.zegocloud.zimkit.services.ZIMKitConfig;
import com.zegocloud.zimkit.services.callback.MessageSentCallback;
import com.zegocloud.zimkit.services.internal.ZIMKitAdvancedKey;
import com.zegocloud.zimkit.services.internal.ZIMKitCore;
import com.zegocloud.zimkit.services.internal.ZIMKitEventHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import im.zego.zim.entity.ZIMError;
import im.zego.zim.enums.ZIMConversationType;

public class ZIMKitMessageActivity extends BaseActivity<ZimkitActivityMessageBinding, ViewModel> {

    private ZIMKitMessageFragment fragment;
    private String title;
    private String type;
    private boolean isFromPush;

    private String joinData = null;

    private static BackToUniappCallback mListener;

    /** 当前打开的聊天 Activity（用于卡片点击后把聊天页让位于 uniapp 页面） */
    private static ZIMKitMessageActivity sCurrent;

    public static void finishCurrent() {
        if (sCurrent != null) {
            sCurrent.finish();
        }
    }

    private  String groupId;

    // ───────────────────────── 社群店铺悬浮球（Figma 3036:2520 / 3036:2515-2518）─────────────────────────
    /**
     * 半隐藏时**露在屏幕内的宽度**（视觉）：球往屏幕里缩，只在边缘留一小条。
     *
     * <p>16dp 是用户确认过的观感；12:18:55 那次未经确认的改动把它改成 32dp（理由是"图标完整可见"），
     * 实际效果是半隐藏看起来胖了一倍 → 已回退。
     * 可点区不依赖它：由**同尺寸透明代理**兜住（见 {@link #syncStoreBallHit()}，半隐藏时仍有 58dp）。
     */
    private static final int STORE_BALL_PEEK_DP = 16;
    /** 展开态贴边后，再往屏幕外方向拖超过这个横向距离(dp)才算"要半隐藏" */
    private static final int STORE_BALL_HIDE_PULL_DP = 12;
    private static final long STORE_BALL_ANIM_MS = 220;
    /** 与消息区重叠时的半透明（用户确认：遮挡消息半透明化） */
    private static final float STORE_BALL_ALPHA = 0.6f;

    private View storeBall;
    private View storeBallHit;      // 透明点击代理（比球宽，跟球一起移动）
    private View storeBallBg;
    private View storeBallCircle;
    private View storeBallIcon;
    private String storeDistributionId = "";
    private int storeBallW;
    private int storeBallH;
   private int storeBallBaseX;        // 吸附态容器的 X（贴左 0 / 贴右 screenW-ballW）

    private int storeBallRestY;        // 本次 onLayout 记录的"当前正确 Y"
    private boolean storeBallHidden;   // 收起态
    private boolean storeBallDragging;
    private boolean storeBallJustExpanded; // 点收起态展开的那一下：抬手不触发进店
    private boolean storeBallAtLeft;       // 当前吸附在左侧（决定圆角朝向 + 收起方向）
    private boolean dragFromCollapsed;     // 本次手势是否从收起态开始
    private boolean dragHideIntent;        // 本次拖动是否已判定为"往边缘收进去"
    /** 监听消息区布局变化（会议横条显隐 → 球要重新避让）；onDestroy 必须移除，否则泄漏 */
    private ViewTreeObserver.OnGlobalLayoutListener storeGlobalLayoutListener;
    /** 性能守卫：上次的消息区顶（父容器坐标）与高度；都没变时不碰 View（滚动时高频回调直接 return） */
    private int lastMsgTopInParent = Integer.MIN_VALUE;
    private int lastMsgH = -1;
   private float ballTouchDownRawX;
    private float ballTouchDownRawY;
    private float ballTouchLastRawX;
    private int ballTouchStartBaseLeft;    // 手势开始时容器的 X
    private int ballTouchStartTop;

    public static void setOnNativeDataListener(BackToUniappCallback listener) {
        mListener = listener;
    }

    /** 供卡片点击兜底取用（Fragment 侧未注册时回退到这里，见 CardMessageHolder） */
    public static BackToUniappCallback getNativeDataListener() {
        return mListener;
    }

    public void sendShareCard(String conversationId,
                              String payloadJson) {
        try {
            ZIMConversationType type =ZIMConversationType.GROUP;
            ZIMKit.sendCustomMessage(payloadJson, 1,
                    conversationId, type, new MessageSentCallback() {
                        @Override
                        public void onMessageSent(ZIMError errorInfo) {
                            System.out.println("clicked here oo1");
                            mBinding.productPreviewCard.setVisibility(View.GONE);
                        }
                    });
        } catch (Exception e) {
            //   invokeFail(callback, e);
            System.out.println("clicked here oo2");
        }
    }
    Bundle productX;
    Bundle NameX;

    @Override
    protected void initView() {
        com.zegocloud.zimkit.common.utils.ZimkitStatusBar.setWhite(this);
        sCurrent = this;
        Bundle bundle = getIntent().getBundleExtra(ZIMKitConstant.RouterConstant.KEY_BUNDLE);
        title = bundle.getString(ZIMKitConstant.MessagePageConstant.KEY_TITLE);
        type = bundle.getString(ZIMKitConstant.MessagePageConstant.KEY_TYPE);
        String id = bundle.getString(ZIMKitConstant.MessagePageConstant.KEY_ID);
        groupId = id;
        if (bundle == null) {
            finish();
            return;
        }

        Bundle productB = getIntent().getBundleExtra("product");
        Bundle NameB = getIntent().getBundleExtra("name");
        //String name = NameB.getString("name");
        productX = productB;
       // bool merchant = false;
        if(productB != null) {
            try {
                String product = productB.getString("product");
                System.out.println(product);
                System.out.println("product here");
                System.out.println(groupId);
                JSONObject jsonObject = new JSONObject(product);
                mBinding.productTitle.setText(jsonObject.getString("productName"));
                mBinding.productPrice.setText(jsonObject.getString("price"));
                mBinding.productPreviewCard.setVisibility(View.VISIBLE);
                mBinding.btnCloseProductCard.setOnClickListener(p -> {
                    System.out.println("clicked here oo");
                    mBinding.productPreviewCard.setVisibility(View.GONE);
                });
                mBinding.btnSendProduct.setOnClickListener(p -> {
                    try {
                        JSONObject newObject = new JSONObject();
                        newObject.put("detail",jsonObject);
                        newObject.put("version",1);
                        newObject.put("cardType","product");
                        String newProduct = newObject.toString();
                        sendShareCard(groupId,newProduct);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                });

                String imageUrl = jsonObject.getString("productImage");
                mBinding.titleBar.setTitle(jsonObject.getString("merchantName"));

                Glide.with(mBinding.getRoot().getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.zimkit_icon_album_loading) // While downloading
                        .error(R.drawable.zimkit_icon_empty_dracula)       // If URL is broken/null
                        .into(mBinding.productImage);
//                sendShareCard()


            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        mBinding.tvGroupNoticeEnter.setOnClickListener(p -> {
           // System.out.println("finding out here");
            mListener.onDataReceived(joinData);

        });


        ZIMKitEventHandler.setOnNativeDataListener(v -> {
            try {
                if (v != null && v.startsWith("stop")) {
                    String cleanedData = v.substring(4);
                    if(cleanedData.equals(id)){
                        mBinding.layoutGroupNotice.setVisibility(View.GONE);
                        joinData = null;
                    }
                    return; // Exit early and do nothing else
                }

                JSONObject jsonObject = new JSONObject(v);
                JSONObject dataObj = jsonObject.getJSONObject("data");
                JSONObject liveModelObj = dataObj.getJSONObject("liveModel");
                String roomName = liveModelObj.getString("roomName");
                if(roomName.equals(id)){
                 mBinding.layoutGroupNotice.setVisibility(View.VISIBLE);
                 joinData = v;
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

                }

        );

        if(NameB != null){
            String name = NameB.getString("name");
            mBinding.titleBar.setTitle(name);
            NameX = NameB;
        }
//        mBinding.layoutGroupNotice.setVisibility(View.INVISIBLE);


        String avatar = bundle.getString(ZIMKitConstant.MessagePageConstant.KEY_AVATAR);
        isFromPush = bundle.getBoolean(ZIMKitConstant.MessagePageConstant.KEY_PUSH, false);
        if(productB != null || NameB != null) {
            mBinding.titleBar.hideRightButton();

        } else{
            mBinding.titleBar.setRightImg(R.mipmap.zimkit_icon_more);
        }

        if (type.equals(ZIMKitConstant.MessagePageConstant.TYPE_GROUP_MESSAGE)) {
            if(NameB == null ) {
                mBinding.titleBar.setTitle(!TextUtils.isEmpty(title) ? title : getString(R.string.zimkit_title_group_chat));
                mBinding.titleBar.setRightCLickListener(v -> {
                    Bundle data = new Bundle();
                    data.putString(ZIMKitConstant.MessagePageConstant.KEY_ID, id);
                    data.putString(ZIMKitConstant.MessagePageConstant.KEY_TITLE, title);
                    Intent intent = new Intent(this, ZIMKitGroupChatSettingActivity.class);
                    intent.putExtra(ZIMKitConstant.RouterConstant.KEY_BUNDLE, data);
                    startActivity(intent, data);
                });
            }
        } else if (type.equals(ZIMKitConstant.MessagePageConstant.TYPE_SINGLE_MESSAGE)) {
            if(productB == null) {
                mBinding.titleBar.setTitle(!TextUtils.isEmpty(title) ? title : getString(R.string.zimkit_title_chat));
                mBinding.titleBar.setRightCLickListener(v -> {
                    Bundle data = new Bundle();
                    data.putString(ZIMKitConstant.MessagePageConstant.KEY_ID, id);
                    data.putString(ZIMKitConstant.MessagePageConstant.KEY_TITLE, title);
                    data.putString(ZIMKitConstant.MessagePageConstant.KEY_AVATAR, avatar);
                    Intent intent = new Intent(this, ZIMKitPrivateChatSettingActivity.class);
                    intent.putExtra(ZIMKitConstant.RouterConstant.KEY_BUNDLE, data);
                    startActivity(intent);
                });
            }
        }
        fragment = new ZIMKitMessageFragment();
        replaceFragment(fragment, bundle);
        if (type.equals(ZIMKitConstant.MessagePageConstant.TYPE_SINGLE_MESSAGE)) {
            fragment.getInformation(type, id);
        }else {
            if (TextUtils.isEmpty(title) || TextUtils.isEmpty(avatar)) {
                fragment.getInformation(type, id);
            }
        }
        // 群聊才有「社群店铺」悬浮球（是否显示由后端 store-entry 的 showStore 决定）
        if (type.equals(ZIMKitConstant.MessagePageConstant.TYPE_GROUP_MESSAGE)) {
            setupStoreBall(id);
        }
    }

    private void replaceFragment(Fragment fragment, Bundle arg) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        fragment.setArguments(arg);
        transaction.replace(R.id.fra_message, fragment);
        transaction.commit();
    }

    // ═══════════════════════════ 社群店铺悬浮球 ═══════════════════════════

    /**
     * 初始化悬浮球（默认 gone）。流程：
     * <ol>
     *   <li>本地判 ZIM 群属性 {@code bizType == "community"}：<b>明确不是</b>（1对1/2人/临时群）→ 直接不渲染、
     *       一次请求都不发；<b>是</b>或<b>查不出来（未登录/超时/异常）</b>→ 都继续走业务接口；</li>
     *   <li>调轻量接口 {@code /social/group/{id}/store-entry}（只回 showStore + distributionId，
     *       不调 Zego、不查禁言/成员 —— 后端已综合"群开启展示 且 群主有 PASS 社群店"）；</li>
     *   <li>{@code showStore == true && distributionId 非空} → 显示；否则不显示。</li>
     * </ol>
     *
     * <p><b>为什么"查不出来也继续"</b>：悬浮球是<b>业务入口</b>，显隐由业务后端决定。
     * ZIM 未登录（如 50130 未激活）时 {@code queryGroupInfo} 回 USER_IS_NOT_LOGGED，
     * 若把这种"查不出来"当成"不是社群群"，悬浮球会因为一个与业务无关的原因永远消失（实测踩过）。
     * 只在<b>明确判定不是社群群</b>时才跳过。
     */
    private void setupStoreBall(String gid) {
        if (mBinding == null || gid == null || gid.isEmpty()) {
            return;
        }
        storeBall = findViewById(R.id.store_ball);
        storeBallHit = findViewById(R.id.store_ball_hit);
        storeBallBg = findViewById(R.id.store_ball_bg);
        storeBallCircle = findViewById(R.id.store_ball_circle);
        storeBallIcon = findViewById(R.id.store_ball_icon);
        android.util.Log.i("StoreEntry", "setupStoreBall gid=" + gid + " ball=" + storeBall
            + " hit=" + storeBallHit + " bg=" + storeBallBg
            + " circle=" + storeBallCircle + " icon=" + storeBallIcon);
        if (storeBall == null || storeBallHit == null) {
            android.util.Log.w("StoreEntry", "setupStoreBall aborted: 布局里找不到悬浮球控件");
            return;
        }
        // 进店走的是触摸回调里的 ACTION_UP（onClick 在本页被 RecyclerView 的触摸拦截吃掉了，实测 onClick 从不触发）
        storeBall.setOnTouchListener(this::handleStoreBallTouch);
        // **透明代理也接同一套触摸**：半隐藏时球本体大半在屏幕外，代理负责"把可点区域放大"
        // （不用 View.setTouchDelegate：它的 bounds 是父 View 本地坐标，挂到根布局会跑到屏幕左上角）
        storeBallHit.setOnTouchListener(this::handleStoreBallTouch);
        // ⚠️ 必须用 INVISIBLE 而不是 GONE：GONE 的 View 不参与测量 → getWidth() 恒 0，
        //    后面 showStoreBall() 的 "w == 0" 守卫会把球永久挡住（实测踩过）
        storeBall.setVisibility(View.INVISIBLE);
        storeBallHit.setVisibility(View.INVISIBLE);
        // 消息区尺寸确定后再摆位；会议横条显隐会改 fra_message 位置 → 每次布局都重算基准
        storeBall.post(() -> {
            computeStoreBallMetrics();
            storeBall.setX(storeBallBaseX);
            repositionStoreBall();
        });
        // 用 lambda 注册（不是匿名内部类）：lambda 内的 `this` 就是 Activity 本身，
        // 匿名内部类的 `this` 是监听器自身 → 那样无法在 onDestroy 里 remove 掉。
        storeGlobalLayoutListener = this::repositionStoreBall;
        mBinding.fraMessage.getViewTreeObserver()
            .addOnGlobalLayoutListener(storeGlobalLayoutListener);
        // 群类型只作为"明确排除"的快速判断：查不出来 ≠ 不是社群群 → 继续让业务后端判定
        try {
            ZIMKitCore.getInstance().zim().queryGroupInfo(gid, (groupInfo, errorInfo) -> runOnUiThread(() -> {
                if (errorInfo != null && errorInfo.code != im.zego.zim.enums.ZIMErrorCode.SUCCESS) {
                    // 查不出来（未登录/网络/异常）→ 不代表"不是社群群"，继续让业务后端判定
                    requestStoreEntry(gid);
                    return;
                }
                String bizType = "";
                if (groupInfo != null && groupInfo.groupAttributes != null) {
                    bizType = String.valueOf(groupInfo.groupAttributes.get("bizType"));
                }
                // 顺手缓存 bizType：聊天设置页要用它"第一帧就对"（查失败/无群信息时不写，避免污染成"非社群"）
                if (groupInfo != null) {
                    com.zegocloud.zimkit.common.utils.GroupBizTypeCache.put(gid, bizType);
                }
                if (!"community".equals(bizType)) {
                    // 明确不是社群频道（1对1/2人/临时群）→ 不渲染悬浮球，也不发业务请求
                    // 打日志是为了可取证：现象"有些群无论如何都不出球"就是走到这里（此前是静默 return）
                    android.util.Log.i("StoreEntry", "gid=" + gid + " bizType=" + bizType
                        + " → 非社群频道，不出球（不发 store-entry）");
                    return;
                }
                requestStoreEntry(gid);
            }));
        } catch (Exception e) {
            android.util.Log.w("StoreEntry", "queryGroupInfo exception gid=" + gid + " : " + e.getMessage()
                + " → fallback to business store-entry");
            requestStoreEntry(gid);
        }
    }

    /** 业务轻量接口：showStore 才是悬浮球的唯一显隐依据（后端已综合群开关 + 群主有无 PASS 店铺） */
    private void requestStoreEntry(String gid) {
        android.util.Log.i("StoreEntry", "requestStoreEntry gid=" + gid);
        StoreEntryApi.fetch(gid, entry -> runOnUiThread(() -> {
            // 生命周期守卫：该回调链最长可存活数十秒（token 闸门 6s + OkHttp 60s + 401 重放），
            // 页面已销毁时不能再动 View / 起动画 / 换背景
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (entry == null || !entry.showStore
                || entry.distributionId == null || entry.distributionId.isEmpty()) {
                return;
            }
            storeDistributionId = entry.distributionId;
            showStoreBall();
        }));
    }

    private void showStoreBall() {
        if (storeBall == null) {
            return;
        }
        // 兜底：万一还没量到尺寸（极端时序），按 dp 直接量一次，避免"宽 0 就永远不显示"
        if (storeBallW == 0 || storeBallH == 0) {
            int w = dp(58);
            int h = dp(58);
            storeBall.measure(
                View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
            storeBall.layout(0, 0, w, h);   // 顺带布局一次，内部 View 才有尺寸
        }
        computeStoreBallMetrics();
        repositionStoreBall();
        applyStoreBallSideShape();
        // 内部元素回展开态（容器居中：白壳 + 橙圆 + 图标）
        applyStoreBallContent(0);
        storeBallHidden = false;
        storeBallBg.animate().cancel();
        storeBallBg.setAlpha(1f);
        storeBallHit.setVisibility(View.VISIBLE);
        storeBall.setVisibility(View.VISIBLE);
        storeBall.setAlpha(STORE_BALL_ALPHA);      // 与消息重叠 → 半透明
        storeBall.setPivotX(storeBallW / 2f);
        storeBall.setPivotY(storeBallH / 2f);
        storeBall.setScaleX(1f);
        storeBall.setScaleY(1f);
        storeBall.setTranslationX(0f);
        storeBall.setTranslationY(0f);
        storeBall.setX(storeBallBaseX);
        storeBall.setY(storeBallRestY);
        syncStoreBallHit();              // 让透明的可点代理跟上球的位置
        storeBall.animate().alpha(1f).setDuration(150).start();
        // 验证用：确认坐标是"父容器坐标"且球落在可见区域内
        android.util.Log.i("StoreEntry", "ball shown: hitTopInParent=" + (storeBallHit == null ? -1 : storeBallHit.getTop())
            + " restY=" + storeBallRestY + " ballH=" + storeBallH
            + " hitH=" + (storeBallHit == null ? -1 : storeBallHit.getHeight())
            + " ballY=" + storeBall.getY() + " hitY=" + (storeBallHit == null ? -1 : storeBallHit.getY()));
    }

    /**
     * 会议横条显隐 → 重新计算纵向基准（球整体下移避让横条，不重叠）。
     *
     * <p><b>坐标（曾出 bug）</b>：球是根 ConstraintLayout 的**直接子 View**，{@code setX/setY} 就是根容器坐标；
     * 消息区容器 {@code fra_message} 同样是它的直接子 View → {@code getTop()/getHeight()} 与球同坐标系，
     * 不需要任何窗口换算。曾经的写法是把球套进一个 58dp 高的透明代理、又按窗口坐标摆位 → 球的 Y 远大于
     * 代理高度（实机 1667px vs 58dp≈203px），被默认 {@code clipChildren=true} 整块裁掉：**能点、但完全看不见**。
     *
     * <p><b>性能守卫</b>：只有"消息区位置/高度真的变了"才动 View。滚动消息列表时布局会高频触发本回调，
     * 若每帧都 {@code setY} → 又请求一次布局（自我反馈）；加了守卫后滚动期间直接 return。
     */
    private void repositionStoreBall() {
        if (storeBall == null || storeBallW == 0 || mBinding == null) {
            return;
        }
        // 消息区容器是根 ConstraintLayout 的直接子 View → getTop()/getHeight() 已经是"根容器坐标"，
        // 与球的 setX/setY 同一坐标系（不需要 getLocationInWindow 那种窗口坐标换算）
        View msgContainer = mBinding.fraMessage;
        if (msgContainer == null) {
            return;
        }
        int msgH = msgContainer.getHeight();
        if (msgH <= 0) {
            return;
        }
        int msgTopInParent = msgContainer.getTop();
        int desiredTop = msgTopInParent + msgH / 2 - storeBallH / 2;
        // 垂直范围限制在消息区（上下各留 8dp）。
        // ⚠️ 消息区的根布局里**包含输入栏**，所以这里无法完全避开输入栏；球本身可被用户拖到任意高度，
        //    这是产品允许的（球贴边、输入栏在底部，正常拖动不会压到发送键）。
        int pad = (int) (8 * getResources().getDisplayMetrics().density);
        desiredTop = Math.max(msgTopInParent + pad,
            Math.min(desiredTop, msgTopInParent + msgH - storeBallH - pad));
        // ── 性能守卫：消息区没变就不碰 View ──
        if (msgTopInParent == lastMsgTopInParent && msgH == lastMsgH && desiredTop == storeBallRestY) {
            return;
        }
        lastMsgTopInParent = msgTopInParent;
        lastMsgH = msgH;
        storeBallRestY = desiredTop;
        if (storeBallDragging) {
            return;
        }
        if (storeBall.getVisibility() != View.VISIBLE) {
            return;
        }
        // 会议横条（layout_group_notice）出现时 fra_message 顶下移 → desiredTop 自然变大 = 下移避让
        storeBall.setY(desiredTop);
        syncStoreBallHit();
    }

    /**
     * 悬浮球触摸：点击进店 / 自由拖动 / 松手吸附到较近一侧（展开态）/ 已贴边后再往外拖 → 半隐藏。
     *
     * <p><b>交互要点（用户确认过的手感）</b>：
     * <ul>
     *   <li>拖动中**隐藏白色外壳**，只留"橙圆 + 白店铺图标"，松手吸附后才恢复外壳；</li>
     *   <li>**吸附本身不触发半隐藏**（以前"用力拖一下就半隐藏"是 bug）：只有球**已经贴在边缘**时，
     *       再往屏幕外方向拖超过阈值才半隐藏；</li>
     *   <li>半隐藏态露出的部分就是图标本身（PEEK 宽故意比小白条大，好点也好认）。</li>
     * </ul>
     */
    private boolean handleStoreBallTouch(View v, MotionEvent ev) {
        if (storeBall == null || storeBall.getVisibility() != View.VISIBLE) {
            return false;
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                storeBallDragging = false;
                storeBallJustExpanded = false;
                dragHideIntent = false;
                storeBall.animate().cancel();
                storeBall.setAlpha(1f);
                ballTouchDownRawX = ev.getRawX();
                ballTouchDownRawY = ev.getRawY();
                ballTouchLastRawX = ev.getRawX();
                dragFromCollapsed = storeBallHidden;
                ballTouchStartBaseLeft = Math.round(storeBall.getX());
                ballTouchStartTop = (int) storeBall.getY();
                if (storeBallHidden) {
                    // 收起态被点住：先展开（点一下就恢复完整）；这一下抬手不再触发"进店"
                    expandStoreBall(false);
                    storeBallJustExpanded = true;
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = ev.getRawX() - ballTouchDownRawX;
                float dy = ev.getRawY() - ballTouchDownRawY;
                ballTouchLastRawX = ev.getRawX();
                // 半隐藏态**不允许拖动改位置**（用户要求：最省事、也最不容易误操作）→ 只有"点击展开"这一种交互，
                // 展开之后才可以拖动。这里把移动整体吃掉（不进入拖动、不判隐藏意图）。
                if (dragFromCollapsed) {
                    return true;
                }
                if (!storeBallDragging && Math.hypot(dx, dy) > dp(6)) {
                    storeBallDragging = true;
                    storeBall.setScaleX(1.05f);
                    storeBall.setScaleY(1.05f);
                    // 拖动中淡出白色外壳，只留橙圆 + 图标
                    storeBallBg.animate().cancel();
                    storeBallBg.animate().alpha(0f).setDuration(120).start();
                }
                if (!storeBallDragging) {
                    return true;
                }
                // 意图判定：球已贴边 + 继续往【屏幕外】方向拉 → 判定为"要收进去"，
                // 立刻吸附到边并给视觉反馈（缩小+淡），不再跟着手指满屏跑（与"拖动换位"区分开）
                if (!dragFromCollapsed && !dragHideIntent) {
                    float outwardDx = storeBallAtLeft ? -dx : dx;
                    int hidePullPx = dp(STORE_BALL_HIDE_PULL_DP);
                    if (outwardDx > hidePullPx) {
                        dragHideIntent = true;
                        storeBall.setX(storeBallBaseX);       // 直接贴回边缘
                        storeBall.setScaleX(0.88f);           // 视觉反馈：正在收进去
                        storeBall.setAlpha(0.85f);
                   }
                }
                if (dragHideIntent) {
                    return true;   // 已进入"收进去"模式：不再跟随手指移动
                }
                float nx = ballTouchStartBaseLeft + dx;
                float ny = ballTouchStartTop + dy;
                int maxX = Math.max(0, getResources().getDisplayMetrics().widthPixels - storeBallW);
                nx = Math.max(0, Math.min(nx, maxX));
                // 纵向限制在消息区内：fra_message 与球同属根布局坐标系，getTop()/getHeight() 直接可比
                View msgContainer = mBinding == null ? null : mBinding.fraMessage;
                if (msgContainer != null && msgContainer.getHeight() > 0) {
                    int msgTop = msgContainer.getTop();
                    ny = Math.max(msgTop, Math.min(ny, msgTop + msgContainer.getHeight() - storeBallH));
                }
                storeBall.setX(nx);
                storeBall.setY(ny);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                storeBall.setScaleX(1f);
                storeBall.setScaleY(1f);
                boolean moved = storeBallDragging;
                boolean justExpanded = storeBallJustExpanded;
                storeBallDragging = false;
                storeBallJustExpanded = false;
                if (moved) {
                    settleStoreBall(dragFromCollapsed);
                    dragFromCollapsed = false;
                    return true;
                }
                dragFromCollapsed = false;
                if (justExpanded) {
                    return true;
                }
                // 纯点击：直接进店（不依赖 onClick —— 本页 RecyclerView 的触摸拦截会把 onClick 吃掉，实测从不触发）
                if (ev.getActionMasked() == MotionEvent.ACTION_UP) {
                   openStoreFromBall();
                }
                return true;
            default:
                return false;
        }
    }

    /** 容器尺寸确定后计算：吸附态容器 X */
    private void computeStoreBallMetrics() {
       storeBallW = storeBall.getWidth();
        storeBallH = storeBall.getHeight();
        if (storeBallW == 0) {
            storeBallW = dp(58);
        }
        if (storeBallH == 0) {
            storeBallH = dp(58);
        }
        storeBallBaseX = storeBallAtLeft ? 0
            : Math.max(0, getResources().getDisplayMetrics().widthPixels - storeBallW);
    }

    /**
     * 内部元素（背板 / 橙圆 / 图标）就位。
     *
     * <p><b>收起态：内部元素一律不动</b>，只让**容器整体滑出屏幕**。这样露在屏幕边缘的是
     * 「白色背板的左端 + 橙色圆的一截弧」——白壳和圆弧一起露出来（用户要的效果），
     * 也彻底避免"圆被推到容器最左端、白壳反而看不见"的错位。
     */
    private void applyStoreBallContent(long animMs) {
        if (storeBallBg == null || storeBallCircle == null || storeBallIcon == null) {
            return;
        }
        // 占位：内部元素永远无位移（historically 这里出过错，别再加 translationX）
        if (animMs <= 0) {
            storeBallCircle.setTranslationX(0f);
            storeBallIcon.setTranslationX(0f);
            storeBallBg.setTranslationX(0f);
        } else {
            storeBallCircle.animate().translationX(0f).setDuration(animMs).start();
            storeBallIcon.animate().translationX(0f).setDuration(animMs).start();
            storeBallBg.animate().translationX(0f).setDuration(animMs).start();
        }
   }

    /** 松手落点判定（容器 = 触摸区，吸附到较近一侧；只有"往屏幕外拉"才会半隐藏） */
    private void settleStoreBall(boolean fromCollapsed) {
        if (storeBall == null) {
            return;
        }
        int screenW = getResources().getDisplayMetrics().widthPixels;
        float centerX = storeBall.getX() + storeBallW / 2f;
        boolean snapLeft = centerX < screenW / 2f;
        storeBallAtLeft = snapLeft;
        applyStoreBallSideShape();
        computeStoreBallMetrics();
        storeBallBg.animate().cancel();
        storeBallBg.setAlpha(1f);          // 吸附态：白壳回来（收起态已改为容器整体滑出 + 背板淡出）
        boolean wasHideIntent = dragHideIntent;
        dragHideIntent = false;
        // ① 从收起态拖出来 / ② 拖动中已判定为"收进去" → 直接半隐藏
        if (fromCollapsed || wasHideIntent) {
           collapseStoreBall();
            return;
        }
        // ③ 其余：吸附到较近一侧并保持展开（绝不停在中间、绝不因"用力拖"就半隐藏）
       expandStoreBall(true);
    }

    /** 根据吸附侧切换外壳形状（贴右=左圆右平；贴左=右圆左平），避免圆角朝外 */
    private void applyStoreBallSideShape() {
        if (storeBallBg == null) {
            return;
        }
        storeBallBg.setBackgroundResource(storeBallAtLeft
            ? R.drawable.zimkit_shape_store_ball_left
            : R.drawable.zimkit_shape_store_ball);
    }

    /** 展开：容器归位贴边（内部元素无位移）+ 白壳回来 + 让代理跟上 */
    private void expandStoreBall(boolean animated) {
        if (storeBall == null) {
            return;
        }
        // ⚠️ 必须先 cancel 球自己的动画：半隐藏的收起动画（220ms）还在跑时用户点球，
        //    若只 setX 会被仍在跑的 .x(targetX) 覆盖 → 视觉还在半隐藏、状态却已"展开"，
        //    再点一下就直接进店（状态与视觉不同源，实测踩过）。
        storeBall.animate().cancel();
        computeStoreBallMetrics();
        storeBallHidden = false;
        applyStoreBallSideShape();
        storeBallBg.animate().cancel();
        applyStoreBallContent(animated ? 200 : 0);
        if (animated) {
            storeBallBg.animate().alpha(1f).setDuration(200).start();
            storeBall.animate()
                .x(storeBallBaseX)
                .alpha(STORE_BALL_ALPHA)
                .setDuration(200)
                .start();
        } else {
            storeBallBg.setAlpha(1f);
            storeBall.setX(storeBallBaseX);
            storeBall.setAlpha(STORE_BALL_ALPHA);
        }
        syncStoreBallHit();
    }

    /**
     * 半隐藏：**容器整体往屏幕外滑**，只在边缘内侧留 {@link #STORE_BALL_PEEK_DP}（白壳一小段 + 完整橙圆图标）。
     *
     * <p>同时：白背板保持可见（贴边小标签）；可点区域由**整宽的透明代理**提供（见 {@link #syncStoreBallHit()}）。
     */
    private void collapseStoreBall() {
        if (storeBall == null) {
            return;
        }
        computeStoreBallMetrics();
        storeBallHidden = true;
        applyStoreBallSideShape();
        applyStoreBallContent(STORE_BALL_ANIM_MS);
        // 白壳**保持可见**：收起时它是"贴在屏幕边缘的白色小标签"，与露出的橙弧一起构成半隐藏外观
        storeBallBg.animate().cancel();
        storeBallBg.setAlpha(1f);
        float targetX = computeCollapsedContainerX();
        storeBall.animate()
            .x(targetX)
            .alpha(1f)
            .setDuration(STORE_BALL_ANIM_MS)
            .withEndAction(this::syncStoreBallHit)
            .start();
   }

    /** 收起态容器应该滑到的 X：整体移出屏幕，只留 PEEK 宽的一截圆弧在屏内 */
    private float computeCollapsedContainerX() {
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int peekPx = dp(STORE_BALL_PEEK_DP);
        return storeBallAtLeft ? -(storeBallW - peekPx) : (screenW - peekPx);
    }

    /**
     * 让"透明点击代理"跟上球的位置与可见性（**替代 View.setTouchDelegate**）。
     *
     * <p>代理与球**同尺寸**（58dp，见布局第 4 段）：纵向跟随球，横向**夹在屏内** —— 半隐藏时球大半滑出屏幕，
     * 代理贴住屏幕边缘，把可点区从"只露 32dp"补回到 58dp（≈ Material 最小可点尺寸）。
     * 不用"整宽代理"：那会在消息区横切出一条 58dp 的热区，点消息变成进店并关聊天页。
     *
     * <p>⚠️ 不用 {@code View.setTouchDelegate}：bounds 是"接收事件的父 View 的本地坐标"，
     * 挂到根 ConstraintLayout 上就落到**屏幕左上角** —— 既没盖住球，还造出一个
     * "点顶部空白就进店并关闭聊天页"的幽灵热区（实测踩过）。
     */
    private void syncStoreBallHit() {
        if (storeBallHit == null || storeBall == null) {
            return;
        }
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int hitW = storeBallHit.getWidth() > 0 ? storeBallHit.getWidth() : storeBallW;
        storeBallHit.setX(Math.max(0f, Math.min(storeBall.getX(), Math.max(0, screenW - hitW))));
        storeBallHit.setY(storeBall.getY());
        storeBallHit.setVisibility(
            storeBall.getVisibility() == View.VISIBLE ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * 点悬浮球 → 打开 uniapp 社群店铺首页。
     *
     * <p>层级关系决定了必须先让位：ZIM 聊天页是**原生 Activity**，社群店铺首页是 **uniapp（webview）**，
     * 原生层级更高 → 不关掉聊天页的话 uniapp 的 navigateTo 执行了也看不见。
     * 与 {@code MemberInfoActivity#openStore} 同一做法：
     * ① 先派发全局事件（uniapp 侧 navigateTo + loading）→ ② 关闭压在 uniapp 之上的原生页 → ③ finish 自己。
     */
    private void openStoreFromBall() {
        android.util.Log.i("StoreEntry", "ball tap → gid=" + groupId
            + " distributionId=" + storeDistributionId);
        if (storeDistributionId == null || storeDistributionId.isEmpty()) {
            Toast.makeText(this, "暂无可进入的社群店铺", Toast.LENGTH_SHORT).show();
            return;
        }
        StoreEntryApi.logClick(groupId, storeDistributionId);
        // 极冷启动（进程被杀后从最近任务恢复到聊天页，uniapp 还没起来）→ 通道未就绪：
        // **暂存事件 + 主动拉起 uniapp**，uniapp 注册通道的瞬间由 TestModule 补发（用户点一次就到位）。
        // 旧做法是弹「请返回应用首页后重试」/「打开店铺失败，请重试」——实测用户只会觉得"点了没反应"，已删除。
        boolean emitted = com.zegocloud.zimkit.common.utils.UniappEventApi.emitOrQueue(
            "OPEN_COMMUNITY_STORE",
            "groupId", groupId == null ? "" : groupId,
            "distributionId", storeDistributionId);
        if (!emitted) {
            android.util.Log.i("StoreEntry", "uniapp 通道未就绪 → 事件已暂存，拉起 uniapp");
            com.zegocloud.zimkit.common.utils.UniappEventApi.bringUniappToFront();
            // 本页先不关：等补发成功后由 TestModule 收掉，避免 uniapp 没起来时露出空白任务栈
            return;
        }
        new Handler(getMainLooper()).postDelayed(() -> {
            try {
                int closed = ZIMKitActivityUtils.finishNativePages(ZIMKitMessageActivity.class);
                android.util.Log.i("StoreEntry", "store jump: closed " + closed + " native page(s); alive="
                    + ZIMKitActivityUtils.describeAliveActivities());
            } catch (Exception e) {
                android.util.Log.w("StoreEntry", "close native pages fail: " + e.getMessage());
            }
            finish();
        }, 150);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.zimkit_activity_message;
    }

    @Override
    protected int getViewModelId() {
        return 0;
    }


    @Override
    protected void initData() {
        if (isFromPush) {
            //Close the page between and session
            ZIMKitActivityUtils.finishActivityForMessage(getComponentName().getClassName());
        }

        ZIMKitConfig zimKitConfig = ZIMKitCore.getInstance().getZimKitConfig();
        if (zimKitConfig != null && zimKitConfig.advancedConfig != null) {
            if (zimKitConfig.advancedConfig.containsKey(ZIMKitAdvancedKey.max_title_width)) {
                String content = zimKitConfig.advancedConfig.get(ZIMKitAdvancedKey.max_title_width);
                int maxTitleWidth = Integer.parseInt(content);
                mBinding.titleBar.setMaxTitleWidth(maxTitleWidth);
            }
        }

        if (fragment != null) {
            fragment.setOnOnTitleClickListener(new ZIMKitMessageFragment.OnTitleClickListener() {
                @Override
                public void titleMultiSelect() {
                    mBinding.titleBar.hideLeftButton();
                    mBinding.titleBar.showLeftTxtButton();
                    if (type.equals(ZIMKitConstant.MessagePageConstant.TYPE_GROUP_MESSAGE)) {
                        mBinding.titleBar.hideRightButton();
                    }
                    mBinding.titleBar.setLeftTxtCLickListener(v -> {
                        if (fragment.isMultiSelect()) {
                            fragment.hideMultiSelectMessage();
                            mBinding.titleBar.hideLeftTxtButton();
                            mBinding.titleBar.showLeftButton();
                        } else {
                            mBinding.titleBar.showLeftButton();
                            mBinding.titleBar.hideLeftTxtButton();
                            if (type.equals(ZIMKitConstant.MessagePageConstant.TYPE_GROUP_MESSAGE)) {
                                mBinding.titleBar.showRightButton();
                            }
                        }
                    });
                }

                @Override
                public void titleNormal() {
                    mBinding.titleBar.showLeftButton();
                    mBinding.titleBar.hideLeftTxtButton();
                    if (type.equals(ZIMKitConstant.MessagePageConstant.TYPE_GROUP_MESSAGE)) {
                        mBinding.titleBar.showRightButton();
                    }
                }

                @Override
                public void setSetTitle(String title) {
                    if (mBinding != null) {
                        if(productX == null && NameX == null) {
                            mBinding.titleBar.setTitle(title);
                        }
                    }
                }
            });
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ZIMKitMessagesListListener listener = ZIMKitCore.getInstance().getMessageListListener();
                if (listener != null) {
                    ZIMKitHeaderBar headerBar = listener.getMessageListHeaderBar(fragment);
                    if (headerBar != null) {
                        mBinding.titleBar.setHeaderBar(headerBar);
                    }
                }
            }
        }, 200);

    }

    @Override
    protected void onDestroy() {
        // 悬浮球：移除布局监听 + 停掉未完成的动画（否则 Activity 销毁后仍会被回调/持有）
        try {
            if (storeGlobalLayoutListener != null && mBinding != null && mBinding.fraMessage != null) {
                mBinding.fraMessage.getViewTreeObserver()
                    .removeOnGlobalLayoutListener(storeGlobalLayoutListener);
            }
        } catch (Exception ignored) {
        }
        storeGlobalLayoutListener = null;
        try {
            if (storeBall != null) {
                storeBall.animate().cancel();
            }
            if (storeBallBg != null) {
                storeBallBg.animate().cancel();
            }
            if (storeBallCircle != null) {
                storeBallCircle.animate().cancel();
            }
            if (storeBallIcon != null) {
                storeBallIcon.animate().cancel();
            }
        } catch (Exception ignored) {
        }
        super.onDestroy();
        if (sCurrent == this) {
            sCurrent = null;
        }
    }
}
