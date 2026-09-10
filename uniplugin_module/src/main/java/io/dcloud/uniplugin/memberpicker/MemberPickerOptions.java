package io.dcloud.uniplugin.memberpicker;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 成员选择器参数（uniapp / 原生内部共用） */
public class MemberPickerOptions {

    public String requestId = "";
    public String title = "选择成员";
    public String dataSource = "GROUP_MEMBERS"; // GROUP_MEMBERS | FRIENDS | CUSTOM
    public String conversationId = "";
    public String mode = "single";              // single | multi
    public int maxCount = 0;                    // 0=不限（multi）
    public List<String> excludeIds = new ArrayList<>();     // 裸 memberId
    public List<String> excludeRoles = new ArrayList<>();   // OWNER | ADMIN
    public List<String> defaultSelectedIds = new ArrayList<>();
    public String scene = "";
    public boolean readOnly = false;
    public boolean searchable = true;
    public String present = "page";             // page | sheet
    /** 拉人场景：自动把该群已有成员标为“已在群中（已选+禁用）”，避免重复拉人 */
    public String excludeGroupId = "";
    /** 场景动作：INVITE / KICK / MENTION / ""（仅选择，动作由外部执行） */
    public String action = "";
    /** 是否按首字母分组（好友 true；群成员 false） */
    public boolean grouping = false;
    /** 底部确认按钮文案（多选时显示；默认"确定"） */
    public String confirmText = "确定";
    /** 仅显示被禁言的群成员（禁言管理/"解除禁言"场景） */
    public boolean muteOnly = false;
    /** 预览模式：使用内置测试数据（张三/李四/王五/赵六/石昊） */
    public boolean previewMock = false;
    /** CUSTOM 数据源：外部传入成员列表（如后端禁言列表） */
    public List<Member> customMembers = new ArrayList<>();
    /**
     * 只读模式下（readOnly=true）点击条目是否仍然回调。
     * 场景：「社群信息 → 社群成员」查看成员资料 —— 只读、单选、点人看资料，不返回选择结果。
     * 默认 false：只读列表点击无响应（历史行为）。
     */
    public boolean returnOnPick = false;

    public static MemberPickerOptions fromJson(String json) {
        MemberPickerOptions o = new MemberPickerOptions();
        if (json == null || json.isEmpty()) {
            return o;
        }
        try {
            JSONObject obj = JSON.parseObject(json);
            o.requestId = obj.getString("requestId") == null ? "" : obj.getString("requestId");
            o.title = obj.getString("title") == null ? "选择成员" : obj.getString("title");
            o.dataSource = obj.getString("dataSource") == null ? "GROUP_MEMBERS" : obj.getString("dataSource");
            o.conversationId = obj.getString("conversationId") == null ? "" : obj.getString("conversationId");
            o.mode = obj.getString("mode") == null ? "single" : obj.getString("mode");
            o.maxCount = obj.getIntValue("maxCount");
            o.scene = obj.getString("scene") == null ? "" : obj.getString("scene");
            o.readOnly = obj.getBooleanValue("readOnly");
            o.searchable = obj.getBoolean("searchable") == null || obj.getBoolean("searchable");
            o.present = obj.getString("present") == null ? "page" : obj.getString("present");
            o.excludeGroupId = obj.getString("excludeGroupId") == null ? "" : obj.getString("excludeGroupId");
            o.action = obj.getString("action") == null ? "" : obj.getString("action");
            o.excludeIds = parseIdList(obj.getJSONArray("excludeIds"));
            o.excludeRoles = parseIdList(obj.getJSONArray("excludeRoles"));
            o.defaultSelectedIds = parseIdList(obj.getJSONArray("defaultSelectedIds"));
            // 好友默认按首字母分组；群成员默认不分组
            o.grouping = obj.containsKey("grouping")
                ? obj.getBooleanValue("grouping")
                : "FRIENDS".equals(o.dataSource);
            o.confirmText = obj.getString("confirmText") == null ? "确定" : obj.getString("confirmText");
            o.muteOnly = obj.getBooleanValue("muteOnly");
            o.previewMock = obj.getBooleanValue("previewMock");
            o.returnOnPick = obj.getBooleanValue("returnOnPick");
            JSONArray arr = obj.getJSONArray("customMembers");
            if (arr != null) {
                for (int i = 0; i < arr.size(); i++) {
                    try {
                        JSONObject m = arr.getJSONObject(i);
                        Member mm = new Member();
                        mm.memberId = m.getString("memberId") == null ? "" : m.getString("memberId");
                        mm.zimUserId = m.getString("zimUserId") == null ? "" : m.getString("zimUserId");
                        mm.userName = m.getString("userName") == null ? "" : m.getString("userName");
                        mm.avatarUrl = m.getString("avatarUrl") == null ? "" : m.getString("avatarUrl");
                        mm.groupRole = m.getString("groupRole") == null ? "" : m.getString("groupRole");
                        mm.isMuted = m.getBooleanValue("isMuted");
                        o.customMembers.add(mm);
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return o;
    }

    public boolean isMulti() {
        return "multi".equals(mode);
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    private static List<String> parseIdList(JSONArray arr) {
        List<String> list = new ArrayList<>();
        if (arr == null) {
            return list;
        }
        for (int i = 0; i < arr.size(); i++) {
            String v = arr.getString(i);
            if (v != null) {
                list.add(Member.stripZimPrefix(v));
            }
        }
        return list;
    }
}
