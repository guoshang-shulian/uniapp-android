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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import uni.dcloud.io.uniplugin_module.R;

/** 成员选择器列表适配器（整页/弹窗共用） */
public class MemberPickerAdapter extends RecyclerView.Adapter<MemberPickerAdapter.VH> {

    public interface Callback {
        void onItemClick(Member member);

        void onSelectionChanged(List<Member> selected);
    }

    private final List<Member> items = new ArrayList<>();
    private final Set<String> selectedIds = new LinkedHashSet<>();
    private final boolean multi;
    private final boolean readOnly;
    private final Callback callback;

    public MemberPickerAdapter(boolean multi, boolean readOnly, Callback callback) {
        this.multi = multi;
        this.readOnly = readOnly;
        this.callback = callback;
    }

    public void setItems(List<Member> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void addItems(List<Member> list) {
        if (list == null) {
            return;
        }
        int start = items.size();
        items.addAll(list);
        notifyItemRangeInserted(start, list.size());
    }

    public void setSelectedIds(Set<String> ids) {
        selectedIds.clear();
        if (ids != null) {
            selectedIds.addAll(ids);
        }
        notifyDataSetChanged();
        fireSelectionChanged();
    }

    public Set<String> getSelectedIds() {
        return new LinkedHashSet<>(selectedIds);
    }

    public List<Member> getSelectedMembers() {
        List<Member> result = new ArrayList<>();
        for (Member m : items) {
            if (selectedIds.contains(m.memberId)) {
                result.add(m);
            }
        }
        return result;
    }

    public List<Member> getItems() {
        return new ArrayList<>(items);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_member_picker, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Member m = items.get(position);
        h.name.setText(m.displayName());
        h.name.setAlpha(m.disabled ? 0.4f : 1f);

        if (m.remark != null && !m.remark.isEmpty()) {
            h.remark.setVisibility(View.VISIBLE);
            h.remark.setText("昵称：" + (m.userName == null ? "" : m.userName));
        } else {
            h.remark.setVisibility(View.GONE);
        }

        if ("OWNER".equals(m.groupRole) || "ADMIN".equals(m.groupRole)) {
            h.role.setVisibility(View.VISIBLE);
            h.role.setText("OWNER".equals(m.groupRole) ? "群主" : "管理员");
        } else {
            h.role.setVisibility(View.GONE);
        }

        if (m.disabled) {
            h.disabled.setVisibility(View.VISIBLE);
            h.disabled.setText(m.disabledReason == null || m.disabledReason.isEmpty()
                ? "不可选择" : m.disabledReason);
        } else {
            h.disabled.setVisibility(View.GONE);
        }

        if (multi && !readOnly) {
            h.checkbox.setVisibility(View.VISIBLE);
            h.checkbox.setImageResource(selectedIds.contains(m.memberId)
                ? R.drawable.ic_mp_checked : R.drawable.ic_mp_unchecked);
            h.checkbox.setAlpha(m.disabled ? 0.4f : 1f);
        } else {
            h.checkbox.setVisibility(View.GONE);
        }

        if (m.avatarUrl != null && !m.avatarUrl.isEmpty()) {
            Glide.with(h.avatar.getContext()).load(m.avatarUrl).circleCrop().into(h.avatar);
        } else {
            h.avatar.setImageResource(android.R.color.transparent);
        }

        h.itemView.setAlpha(m.disabled ? 0.6f : 1f);
        h.itemView.setOnClickListener(v -> {
            if (m.disabled || readOnly) {
                return;
            }
            if (!multi) {
                if (callback != null) {
                    callback.onItemClick(m);
                }
                return;
            }
            if (selectedIds.contains(m.memberId)) {
                selectedIds.remove(m.memberId);
            } else {
                selectedIds.add(m.memberId);
            }
            notifyItemChanged(position);
            fireSelectionChanged();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void fireSelectionChanged() {
        if (callback != null) {
            callback.onSelectionChanged(getSelectedMembers());
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView avatar;
        TextView name;
        TextView remark;
        TextView role;
        TextView disabled;
        ImageView checkbox;

        VH(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.mpAvatar);
            name = itemView.findViewById(R.id.mpName);
            remark = itemView.findViewById(R.id.mpRemark);
            role = itemView.findViewById(R.id.mpRole);
            disabled = itemView.findViewById(R.id.mpDisabled);
            checkbox = itemView.findViewById(R.id.mpCheckbox);
        }
    }
}
