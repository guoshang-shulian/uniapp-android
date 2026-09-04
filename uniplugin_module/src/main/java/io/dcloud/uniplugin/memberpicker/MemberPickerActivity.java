package io.dcloud.uniplugin.memberpicker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

import uni.dcloud.io.uniplugin_module.R;
import io.dcloud.uniplugin.TestModule;

/** 成员选择器整页壳（uniapp 调用默认走这里，原生也可复用） */
public class MemberPickerActivity extends android.app.Activity {

    private MemberPickerOptions options;
    private MemberPickerDataSource dataSource;
    private MemberPickerAdapter adapter;
    private List<Member> currentList;
    private TextView confirmBtn;
    private TextView emptyView;
    private ProgressBar progressBar;
    private EditText searchInput;
    private boolean loading = false;

    public static void start(Context context, String optionsJson) {
        Intent intent = new Intent(context, MemberPickerActivity.class);
        intent.putExtra("optionsJson", optionsJson == null ? "" : optionsJson);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_picker);

        options = MemberPickerOptions.fromJson(getIntent().getStringExtra("optionsJson"));
        if (options.conversationId != null && options.conversationId.isEmpty()
            && "GROUP_MEMBERS".equals(options.dataSource)) {
            Toast.makeText(this, "缺少会话ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView title = findViewById(R.id.mpTitle);
        title.setText(options.title == null || options.title.isEmpty() ? "选择成员" : options.title);

        confirmBtn = findViewById(R.id.mpConfirm);
        emptyView = findViewById(R.id.mpEmpty);
        progressBar = findViewById(R.id.mpProgress);
        searchInput = findViewById(R.id.mpSearch);

        findViewById(R.id.mpBack).setOnClickListener(v -> cancelAndFinish());
        confirmBtn.setOnClickListener(v -> confirmAndFinish());

        dataSource = MemberPickerDataSource.create(options);
        currentList = dataSource.getCache();

        adapter = new MemberPickerAdapter(options.isMulti(), options.isReadOnly(), new MemberPickerAdapter.Callback() {
            @Override
            public void onItemClick(Member member) {
                deliverSuccess(member);
                finish();
            }

            @Override
            public void onSelectionChanged(List<Member> selected) {
                if (options.isMulti() && !options.isReadOnly()) {
                    confirmBtn.setText(selected.isEmpty() ? "确定" : "确定 (" + selected.size() + ")");
                }
            }
        });

        RecyclerView list = findViewById(R.id.mpList);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm != null && lm.findLastVisibleItemPosition() >= adapter.getItemCount() - 3
                    && dy > 0 && !loading) {
                    loadMore();
                }
            }
        });

        if (options.isMulti() && !options.isReadOnly()) {
            confirmBtn.setVisibility(View.VISIBLE);
            adapter.setSelectedIds(new java.util.LinkedHashSet<>(options.defaultSelectedIds));
        }

        if (!options.isReadOnly() && !options.isMulti()) {
            confirmBtn.setVisibility(View.GONE);
        }
        if (options.isReadOnly()) {
            confirmBtn.setVisibility(View.GONE);
            searchInput.setVisibility(options.searchable ? View.VISIBLE : View.GONE);
        }

        if (options.searchable) {
            searchInput.setHint(options.dataSource.equals("FRIENDS") ? "搜索好友昵称/备注" : "搜索成员昵称");
            searchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    search(String.valueOf(s));
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        } else {
            searchInput.setVisibility(View.GONE);
        }

        loadFirst();
    }

    private void loadFirst() {
        loading = true;
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        dataSource.loadFirst(new MemberPickerDataSource.Callback() {
            @Override
            public void onLoaded(List<Member> members, boolean finished) {
                runOnUiThread(() -> {
                    loading = false;
                    progressBar.setVisibility(View.GONE);
                    currentList = members;
                    adapter.setItems(members);
                    refreshEmpty(members);
                });
            }

            @Override
            public void onError(int code, String message) {
                runOnUiThread(() -> {
                    loading = false;
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MemberPickerActivity.this,
                        message == null ? "加载失败" : message, Toast.LENGTH_SHORT).show();
                    refreshEmpty(null);
                });
            }
        });
    }

    private void loadMore() {
        if (loading || dataSource.isFinished()) {
            return;
        }
        loading = true;
        dataSource.loadMore(new MemberPickerDataSource.Callback() {
            @Override
            public void onLoaded(List<Member> members, boolean finished) {
                runOnUiThread(() -> {
                    loading = false;
                    currentList = members;
                    adapter.setItems(members);
                    refreshEmpty(members);
                });
            }

            @Override
            public void onError(int code, String message) {
                runOnUiThread(() -> loading = false);
            }
        });
    }

    private void search(String keyword) {
        if (/* empty keyword */ keyword == null || keyword.trim().isEmpty()) {
            currentList = dataSource.getCache();
            adapter.setItems(currentList);
            refreshEmpty(currentList);
            return;
        }
        dataSource.search(keyword, new MemberPickerDataSource.Callback() {
            @Override
            public void onLoaded(List<Member> members, boolean finished) {
                runOnUiThread(() -> {
                    currentList = members;
                    adapter.setItems(members);
                    refreshEmpty(members);
                });
            }

            @Override
            public void onError(int code, String message) {
                runOnUiThread(() -> {
                });
            }
        });
    }

    private void refreshEmpty(List<Member> list) {
        if (list == null || list.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }

    private void confirmAndFinish() {
        List<Member> selected = adapter.getSelectedMembers();
        if (selected.isEmpty()) {
            Toast.makeText(this, "请选择成员", Toast.LENGTH_SHORT).show();
            return;
        }
        deliverSuccess(selected);
        finish();
    }

    private void deliverSuccess(Member member) {
        java.util.ArrayList<Member> list = new java.util.ArrayList<>();
        list.add(member);
        deliverSuccess(list);
    }

    private void deliverSuccess(List<Member> members) {
        JSONObject result = new JSONObject();
        result.put("requestId", options.requestId);
        result.put("success", true);
        result.put("canceled", false);
        JSONArray arr = new JSONArray();
        if (members != null) {
            for (Member m : members) {
                arr.add(m.toJson());
            }
        }
        result.put("members", arr);
        TestModule.deliverMemberPickResult(options.requestId, result.toJSONString());
    }

    private void cancelAndFinish() {
        JSONObject result = new JSONObject();
        result.put("requestId", options.requestId);
        result.put("success", false);
        result.put("canceled", true);
        result.put("members", new JSONArray());
        TestModule.deliverMemberPickResult(options.requestId, result.toJSONString());
        finish();
    }

    @Override
    public void onBackPressed() {
        cancelAndFinish();
        super.onBackPressed();
    }
}
