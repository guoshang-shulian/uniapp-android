package io.dcloud.uniplugin.memberpicker;

import com.zegocloud.zimkit.services.internal.ZIMKitCore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import im.zego.zim.callback.ZIMGroupMemberListQueriedCallback;
import im.zego.zim.callback.ZIMGroupUsersInvitedCallback;
import im.zego.zim.entity.ZIMError;
import im.zego.zim.entity.ZIMFriendInfo;
import im.zego.zim.entity.ZIMFriendListQueryConfig;
import im.zego.zim.entity.ZIMFriendSearchConfig;
import im.zego.zim.entity.ZIMGroupMemberInfo;
import im.zego.zim.entity.ZIMGroupMemberQueryConfig;
import im.zego.zim.enums.ZIMErrorCode;

/**
 * 成员数据源（统一抽象）。
 * - GROUP_MEMBERS：ZIM 群成员（分页；角色+加入时间排序；不分组）
 * - FRIENDS：ZIM 好友列表（分页；拼音排序+首字母分组；搜索支持昵称/备注/拼音；
 *   excludeGroupId 场景：自动查询群成员 → “已在群中”= 已选+禁用，避免重复拉人）
 */
public abstract class MemberPickerDataSource {

    public interface Callback {
        void onLoaded(List<Member> members, boolean finished);
        void onError(int code, String message);
    }

    protected final MemberPickerOptions options;
    protected final List<Member> cache = new ArrayList<>();
    protected boolean finished = false;
    protected boolean loading = false;

    public MemberPickerDataSource(MemberPickerOptions options) {
        this.options = options;
    }

    public List<Member> getCache() {
        return cache;
    }

    public boolean isFinished() {
        return finished;
    }

    public abstract void loadFirst(Callback cb);

    public abstract void loadMore(Callback cb);

    public abstract void search(String keyword, Callback cb);

    protected boolean isExcluded(Member m) {
        if (options.excludeIds.contains(m.memberId)) {
            m.disabled = true;
            if (m.disabledReason == null || m.disabledReason.isEmpty()) {
                m.disabledReason = "不可选择";
            }
            return true;
        }
        if (options.excludeRoles.contains(m.groupRole)) {
            m.disabled = true;
            if (m.disabledReason == null || m.disabledReason.isEmpty()) {
                m.disabledReason = "该角色不可选择";
            }
            return true;
        }
        return false;
    }

    public static MemberPickerDataSource create(MemberPickerOptions options) {
        if ("FRIENDS".equals(options.dataSource)) {
            return new FriendMemberSource(options);
        }
        return new GroupMemberSource(options);
    }

    protected static void fillPinyin(Member m) {
        String name = m.displayName();
        m.pinyin = PinyinUtil.fullPinyin(name);
        m.initial = PinyinUtil.initial(name);
    }

    protected List<Member> copy(List<Member> src) {
        return new ArrayList<>(src);
    }

    /** ------------------ 群成员 ------------------ */
    static class GroupMemberSource extends MemberPickerDataSource {

        private int nextFlag = 0;

        GroupMemberSource(MemberPickerOptions options) {
            super(options);
        }

        @Override
        public void loadFirst(Callback cb) {
            nextFlag = 0;
            query(cb);
        }

        @Override
        public void loadMore(Callback cb) {
            query(cb);
        }

