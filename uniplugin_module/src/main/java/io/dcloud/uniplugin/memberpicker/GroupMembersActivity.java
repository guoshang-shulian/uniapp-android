package io.dcloud.uniplugin.memberpicker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zegocloud.zimkit.services.internal.ZIMKitCore;

import java.util.ArrayList;
import java.util.List;

import io.dcloud.uniplugin.TestModule;
import uni.dcloud.io.uniplugin_module.R;
import im.zego.zim.callback.ZIMGroupMemberInfoQueriedCallback;
import im.zego.zim.callback.ZIMGroupMemberKickedCallback;
import im.zego.zim.callback.ZIMGroupUsersInvitedCallback;
import im.zego.zim.entity.ZIMError;
import im.zego.zim.entity.ZIMErrorUserInfo;
import im.zego.zim.entity.ZIMGroupMemberInfo;
import im.zego.zim.enums.ZIMErrorCode;

/**
 * 微信式群成员宫格页：
 * - 宫格展示成员头像+昵称；
 * - 末尾 "+"（所有人）：从好友列表多选拉人进群（已在群好友自动禁用）；
 * - 末尾 "−"（仅管理员）：多选移除群成员（群主不可选）。
 */
public class GroupMembersActivity extends Activity {

    private String groupId = "";
    private boolean isAdmin = false;

    private MemberPickerDataSource dataSource;
    private GroupMembersAdapter adapter;
    private ProgressBar progressBar;
    private View emptyView;

    public static void start(Context context, String groupId, String title, boolean isAdmin) {
        Intent intent = new Intent(context, GroupMembersActivity.class);
        intent.putExtra("groupId", groupId == null ? "" : groupId);
        intent.putExtra("title", title == null ? "群成员" : title);
        intent.putExtra("isAdmin", isAdmin);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(MpRes.layout(this, "activity_group_members_grid"));

        groupId = getIntent().getStringExtra("groupId");
        if (groupId == null) groupId = "";
        isAdmin = getIntent().getBooleanExtra("isAdmin", false);
        String title = getIntent().getStringExtra("title");
        TextView titleView = findViewById(MpRes.id(this, "gmTitle"));
        titleView.setText(title == null || title.isEmpty() ? "群成员" : title);

        findViewById(MpRes.id(this, "gmBack")).setOnClickListener(v -> finish());
        progressBar = findViewById(MpRes.id(this, "gmProgress"));
        emptyView = findViewById(MpRes.id(this, "gmEmpty"));

        adapter = new GroupMembersAdapter(isAdmin, new GroupMembersAdapter.Callback() {
            @Override
            public void onPlusClicked() {
                showInvitePicker();
            }

            @Override
            public void onMinusClicked() {
                showKickPicker();
            }

            @Override
            public void onMemberClicked(Member member) {
                TestModule.deliverGroupMemberClick(member.memberId);
                finish();
            }
        });

        RecyclerView list = findViewById(MpRes.id(this, "gmList"));
        list.setLayoutManager(new GridLayoutManager(this, 5));
        list.setAdapter(adapter);

        MemberPickerOptions options = new MemberPickerOptions();
        options.dataSource = "GROUP_MEMBERS";
        options.conversationId = groupId;
        options.mode = "single";
        options.readOnly = true;
        dataSource = MemberPickerDataSource.create(options);

        selfRoleQuery();
        loadFirst();
    }

    /** 查询自己在本群的角色：OWNER(1)/ADMIN(2) 才显示“移除” */
    private void selfRoleQuery() {
        String selfZim = "user_" + TestModule.getLocalUserId();
        if (selfZim.equals("user_")) {
            return;
        }
        ZIMKitCore.getInstance().zim().queryGroupMemberInfo(selfZim, groupId,
            new ZIMGroupMemberInfoQueriedCallback() {
                @Override
                public void onGroupMemberInfoQueried(String groupID, ZIMGroupMemberInfo info, ZIMError errorInfo) {
                    if (errorInfo != null && errorInfo.code == ZIMErrorCode.SUCCESS && info != null
                        && (info.memberRole == 1 || info.memberRole == 2)) {
                        runOnUiThread(() -> {
                            isAdmin = true;
                            adapter.setShowMinus(true);
                        });
                    }
                }
            });
    }

    private void loadFirst() {
        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        dataSource.loadFirst(new MemberPickerDataSource.Callback() {
            @Override
            public void onLoaded(List<Member> members, boolean finished) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    adapter.setMembers(members);
                    refreshEmpty(members);
                    if (!finished) {
                        loadMore();
                    }
                });
            }

