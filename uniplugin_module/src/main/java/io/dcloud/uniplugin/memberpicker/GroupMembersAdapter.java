package io.dcloud.uniplugin.memberpicker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import uni.dcloud.io.uniplugin_module.R;

/** 微信式群成员宫格适配器：成员头像 + 邀请(+) / 移除(−) 占位 */
public class GroupMembersAdapter extends RecyclerView.Adapter<GroupMembersAdapter.VH> {

    public interface Callback {
        void onPlusClicked();

        void onMinusClicked();

        void onMemberClicked(Member member);
    }

    public static final int TYPE_MEMBER = 0;
    public static final int TYPE_PLUS = 1;
    public static final int TYPE_MINUS = 2;

    private final List<Object> items = new ArrayList<>();
    private boolean showMinus;
    private final Callback callback;

    public GroupMembersAdapter(boolean showMinus, Callback callback) {
        this.showMinus = showMinus;
        this.callback = callback;
    }

    public void setMembers(List<Member> members) {
        items.clear();
        if (members != null) {
            items.addAll(members);
        }
        items.add(TYPE_PLUS);
        if (showMinus) {
            items.add(TYPE_MINUS);
        }
        notifyDataSetChanged();
    }

    public void setShowMinus(boolean show) {
        if (showMinus == show) {
            return;
        }
        showMinus = show;
        List<Member> members = getMembers();
        setMembers(members);
    }

    private List<Member> getMembers() {
        List<Member> members = new ArrayList<>();
        for (Object o : items) {
            if (o instanceof Member) {
                members.add((Member) o);
            }
        }
        return members;
    }

    @Override
    public int getItemViewType(int position) {
        Object o = items.get(position);
        if (o instanceof Member) {
            return TYPE_MEMBER;
        }
        return ((Integer) o) == TYPE_MINUS ? TYPE_MINUS : TYPE_PLUS;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
            .inflate(MpRes.layout(parent.getContext(), "item_group_member"), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        int type = getItemViewType(position);
        if (type == TYPE_MEMBER) {
            Member m = (Member) items.get(position);
            h.name.setText(m.displayName());
            if (m.avatarUrl != null && !m.avatarUrl.isEmpty()) {
                Glide.with(h.avatar.getContext()).load(m.avatarUrl).circleCrop().into(h.avatar);
            } else {
                h.avatar.setImageResource(android.R.color.transparent);
            }
            h.itemView.setOnClickListener(v -> {
                if (callback != null) {
                    callback.onMemberClicked(m);
                }
            });
        } else if (type == TYPE_PLUS) {
            h.avatar.setImageResource(MpRes.drawable(h.itemView.getContext(), "ic_group_member_plus"));
            h.name.setText("邀请");
            h.itemView.setOnClickListener(v -> {
                if (callback != null) {
                    callback.onPlusClicked();
                }
            });
        } else {
            h.avatar.setImageResource(MpRes.drawable(h.itemView.getContext(), "ic_group_member_minus"));
            h.name.setText("移除");
            h.itemView.setOnClickListener(v -> {
                if (callback != null) {
                    callback.onMinusClicked();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView name;

        VH(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(MpRes.id(itemView.getContext(), "gmAvatar"));
            name = itemView.findViewById(MpRes.id(itemView.getContext(), "gmName"));
        }
    }
}