        private void query(Callback cb) {
            if (loading || finished) {
                return;
            }
            loading = true;
            ZIMGroupMemberQueryConfig config = new ZIMGroupMemberQueryConfig();
            config.count = 100;
            config.nextFlag = nextFlag;
            ZIMKitCore.getInstance().zim().queryGroupMemberList(options.conversationId, config,
                new ZIMGroupMemberListQueriedCallback() {
                    @Override
                    public void onGroupMemberListQueried(String groupId,
                        ArrayList<ZIMGroupMemberInfo> memberList, int flag, ZIMError errorInfo) {
                        loading = false;
                        if (errorInfo == null || errorInfo.code != ZIMErrorCode.SUCCESS) {
                            if (cb != null) {
                                cb.onError(-1, errorInfo == null ? "群成员查询失败" : errorInfo.message);
                            }
                            return;
                        }
                        nextFlag = flag;
                        if (nextFlag == 0) {
                            finished = true;
                        }
                        Set<String> seen = new HashSet<>();
                        for (Member m : cache) {
                            seen.add(m.memberId);
                        }
                        if (memberList != null) {
                            for (ZIMGroupMemberInfo info : memberList) {
                                Member m = new Member();
                                m.zimUserId = info.userID == null ? "" : info.userID;
                                m.memberId = Member.stripZimPrefix(info.userID);
                                String nick = info.memberNickname;
                                if (nick == null || nick.isEmpty()) {
                                    nick = info.userName;
                                }
                                m.userName = nick == null ? "" : nick;
                                m.avatarUrl = info.memberAvatarUrl == null ? "" : info.memberAvatarUrl;
                                m.remark = "";
                                if (info.memberRole == 1) {
                                    m.groupRole = "OWNER";
                                } else if (info.memberRole == 2) {
                                    m.groupRole = "ADMIN";
                                } else {
                                    m.groupRole = "MEMBER";
                                }
                                m.isMuted = info.muteExpiredTime > 0;
                                fillPinyin(m);
                                isExcluded(m);
                                if (!seen.contains(m.memberId)) {
                                    seen.add(m.memberId);
                                    cache.add(m);
                                }
                            }
                        }
                        sortMembers(cache);
                        if (cb != null) {
                            cb.onLoaded(copy(cache), finished);
                        }
                    }
                });
        }

        @Override
        public void search(String keyword, Callback cb) {
            String key = keyword == null ? "" : keyword.trim();
            List<Member> result = new ArrayList<>();
            for (Member m : cache) {
                if (key.isEmpty() || contains(m.displayName(), key) || contains(m.userName, key)
                    || contains(m.pinyin, key)) {
                    result.add(m);
                }
            }
            if (cb != null) {
                cb.onLoaded(result, true);
            }
        }

        private boolean contains(String s, String key) {
            return s != null && !s.isEmpty() && s.toLowerCase().contains(key.toLowerCase());
        }
    }

    /** ------------------ 聊天好友 ------------------ */
    static class FriendMemberSource extends MemberPickerDataSource {

        private int nextFlag = 0;
        private final Set<String> inGroupIds = new HashSet<>();
        private boolean inGroupPrepared = false;

        FriendMemberSource(MemberPickerOptions options) {
            super(options);
        }

        @Override
        public void loadFirst(Callback cb) {
            nextFlag = 0;
            prepareInGroup(cb);
        }

        @Override
        public void loadMore(Callback cb) {
            query(cb);
        }

        private void prepareInGroup(final Callback cb) {
            if (options.excludeGroupId == null || options.excludeGroupId.isEmpty()) {
                inGroupPrepared = true;
                query(cb);
                return;
            }
            // 拉人场景：先查该群现有成员 → 已在群好友将被“已选+禁用”
            final ArrayList<ZIMGroupMemberInfo> all = new ArrayList<>();
            fetchGroupMembers(options.excludeGroupId, 0, all, () -> {
                inGroupIds.clear();
                for (ZIMGroupMemberInfo info : all) {
                    if (info != null && info.userID != null) {
                        inGroupIds.add(Member.stripZimPrefix(info.userID));
                    }
                }
                inGroupPrepared = true;
                query(cb);
            });
        }

        private void fetchGroupMembers(String groupId, int nextFlag,
            ArrayList<ZIMGroupMemberInfo> all, Runnable done) {
            ZIMGroupMemberQueryConfig config = new ZIMGroupMemberQueryConfig();
            config.count = 100;
            config.nextFlag = nextFlag;
            ZIMKitCore.getInstance().zim().queryGroupMemberList(groupId, config,
                new ZIMGroupMemberListQueriedCallback() {
                    @Override
                    public void onGroupMemberListQueried(String gid,
                        ArrayList<ZIMGroupMemberInfo> memberList, int flag, ZIMError errorInfo) {
                        if (memberList != null) {
                            all.addAll(memberList);
                        }
                        if (flag != 0 && all.size() < 500) {
                            fetchGroupMembers(groupId, flag, all, done);
                        } else {
                            done.run();
                        }
                    }
                });
        }

