package io.dcloud.uniplugin.memberpicker;

import com.alibaba.fastjson.JSONObject;

/** 统一的成员模型（群成员/好友通用） */
public class Member {

    public String memberId = "";
    public String zimUserId = "";
    public String userName = "";
    public String avatarUrl = "";
    public String groupRole = "";   // OWNER | ADMIN | MEMBER（GROUP_MEMBERS 源）
    public boolean isMuted = false;
    public String remark = "";      // 好友备注名 alias
    public boolean disabled = false;
    public String disabledReason = "";
    /** 拼音/首字母（排序+分组用，不参与业务） */
    public String pinyin = "";
    public String initial = "#";

    /** 展示名：备注优先，其次昵称 */
    public String displayName() {
        if (remark != null && !remark.isEmpty()) {
            return remark;
        }
        return userName == null ? "" : userName;
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("memberId", memberId == null ? "" : memberId);
        obj.put("zimUserId", zimUserId == null ? "" : zimUserId);
        obj.put("userName", userName == null ? "" : userName);
        obj.put("displayName", displayName());
        obj.put("avatarUrl", avatarUrl == null ? "" : avatarUrl);
        obj.put("groupRole", groupRole == null ? "" : groupRole);
        obj.put("isMuted", isMuted);
        obj.put("remark", remark == null ? "" : remark);
        obj.put("disabled", disabled);
        obj.put("disabledReason", disabledReason == null ? "" : disabledReason);
        return obj;
    }

    public static String stripZimPrefix(String id) {
        if (id == null) return "";
        return id.startsWith("user_") ? id.substring("user_".length()) : id;
    }
}
