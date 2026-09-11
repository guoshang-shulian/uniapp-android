package com.zegocloud.zimkit.components.message.widget.viewholder;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.databinding.ViewDataBinding;
import com.bumptech.glide.Glide;
import com.zegocloud.zimkit.R;
import com.zegocloud.zimkit.components.message.model.CustomMessageModel;
import com.zegocloud.zimkit.components.message.model.ZIMKitMessageModel;
import com.zegocloud.zimkit.components.message.ui.BackToUniappCallback;
import com.zegocloud.zimkit.components.message.ui.ZIMKitMessageActivity;
import com.zegocloud.zimkit.components.message.ui.ZIMKitMessageFragment;
import com.zegocloud.zimkit.services.model.CardMessageContent;
import com.zegocloud.zimkit.services.model.ZIMKitMessageSubType;

/**
 * 微信聊天风格卡片渲染：
 * - 商品/店铺/文章卡：白色圆角卡，左图右文（WeChat 小程序卡片样式）
 * - 红包卡：红渐变卡片 + 微信红包 icon + 白/金文案
 */
public class CardMessageHolder extends MessageViewHolder {

    private static final int COLOR_TITLE_DARK = 0xFF2A2A2A;
    private static final int COLOR_SUB_GRAY = 0xFF646A73;
    private static final int COLOR_STATUS_GRAY = 0xFF86A3B8;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GOLD = 0xFFFFD78C;
    private static final int COLOR_GOLD_SOFT = 0xFFFFE3C2;

    private final ViewDataBinding binding;
    private final View root;

    public CardMessageHolder(ViewDataBinding binding) {
        super(binding);
        this.binding = binding;
        this.root = binding.getRoot();
    }

    @Override
    public void bind(int id, int position, ZIMKitMessageModel model) {
        super.bind(id, position, model);
        if (!(model instanceof CustomMessageModel)) {
            return;
        }
        CustomMessageModel customModel = (CustomMessageModel) model;
        CardMessageContent card = customModel.getCardContent();
        if (card == null || !card.isValid()) {
            return;
        }
        applyCardStyle(card.cardSubType);
        fillCard(card);
        // 社群邀请卡：先弹确认框，确认后走 open_card（uniapp 加入流程）
        if (card.cardSubType == ZIMKitMessageSubType.COMMUNITY_INVITE) {
            root.findViewById(R.id.item_message_layout).setOnClickListener(v -> showInviteConfirm(card));
            return;
        }
        root.findViewById(R.id.item_message_layout).setOnClickListener(v -> {
            // 卡片动作回调有两个静态注册点：Fragment（本类读的）和 Activity（startGroupChat 只注册了它）。
            // 首次进 App 走 startGroupChat 时 Fragment 侧为 null → 原来直接 show nothing（点了没反应）；
            // 这里加 Activity 兜底，两条入口都能点到就跳。
            BackToUniappCallback cb = ZIMKitMessageFragment.getNativeDataListener();
            if (cb == null) {
                cb = ZIMKitMessageActivity.getNativeDataListener();
            }
            if (cb != null) {
                cb.onCardAction("open_card", card.payloadJson);
            } else {
                System.out.println("[CardBridge] card clicked but no listener registered");
            }
        });
    }