        private void query(Callback cb) {
            if (loading || finished) {
                return;
            }
            loading = true;
            ZIMFriendListQueryConfig config = new ZIMFriendListQueryConfig();
            config.count = 100;
            ZIMKitCore.getInstance().zim().queryFriendList(config,
                (friendList, flag, errorInfo) -> {
                    loading = false;
                    if (errorInfo == null || errorInfo.code != ZIMErrorCode.SUCCESS) {
                        if (cb != null) {
                            cb.onError(-1, errorInfo == null ? "好友查询失败" : errorInfo.message);
                        }
                        return;
                    }
                    nextFlag = flag;
                    if (nextFlag == 0) {
                        finished = true;
                    }
                    Set<String> seen = new HashSet<>();
                    for (Member m : cache) {
                        seen.add(m.memberId);
                    }
                    if (friendList != null) {
                        for (ZIMFriendInfo info : friendList) {
                            Member m = new Member();
                            m.zimUserId = info.userID == null ? "" : info.userID;
                            m.memberId = Member.stripZimPrefix(info.userID);
                            m.userName = info.userName == null ? "" : info.userName;
                            m.avatarUrl = info.userAvatarUrl == null ? "" : info.userAvatarUrl;
                            m.remark = info.friendAlias == null ? "" : info.friendAlias;
                            m.groupRole = "";
                            fillPinyin(m);
                            if (inGroupIds.contains(m.memberId)) {
                                // 已在群中：已选 + 禁用（不可取消）
                                m.disabled = true;
                                m.disabledReason = "已在群中";
                            } else {
                                isExcluded(m);
                            }
                            if (!seen.contains(m.memberId)) {
                                seen.add(m.memberId);
                                cache.add(m);
                            }
                        }
                    }
                    sortMembers(cache);
                    if (cb != null) {
                        cb.onLoaded(copy(cache), finished);
                    }
                });
        }

        @Override
        public void search(String keyword, Callback cb) {
            String key = keyword == null ? "" : keyword.trim();
            List<Member> result = new ArrayList<>();
            for (Member m : cache) {
                if (key.isEmpty() || contains(m.displayName(), key) || contains(m.userName, key)
                    || contains(m.remark, key) || contains(m.pinyin, key)
                    || contains(m.pinyin.replace("sh", "s"), key)) {
                    result.add(m);
                }
            }
            if (cb != null) {
                cb.onLoaded(result, true);
            }
        }

        private boolean contains(String s, String key) {
            return s != null && !s.isEmpty() && s.toLowerCase().contains(key.toLowerCase());
        }
    }

    /** 好友：拼音排序；群成员：角色（群主>管理员>成员）+加入时间，再拼音 */
    private static void sortMembers(List<Member> list) {
        Collections.sort(list, new Comparator<Member>() {
            @Override
            public int compare(Member a, Member b) {
                int ra = roleRank(a.groupRole);
                int rb = roleRank(b.groupRole);
                if (ra != rb) {
                    return ra - rb;
                }
                String pa = a.pinyin == null ? "" : a.pinyin;
                String pb = b.pinyin == null ? "" : b.pinyin;
                int c = pa.compareTo(pb);
                if (c != 0) {
                    return c;
                }
                return (a.displayName() == null ? "" : a.displayName())
                    .compareTo(b.displayName() == null ? "" : b.displayName());
            }
        });
    }

    private static int roleRank(String role) {
        if ("OWNER".equals(role)) {
            return 0;
        }
        if ("ADMIN".equals(role)) {
            return 1;
        }
        return 2;
    }
}
