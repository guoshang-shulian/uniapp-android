package io.dcloud.uniplugin.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.bumptech.glide.Glide;
import com.zegocloud.zimkit.services.ZIMKit;

import java.util.ArrayList;
import java.util.List;

import im.zego.zim.callback.ZIMGroupMemberListQueriedCallback;
import im.zego.zim.entity.ZIMError;
import im.zego.zim.entity.ZIMGroupMemberInfo;
import im.zego.zim.entity.ZIMGroupMemberQueryConfig;
import im.zego.zim.enums.ZIMConversationType;
import im.zego.zim.enums.ZIMErrorCode;
import uni.dcloud.io.uniplugin_module.R;
import io.dcloud.uniplugin.TestModule;
import io.dcloud.uniplugin.memberpicker.Member;
import io.dcloud.uniplugin.memberpicker.MemberPickerBottomSheet;
import io.dcloud.uniplugin.memberpicker.MemberPickerOptions;
import io.dcloud.uniplugin.others.RedPacketApi;

/**
 * 原生发红包页（方案A）。
 * UI 按旧 pages/pagesGoEasy/envelope_sending/index 移植，新增「普通红包」，
 * 金额单位为流动积分（最小0.01，不显示￥）。
 */
public class RedPacketSendActivity extends android.app.Activity {

    /** 正式请求后端红包接口（上线后保持 false，禁止带 mock） */
    private static final boolean NATIVE_MOCK = false;

    private String conversationId = "";
    private String conversationType = "group";
    private String currentType = "fortune"; // normal | fortune | exclusive

    private String selectedMemberId = "";
    private String selectedMemberName = "";
    private String selectedMemberAvatar = "";

    private LinearLayout normalFields;
    private LinearLayout fortuneFields;
    private LinearLayout exclusiveFields;
    private TextView typeText;
    private TextView exclusiveUser;
    private TextView memberCountText;
    private TextView normalMemberCountText;
    private TextView moneyText;
    private TextView validationBar;
    private ImageView exclusiveAvatar;
    private EditText normalAmount;
    private EditText normalCount;
    private EditText fortuneCount;
    private EditText fortuneAmount;
    private EditText exclusiveAmount;
    private EditText remarkInput;
    private Button submitBtn;

    private List<ZIMGroupMemberInfo> members = new ArrayList<>();
    private boolean membersLoaded = false;
    private boolean pendingMemberDialog = false;
    private boolean isSubmitting = false;

    public static void start(Context context, String conversationId, String conversationType) {
        Intent intent = new Intent(context, RedPacketSendActivity.class);
        intent.putExtra("conversationId", conversationId == null ? "" : conversationId);
        intent.putExtra("conversationType",
            "peer".equals(conversationType) ? "peer" : "group");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_red_packet_send);
        // 沉浸式：页面 #EDEDED 背景延伸进状态栏（状态栏透明 + 深色图标），不再是单独一条色带
        io.dcloud.uniplugin.otherutils.ImmersiveBar.colorBar(this, 0xFFEDEDED, true);

        conversationId = getIntent().getStringExtra("conversationId");
        conversationType = getIntent().getStringExtra("conversationType");
        if (conversationId == null) conversationId = "";
        if (conversationType == null || conversationType.isEmpty()) conversationType = "group";

