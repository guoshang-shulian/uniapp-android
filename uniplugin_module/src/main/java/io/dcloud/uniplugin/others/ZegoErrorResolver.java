package io.dcloud.uniplugin.others;
import com.alibaba.fastjson.JSONObject;

public class ZegoErrorResolver {

    public static JSONObject resolve(int errorCode, String defaultMessage) {
        JSONObject errorObj = new JSONObject();
        errorObj.put("code", errorCode);
        errorObj.put("rawMessage", defaultMessage);

        // Default recovery action
        String action = "SHOW_TOAST";
        String userFriendlyMsg = "聊天服务出现异常，请稍后重试";

        switch (errorCode) {
            case 6000101:
                action = "RE_AUTHENTICATE";
                userFriendlyMsg = "认证授权失败，请重新登录商家系统。";
                break;
            case 6000103:
            case 6000106:
                // Token issues -> Trigger automated silent background token update
                action = "REFRESH_TOKEN";
                userFriendlyMsg = "安全连接凭证已过期，正在自动修复...";
                break;
            case 6000108:
                action = "REFRESH_TOKEN";
                userFriendlyMsg = "安全连接生命周期过短，正在重新签名链路...";
                break;
            case 6000104:
            case 6000105:
                action = "NETWORK_RETRY";
                userFriendlyMsg = "当前网络环境不稳定，请检查您的移动网络或Wi-Fi连接。";
                break;
            case 6000111:
            case 6000123:
            case 6000124:
                // Account collision / Multi-account issue
                action = "FORCE_LOGOUT_CLEAN";
                userFriendlyMsg = "检测到多重设备账号冲突，系统正在为您执行安全登出。";
                break;
            case 6000121:
            case 6000122:
                action = "FORCE_RELOGIN";
                userFriendlyMsg = "您尚未登录聊天子系统，请重新加载会话通道。";
                break;
            default:
                if (errorCode == 0) {
                    action = "SUCCESS";
                    userFriendlyMsg = "操作成功";
                }
                break;
        }

        errorObj.put("action", action);
        errorObj.put("message", userFriendlyMsg);
        return errorObj;
    }
}
