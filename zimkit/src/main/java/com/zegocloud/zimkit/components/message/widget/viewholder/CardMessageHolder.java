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
            ? R.drawable.zimkit_shape_8dp_red_packet
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
            subtitle.setTextColor(redPacket ? COLOR_GOLD : COLOR_SUB_GRAY);
        }
        if (desc != null) {
            desc.setTextColor(redPacket ? COLOR_GOLD_SOFT : COLOR_SUB_GRAY);
        }
        if (footer != null) {
            footer.setTextColor(redPacket ? COLOR_GOLD_SOFT : COLOR_SUB_GRAY);
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

    private void bindRedPacket(CardMessageContent card, TextView title, TextView subtitle, TextView desc,
        TextView footer, TextView status, ImageView icon) {
        String type = card.getNestedString("detail", "type");

        // 微信红包卡片：第一行=祝福语，第二行=“微信红包”，底行=类型/金额/状态
        String remark = card.getNestedString("detail", "remark");
        title.setText(TextUtils.isEmpty(remark) ? "恭喜发财，大吉大利" : remark);
        subtitle.setText("微信红包");
        subtitle.setVisibility(View.VISIBLE);
        desc.setVisibility(View.GONE);

        String typeLabel;
        if ("fortune".equals(type)) {
            typeLabel = "拼手气红包";
        } else if ("exclusive".equals(type)) {
            typeLabel = "专属红包";
        } else {
            typeLabel = "普通红包";
        }

        String statusText;
        String detailStatus = card.getNestedString("detail", "status");
        double myDrawPoints = card.getNestedDouble("detail", "myDrawPoints");
        int drawCount = card.getNestedInt("detail", "drawCount");
        int count = card.getNestedInt("detail", "count");
        double totalPoints = card.getNestedDouble("detail", "totalPoints");

        if (myDrawPoints > 0) {
            statusText = "已领取 " + trimZero(myDrawPoints) + " 积分";
        } else if ("ended".equals(detailStatus)) {
            statusText = "已抢完";
        } else if ("expired".equals(detailStatus) || "refunded".equals(detailStatus)) {
            statusText = "已过期";
        } else if ("exclusive".equals(type)) {
            statusText = "专属红包 · 点击领取";
        } else {
            statusText = drawCount + "/" + count + "个 · 点击领取";
        }
        status.setText(statusText);
        status.setVisibility(View.VISIBLE);

        footer.setText(typeLabel + " · " + trimZero(totalPoints) + "积分");
        footer.setVisibility(View.VISIBLE);

        // 微信红包 icon：暖红包身 + 金色圆扣，去掉缩略图圆角底
        icon.setImageResource(R.drawable.zimkit_ic_red_packet);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setBackground(null);
        ViewGroup.LayoutParams lp = icon.getLayoutParams();
        if (lp != null) {
            lp.width = dp(44);
            lp.height = dp(44);
            icon.setLayoutParams(lp);
        }
        icon.setVisibility(View.VISIBLE);
    }

    private void bindProduct(CardMessageContent card, TextView title, TextView subtitle, TextView desc,
        TextView footer, TextView status, ImageView icon) {
        title.setText(card.getNestedString("detail", "productName"));
        subtitle.setText(card.getNestedString("detail", "merchantName"));
        desc.setText(card.getNestedString("detail", "remark"));
        desc.setVisibility(TextUtils.isEmpty(card.getNestedString("detail", "remark"))
            ? View.GONE : View.VISIBLE);
        String price = card.getNestedString("detail", "price");
        String points = card.getNestedString("detail", "pointsPrice");
        footer.setText((TextUtils.isEmpty(price) ? "" : "¥" + price)
            + (TextUtils.isEmpty(points) ? "" : " · " + points + "积分"));
        footer.setTextColor(0xFFFA5151);
        footer.setVisibility(View.VISIBLE);
        status.setVisibility(View.GONE);
        icon.setBackgroundResource(R.drawable.zimkit_shape_6dp_image_bg);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        syncIconSize(icon, dp(56));
        loadImage(icon, card.getNestedString("detail", "productImage"));
    }

    private void bindShop(CardMessageContent card, TextView title, TextView subtitle, TextView desc,
        TextView footer, TextView status, ImageView icon) {
        title.setText(card.getNestedString("detail", "storeName"));
        subtitle.setText(card.getNestedString("detail", "storeDesc"));
        desc.setText(card.getNestedString("detail", "remark"));
        desc.setVisibility(TextUtils.isEmpty(card.getNestedString("detail", "remark"))
            ? View.GONE : View.VISIBLE);
        footer.setText(card.getNestedString("detail", "tag"));
        footer.setVisibility(TextUtils.isEmpty(card.getNestedString("detail", "tag"))
            ? View.GONE : View.VISIBLE);
        status.setVisibility(View.GONE);
        icon.setBackgroundResource(R.drawable.zimkit_shape_6dp_image_bg);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        syncIconSize(icon, dp(56));
        loadImage(icon, card.getNestedString("detail", "storeLogo"));
    }

    private void bindArticle(CardMessageContent card, TextView title, TextView subtitle, TextView desc,
        TextView footer, TextView status, ImageView icon) {
        title.setText(card.getNestedString("detail", "title"));
        subtitle.setText(card.getNestedString("detail", "summary"));
        desc.setText(card.getNestedString("detail", "tag"));
        desc.setVisibility(TextUtils.isEmpty(card.getNestedString("detail", "tag"))
            ? View.GONE : View.VISIBLE);
        footer.setText(card.getNestedString("detail", "authorName"));
        footer.setVisibility(TextUtils.isEmpty(card.getNestedString("detail", "authorName"))
            ? View.GONE : View.VISIBLE);
        String publishTime = card.getNestedString("detail", "publishTime");
        if (TextUtils.isEmpty(publishTime)) {
            status.setVisibility(View.GONE);
        } else {
            status.setText(publishTime);
            status.setVisibility(View.VISIBLE);
        }
        icon.setBackgroundResource(R.drawable.zimkit_shape_6dp_image_bg);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        syncIconSize(icon, dp(56));
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
