package io.dcloud.uniplugin.others;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;

public class SPUtil {

    private static final String FILE_NAME = "DemoSPFile";
    private static final String KEY_IS_AGREE_PRIVACY = "is_agree_privacy";
    private static final String KEY_IS_PERSONAL_RECOMMEND = "is_personal_recommend";
    private static final String KEY_REWARD_CACHE = "is_reward_cache";
    private static final String KEY_UID = "uid";
    private static final String KEY_PRIVACY = "privacy";

    public static SPUtil getInstance() {
        return InstanceHolder.instance;
    }

    private SharedPreferences sp;

    private Context context;

    private SharedPreferences getSp() {
        if (sp == null) {
           sp = context.getApplicationContext().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        }
        return sp;
    }

    /**
     * 是否已同意隐私政策
     */
    public boolean isAgreePrivacy() {
        return getSp().getBoolean(KEY_IS_AGREE_PRIVACY, false);
    }

    /**
     * 已同意隐私政策
     */
    public void onAgreePrivacy() {
        getSp().edit().putBoolean(KEY_IS_AGREE_PRIVACY, true).apply();
    }

    /**
     * 个性化推荐
     */
    public boolean isPersonalRecommend() {
        return getSp().getBoolean(KEY_IS_PERSONAL_RECOMMEND, false);
    }

    /**
     * 个性化推荐
     */
    public void isPersonalRecommend(boolean val) {
        getSp().edit().putBoolean(KEY_IS_PERSONAL_RECOMMEND, val).apply();
    }

    /**
     * 是否开启激励视频预加载
     */
    public boolean isRewardCache() {
        return getSp().getBoolean(KEY_REWARD_CACHE, false);
    }

    /**
     * 激励视频预加载状态改变
     */
    public void isRewardCache(boolean val) {
        getSp().edit().putBoolean(KEY_REWARD_CACHE, val).apply();
    }

    /**
     * 获取用户ID
     */
    public String getUID() {
        return getSp().getString(KEY_UID, null);
    }

    public void setContext(Context xcontext){
        context = xcontext;
    }

    /**
     * 设置用户ID
     */
    public void setUid(String uid) {
        getSp().edit().putString(KEY_UID, uid).apply();
    }

    /**
     * 获取隐私信息配置
     */
    public PrivacyData getPrivacy() {
        String string = getSp().getString(KEY_PRIVACY, null);
        if (TextUtils.isEmpty(string)) {
            return new PrivacyData();
        }
        return new Gson().fromJson(string, PrivacyData.class);
    }

    /**
     * 更新隐私信息配置
     */
    public void setPrivacy(PrivacyData privacy) {
        getSp().edit().putString(KEY_PRIVACY, new Gson().toJson(privacy)).apply();
    }

    private static class InstanceHolder {
        private static final SPUtil instance = new SPUtil();
    }

}
