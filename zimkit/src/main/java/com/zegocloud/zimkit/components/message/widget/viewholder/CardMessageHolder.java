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
            System.out.println("[CardBridge] card clicked, listener="
                + (ZIMKitMessageFragment.getNativeDataListener() != null));
            if (ZIMKitMessageFragment.getNativeDataListener() != null) {
                ZIMKitMessageFragment.getNativeDataListener()
                    .onCardAction("open_card", card.payloadJson);
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
        TextView desc = root.findViewById(R.id.card_desc);
        TextView footer = root.findViewById(R.id.card_footer);
        TextView status = root.findViewById(R.id.card_status);
        if (title != null) {
            title.setTextColor(redPacket ? COLOR_WHITE : COLOR_TITLE_DARK);
        }
        if (subtitle != null) {
            subtitle.setTextColor(redPacket ? COLOR_WHITE : COLOR_SUB_GRAY);
        }
        if (desc != null) {
            desc.setTextColor(redPacket ? COLOR_WHITE : COLOR_SUB_GRAY);
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
        // 红包卡 / 其他卡各有独立布局（红包卡为 Figma 精确定位的 FrameLayout，见 zimkit_item_message_*_card.xml）
        showCardLayout(redPacket);
        ViewGroup row = root.findViewById(redPacket ? R.id.item_card_layout : R.id.item_card_row);
        TextView title = root.findViewById(redPacket ? R.id.card_title : R.id.card_title_row);
        TextView subtitle = root.findViewById(redPacket ? R.id.card_subtitle : R.id.card_subtitle_row);
        TextView desc = root.findViewById(redPacket ? R.id.card_desc : R.id.card_desc_row);
        TextView footer = root.findViewById(redPacket ? R.id.card_footer : R.id.card_footer_row_text);
        TextView status = root.findViewById(redPacket ? R.id.card_status : R.id.card_status_row);
        ImageView icon = root.findViewById(redPacket ? R.id.card_icon : R.id.card_icon_row);
        View dividerView = root.findViewById(R.id.card_divider);
        if (dividerView != null) {
            dividerView.setVisibility(View.GONE);
        }
        View chipRowView = root.findViewById(R.id.card_chip_row);
        if (chipRowView != null) {
            chipRowView.setVisibility(View.GONE);
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
                bindCommunityInvite(card, row, title, subtitle, footer, icon);
                break;
            default:
                title.setText(card.getSummary());
                title.setVisibility(View.VISIBLE);
                subtitle.setVisibility(View.GONE);
                desc.setVisibility(View.GONE);
                footer.setVisibility(View.GONE);
                status.setVisibility(View.GONE);
                icon.setVisibility(View.GONE);
                break;
        }
    }

    /** 红包卡（FrameLayout）与其他卡（左图右文行）互斥显示，避免 Holder 复用时样式串台 */
    private void showCardLayout(boolean redPacket) {
        View packetLayout = root.findViewById(R.id.item_card_layout);
        View rowLayout = root.findViewById(R.id.item_card_row);
        if (packetLayout != null) {
            packetLayout.setVisibility(redPacket ? View.VISIBLE : View.GONE);
        }
        if (rowLayout != null) {
            rowLayout.setVisibility(redPacket ? View.GONE : View.VISIBLE);
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
     * 积分红包卡（严格按 Figma 5339:6257，750 宽稿 @2x → dp）：
     * · 整卡 496×168 → 248×84dp，底 #FD9728，圆角 24px → 12dp
     * · 封套 76×90 → 38×45dp，卡内偏移 x24,y16 → 12dp,8dp
     * · 分隔线 448×1 → 224×0.5dp，白色 30%，卡内 y 122px → 61dp
     * · 「积分红包」20px → 10sp 白，卡内 y 131px → 65.5dp
     * · 普通卡祝福语 32px → 16sp，卡内 58dp,19dp（正文左边界 116px → 58dp）
     * · 专属卡首行「给{昵称}的红包」16sp（19dp）+ 次行祝福语 14sp（49dp）
     * · 已被领完：整卡 op=0.5 + 白色撕开封套 + 「已被领完」14sp
     * 尺寸/字号/间距全部由布局 XML 固定，这里只切状态与文案。
     */
    private void bindRedPacket(CardMessageContent card, ViewGroup row, TextView title, TextView subtitle,
        TextView footer, ImageView icon) {
        View bubble = root.findViewById(R.id.item_message_layout);
        View divider = root.findViewById(R.id.card_divider);
        if (bubble != null) {
            // 卡内边距：上边距加大、下边距 0（下边距会把「积分红包」顶向白线）
            bubble.setPadding(dp(6), dp(8), dp(6), 0);
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

        // 「积分红包」底行 + 白 30% 分隔线（Figma：卡内 y122px→61dp / y131px→65.5dp）
        footer.setText("积分红包");
        footer.setVisibility(View.VISIBLE);
        if (divider != null) {
            divider.setVisibility(View.VISIBLE);
        }

        // 文案与其左侧封套**上下居中对齐**：
        //   普通卡（1 行）→ 与封套同心（封套 8+45/2=30.5dp，行高 22.4dp → top 22dp）
        //   专属卡（2 行）→ 两行整体以封套中心对称（19dp / 42dp ⇒ 中心 30.5dp）
        final int titleTop = exclusive ? 19 : 22;
        setTopMargin(title, titleTop);
        setTopMargin(subtitle, exclusive ? 42 : 43);
        setTopMargin(footer, 74);
        footer.setTextSize(11);

        if (ended) {
            // 已领完（含自己领完）：整卡变灰 + 白色撕开封套 + 「已被领完」
            if (bubble != null) {
                bubble.setAlpha(0.5f);
            }
            title.setText("已被领完");
            title.setVisibility(View.VISIBLE);
            subtitle.setVisibility(View.GONE);
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
        ViewGroup.LayoutParams lp = icon.getLayoutParams();
        if (lp != null) {
            lp.width = dp(38);
            lp.height = dp(45);
            icon.setLayoutParams(lp);
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
        //   标题 32px→16sp #111111（卡内 y55px→27.5dp，左 16px→8dp）
        //   胶囊 108×42 → 54×21dp r6→3dp #F1F1F1，12sp #999999（卡内 y108px→54dp）
        //   价格 32px→16sp #FF6363（卡内 y166px→83dp）
        setBubblePadding(0, 0, 0, 0);

        // 行改纵向：图在上、文本在下；图标整卡宽 ×169.5dp
        if (row instanceof android.widget.LinearLayout) {
            ((android.widget.LinearLayout) row).setOrientation(android.widget.LinearLayout.VERTICAL);
        }
        setTextColumnFullWidth();

        icon.setBackgroundResource(R.drawable.zimkit_shape_card_image_top);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ViewGroup.LayoutParams lp = icon.getLayoutParams();
        if (lp != null) {
            lp.width = dpI(248);
            lp.height = dpI(169.5f);
            icon.setLayoutParams(lp);
        }
        // 上边距 = 左边距（用户要求：两者一致）
        setTopMargin(icon, 8);
        setStartMargin(icon, 8);
        loadImage(icon, card.getNestedString("detail", "productImage"));

        // 文本区：卡内 8dp 内边距（Figma 16px→8dp），底部多留一点把卡撑到设计稿高度
        setTextColumnPadding(8, 10, 8, 12);
        title.setText(card.getNestedString("detail", "productName"));
        title.setTextSize(16);
        title.setMaxLines(2);
        title.setTextColor(COLOR_TITLE_DARK);
        title.setVisibility(View.VISIBLE);
        setStartMargin(title, 0);
        setTopMargin(title, 2);

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
        // Figma 未给店铺卡帧 → 沿用商品/文章卡的节奏：卡 248dp 宽、左 logo 62dp r8、名称 16sp #111、简介 12sp #666 两行、胶囊 21dp
        setBubblePadding(0, 0, 0, 0);
        // 行改横向：logo 在左、文本在右
        if (row instanceof android.widget.LinearLayout) {
            ((android.widget.LinearLayout) row).setOrientation(android.widget.LinearLayout.HORIZONTAL);
        }
        setTextColumnRemainder();

        icon.setBackgroundResource(R.drawable.zimkit_shape_8dp_image_bg_square);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        syncIconSize(icon, dp(62));
        ViewGroup.LayoutParams lp = icon.getLayoutParams();
        if (lp != null) {
            lp.width = dp(62);
            lp.height = dp(62);
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) lp).setMarginEnd(dp(8));
            }
            icon.setLayoutParams(lp);
        }
        setStartMargin(icon, 8);
        setTopMargin(icon, 8);
        loadImage(icon, card.getNestedString("detail", "storeLogo"));
        // 文本区：卡内 8dp 内边距，logo 右侧 8dp（Figma 商品/文章卡同节奏）
        setTextColumnPadding(8, 8, 12, 8);

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
        // 行改横向：缩略图在左、文本在右
        if (row instanceof android.widget.LinearLayout) {
            ((android.widget.LinearLayout) row).setOrientation(android.widget.LinearLayout.HORIZONTAL);
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
                ((ViewGroup.MarginLayoutParams) lp).setMarginEnd(0);
            }
            icon.setLayoutParams(lp);
        }
        setStartMargin(icon, 8);
        setTopMargin(icon, 8);
        loadImage(icon, card.getNestedString("detail", "cover"));
        // 文本区：卡内 8dp 内边距，缩略图右侧 8dp
        setTextColumnPadding(8, 8, 8, 8);

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

    /** 社群邀请卡：logo + 邀请您加入社群 + 社群名 + 居中“申请加入” */
    private void bindCommunityInvite(CardMessageContent card, ViewGroup row, TextView title,
        TextView subtitle, TextView footer, ImageView icon) {
        setBubblePadding(12, 12, 12, 12);
        title.setText("邀请您加入社群");
        title.setTextSize(14);
        title.setTextColor(COLOR_TITLE_DARK);
        subtitle.setText(card.getNestedString("detail", "groupName"));
        subtitle.setTextSize(12);
        subtitle.setVisibility(View.VISIBLE);
        footer.setText("申请加入");
        footer.setTextColor(0xFF9079F9);
        footer.setVisibility(View.VISIBLE);
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