        bindViews();
        bindEvents();
        updateTypeUI();
        updateMoney();
        // 进入页面即拉群成员，显示“本群共 N 个人”（无需先点“发给谁”）
        if ("group".equals(conversationType)) {
            loadMembers();
        }
    }

    private void bindViews() {
        normalFields = findViewById(R.id.normalFields);
        fortuneFields = findViewById(R.id.fortuneFields);
        exclusiveFields = findViewById(R.id.exclusiveFields);
        typeText = findViewById(R.id.typeText);
        exclusiveUser = findViewById(R.id.exclusiveUser);
        memberCountText = findViewById(R.id.memberCountText);
        normalMemberCountText = findViewById(R.id.normalMemberCountText);
        moneyText = findViewById(R.id.moneyText);
        validationBar = findViewById(R.id.validationBar);
        exclusiveAvatar = findViewById(R.id.exclusiveAvatar);
        normalAmount = findViewById(R.id.normalAmount);
        normalCount = findViewById(R.id.normalCount);
        fortuneCount = findViewById(R.id.fortuneCount);
        fortuneAmount = findViewById(R.id.fortuneAmount);
        exclusiveAmount = findViewById(R.id.exclusiveAmount);
        remarkInput = findViewById(R.id.remarkInput);
        submitBtn = findViewById(R.id.submitBtn);
    }

    private void bindEvents() {
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        findViewById(R.id.typeRow).setOnClickListener(v -> showTypeDialog());

        View exclusiveRow = findViewById(R.id.exclusiveRow);
        if (exclusiveRow != null) {
            exclusiveRow.setOnClickListener(v -> onPickMemberClick());
        }

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateMoney();
                hideValidation();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 金额/积分最多两位小数（正常红包单个积分 / 拼手气总积分 / 专属积分）
                String raw = s == null ? "" : s.toString();
                if (raw.isEmpty() || raw.equals(".") || raw.equals("0.")) {
                    return;
                }
                int dot = raw.indexOf('.');
                if (dot >= 0 && raw.length() - dot - 1 > 2) {
                    String fixed = raw.substring(0, dot + 3);
                    s.replace(0, s.length(), fixed);
                }
            }
        };
        normalAmount.addTextChangedListener(watcher);
        normalCount.addTextChangedListener(watcher);
        fortuneCount.addTextChangedListener(watcher);
        fortuneAmount.addTextChangedListener(watcher);
        exclusiveAmount.addTextChangedListener(watcher);

        submitBtn.setOnClickListener(v -> submit());
    }

    private void showTypeDialog() {
        Dialog dialog = createBottomDialog(R.layout.dialog_red_packet_type);
        dialog.findViewById(R.id.optionNormal).setOnClickListener(v -> {
            currentType = "normal";
            updateTypeUI();
            updateMoney();
            dialog.dismiss();
        });
        dialog.findViewById(R.id.optionFortune).setOnClickListener(v -> {
            currentType = "fortune";
            updateTypeUI();
            updateMoney();
            dialog.dismiss();
        });
        dialog.findViewById(R.id.optionExclusive).setOnClickListener(v -> {
            currentType = "exclusive";
            updateTypeUI();
            updateMoney();
            dialog.dismiss();
        });
        dialog.findViewById(R.id.cancelBtn).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /** 微信式底部弹窗：透明背景 + 贴底 + 滑入滑出动画 */
    private Dialog createBottomDialog(int layoutRes) {
        Dialog dialog = new Dialog(this, R.style.BottomSheetDialogTheme);
        dialog.setContentView(layoutRes);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    private void onPickMemberClick() {
        if (!membersLoaded) {
            loadMembers();
            toast("正在加载群成员...");
        }
        if (members.isEmpty()) {
            pendingMemberDialog = true;
            return;
        }
        showMemberDialog();
    }

    private void showMemberDialog() {
        MemberPickerOptions opts = new MemberPickerOptions();
        opts.title = "选择收红包人";
        opts.dataSource = "GROUP_MEMBERS";
        opts.conversationId = conversationId;
        opts.mode = "single";
        opts.scene = "red_packet_exclusive";
        opts.present = "sheet";
        MemberPickerBottomSheet.show(this, opts, (success, canceled, list) -> {
            if (success && list != null && !list.isEmpty()) {
                Member m = list.get(0);
                selectedMemberId = m.memberId;
                selectedMemberName = m.displayName();
                selectedMemberAvatar = m.avatarUrl == null ? "" : m.avatarUrl;
                exclusiveUser.setText(selectedMemberName);
                if (exclusiveAvatar != null) {
                    exclusiveAvatar.setVisibility(View.VISIBLE);
                    if (!selectedMemberAvatar.isEmpty()) {
                        Glide.with(this).load(selectedMemberAvatar).circleCrop().into(exclusiveAvatar);
                    } else {
                        exclusiveAvatar.setImageResource(R.drawable.bg_avatar_round_gray);
                    }
                }
            }
        });
    }

    private void updateTypeUI() {
        normalFields.setVisibility(currentType.equals("normal") ? View.VISIBLE : View.GONE);
        fortuneFields.setVisibility(currentType.equals("fortune") ? View.VISIBLE : View.GONE);
        exclusiveFields.setVisibility(currentType.equals("exclusive") ? View.VISIBLE : View.GONE);
        if (currentType.equals("normal")) {
            typeText.setText("普通红包");
        } else if (currentType.equals("fortune")) {
            typeText.setText("拼手气红包");
        } else {
            typeText.setText("专属红包");
        }
    }

    private void updateMoney() {
        double value = calcTotal();
        moneyText.setText(format(value));
    }

    private double calcTotal() {
        try {
            if (currentType.equals("normal")) {
                double unit = Double.parseDouble(normalAmount.getText().toString());
                int count = parseInt(normalCount.getText().toString());
                return unit * count;
            } else if (currentType.equals("fortune")) {
                return Double.parseDouble(fortuneAmount.getText().toString());
            } else {
                return Double.parseDouble(exclusiveAmount.getText().toString());
            }
        } catch (Exception e) {
            return 0;
        }
    }

    private void loadMembers() {
        membersLoaded = true;
        ZIMGroupMemberQueryConfig config = new ZIMGroupMemberQueryConfig();
        config.count = 500;
        config.nextFlag = 0;
        ZIMKitCoreAbs.queryGroupMembers(conversationId, config, new ZIMGroupMemberListQueriedCallback() {
            @Override
            public void onGroupMemberListQueried(String groupId,
                ArrayList<ZIMGroupMemberInfo> memberList, int nextFlag, ZIMError errorInfo) {
                if (errorInfo.code != ZIMErrorCode.SUCCESS || memberList == null) {
                    membersLoaded = false;
                    return;
                }
                runOnUiThread(() -> {
                    members.clear();
                    members.addAll(memberList);
                    if (memberCountText != null) {
                        memberCountText.setText("本群共" + members.size() + "个人");
                    }
                    if (normalMemberCountText != null) {
                        normalMemberCountText.setText("本群共" + members.size() + "个人");
                    }
                    if (pendingMemberDialog && !members.isEmpty()) {
                        pendingMemberDialog = false;
                        showMemberDialog();
                    }
                });
            }
        });
    }

    private void submit() {
        if (isSubmitting) {
            return;
        }
        hideValidation();

        int count;
        double total;
        if (currentType.equals("normal")) {
            double unit = parseDouble(normalAmount.getText().toString());
            count = parseInt(normalCount.getText().toString());
            if (TextUtils.isEmpty(normalCount.getText().toString())) {
                showValidation("未填写【红包个数】");
                return;
            }
            if (TextUtils.isEmpty(normalAmount.getText().toString())) {
                showValidation("未填写【单个积分】");
                return;
            }
            if (count < 1 || count > 500) {
                showValidation("红包个数需在1~500之间");
                return;
            }
            if (unit < 0.01) {
                showValidation("单个积分不能小于0.01");
                return;
            }
            total = unit * count;
            if (total < 0.01) {
                showValidation("总积分不能小于0.01");
                return;
            }
        } else if (currentType.equals("fortune")) {
            count = parseInt(fortuneCount.getText().toString());
            total = parseDouble(fortuneAmount.getText().toString());
            if (TextUtils.isEmpty(fortuneCount.getText().toString())) {
                showValidation("未填写【红包个数】");
                return;
            }
            if (TextUtils.isEmpty(fortuneAmount.getText().toString())) {
                showValidation("未填写【总金额】");
                return;
            }
            if (count < 1 || count > 500) {
                showValidation("红包个数需在1~500之间");
                return;
            }
            if (total < 0.01 || total / count < 0.01) {
                showValidation("总积分/单份不能小于0.01");
                return;
            }
        } else {
            if (selectedMemberId.isEmpty()) {
                showValidation("未选择【收红包人】");
                return;
            }
            if (TextUtils.isEmpty(exclusiveAmount.getText().toString())) {
                showValidation("未填写【积分金额】");
                return;
            }
            count = 1;
            total = parseDouble(exclusiveAmount.getText().toString());
            if (total < 0.01) {
                showValidation("积分不能小于0.01");
                return;
            }
        }

        if (total > 10000) {
            showValidation("单个红包金额不能大于10000积分");
            return;
        }

        JSONObject params = new JSONObject();
        params.put("groupId", conversationId);
        params.put("conversationType", conversationType);
        params.put("type", currentType);
        params.put("totalPoints", String.format("%.2f", total));
        // unitPoints 仅 normal/exclusive 必填；fortune 不传
        if (currentType.equals("normal") || currentType.equals("exclusive")) {
            params.put("unitPoints", parseDouble(currentType.equals("normal")
                ? normalAmount.getText().toString() : exclusiveAmount.getText().toString()));
        }
        params.put("count", currentType.equals("exclusive") ? 1 : count);
        params.put("toUserId", currentType.equals("exclusive") ? selectedMemberId : "");
        params.put("remark", remarkInput.getText().toString().isEmpty()
            ? "恭喜发财，大吉大利" : remarkInput.getText().toString());
        params.put("clientRequestId", "rp_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 10000));
        params.put("expireSeconds", 86400);

        isSubmitting = true;
        submitBtn.setEnabled(false);
        submitBtn.setText("发送中...");
        System.out.println("[RedPacketSend] submit type=" + currentType + " total="
            + String.format("%.2f", total) + " conversationId=" + conversationId);

        try {
            if (NATIVE_MOCK) {
                JSONObject mock = new JSONObject();
                mock.put("redPacketId", "RP_MOCK_" + System.currentTimeMillis());
                mock.put("type", currentType);
                mock.put("totalPoints", total);
                mock.put("unitPoints", params.getDouble("unitPoints"));
                mock.put("count", params.getIntValue("count"));
                mock.put("toUserId", params.getString("toUserId"));
                mock.put("toUserName", params.getString("toUserName"));
                mock.put("remark", params.getString("remark"));
                mock.put("status", "active");
                mock.put("drawCount", 0);
                mock.put("myDrawPoints", 0.0);
                mock.put("expireAt", System.currentTimeMillis() + 86400L * 1000);
                resetSubmitting();
                onCreated(mock);
                return;
            }

            RedPacketApi.create(params, new RedPacketApi.Callback() {
                @Override
                public void onSuccess(JSONObject result) {
                    runOnUiThread(() -> {
                        resetSubmitting();
                        onCreated(result);
                    });
                }

                @Override
                public void onError(int code, String message) {
                    runOnUiThread(() -> {
                        resetSubmitting();
                        String msg = message == null || message.isEmpty()
                            ? ("错误码 " + code) : message;
                        System.out.println("[RedPacketSend] create error code=" + code + " msg=" + msg);
                        if (code == -1 && msg.toLowerCase().contains("timeout")) {
                            // 超时：后端可能已扣积分/已创建（clientRequestId 幂等）；避免用户重复提交
                            showValidation("发送超时：若积分已扣除，请勿重复提交，稍后在红包详情确认");
                            toast("发送超时，请勿重复提交");
                        } else {
                            showValidation("发送失败：" + msg);
                            toast(msg);
                        }
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            resetSubmitting();
            toast("发送失败：" + e.getMessage());
        }
    }

    private void resetSubmitting() {
        isSubmitting = false;
        if (submitBtn != null) {
            submitBtn.setEnabled(true);
            submitBtn.setText("塞积分进红包");
        }
    }

    private void showValidation(String message) {
        if (validationBar != null) {
            validationBar.setText(message);
            validationBar.setVisibility(View.VISIBLE);
        } else {
            toast(message);
        }
    }

    private void hideValidation() {
        if (validationBar != null) {
            validationBar.setVisibility(View.GONE);
        }
    }

    private void onCreated(JSONObject detail) {
        JSONObject payload = new JSONObject();
        payload.put("version", 1);
        payload.put("cardType", "red_packet");
        payload.put("conversationType", conversationType);
        payload.put("conversationId", conversationId);

        JSONObject sender = new JSONObject();
        sender.put("userId", TestModule.getLocalUserId());
        sender.put("userName", TestModule.getLocalUserName());
        sender.put("avatarUrl", TestModule.getLocalUserAvatar());
        payload.put("sender", sender);
        payload.put("createdAt", System.currentTimeMillis());

        JSONObject d = detail == null ? new JSONObject() : detail;
        // 后端 create 可能返回 id 而不是 redPacketId → 统一为 redPacketId（详情页/领取依赖它）
        if (d.getString("redPacketId") == null || d.getString("redPacketId").isEmpty()) {
            String id2 = d.getString("id");
            if (id2 != null && !id2.isEmpty()) {
                d.put("redPacketId", id2);
            }
        }
        d.put("type", currentType);
        d.put("totalPoints", d.getDoubleValue("totalPoints"));
        d.put("unitPoints", currentType.equals("normal") ? parseDouble(normalAmount.getText().toString()) : 0);
        d.put("count", currentType.equals("exclusive") ? 1 : parseInt(
            currentType.equals("normal") ? normalCount.getText().toString() : fortuneCount.getText().toString()));
        d.put("toUserId", currentType.equals("exclusive") ? selectedMemberId : "");
        d.put("toUserName", currentType.equals("exclusive") ? selectedMemberName : "");
        d.put("remark", remarkInput.getText().toString().isEmpty()
            ? "恭喜发财，大吉大利" : remarkInput.getText().toString());
        // 状态严格以后端返回为准（不自行默认 active，避免误导）
        if (d.getString("status") != null) {
            d.put("status", d.getString("status"));
        }
        d.put("drawCount", d.getIntValue("drawCount"));
        d.put("myDrawPoints", d.getDoubleValue("myDrawPoints"));
        payload.put("detail", d);

        ZIMConversationType type = "peer".equals(conversationType)
            ? ZIMConversationType.PEER : ZIMConversationType.GROUP;
        ZIMKit.sendCustomMessage(payload.toJSONString(), 4, conversationId, type, error -> {
            runOnUiThread(() -> {
                if (error != null && error.code != ZIMErrorCode.SUCCESS) {
                    toast(error.message);
                } else {
                    toast("红包已发出");
                    finish();
                }
            });
        });
    }

    private int parseInt(String value) {
        try {
            return (int) Double.parseDouble(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private String format(double value) {
        return String.format("%.2f", value);
    }

    private String stripUserPrefix(String zimUserId) {
        if (zimUserId == null) return "";
        return zimUserId.startsWith("user_") ? zimUserId.substring(5) : zimUserId;
    }

    private void toast(String message) {
        if (message == null) return;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /** 隔离 ZIMKitCore 依赖，避免 Activity 直接 import 内部类 */
    private static class ZIMKitCoreAbs {
        static void queryGroupMembers(String groupId, ZIMGroupMemberQueryConfig config,
            ZIMGroupMemberListQueriedCallback callback) {
            com.zegocloud.zimkit.services.internal.ZIMKitCore.getInstance().zim()
                .queryGroupMemberList(groupId, config, callback);
        }
    }
}
