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

/** 成员选择器列表适配器（整页/弹窗共用；支持首字母分组头 + 已选/禁用态；分页后勾选保持） */
public class MemberPickerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_MEMBER = 1;

    public interface Callback {
        void onItemClick(Member member);

        void onSelectionChanged(List<Member> selected);
    }

    /** 展示条目：String=分组头，Member=成员 */
    private final List<Object> items = new ArrayList<>();
    private final Set<String> selectedIds = new LinkedHashSet<>();
    private final boolean multi;
    private final boolean readOnly;
    private final boolean grouping;
    private final boolean preselectDisabled;
    private final Callback callback;

    public MemberPickerAdapter(boolean multi, boolean readOnly, boolean grouping,
        boolean preselectDisabled, Callback callback) {
        this.multi = multi;
        this.readOnly = readOnly;
        this.grouping = grouping;
        this.preselectDisabled = preselectDisabled;
        this.callback = callback;
    }

    public void setItems(List<Member> list) {
        items.clear();
        if (list != null) {
            String last = null;
            for (Member m : list) {
                if (m == null) {
                    continue;
                }
                if (grouping) {
                    if (last == null || !last.equals(m.initial)) {
                        items.add(m.initial);
                        last = m.initial;
                    }
                }
                if (preselectDisabled && m.disabled) {
                    selectedIds.add(m.memberId);
                }
                items.add(m);
            }
        }
        notifyDataSetChanged();
        fireSelectionChanged();
    }

    public void addItems(List<Member> list) {
        if (list == null) {
            return;
        }
        int start = items.size();
        String last = groupOf(items.size() - 1);
        for (Member m : list) {
            if (m == null) {
                continue;
            }
            if (grouping && (last == null || !last.equals(m.initial))) {
                items.add(m.initial);
                last = m.initial;
            }
            if (preselectDisabled && m.disabled) {
                selectedIds.add(m.memberId);
            }
            items.add(m);
        }
        notifyItemRangeInserted(start, items.size() - start);
        fireSelectionChanged();
    }

    private String groupOf(int pos) {
        for (int i = pos; i >= 0; i--) {
            Object o = items.get(i);
            if (o instanceof String) {
                return (String) o;
            }
        }
        return null;
    }

    /** 兼容旧调用 */
    public void setItemsSimple(List<Member> list) {
        setItems(list);
    }

    public void setSelectedIds(Set<String> ids) {
        selectedIds.clear();
        if (ids != null) {
            selectedIds.addAll(ids);
        }
        notifyDataSetChanged();
        fireSelectionChanged();
    }

    /** 清空用户选择（保留“已在群中/禁用”的必选） */
    public void clearUserSelection() {
        List<String> keep = new ArrayList<>();
        if (preselectDisabled) {
            for (Object o : items) {
                if (o instanceof Member) {
                    Member m = (Member) o;
                    if (m.disabled) {
                        keep.add(m.memberId);
                    }
                }
            }
        }
        selectedIds.clear();
        selectedIds.addAll(keep);
        notifyDataSetChanged();
        fireSelectionChanged();
    }

    public Set<String> getSelectedIds() {
        return new LinkedHashSet<>(selectedIds);
    }

    public List<Member> getSelectedMembers() {
        List<Member> result = new ArrayList<>();
        for (Object o : items) {
            if (o instanceof Member) {
                Member m = (Member) o;
                if (selectedIds.contains(m.memberId)) {
                    result.add(m);
                }
            }
        }
        return result;
    }

    public List<Member> getItems() {
        List<Member> result = new ArrayList<>();
        for (Object o : items) {
            if (o instanceof Member) {
                result.add((Member) o);
            }
        }
        return result;
    }

    /** 分组头首字母 → 列表位置（索引条跳转用） */
    public int positionOfInitial(String letter) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof String && letter.equals(items.get(i))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_MEMBER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(MpRes.layout(parent.getContext(), "item_member_picker_header"), parent, false);
            return new HeaderVH(v);
        }
        View v = LayoutInflater.from(parent.getContext())
            .inflate(MpRes.layout(parent.getContext(), "item_member_picker"), parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object o = items.get(position);
        if (holder instanceof HeaderVH) {
            ((HeaderVH) holder).text.setText((String) o);
            return;
        }
        Member m = (Member) o;
        VH h = (VH) holder;
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

        if (!readOnly) {
            h.checkbox.setVisibility(View.VISIBLE);
            h.checkbox.setImageResource(selectedIds.contains(m.memberId)
                ? MpRes.drawable(h.itemView.getContext(), "ic_mp_checked") : MpRes.drawable(h.itemView.getContext(), "ic_mp_unchecked"));
            h.checkbox.setAlpha(m.disabled ? 0.45f : 1f);
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

    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView text;

        HeaderVH(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(MpRes.id(itemView.getContext(), "mpHeader"));
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
            avatar = itemView.findViewById(MpRes.id(itemView.getContext(), "mpAvatar"));
            name = itemView.findViewById(MpRes.id(itemView.getContext(), "mpName"));
            remark = itemView.findViewById(MpRes.id(itemView.getContext(), "mpRemark"));
            role = itemView.findViewById(MpRes.id(itemView.getContext(), "mpRole"));
            disabled = itemView.findViewById(MpRes.id(itemView.getContext(), "mpDisabled"));
            checkbox = itemView.findViewById(MpRes.id(itemView.getContext(), "mpCheckbox"));
        }
    }
}
