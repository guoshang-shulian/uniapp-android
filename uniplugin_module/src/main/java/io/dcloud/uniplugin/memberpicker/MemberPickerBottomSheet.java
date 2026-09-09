package io.dcloud.uniplugin.memberpicker;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import uni.dcloud.io.uniplugin_module.R;

/** 成员选择器底部弹窗壳（半屏；含搜索框，保留分组头，无字母索引条） */
public class MemberPickerBottomSheet {

    public interface ResultListener {
        void onResult(boolean success, boolean canceled, List<Member> members);
    }

    public static void show(final Activity activity, final MemberPickerOptions options, final ResultListener listener) {
        final Dialog dialog = new Dialog(activity, MpRes.style(activity, "BottomSheetDialogTheme"));
        dialog.setContentView(MpRes.layout(activity, "dialog_member_picker"));
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.4f);
        }
        // 面板固定 2/3 屏（窗口全屏，遮罩/liquid 覆盖到底部）
        View panel = dialog.findViewById(MpRes.id(activity, "mpSheetPanel"));
        if (panel != null) {
            int height = (int) (activity.getResources().getDisplayMetrics().heightPixels * 2f / 3f);
            ViewGroup.LayoutParams lp = panel.getLayoutParams();
            lp.height = height;
            panel.setLayoutParams(lp);
        }
        View mask = dialog.findViewById(MpRes.id(activity, "mpSheetMask"));
        if (mask != null) {
            mask.setOnClickListener(v -> {
                listener.onResult(false, true, new ArrayList<Member>());
                dialog.dismiss();
            });
        }

        TextView title = dialog.findViewById(MpRes.id(activity, "mpSheetTitle"));
        title.setText(options.title == null || options.title.isEmpty() ? "选择成员" : options.title);
        EditText search = dialog.findViewById(MpRes.id(activity, "mpSheetSearch"));
        final TextView confirm = dialog.findViewById(MpRes.id(activity, "mpSheetConfirm"));

        MemberPickerDataSource dataSource = MemberPickerDataSource.create(options);
        final List<Member> current = new ArrayList<>();
        final MemberPickerAdapter adapter = new MemberPickerAdapter(options.isMulti(), options.isReadOnly(),
            options.grouping, !("KICK".equals(options.action) || "MUTE".equals(options.action)),
            new MemberPickerAdapter.Callback() {
            @Override
            public void onItemClick(Member member) {
                List<Member> result = new ArrayList<>();
                result.add(member);
                listener.onResult(true, false, result);
                dialog.dismiss();
            }

            @Override
            public void onSelectionChanged(List<Member> selected) {
                if (options.isMulti() && !options.isReadOnly()) {
                    String label = options.confirmText == null || options.confirmText.isEmpty()
                        ? "确定" : options.confirmText;
                    confirm.setText(selected == null || selected.isEmpty()
                        ? label : label + " (" + selected.size() + ")");
                }
            }
        });

        RecyclerView list = dialog.findViewById(MpRes.id(activity, "mpSheetList"));
        list.setLayoutManager(new LinearLayoutManager(activity));
        list.setAdapter(adapter);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm != null && lm.findLastVisibleItemPosition() >= adapter.getItemCount() - 3
                    && dy > 0 && !dataSource.isFinished()) {
                    dataSource.loadMore(new MemberPickerDataSource.Callback() {
                        @Override
                        public void onLoaded(List<Member> members, boolean finished) {
                            new Handler(Looper.getMainLooper()).post(() -> adapter.setItems(members));
                        }

                        @Override
                        public void onError(int code, String message) {
                        }
                    });
                }
            }
        });

        if (options.isMulti() && !options.isReadOnly()) {
            confirm.setVisibility(View.VISIBLE);
            confirm.setText(options.confirmText == null || options.confirmText.isEmpty()
                ? "确定" : options.confirmText);
            adapter.setSelectedIds(new java.util.LinkedHashSet<>(options.defaultSelectedIds));
        }

        dialog.findViewById(MpRes.id(activity, "mpSheetClose")).setOnClickListener(v -> {
            listener.onResult(false, true, new ArrayList<Member>());
            dialog.dismiss();
        });
        dialog.findViewById(MpRes.id(activity, "mpSheetCancel")).setOnClickListener(v -> {
            listener.onResult(false, true, new ArrayList<Member>());
            dialog.dismiss();
        });
        confirm.setOnClickListener(v -> {
            List<Member> selected = adapter.getSelectedMembers();
            if (selected.isEmpty()) {
                Toast.makeText(activity, "请选择成员", Toast.LENGTH_SHORT).show();
                return;
            }
            listener.onResult(true, false, selected);
            dialog.dismiss();
        });

        if (options.searchable) {
            search.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String key = String.valueOf(s);
                    if (key.trim().isEmpty()) {
                        current.clear();
                        current.addAll(dataSource.getCache());
                        adapter.setItems(current);
                        return;
                    }
                    dataSource.search(key, new MemberPickerDataSource.Callback() {
                        @Override
                        public void onLoaded(List<Member> members, boolean finished) {
                            final List<Member> copy = new ArrayList<>(members);
                            new Handler(Looper.getMainLooper()).post(() -> {
                                current.clear();
                                current.addAll(copy);
                                adapter.setItems(copy);
                            });
                        }

                        @Override
                        public void onError(int code, String message) {
                        }
                    });
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {
                }
            });
        } else {
            search.setVisibility(View.GONE);
        }

        dataSource.loadFirst(new MemberPickerDataSource.Callback() {
            @Override
            public void onLoaded(List<Member> members, boolean finished) {
                final List<Member> copy = new ArrayList<>(members);
                new Handler(Looper.getMainLooper()).post(() -> {
                    current.clear();
                    current.addAll(copy);
                    adapter.setItems(copy);
                });
            }

            @Override
            public void onError(int code, String message) {
                new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(activity, message == null ? "加载失败" : message, Toast.LENGTH_SHORT).show());
            }
        });

        dialog.show();
    }
}
