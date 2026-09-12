package com.zegocloud.zimkit.components.message.adapter;

import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.RecyclerView;
import com.zegocloud.zimkit.BR;
import com.zegocloud.zimkit.R;
import com.zegocloud.zimkit.components.message.adapter.ZIMKitInputMoreAdapter.InputMoreItemViewHolder;
import com.zegocloud.zimkit.components.message.model.ZIMKitInputButtonModel;
import java.util.ArrayList;
import java.util.List;

public class ZIMKitInputMoreAdapter extends RecyclerView.Adapter<InputMoreItemViewHolder> {

    private List<ZIMKitInputButtonModel> itemModels = new ArrayList<>();

    @NonNull
    @Override
    public InputMoreItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewDataBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
            R.layout.zimkit_item_input_more, parent, false);
        return new InputMoreItemViewHolder(binding);
    }


    @Override
    public void onBindViewHolder(@NonNull InputMoreItemViewHolder holder, int position) {
        holder.bind(BR.model, itemModels.get(position));
        // 说明：这里**不再**对红包按钮做特殊处理。
        // 早先为了让红包"直接呈现在灰底上"（仿微信「+」面板），曾把它的白底去掉
        // （icon.setBackground(null)），结果红包成了整个面板里唯一没有白色方块底的按钮，
        // 而且 48dp 的图标在 52dp 容器里几乎铺满，视觉上比旁边大一圈。
        // 现在统一：所有按钮都用 zimkit_item_input_more 的 52dp 白底 + 12dp 圆角，
        // 红包图标本身在 drawable 里缩到 32dp（见 zimkit_ic_red_packet.xml），口径一致。
    }

    @Override
    public int getItemCount() {
        return itemModels.size();
    }

    public void setItemModels(List<ZIMKitInputButtonModel> itemModels) {
        this.itemModels.clear();
        this.itemModels.addAll(itemModels);
        notifyDataSetChanged();
    }

    public ZIMKitInputButtonModel getItemModel(int position) {
        return itemModels.get(position);
    }

    public static class InputMoreItemViewHolder extends RecyclerView.ViewHolder {

        private final ViewDataBinding mBinding;

        public InputMoreItemViewHolder(ViewDataBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
        }

        public void bind(int id, ZIMKitInputButtonModel model) {
            if (mBinding != null) {
                mBinding.setVariable(id, model);
                mBinding.executePendingBindings();
            }
        }
    }

    public static int dp2px(float v, DisplayMetrics displayMetrics) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, displayMetrics);
    }
}
