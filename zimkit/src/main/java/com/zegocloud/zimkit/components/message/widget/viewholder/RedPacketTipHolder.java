package com.zegocloud.zimkit.components.message.widget.viewholder;

import android.text.TextUtils;
import android.widget.TextView;
import androidx.databinding.ViewDataBinding;
import com.zegocloud.zimkit.R;
import com.zegocloud.zimkit.components.message.model.CustomMessageModel;
import com.zegocloud.zimkit.components.message.model.ZIMKitMessageModel;
import com.zegocloud.zimkit.services.internal.ZIMKitCore;
import com.zegocloud.zimkit.services.model.CardMessageContent;

/**
 * 红包领取提示（Figma 5339:6374）：居中一行——迷你封套 + 12sp #999999 文案。
 * 文案规则：
 * · 我领的 → 你领取了{发红包的人}的红包
 * · 别人领我的 → {昵称}领取了你的红包[，你的红包已被领完]
 * · 其他 → {昵称}领取了红包
 */
public class RedPacketTipHolder extends MessageViewHolder {

    private final TextView textView;
    private final ViewDataBinding dataBinding;

    public RedPacketTipHolder(ViewDataBinding binding) {
        super(binding);
        this.dataBinding = binding;
        this.textView = binding.getRoot().findViewById(R.id.rpTipText);
    }

    @Override
    public void bind(int id, int position, ZIMKitMessageModel model) {
        super.bind(id, position, model);
        if (textView == null || !(model instanceof CustomMessageModel)) {
            return;
        }
        CardMessageContent card = ((CustomMessageModel) model).getCardContent();
        if (card == null) {
            return;
        }
        String drawerId = card.getNestedString("detail", "userId");
        String drawerName = card.getNestedString("detail", "userName");
        String ownerId = card.getNestedString("detail", "packetOwnerId");
        String ownerName = card.getNestedString("detail", "packetOwnerName");
        boolean isLast = "true".equals(card.getNestedString("detail", "isLast"));
        String selfId = ZIMKitCore.getInstance().getLocalUser() == null ? ""
            : ZIMKitCore.getInstance().getLocalUser().getId();
        if (TextUtils.isEmpty(drawerName)) {
            drawerName = "好友";
        }
        if (TextUtils.isEmpty(ownerName)) {
            ownerName = "好友";
        }

        String text;
        if (!TextUtils.isEmpty(selfId) && selfId.equals(drawerId)) {
            text = "你领取了" + ownerName + "的红包";
        } else if (!TextUtils.isEmpty(selfId) && selfId.equals(ownerId)) {
            text = drawerName + "领取了你的红包" + (isLast ? "，你的红包已被领完" : "");
        } else {
            text = drawerName + "领取了红包" + (isLast ? "，红包已被领完" : "");
        }
        textView.setText(text);
    }
}
