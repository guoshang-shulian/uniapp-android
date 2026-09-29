package com.zegocloud.zimkit.components.group.adapter;

import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.zegocloud.zimkit.R;
import com.zegocloud.zimkit.common.glide.ZIMKitGlideLoader;
import com.zegocloud.zimkit.components.group.bean.ZIMKitGroupMemberInfo;
import java.util.ArrayList;
import java.util.List;

public class GroupMemberAdapter extends RecyclerView.Adapter<ViewHolder> {

    private List<ZIMKitGroupMemberInfo> memberList = new ArrayList<>();


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View inflate = View.inflate(parent.getContext(), R.layout.zimkit_item_group_member, null);
        DisplayMetrics displayMetrics = parent.getResources().getDisplayMetrics();
        inflate.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, dp2px(68, displayMetrics)));
        return new ViewHolder(inflate) {
        };
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ZIMKitGroupMemberInfo groupMember = memberList.get(position);
        ImageView memberIcon = holder.itemView.findViewById(R.id.member_icon);
        TextView memberName = holder.itemView.findViewById(R.id.member_name);
        ZIMKitGlideLoader.displayMessageAvatarImage(memberIcon, groupMember.getAvatarUrl());
        if (TextUtils.isEmpty(groupMember.getNickName())) {
            memberName.setText(groupMember.getName());
        } else {
            memberName.setText(groupMember.getNickName());
        }
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    /**
     * 设置成员列表。
     *
     * <p>⚠️ 入参**可能为 null**：{@code ZIMKitCore.getInstance().getGroupMemberList(groupId)} 在本地缓存
     * 还没拉到成员时返回 null（老代码直接 addAll(null) → 崩溃 → 进程重启，用户看到的就是"白屏"）。
     * 历史崩溃栈：
     * <pre>
     * FATAL EXCEPTION: main
     * java.lang.NullPointerException: ArrayList.addAll on a null object reference
     *   at java.util.ArrayList.addAll(ArrayList.java:677)
     *   at GroupMemberAdapter.setMemberList(GroupMemberAdapter.java:55)
     *   at ZIMKitGroupMembersActivity.onCreate(ZIMKitGroupMembersActivity.java:112)
     * </pre>
     * 现在按"空列表"处理，页面先渲染空态，异步名单回来后再 setMemberList 填充。
     */
    public void setMemberList(List<ZIMKitGroupMemberInfo> memberList) {
        this.memberList.clear();
        if (memberList != null) {
            this.memberList.addAll(memberList);
        }
        notifyDataSetChanged();
    }

    public ZIMKitGroupMemberInfo getItemData(int position) {
        return memberList.get(position);
    }

    public static int dp2px(float v, DisplayMetrics displayMetrics) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, displayMetrics);
    }
}
