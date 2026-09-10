package io.dcloud.uniplugin.memberpicker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
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
    private View emptyView;
    private TextView pickCount;
    private ProgressBar progressBar;
    private EditText searchInput;
    private LinearLayout indexBar;
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
        try {
            com.zegocloud.zimkit.common.utils.ZimkitStatusBar.setWhite(this);
            setContentView(MpRes.layout(this, "activity_member_picker"));
            options = MemberPickerOptions.fromJson(getIntent().getStringExtra("optionsJson"));
            if (options == null) {
                deliverCancelAndFinish("options null");
                return;
            }
            android.util.Log.d("MemberPicker", "ids mpkTitle=" + MpRes.id(this, "mpkTitle")
                + " mpkConfirm=" + MpRes.id(this, "mpkConfirm")
                + " mpkList=" + MpRes.id(this, "mpkList")
                + " mpkIndex=" + MpRes.id(this, "mpkIndex"));
            initViews();
            android.util.Log.d("MemberPicker", "onCreate ok dataSource=" + options.dataSource
                + " title=" + options.title + " grouping=" + options.grouping);
        } catch (Throwable t) {
            android.util.Log.e("MemberPicker", "onCreate failed", t);
            if (options != null) {
                TestModule.deliverMemberPickResult(options.requestId,
                    "{\"success\":false,\"canceled\":true,\"members\":[],\"message\":\""
                        + (t == null ? "unknown" : t.getMessage()) + "\"}");
            }
            finish();
        }
    }

    private void initViews() {
        if (options.conversationId != null && options.conversationId.isEmpty()
            && "GROUP_MEMBERS".equals(options.dataSource)) {
            Toast.makeText(this, "缺少会话ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView title = findViewById(MpRes.id(this, "mpkTitle"));
        title.setText(options.title == null || options.title.isEmpty() ? "选择成员" : options.title);

        confirmBtn = findViewById(MpRes.id(this, "mpkConfirm"));
        emptyView = findViewById(MpRes.id(this, "mpkEmpty"));
        progressBar = findViewById(MpRes.id(this, "mpkProgress"));
        searchInput = findViewById(MpRes.id(this, "mpkSearch"));
        pickCount = findViewById(MpRes.id(this, "mpkPickCount"));
        indexBar = findViewById(MpRes.id(this, "mpkIndex"));

        findViewById(MpRes.id(this, "mpkBack")).setOnClickListener(v -> cancelAndFinish());
        confirmBtn.setOnClickListener(v -> confirmAndFinish());
        // 已选数 → 点击清空（保留“已在群中”必选）
        pickCount.setOnClickListener(v -> {
            adapter.clearUserSelection();
            Toast.makeText(this, "已清空选择", Toast.LENGTH_SHORT).show();
        });

        dataSource = MemberPickerDataSource.create(options);
        currentList = dataSource.getCache();

        adapter = new MemberPickerAdapter(options.isMulti(), options.isReadOnly(), options.grouping,
            !("KICK".equals(options.action) || "MUTE".equals(options.action)), options.returnOnPick,
            new MemberPickerAdapter.Callback() {
            @Override
            public void onItemClick(Member member) {
                // 只读 + returnOnPick：点成员看资料（原生资料页压在列表之上，返回仍在列表，与微信一致）
                if (options.isReadOnly()) {
                    android.util.Log.i("MemberClick", "picker(page) click gid=" + options.conversationId
                        + " memberId=" + (member == null ? "null" : member.memberId)
                        + " zim=" + (member == null ? "null" : member.zimUserId));
                    if (member != null) {
                        // 传列表里现成的 ZIM userId（最可靠），没有才退回裸 memberId
                        String uid = member.zimUserId == null || member.zimUserId.isEmpty()
                            ? member.memberId : member.zimUserId;
                        TestModule.openMemberProfileWith(MemberPickerActivity.this,
                            options.conversationId, uid);
                    }
                    return;
                }
                deliverSuccess(member);
                finish();
            }

            @Override
            public void onSelectionChanged(List<Member> selected) {
                updateCount(selected);
            }
        });

        RecyclerView list = findViewById(MpRes.id(this, "mpkList"));
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
            pickCount.setVisibility(View.VISIBLE);
            adapter.setSelectedIds(new java.util.LinkedHashSet<>(options.defaultSelectedIds));
        }

        if (options.isReadOnly()) {
            confirmBtn.setVisibility(View.GONE);
            searchInput.setVisibility(options.searchable ? View.VISIBLE : View.GONE);
        }

        if (options.grouping) {
            buildIndexBar();
        } else {
            indexBar.setVisibility(View.GONE);
        }

        if (options.searchable) {
            searchInput.setHint(options.dataSource.equals("FRIENDS") ? "搜索好友昵称/备注/手机号" : "搜索成员昵称");
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

    private void buildIndexBar() {
        indexBar.removeAllViews();
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#";
        for (int i = 0; i < letters.length(); i++) {
            final String letter = String.valueOf(letters.charAt(i));
            TextView tv = new TextView(this);
            tv.setText(letter);
            tv.setTextSize(10);
            tv.setTextColor(0xFF646A73);
            tv.setGravity(Gravity.CENTER);
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            tv.setOnClickListener(v -> jumpToInitial(letter));
            indexBar.addView(tv);
        }
    }

    private void jumpToInitial(String letter) {
        int pos = adapter.positionOfInitial(letter);
        if (pos < 0) {
            return;
        }
        RecyclerView list = findViewById(MpRes.id(this, "mpkList"));
        LinearLayoutManager lm = (LinearLayoutManager) list.getLayoutManager();
        if (lm != null) {
            lm.scrollToPositionWithOffset(pos, 0);
        }
    }

    private void updateCount(List<Member> selected) {
        if (pickCount != null) {
            pickCount.setText("已选 " + (selected == null ? 0 : selected.size()) + " 人");
        }
    }

    private void loadFirst() {
        loading = true;
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        dataSource.loadFirst(new MemberPickerDataSource.Callback() {
            @Override
            public void onLoaded(List<Member> members, boolean finished) {
                android.util.Log.d("MemberPicker", "loadFirst loaded=" + (members == null ? 0 : members.size())
                    + " finished=" + finished);
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
                android.util.Log.d("MemberPicker", "loadFirst error code=" + code + " msg=" + message);
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
        if (keyword == null || keyword.trim().isEmpty()) {
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
        result.put("action", options.action == null ? "" : options.action);
        TestModule.deliverMemberPickResult(options.requestId, result.toJSONString());
    }

    private void deliverCancelAndFinish(@SuppressWarnings("unused") String reason) {
        if (options != null) {
            cancelAndFinish();
        } else {
            finish();
        }
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