            @Override
            public void onError(int code, String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    toast(message == null ? "成员加载失败" : message);
                    refreshEmpty(null);
                });
            }
        });
    }

    private void loadMore() {
        dataSource.loadMore(new MemberPickerDataSource.Callback() {
            @Override
            public void onLoaded(List<Member> members, boolean finished) {
                runOnUiThread(() -> {
                    adapter.setMembers(members);
                    refreshEmpty(members);
                    if (!finished) {
                        loadMore();
                    }
                });
            }

            @Override
            public void onError(int code, String message) {
                runOnUiThread(() -> {
                });
            }
        });
    }

    /** 邀请：好友列表多选，已在群好友由 FriendMemberSource 自动禁用 */
    private void showInvitePicker() {
        MemberPickerOptions opts = new MemberPickerOptions();
        opts.title = "拉人进群";
        opts.dataSource = "FRIENDS";
        opts.mode = "multi";
        opts.scene = "invite";
        opts.present = "sheet";
        for (Member m : dataSource.getCache()) {
            opts.excludeIds.add(m.memberId);
        }
        MemberPickerBottomSheet.show(this, opts, (success, canceled, selected) -> {
            if (success && selected != null && !selected.isEmpty()) {
                inviteUsers(selected);
            }
        });
    }

    private void inviteUsers(List<Member> selected) {
        ArrayList<String> ids = new ArrayList<>();
        for (Member m : selected) {
            ids.add(m.zimUserId == null || m.zimUserId.isEmpty() ? "user_" + m.memberId : m.zimUserId);
        }
        ZIMKitCore.getInstance().zim().inviteUsersIntoGroup(ids, groupId,
            new ZIMGroupUsersInvitedCallback() {
                @Override
                public void onGroupUsersInvited(String g, ArrayList<ZIMGroupMemberInfo> userList,
                    ArrayList<ZIMErrorUserInfo> errorUserList, ZIMError errorInfo) {
                    runOnUiThread(() -> {
                        if (errorInfo != null && errorInfo.code == ZIMErrorCode.SUCCESS) {
                            toast("已邀请 " + selected.size() + " 人");
                        } else {
                            toast(errorInfo == null ? "邀请失败" : errorInfo.message);
                        }
                        loadFirst();
                    });
                }
            });
    }

    /** 移除：群成员多选（群主排除），确认后踢人 */
    private void showKickPicker() {
        MemberPickerOptions opts = new MemberPickerOptions();
        opts.title = "选择要移除的成员";
        opts.dataSource = "GROUP_MEMBERS";
        opts.conversationId = groupId;
        opts.mode = "multi";
        opts.scene = "kick";
        opts.present = "sheet";
        opts.excludeRoles.add("OWNER");
        MemberPickerBottomSheet.show(this, opts, (success, canceled, selected) -> {
            if (success && selected != null && !selected.isEmpty()) {
                confirmKick(selected);
            }
        });
    }

    private void confirmKick(final List<Member> selected) {
        new AlertDialog.Builder(this)
            .setTitle("移除成员")
            .setMessage("确定将 " + selected.size() + " 位成员移出群聊？")
            .setPositiveButton("移出", (d, w) -> kickUsers(selected))
            .setNegativeButton("取消", null)
            .show();
    }

    private void kickUsers(List<Member> selected) {
        ArrayList<String> ids = new ArrayList<>();
        for (Member m : selected) {
            ids.add(m.zimUserId == null || m.zimUserId.isEmpty() ? "user_" + m.memberId : m.zimUserId);
        }
        ZIMKitCore.getInstance().zim().kickGroupMembers(ids, groupId, new ZIMGroupMemberKickedCallback() {
            @Override
            public void onGroupMemberKicked(String groupID, ArrayList<String> kickedUserIDs,
                ArrayList<ZIMErrorUserInfo> errorUserList, ZIMError errorInfo) {
                runOnUiThread(() -> {
                    if (errorInfo != null && errorInfo.code == ZIMErrorCode.SUCCESS) {
                        toast("已移出 " + selected.size() + " 人");
                    } else {
                        toast(errorInfo == null ? "移除失败" : errorInfo.message);
                    }
                    loadFirst();
                });
            }
        });
    }

    private void refreshEmpty(List<Member> members) {
        if (members == null || members.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
        }
    }

    private void toast(String msg) {
        Toast.makeText(this, msg == null ? "" : msg, Toast.LENGTH_SHORT).show();
    }
}
