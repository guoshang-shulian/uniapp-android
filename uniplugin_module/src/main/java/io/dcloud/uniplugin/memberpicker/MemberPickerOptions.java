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
            o.excludeIds = parseIdList(obj.getJSONArray("excludeIds"));
            o.excludeRoles = parseIdList(obj.getJSONArray("excludeRoles"));
            o.defaultSelectedIds = parseIdList(obj.getJSONArray("defaultSelectedIds"));
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