    /** 社群邀请：现代弹窗确认 → 直接调接口申请/加入（不再跳转 uniapp 页面） */
    private void showInviteConfirm(final CardMessageContent card) {
        final String groupName = card.getNestedString("detail", "groupName");
        final String groupLogo = card.getNestedString("detail", "groupLogo");
        final String groupId = card.getNestedString("detail", "groupId");
        final android.app.Dialog dialog = new android.app.Dialog(root.getContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.zimkit_dialog_community_invite);
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                android.graphics.Color.TRANSPARENT));
            window.setLayout(android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        ((TextView) dialog.findViewById(R.id.ciGroupName)).setText(
            TextUtils.isEmpty(groupName) ? "" : groupName);
        ImageView logo = dialog.findViewById(R.id.ciLogo);
        if (!TextUtils.isEmpty(groupLogo)) {
            Glide.with(logo.getContext()).load(groupLogo).circleCrop().into(logo);
        } else {
            logo.setBackgroundResource(R.drawable.zimkit_shape_6dp_image_bg);
        }
        dialog.findViewById(R.id.ciCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.ciConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            applyJoinCommunity(groupId);
        });
        dialog.show();
    }

    /** 确认加入：桥接 uniplugin 执行「申请/直接加入」 */
    private void applyJoinCommunity(String groupId) {
        if (TextUtils.isEmpty(groupId)) {
            android.widget.Toast.makeText(root.getContext(), "社群信息缺失", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        com.zegocloud.zimkit.services.internal.CommunityInviteBridge.Listener listener =
            com.zegocloud.zimkit.services.internal.CommunityInviteBridge.getListener();
        if (listener == null) {
            android.widget.Toast.makeText(root.getContext(), "功能未就绪", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        listener.applyJoin(groupId, (joined, message) -> root.post(() ->
            android.widget.Toast.makeText(root.getContext(),
                message == null || message.isEmpty() ? (joined ? "已加入" : "已提交申请")
                    : message,
                android.widget.Toast.LENGTH_SHORT).show()));
    }

    /** 按卡片类型切换卡片底色（微信式白卡 / 红包红渐变卡）与文字配色 */
    private void applyCardStyle(int subType) {
        View bubble = root.findViewById(R.id.item_message_layout);
        if (bubble == null) {
            return;
        }
        boolean redPacket = subType == ZIMKitMessageSubType.RED_PACKET;
        bubble.setBackgroundResource(redPacket
            ? R.drawable.zimkit_shape_red_packet_card
            : R.drawable.zimkit_shape_8dp_white_card);

        TextView title = root.findViewById(R.id.card_title);
        TextView subtitle = root.findViewById(R.id.card_subtitle);
        TextView footer = root.findViewById(R.id.card_footer);
        TextView status = root.findViewById(R.id.card_status);
        if (title != null) {
            title.setTextColor(redPacket ? COLOR_WHITE : COLOR_TITLE_DARK);
        }
        if (subtitle != null) {
            subtitle.setTextColor(redPacket ? COLOR_WHITE : COLOR_SUB_GRAY);
        }
        if (footer != null) {
            footer.setTextColor(redPacket ? COLOR_WHITE : COLOR_SUB_GRAY);
        }
        if (status != null) {
            status.setTextColor(redPacket ? 0xFFFFF1E0 : COLOR_STATUS_GRAY);
        }
    }

    private void fillCard(CardMessageContent card) {
        boolean redPacket = card.cardSubType == ZIMKitMessageSubType.RED_PACKET;
        boolean invite = card.cardSubType == ZIMKitMessageSubType.COMMUNITY_INVITE;
        // 三套布局互斥显示：红包卡 / 社群邀请卡（上下居中）/ 商品·店铺·文章卡（左图右文·上图下文）
        showCardLayout(redPacket, invite);
        ViewGroup row = root.findViewById(redPacket ? R.id.item_card_layout
            : (invite ? R.id.item_card_invite : R.id.item_card_row));
        TextView title = tv(redPacket ? R.id.card_title : (invite ? R.id.card_invite_title : R.id.card_title_row),
            redPacket ? R.id.card_title : R.id.card_title_row);
        TextView subtitle = tv(
            redPacket ? R.id.card_subtitle : (invite ? R.id.card_invite_sub : R.id.card_subtitle_row),
            redPacket ? R.id.card_subtitle : R.id.card_subtitle_row);
        TextView footer = tv(redPacket ? R.id.card_footer : R.id.card_footer_row_text,
            redPacket ? R.id.card_footer : R.id.card_footer_row_text);
        TextView status = tv(redPacket ? R.id.card_status : R.id.card_status_row,
            redPacket ? R.id.card_status : R.id.card_status_row);
        ImageView icon = iv(redPacket ? R.id.card_icon : (invite ? R.id.card_invite_logo : R.id.card_icon_row),
            redPacket ? R.id.card_icon : R.id.card_icon_row);
        View dividerView = find(R.id.card_divider, R.id.card_divider);
        if (dividerView != null) {
            dividerView.setVisibility(View.GONE);
        }
        // 胶囊行是 LinearLayout，必须按 View 取（不能走 tv()，否则 ClassCastException）
        View chipRowView = find(R.id.card_chip_row, R.id.card_chip_row);
        if (chipRowView != null) {
            chipRowView.setVisibility(View.GONE);
        }
        // 社群邀请卡的「申请加入」按钮：不在 chip/footer 行里，单独复位
        TextView inviteJoin = tv(R.id.card_invite_join, R.id.card_invite_join);
        if (inviteJoin != null) {
            inviteJoin.setVisibility(View.VISIBLE);
        }
        resetCardStyle();

        switch (card.cardSubType) {
            case ZIMKitMessageSubType.RED_PACKET:
                bindRedPacket(card, row, title, subtitle, footer, icon);
                break;
            case ZIMKitMessageSubType.PRODUCT_CARD:
                bindProduct(card, row, title, subtitle, footer, icon);
                break;
            case ZIMKitMessageSubType.SHOP_CARD:
                bindShop(card, row, title, subtitle, icon);
                break;
            case ZIMKitMessageSubType.ARTICLE_CARD:
                bindArticle(card, row, title, subtitle, icon);
                break;
            case ZIMKitMessageSubType.COMMUNITY_INVITE:
                bindCommunityInvite(card, row, title, subtitle, inviteJoin, icon);
                break;
            default:
                title.setText(card.getSummary());
                title.setVisibility(View.VISIBLE);
                subtitle.setVisibility(View.GONE);
                footer.setVisibility(View.GONE);
                status.setVisibility(View.GONE);
                icon.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * 在"红包卡专用视图 / 普通卡视图"之间按需取视图。
     * 注意：**不能在这里强转成具体类型** —— 各 id 的 View 类型不同
     * （card_title 是 TextView、card_chip_row 是 LinearLayout、card_divider 是 View），
     * 统一强转会在绑定卡片时直接抛 ClassCastException（曾导致点卡片闪退）。
     * 所以只返回 View，由调用方按各自类型安全取用。
     */
    private View find(int primary, int fallback) {
        View v = root.findViewById(primary);
        if (v == null && fallback != primary) {
            v = root.findViewById(fallback);
        }
        return v;
    }

    private TextView tv(int primary, int fallback) {
        View v = find(primary, fallback);
        return v instanceof TextView ? (TextView) v : null;
    }

    private ImageView iv(int primary, int fallback) {
        View v = find(primary, fallback);
        return v instanceof ImageView ? (ImageView) v : null;
    }

    /** 三套卡片布局互斥显示，避免 Holder 复用时样式串台 */
    private void showCardLayout(boolean redPacket, boolean invite) {
        setVis(R.id.item_card_layout, redPacket);
        setVis(R.id.item_card_invite, invite);
        setVis(R.id.item_card_row, !redPacket && !invite);
    }

    private void setVis(int id, boolean visible) {
        View v = root.findViewById(id);
        if (v != null) {
            v.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /** 文本区在纵向行里占满整卡宽（商品卡：图在上、文本在下） */
    private void setTextColumnFullWidth() {
        View texts = root.findViewById(R.id.card_text_column);
        if (texts == null) {
            return;
        }
        ViewGroup.LayoutParams lp = texts.getLayoutParams();
        if (lp instanceof android.widget.LinearLayout.LayoutParams) {
            android.widget.LinearLayout.LayoutParams llp = (android.widget.LinearLayout.LayoutParams) lp;
            llp.width = android.widget.LinearLayout.LayoutParams.MATCH_PARENT;
            llp.weight = 0f;
            texts.setLayoutParams(llp);
        } else if (lp != null) {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            texts.setLayoutParams(lp);
        }
    }

    /** 文本区在横向行里占剩余宽（店铺/文章卡：左图右文） */
    private void setTextColumnRemainder() {
        View texts = root.findViewById(R.id.card_text_column);
        if (texts == null) {
            return;
        }
        ViewGroup.LayoutParams lp = texts.getLayoutParams();
        if (lp instanceof android.widget.LinearLayout.LayoutParams) {
            android.widget.LinearLayout.LayoutParams llp = (android.widget.LinearLayout.LayoutParams) lp;
            llp.width = 0;
            llp.weight = 1f;
            texts.setLayoutParams(llp);
        }
    }

    /** 卡片内边距（Figma 24px → 12dp；商品/店铺/文章卡为 0 + 文本区 8dp） */
    private void setBubblePadding(int left, int top, int right, int bottom) {
        View bubble = root.findViewById(R.id.item_message_layout);
        if (bubble != null) {
            bubble.setPadding(dp(left), dp(top), dp(right), dp(bottom));
        }
    }

    private void setTextColumnPadding(int left, int top, int right, int bottom) {
        View texts = root.findViewById(R.id.card_text_column);
        if (texts != null) {
            texts.setPadding(dp(left), dp(top), dp(right), dp(bottom));
        }
    }

    /**
     * 积分红包卡（Figma 5339:6257，750 宽稿 @2x → dp）：
     * · 整卡 248dp 宽、minHeight 92dp，底 #FD9728，圆角 24px → 12dp
     * · 内容行（封面 38×45dp + 文案列）高 45dp、gravity=center_vertical
     *   → 文案与封套**上下居中对齐**（自然布局实现，不再手工算 topMargin；行高固定所以单行/两行都同心）
     * · 底行：分隔线 224×0.5dp 白 30% + 「积分红包」10sp 白，整体贴卡底（layout_gravity=bottom）
     * · 专属卡：未领完 → 「给{昵称}的红包」16sp + 祝福语 14sp；已领完 → 「给{昵称}的红包」+「已被领完」
     * · 已被领完：整卡 op=0.5 + 白色撕开封套；普通卡文案「已被领完」，专属卡保留「给XX的红包」并加「已被领完」
     */
    private void bindRedPacket(CardMessageContent card, ViewGroup row, TextView title, TextView subtitle,
        TextView footer, ImageView icon) {
        View bubble = root.findViewById(R.id.item_message_layout);
        View divider = root.findViewById(R.id.card_divider);
        if (bubble != null) {
            // 卡内边距交回布局 XML（FrameLayout 自带 8/8/8/6dp），这里只复位透明度
            bubble.setAlpha(1f);
        }
        String type = card.getNestedString("detail", "type");
        String remark = card.getNestedString("detail", "remark");
        if (TextUtils.isEmpty(remark)) {
            remark = "恭喜发财，大吉大利";
        }
        String detailStatus = card.getNestedString("detail", "status");
        int drawCount = card.getNestedInt("detail", "drawCount");
        int count = card.getNestedInt("detail", "count");
        // 仿微信：未领完 → 卡片保持原色 + 关闭的红封套；已领完 → 整卡变灰 + 白色撕开封套 + 「已被领完」
        boolean ended = "ended".equals(detailStatus) || (count > 0 && drawCount >= count);
        boolean exclusive = !ended && "exclusive".equals(type);
        String toName = card.getNestedString("detail", "toUserName");

        // 底行「积分红包」+ 白 30% 分隔线（visibility 已由 fillCard 复位为 GONE，这里按红包卡打开）
        footer.setText("积分红包");
        footer.setTextSize(11);
        footer.setVisibility(View.VISIBLE);
        if (divider != null) {
            divider.setVisibility(View.VISIBLE);
        }
        // 底行状态（如「100积分」）：红包卡不展示
        TextView status = root.findViewById(R.id.card_status);
        if (status != null) {
            status.setVisibility(View.GONE);
        }

        if (ended) {
            // 已领完：整卡变灰(op 0.5) + 白色撕开封套
            if (bubble != null) {
                bubble.setAlpha(0.5f);
            }
            boolean hasTo = !TextUtils.isEmpty(toName);
            if (hasTo) {
                // 专属卡保留「给XX的红包」，第二行说明已领完（不丢"给谁"这个信息）
                title.setText("给" + toName + "的红包");
                subtitle.setText("已被领完");
                subtitle.setVisibility(View.VISIBLE);
            } else {
                title.setText("已被领完");
                subtitle.setVisibility(View.GONE);
            }
            title.setVisibility(View.VISIBLE);
            icon.setImageResource(R.drawable.zimkit_ic_envelope_opened);
        } else if (exclusive && !TextUtils.isEmpty(toName)) {
            // 专属红包未领完：原色 + 关闭的红封套 + 「给XX的红包」/ 祝福语
            if (bubble != null) {
                bubble.setAlpha(1f);
            }
            title.setText("给" + toName + "的红包");
            title.setVisibility(View.VISIBLE);
            subtitle.setText(remark);
            subtitle.setVisibility(View.VISIBLE);
            icon.setImageResource(R.drawable.zimkit_ic_envelope_card);
        } else {
            // 普通红包未领完：原色（不变灰）+ 关闭的红封套
            if (bubble != null) {
                bubble.setAlpha(1f);
            }
            title.setText(remark);
            title.setVisibility(View.VISIBLE);
            subtitle.setVisibility(View.GONE);
            icon.setImageResource(R.drawable.zimkit_ic_envelope_card);
        }
        // 封套尺寸由 XML 固定（38×45dp），清掉可能残留的外边距
        ViewGroup.LayoutParams lp = icon.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
            mlp.width = dp(38);
            mlp.height = dp(45);
            mlp.setMarginStart(0);
            mlp.topMargin = 0;
            icon.setLayoutParams(mlp);
        }
        icon.setVisibility(View.VISIBLE);
    }

    /** 设置顶部外边距（单位 dp，FrameLayout 内用 marginTop 定位） */
    private void setTopMargin(View view, int dpValue) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) lp).topMargin = dp2px(dpValue);
            view.setLayoutParams(lp);
        }
    }

    /** 设置起始（左）外边距（单位 dp） */
    private void setStartMargin(View view, int dpValue) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) lp).setMarginStart(dp2px(dpValue));
            view.setLayoutParams(lp);
        }
    }

    /** dp → px（支持小数 dp，不做取整到整数 dp） */
    private int dpI(float dpValue) {
        return (int) (dpValue * root.getContext().getResources().getDisplayMetrics().density + 0.5f);
    }

    /** dp → px（不做四舍五入到 0.5，保留精度） */
    private int dp2px(int dpValue) {
        return dpI(dpValue);
    }

    /** 卡片复用前复位（Holder 会回收，避免上一条卡片样式串到下一条） */
    private void resetCardStyle() {
        View bubble = root.findViewById(R.id.item_message_layout);
        if (bubble != null) {
            bubble.setAlpha(1f);
        }
        // 红包卡为固定尺寸 FrameLayout（XML 已按 Figma 定死），此处只复位文本区样式
        TextView title = root.findViewById(R.id.card_title);
        if (title != null) {
            title.setMaxLines(1);
        }
        TextView titleRow = root.findViewById(R.id.card_title_row);
        if (titleRow != null) {
            titleRow.setTextSize(14);
            titleRow.setMaxLines(2);
        }
        TextView descView = root.findViewById(R.id.card_desc_row);
        if (descView != null) {
            descView.setMaxLines(2);
        }
    }

    private void bindProduct(CardMessageContent card, ViewGroup row, TextView title, TextView subtitle,
        TextView footer, ImageView icon) {
        // Figma 2455:2577 商品卡：496×527px → 248×263.5dp
        //   图 496×339 → 248×169.5dp（仅顶部圆角 r24→12dp）
        //   标题 32px→16sp #111111；胶囊 108×42 → 54×21dp r6→3dp #F1F1F1，12sp #999999；价格 32px→16sp #FF6363
        // 布局：卡 body 自带 8dp 内边距（Figma 16px），图在上、文案在下 —— 全部自然排布，不用绝对定位/外边距硬撑
        setBubblePadding(0, 0, 0, 0);

        if (row instanceof android.widget.LinearLayout) {
            android.widget.LinearLayout ll = (android.widget.LinearLayout) row;
            ll.setOrientation(android.widget.LinearLayout.VERTICAL);
            ll.setPadding(dp(8), dp(8), dp(8), dp(8));
        }
        setTextColumnFullWidth();
        // 文案列不再自己补边距（父容器已有 8dp）
        setTextColumnPadding(0, 0, 0, 0);

        icon.setBackgroundResource(R.drawable.zimkit_shape_card_image_top);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ViewGroup.LayoutParams lp = icon.getLayoutParams();
        if (lp != null) {
            // 宽度必须用 MATCH_PARENT（= 卡 248dp − 卡内边距 8×2 = 232dp）。
            // 之前写死 248dp：比可用宽度多 16dp → 图右侧溢出被裁，视觉上「左边距 8dp、右边距 0」不对等。
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = dpI(169.5f);
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                mlp.setMarginStart(0);
                mlp.topMargin = 0;
                // 图文间距：标题与图片留 10dp
                mlp.bottomMargin = dp(10);
            }
            icon.setLayoutParams(lp);
        }
        loadImage(icon, card.getNestedString("detail", "productImage"));

        title.setText(card.getNestedString("detail", "productName"));
        title.setTextSize(16);
        title.setMaxLines(2);
        title.setTextColor(COLOR_TITLE_DARK);
        title.setVisibility(View.VISIBLE);
        setStartMargin(title, 0);
        setTopMargin(title, 0);

        // 店铺名（设计稿商品卡不含，保留但不展示）
        subtitle.setVisibility(View.GONE);

        // 已售 / 库存 两枚胶囊
        String sales = card.getNestedString("detail", "sales");
        String stock = card.getNestedString("detail", "stock");
        View chipRow = root.findViewById(R.id.card_chip_row);
        TextView chip1 = root.findViewById(R.id.card_chip_1);
        TextView chip2 = root.findViewById(R.id.card_chip_2);
        if (chipRow != null && chip1 != null && chip2 != null) {
            // 设计稿固定展示两枚胶囊（无数据时显示 0，避免卡片塌矮、与设计稿差 26dp）
            String s1 = TextUtils.isEmpty(sales) ? "0" : sales.trim();
            String s2 = TextUtils.isEmpty(stock) ? "0" : stock.trim();
            chip1.setText("已售 " + s1);
            chip2.setText("库存 " + s2);
            chip1.setVisibility(View.VISIBLE);
            chip2.setVisibility(View.VISIBLE);
            chipRow.setVisibility(View.VISIBLE);
            setStartMargin(chipRow, 0);
            ViewGroup.LayoutParams clp = chipRow.getLayoutParams();
            if (clp instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) clp).topMargin = dpI(3);
            }
        }

        String price = card.getNestedString("detail", "price");
        String points = card.getNestedString("detail", "pointsPrice");
        String priceText = TextUtils.isEmpty(price) ? "" : "￥" + price;
        if (TextUtils.isEmpty(priceText) && !TextUtils.isEmpty(points)) {
            priceText = points + "积分";
        }
        footer.setText(priceText);
        footer.setTextSize(16);
        footer.setTextColor(0xFFFF6363);
        footer.setVisibility(TextUtils.isEmpty(priceText) ? View.GONE : View.VISIBLE);
        setStartMargin(footer, 0);
        // 价格距胶囊 16px→8dp
        View footerRow = (View) footer.getParent();
        if (footerRow != null && footerRow.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) footerRow.getLayoutParams()).topMargin = dpI(8);
            footerRow.requestLayout();
        }
        footerRowSetVisible(footerRow, !TextUtils.isEmpty(priceText));
    }

    /** 价格行（footer 的父容器）显隐，避免空行占位 */
    private void footerRowSetVisible(View footerRow, boolean visible) {
        if (footerRow != null) {
            footerRow.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /** 店铺卡：左 logo + 名称/简介 + 信息胶囊（与商品/文章卡同一套卡片语言） */
    private void bindShop(CardMessageContent card, ViewGroup row, TextView title, TextView subtitle,
        ImageView icon) {
        // 卡 248dp 宽、卡内边距 8dp（父容器统一给），logo 62dp r8 在左、文案在右，**整行垂直居中**
        setBubblePadding(0, 0, 0, 0);
        if (row instanceof android.widget.LinearLayout) {
            android.widget.LinearLayout ll = (android.widget.LinearLayout) row;
            ll.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            ll.setGravity(android.view.Gravity.CENTER_VERTICAL);
            ll.setPadding(dp(8), dp(8), dp(8), dp(8));
        }
        setTextColumnRemainder();
        // 文案列不再补边距（父容器已有 8dp），logo 右侧 8dp 由 icon 的 endMargin 负责
        setTextColumnPadding(0, 0, 0, 0);

        icon.setBackgroundResource(R.drawable.zimkit_shape_8dp_image_bg_square);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ViewGroup.LayoutParams lp = icon.getLayoutParams();
        if (lp != null) {
            lp.width = dp(62);
            lp.height = dp(62);
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                mlp.setMarginStart(0);
                mlp.setMarginEnd(dp(8));
                mlp.topMargin = 0;
            }
            icon.setLayoutParams(lp);
        }
        loadImage(icon, card.getNestedString("detail", "storeLogo"));

        title.setText(card.getNestedString("detail", "storeName"));
        title.setTextSize(16);
        title.setMaxLines(2);
        title.setTextColor(COLOR_TITLE_DARK);
        title.setVisibility(View.VISIBLE);
        setStartMargin(title, 0);
        setTopMargin(title, 0);

        String storeDesc = card.getNestedString("detail", "storeDesc");
        subtitle.setText(storeDesc);
        subtitle.setTextSize(12);
        subtitle.setMaxLines(2);
        subtitle.setTextColor(COLOR_SUB_GRAY);
        subtitle.setVisibility(TextUtils.isEmpty(storeDesc) ? View.GONE : View.VISIBLE);
        setStartMargin(subtitle, 0);
        setTopMargin(subtitle, 3);

        // 信息胶囊：标签（联盟商家）+ 商品数 / 收藏数 / 地区（取前两个有值的）
        String tag = card.getNestedString("detail", "tag");
        String goodsNum = card.getNestedString("detail", "goodsNum");
        String collectionNum = card.getNestedString("detail", "collectionNum");
        String location = card.getNestedString("detail", "location");
        String c1 = !TextUtils.isEmpty(tag) ? tag
            : (!TextUtils.isEmpty(goodsNum) ? "商品 " + goodsNum : "");
        String c2 = !TextUtils.isEmpty(collectionNum) ? "收藏 " + collectionNum : location;
        View chipRow = root.findViewById(R.id.card_chip_row);
        TextView chip1 = root.findViewById(R.id.card_chip_1);
        TextView chip2 = root.findViewById(R.id.card_chip_2);
        if (chipRow != null && chip1 != null && chip2 != null) {
            boolean has1 = !TextUtils.isEmpty(c1);
            boolean has2 = !TextUtils.isEmpty(c2);
            chip1.setText(c1);
            chip2.setText(c2);
            chip1.setVisibility(has1 ? View.VISIBLE : View.GONE);
            chip2.setVisibility(has2 ? View.VISIBLE : View.GONE);
            chipRow.setVisibility(has1 || has2 ? View.VISIBLE : View.GONE);
            setStartMargin(chipRow, 0);
            ViewGroup.LayoutParams clp = chipRow.getLayoutParams();
            if (clp instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) clp).topMargin = dpI(4);
            }
        }
    }

    private void bindArticle(CardMessageContent card, ViewGroup row, TextView title, TextView subtitle,
        ImageView icon) {
        // Figma 2455:2643 文章卡：496×155px → 248×77.5dp（白卡）
        //   缩略图 123×123 → 61.5×61.5dp #D9D9D9（卡内 x16→8dp, y16→8dp，无圆角）
        //   标题 28px→14sp #111111（卡内 x155px→77.5dp, y16→8dp，宽 325px→162.5dp）
        //   描述 24px→12sp #666666（卡内 y71px→35.5dp，两行）
        setBubblePadding(0, 0, 0, 0);
        // 缩略图在左、文案在右，**整行垂直居中**
        if (row instanceof android.widget.LinearLayout) {
            android.widget.LinearLayout ll = (android.widget.LinearLayout) row;
            ll.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            ll.setGravity(android.view.Gravity.CENTER_VERTICAL);
            ll.setPadding(dp(8), dp(8), dp(8), dp(8));
        }
        setTextColumnRemainder();

        icon.setBackgroundColor(0xFFD9D9D9);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        int thumb = dpI(61.5f);
        ViewGroup.LayoutParams lp = icon.getLayoutParams();
        if (lp != null) {
            lp.width = thumb;
            lp.height = thumb;
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) lp;
                mlp.setMarginStart(0);
                mlp.setMarginEnd(dp(8));
                mlp.topMargin = 0;
            }
            icon.setLayoutParams(lp);
        }
        loadImage(icon, card.getNestedString("detail", "cover"));
        // 文案列不再补边距（父容器已有 8dp）
        setTextColumnPadding(0, 0, 0, 0);

        title.setText(card.getNestedString("detail", "title"));
        title.setTextSize(14);
        title.setMaxLines(2);
        title.setTextColor(COLOR_TITLE_DARK);
        title.setVisibility(View.VISIBLE);
        setStartMargin(title, 0);
        setTopMargin(title, 0);

        String summary = card.getNestedString("detail", "summary");
        subtitle.setText(summary);
        subtitle.setTextSize(12);
        subtitle.setMaxLines(2);
        subtitle.setTextColor(0xFF666666);
        subtitle.setVisibility(TextUtils.isEmpty(summary) ? View.GONE : View.VISIBLE);
        setStartMargin(subtitle, 0);
        setTopMargin(subtitle, 3);

        // 文章卡无胶囊
        View chipRow = root.findViewById(R.id.card_chip_row);
        if (chipRow != null) {
            chipRow.setVisibility(View.GONE);
        }
    }

    /**
     * 社群邀请卡（上下两段）
     * · 上段：左=社群 logo 48dp，右=邀请语「邀请您加入社群」14sp + 社群名 12sp（两行左对齐，与 logo 垂直居中）
     * · 下段：「申请加入」紫色文字按钮，独占一行、整行居中（样式在布局 XML 固定）
     */
    private void bindCommunityInvite(CardMessageContent card, ViewGroup row, TextView title,
        TextView subtitle, TextView join, ImageView icon) {
        // 卡内边距交回布局 XML（邀请卡自带 12/12/12/8dp），bubble 不再叠加
        setBubblePadding(0, 0, 0, 0);
        title.setText("邀请您加入社群");
        title.setTextSize(14);
        title.setTextColor(COLOR_TITLE_DARK);
        title.setVisibility(View.VISIBLE);
        // 上段是"左图右文"，文字保持左对齐（不要居中）
        title.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);

        subtitle.setText(card.getNestedString("detail", "groupName"));
        subtitle.setTextSize(12);
        subtitle.setVisibility(View.VISIBLE);
        subtitle.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);

        if (join != null) {
            join.setVisibility(View.VISIBLE);
            // 下段按钮：整行居中
            join.setGravity(android.view.Gravity.CENTER);
        }
        icon.setBackgroundResource(R.drawable.zimkit_shape_6dp_image_bg);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        syncIconSize(icon, dp(48));
        loadImage(icon, card.getNestedString("detail", "groupLogo"));
    }

    private void syncIconSize(ImageView icon, int size) {
        ViewGroup.LayoutParams lp = icon.getLayoutParams();
        if (lp != null && (lp.width != size || lp.height != size)) {
            lp.width = size;
            lp.height = size;
            icon.setLayoutParams(lp);
        }
    }

    private void loadImage(ImageView icon, String url) {
        if (TextUtils.isEmpty(url)) {
            icon.setImageResource(R.drawable.zimkit_icon_empty_default);
            icon.setVisibility(View.VISIBLE);
            return;
        }
        Glide.with(icon.getContext()).load(url).into(icon);
        icon.setVisibility(View.VISIBLE);
    }

    private int dp(int value) {
        return (int) (value * root.getContext().getResources().getDisplayMetrics().density + 0.5f);
    }

    private String trimZero(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
