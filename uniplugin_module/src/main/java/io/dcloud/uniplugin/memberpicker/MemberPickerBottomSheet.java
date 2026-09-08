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

/** 成员选择器底部弹窗壳（发红包选人/原生内轻量使用） */
public class MemberPickerBottomSheet {

    public interface ResultListener {
        void onResult(boolean success, boolean canceled, List<Member> members);
    }

    public static void show(final Activity activity, final MemberPickerOptions options, final ResultListener listener) {
        final Dialog dialog = new Dialog(activity, R.style.BottomSheetDialogTheme);
        dialog.setContentView(R.layout.dialog_member_picker);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView title = dialog.findViewById(R.id.mpSheetTitle);
        title.setText(options.title == null || options.title.isEmpty() ? "选择成员" : options.title);
        EditText search = dialog.findViewById(R.id.mpSheetSearch);
        TextView confirm = dialog.findViewById(R.id.mpSheetConfirm);

        final MemberPickerDataSource dataSource = MemberPickerDataSource.create(options);
        final List<Member> current = new ArrayList<>();
        final MemberPickerAdapter adapter = new MemberPickerAdapter(options.isMulti(), options.isReadOnly(),
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
                        confirm.setText(selected.isEmpty() ? "确定" : "确定 (" + selected.size() + ")");
                    }
                }
            });

        RecyclerView list = dialog.findViewById(R.id.mpSheetList);
        list.setLayoutManager(new LinearLayoutManager(activity));
        list.setAdapter(adapter);

        if (options.isMulti() && !options.isReadOnly()) {
            confirm.setVisibility(View.VISIBLE);
            adapter.setSelectedIds(new java.util.LinkedHashSet<>(options.defaultSelectedIds));
        }

        dialog.findViewById(R.id.mpSheetClose).setOnClickListener(v -> {
            listener.onResult(false, true, new ArrayList<Member>());
            dialog.dismiss();
        });
        dialog.findViewById(R.id.mpSheetCancel).setOnClickListener(v -> {
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
