package io.dcloud.uniplugin.memberpicker;

import com.github.promeg.pinyinhelper.Pinyin;

import java.util.HashMap;
import java.util.Map;

/** 拼音/首字母工具（TinyPinyin；结果按姓名缓存，避免重复计算） */
public class PinyinUtil {

    private static final Map<String, String> sCache = new HashMap<>();

    /** 全拼（用于排序），非汉字按原字符 */
    public static String fullPinyin(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        String cached = sCache.get(name);
        if (cached != null) {
            return cached;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Pinyin.isChinese(c)) {
                sb.append(Pinyin.toPinyin(c));
            } else {
                sb.append(c);
            }
        }
        String result = sb.toString().toLowerCase();
        if (sCache.size() > 2000) {
            sCache.clear();
        }
        sCache.put(name, result);
        return result;
    }

    /** 分组首字母：A-Z；数字/符号/其它 → # */
    public static String initial(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "#";
        }
        String p = fullPinyin(name.trim());
        if (p.isEmpty()) {
            return "#";
        }
        char c = p.charAt(0);
        if (c >= 'a' && c <= 'z') {
            return String.valueOf((char) (c - 32));
        }
        if (c >= 'A' && c <= 'Z') {
            return String.valueOf(c);
        }
        return "#";
    }
}
