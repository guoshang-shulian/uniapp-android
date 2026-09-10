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
        TextView title = root.findViewById(R.id.card_title);
        TextView subtitle = root.findViewById(R.id.card_subtitle);
        TextView desc = root.findViewById(R.id.card_desc);
        TextView footer = root.findViewById(R.id.card_footer);
        TextView status = root.findViewById(R.id.card_status);
        ImageView icon = root.findViewById(R.id.card_icon);
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
                bindRedPacket(card, title, subtitle, desc, footer, status, icon);
                break;
            case ZIMKitMessageSubType.PRODUCT_CARD:
                bindProduct(card, title, subtitle, desc, footer, status, icon);
                break;
            case ZIMKitMessageSubType.SHOP_CARD:
                bindShop(card, title, subtitle, desc, footer, status, icon);
                break;
            case ZIMKitMessageSubType.ARTICLE_CARD:
                bindArticle(card, title, subtitle, desc, footer, status, icon);
                break;
            case ZIMKitMessageSubType.COMMUNITY_INVITE:
                bindCommunityInvite(card, title, subtitle, desc, footer, status, icon);
                break;
            default:
                title.setText(card.getSummary());
                subtitle.setVisibility(View.GONE);
                desc.setVisibility(View.GONE);
                footer.setVisibility(View.GONE);
                status.setVisibility(View.GONE);
                icon.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * 积分红包卡（严格按 Figma 5339:6257 三种状态）：
     * · 普通卡：#FD9728 底 + 红包封（76×90）+ 祝福语 16sp 白 + 分隔线(白 30%) + 「积分红包」10sp 白
     * · 专属卡：首行「给{昵称}的红包」16sp + 祝福语 14sp + 分隔线 + 「积分红包」
     * · 已被领完：整卡 op=0.5 + 白色撕开封套 + 「已被领完」14sp
     */
    private void bindRedPacket(CardMessageContent card, TextView title, TextView subtitle, TextView desc,
        TextView footer, TextView status, ImageView icon) {
        View bubble = root.findViewById(R.id.item_message_layout);
        View divider = root.findViewById(R.id.card_divider);
        String type = card.getNestedString("detail", "type");
        String remark = card.getNestedString("detail", "remark");
        if (TextUtils.isEmpty(remark)) {
            remark = "恭喜发财，大吉大利";
        }
        String detailStatus = card.getNestedString("detail", "status");
        int drawCount = card.getNestedInt("detail", "drawCount");
        int count = card.getNestedInt("detail", "count");
        boolean ended = "ended".equals(detailStatus) || (count > 0 && drawCount >= count);

        if (ended) {
            if (bubble != null) {
                bubble.setAlpha(0.5f);
            }
            title.setText("已被领完");
            title.setTextSize(14);
            subtitle.setVisibility(View.GONE);
            desc.setVisibility(View.GONE);
            icon.setImageResource(R.drawable.zimkit_ic_envelope_opened);
        } else {
            if (bubble != null) {
                bubble.setAlpha(1f);
            }
            title.setText(remark);
            title.setTextSize(16);
            boolean exclusive = "exclusive".equals(type);
            String toName = card.getNestedString("detail", "toUserName");
            if (exclusive && !TextUtils.isEmpty(toName)) {
                // 专属卡（Figma）：首行=「给XX的红包」32px→16sp，次行=祝福语 28px→14sp
                title.setText("给" + toName + "的红包");
                title.setTextSize(16);
                title.setVisibility(View.VISIBLE);
                subtitle.setText(remark);
                subtitle.setTextSize(14);
                subtitle.setVisibility(View.VISIBLE);
                desc.setVisibility(View.GONE);
            } else {
                subtitle.setVisibility(View.GONE);
                desc.setVisibility(View.GONE);
            }
            icon.setImageResource(R.drawable.zimkit_ic_envelope_card);
        }
        // 空状态归一：普通卡不显示副标题/描述行
        if (!ended && !"exclusive".equals(type)) {
            subtitle.setVisibility(View.GONE);
            desc.setVisibility(View.GONE);
        }

        // 「积分红包」+ 分隔线（Figma：白 1px @30%）
        footer.setText("积分红包");
        footer.setVisibility(View.VISIBLE);
        footer.setTextSize(10);
        if (divider != null) {
            divider.setVisibility(View.VISIBLE);
        }
        status.setVisibility(View.GONE);

        // 封套图标：38×45dp（Figma 76×90 @2x）
        // 卡片高 168px→84dp = 上下留白（上 16px→8dp / 下 12px→6dp）+ 图标 45dp + 分隔线+底行
        icon.setBackground(null);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (bubble != null) {
            bubble.setPadding(dp(12), dp(8), dp(12), dp(6));
        }
        // 红包卡内容宽 = 248 - 左右内边距 24 = 224dp（保证整卡 248dp，与 Figma 496px@2x 一致）
        View cardLayoutView = root.findViewById(R.id.item_card_layout);
        if (cardLayoutView != null && cardLayoutView.getLayoutParams() != null) {
            cardLayoutView.getLayoutParams().width = dp(224);
            cardLayoutView.requestLayout();
        }
        ViewGroup.LayoutParams lp = icon.getLayoutParams();
        if (lp != null) {
            lp.width = dp(38);
            lp.height = dp(45);
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) lp).setMarginEnd(dp(12));
            }
            icon.setLayoutParams(lp);
        }
        icon.setVisibility(View.VISIBLE);
    }

    /** 卡片复用前复位（Holder 会回收，避免上一条卡片样式串到下一条） */
    private void resetCardStyle() {
        View bubble = root.findViewById(R.id.item_message_layout);
        if (bubble != null) {
            bubble.setAlpha(1f);
            int pad = dp(10);
            bubble.setPadding(pad, pad, pad, pad);
        }
        View cardLayout = root.findViewById(R.id.item_card_layout);
        if (cardLayout instanceof android.widget.LinearLayout) {
            android.widget.LinearLayout layout = (android.widget.LinearLayout) cardLayout;
            layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            // 卡片总宽 Figma 496px→248dp（含内边距）
            ViewGroup.LayoutParams clp = layout.getLayoutParams();
            if (clp != null) {
                clp.width = dp(248);
                layout.setLayoutParams(clp);
            }
            if (layout.getChildCount() > 1 && layout.getChildAt(1) instanceof android.widget.LinearLayout) {
                ((android.widget.LinearLayout) layout.getChildAt(1)).setPadding(0, 0, 0, 0);
            }
        }
        TextView title = root.findViewById(R.id.card_title);
        if (title != null) {
            title.setTextSize(14);
            title.setMaxLines(2);
            title.setLineSpacing(0, 1.4f);
        }
        TextView subtitle = root.findViewById(R.id.card_subtitle);
        if (subtitle != null) {
            subtitle.setTextSize(12);
            subtitle.setLineSpacing(0, 1.4f);
        }
        TextView descView = root.findViewById(R.id.card_desc);
        if (descView != null) {
            descView.setLineSpacing(0, 1.4f);
        }
        TextView footer = root.findViewById(R.id.card_footer);
        if (footer != null) {
            footer.setTextSize(11);
            footer.setLineSpacing(0, 1.4f);
        }
        TextView statusView = root.findViewById(R.id.card_status);
        if (statusView != null) {
            statusView.setLineSpacing(0, 1.4f);
        }
    }

    private void bindProduct(CardMessageContent card, TextView title, TextView subtitle, TextView desc,
        TextView footer, TextView status, ImageView icon) {
        // Figma 2455:2557 商品卡：白卡（248×263dp）上图（248×169dp，顶部圆角）+ 标题 16sp#111
        // + 价格 16sp#FF6363 + 店铺/规格信息 12sp#999
        View bubble = root.findViewById(R.id.item_message_layout);
        if (bubble != null) {
            bubble.setPadding(0, 0, 0, 0);
        }
        View cardLayout = root.findViewById(R.id.item_card_layout);
        if (cardLayout instanceof android.widget.LinearLayout) {
            android.widget.LinearLayout layout = (android.widget.LinearLayout) cardLayout;
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            if (layout.getChildCount() > 1 && layout.getChildAt(1) instanceof android.widget.LinearLayout) {
                android.widget.LinearLayout texts = (android.widget.LinearLayout) layout.getChildAt(1);
                // Figma：卡片左右内边距 16px→8dp，标题距图 16px→8dp，底部 16px→8dp
                texts.setPadding(dp(8), dp(8), dp(12), dp(8));
            }
        }
        title.setText(card.getNestedString("detail", "productName"));
        title.setTextSize(16);
        title.setMaxLines(2);
        title.setTextColor(COLOR_TITLE_DARK);

        String merchant = card.getNestedString("detail", "merchantName");
        subtitle.setText(merchant);
        subtitle.setTextSize(12);
        subtitle.setVisibility(TextUtils.isEmpty(merchant) ? View.GONE : View.VISIBLE);

        String sales = card.getNestedString("detail", "sales");
        String stock = card.getNestedString("detail", "stock");
        // Figma：两枚胶囊 54×21dp r3 #F1F1F1 + 12sp #999
        View chipRow = root.findViewById(R.id.card_chip_row);
        TextView chip1 = root.findViewById(R.id.card_chip_1);
        TextView chip2 = root.findViewById(R.id.card_chip_2);
        if (chipRow != null && chip1 != null && chip2 != null) {
            boolean has1 = !TextUtils.isEmpty(sales);
            boolean has2 = !TextUtils.isEmpty(stock);
            chip1.setText(has1 ? "已售 " + sales : "");
            chip2.setText(has2 ? "库存 " + stock : "");
            chip1.setVisibility(has1 ? View.VISIBLE : View.GONE);
            chip2.setVisibility(has2 ? View.VISIBLE : View.GONE);
            chipRow.setVisibility(has1 || has2 ? View.VISIBLE : View.GONE);
        }
        desc.setVisibility(View.GONE);

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
        status.setVisibility(View.GONE);
        // Figma：胶囊底 → 价格 16px→8dp
        View footerRow = (View) footer.getParent();
        if (footerRow != null && footerRow.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) footerRow.getLayoutParams()).topMargin = dp(8);
            footerRow.requestLayout();
        }

        // 大图在顶部：宽撑满卡片、高 169dp、只留顶部圆角
        icon.setBackgroundResource(R.drawable.zimkit_shape_card_image_top);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ViewGroup.LayoutParams lp = icon.getLayoutParams();
        if (lp != null) {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = dp(169);
            icon.setLayoutParams(lp);
        }
        loadImage(icon, card.getNestedString("detail", "productImage"));
    }

    /** 店铺卡（与商品卡同一套现代化卡片语言：左 logo 62dp + 名称 16sp + 简介 12sp 两行 + 信息胶囊） */
    private void bindShop(CardMessageContent card, TextView title, TextView subtitle, TextView desc,
        TextView footer, TextView status, ImageView icon) {
        View bubble = root.findViewById(R.id.item_message_layout);
        if (bubble != null) {
            bubble.setPadding(0, 0, 0, 0);
        }
        View cardLayout = root.findViewById(R.id.item_card_layout);
        if (cardLayout instanceof android.widget.LinearLayout) {
            android.widget.LinearLayout layout = (android.widget.LinearLayout) cardLayout;
            layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            if (layout.getChildCount() > 1 && layout.getChildAt(1) instanceof android.widget.LinearLayout) {
                android.widget.LinearLayout texts = (android.widget.LinearLayout) layout.getChildAt(1);
                texts.setPadding(0, dp(8), dp(12), dp(8));
            }
        }
        title.setText(card.getNestedString("detail", "storeName"));
        title.setTextSize(16);
        title.setMaxLines(2);
        title.setTextColor(COLOR_TITLE_DARK);

        String storeDesc = card.getNestedString("detail", "storeDesc");
        subtitle.setText(storeDesc);
        subtitle.setTextSize(12);
        subtitle.setMaxLines(2);
        subtitle.setVisibility(TextUtils.isEmpty(storeDesc) ? View.GONE : View.VISIBLE);

        desc.setVisibility(View.GONE);
        footer.setVisibility(View.GONE);
        status.setVisibility(View.GONE);

        // 信息胶囊：标签（联盟商家等）+ 所在地区
        String tag = card.getNestedString("detail", "tag");
        String location = card.getNestedString("detail", "location");
        View chipRow = root.findViewById(R.id.card_chip_row);
        TextView chip1 = root.findViewById(R.id.card_chip_1);
        TextView chip2 = root.findViewById(R.id.card_chip_2);
        if (chipRow != null && chip1 != null && chip2 != null) {
            boolean has1 = !TextUtils.isEmpty(tag);
            boolean has2 = !TextUtils.isEmpty(location);
            chip1.setText(tag);
            chip2.setText(location);
            chip1.setVisibility(has1 ? View.VISIBLE : View.GONE);
            chip2.setVisibility(has2 ? View.VISIBLE : View.GONE);
            chipRow.setVisibility(has1 || has2 ? View.VISIBLE : View.GONE);
        }

        // 左 logo 62dp 圆角 8dp（与文章卡缩略图同一节奏）
        icon.setBackgroundResource(R.drawable.zimkit_shape_8dp_image_bg_square);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        syncIconSize(icon, dp(62));
        ViewGroup.LayoutParams lp = icon.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) lp).setMarginEnd(dp(8));
            icon.setLayoutParams(lp);
        }
        loadImage(icon, card.getNestedString("detail", "storeLogo"));
    }

    private void bindArticle(CardMessageContent card, TextView title, TextView subtitle, TextView desc,
        TextView footer, TextView status, ImageView icon) {
        // Figma 2455:2557 文章卡：白卡 496×155@2x → 左缩略图 123×123（r0）+ 标题 14sp#111（2 行）
        // + 描述 12sp#666（2 行）+ 作者 12sp#999
        View bubble = root.findViewById(R.id.item_message_layout);
        if (bubble != null) {
            bubble.setPadding(0, 0, 0, 0);
        }
        View cardLayout = root.findViewById(R.id.item_card_layout);
        if (cardLayout instanceof android.widget.LinearLayout) {
            android.widget.LinearLayout layout = (android.widget.LinearLayout) cardLayout;
            layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            if (layout.getChildCount() > 1 && layout.getChildAt(1) instanceof android.widget.LinearLayout) {
                android.widget.LinearLayout texts = (android.widget.LinearLayout) layout.getChildAt(1);
                // Figma 文章卡：缩略图左右 16px→8dp，上下 16px→8dp
                texts.setPadding(0, dp(8), dp(8), dp(8));
            }
        }
        title.setText(card.getNestedString("detail", "title"));
        title.setTextSize(14);
        title.setMaxLines(2);
        title.setTextColor(COLOR_TITLE_DARK);

        String summary = card.getNestedString("detail", "summary");
        subtitle.setText(summary);
        subtitle.setTextSize(12);
        subtitle.setMaxLines(2);
        subtitle.setVisibility(TextUtils.isEmpty(summary) ? View.GONE : View.VISIBLE);

        desc.setVisibility(View.GONE);
        // Figma 文章卡内不含作者行 → 隐藏底行
        footer.setVisibility(View.GONE);
        status.setVisibility(View.GONE);

        // 左缩略图 123px → 61.5dp（Figma 方形、无圆角、左右 8dp）
        icon.setBackgroundResource(R.drawable.zimkit_shape_card_image_top);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        int thumb = (int) (61.5f * root.getContext().getResources().getDisplayMetrics().density + 0.5f);
        ViewGroup.LayoutParams lp = icon.getLayoutParams();
        if (lp != null) {
            lp.width = thumb;
            lp.height = thumb;
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) lp).setMarginEnd(dp(8));
            }
            icon.setLayoutParams(lp);
        }
        loadImage(icon, card.getNestedString("detail", "cover"));
    }

    /** 社群邀请卡：logo + 邀请您加入社群 + 社群名 + 居中“申请加入” */
    private void bindCommunityInvite(CardMessageContent card, TextView title, TextView subtitle, TextView desc,
        TextView footer, TextView status, ImageView icon) {
        title.setText("邀请您加入社群");
        subtitle.setText(card.getNestedString("detail", "groupName"));
        desc.setVisibility(View.GONE);
        footer.setText("申请加入");
        footer.setTextColor(0xFF9079F9);
        footer.setVisibility(View.VISIBLE);
        status.setVisibility(View.GONE);
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
