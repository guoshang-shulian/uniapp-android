package io.dcloud.uniplugin.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.zegocloud.zimkit.common.utils.ZimkitStatusBar;
import com.zegocloud.zimkit.components.message.ui.QRCodeGenerator;
import com.zegocloud.zimkit.services.internal.ZIMKitCore;

import java.io.OutputStream;

import uni.dcloud.io.uniplugin_module.R;

/**
 * 原生「群二维码」页（与 uniapp group_qrcode 页面样式一致）：
 * 头像 + 群名 + 二维码 + 提示；群ID + 复制；保存到相册。
 */
public class GroupQrcodeActivity extends android.app.Activity {

    private String groupId = "";
    private String groupName = "";
    private Bitmap qrBitmap;

    public static void start(Context context, String groupId, String groupName) {
        Intent intent = new Intent(context, GroupQrcodeActivity.class);
        intent.putExtra("groupId", groupId == null ? "" : groupId);
        intent.putExtra("groupName", groupName == null ? "" : groupName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void start(Context context, String groupId) {
        start(context, groupId, "");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_qrcode);
        ZimkitStatusBar.setWhite(this);

        groupId = getIntent().getStringExtra("groupId");
        if (groupId == null) groupId = "";
        groupName = getIntent().getStringExtra("groupName");
        if (groupName == null) groupName = "";

        findViewById(R.id.gqBack).setOnClickListener(v -> finish());
        TextView idValue = findViewById(R.id.gqIdValue);
        idValue.setText(groupId);
        findViewById(R.id.gqCopy).setOnClickListener(v -> copyId());
        findViewById(R.id.gqSave).setOnClickListener(v -> saveQr());

        // 先用调用方带来的群名展示（秒显），再查 ZIM 细化
        if (!groupName.isEmpty()) {
            ((TextView) findViewById(R.id.gqName)).setText(groupName);
        }
        applyLocalConversationInfo();

        // 二维码：内容必须用**后端返回的 qrContent**（格式 TEMP_GROUP:GROUP_xxx）。
        // 以前是本地 QRCodeGenerator.generate(groupId)（裸 groupId）—— 违反后端约定
        // "二维码不写 TEMP_GROUP: 前缀 → 扫码解析失败或识别错误"，导致首页扫一扫扫不出群聊码。
        // 先画一版兜底（后端没回来/拿不到时不至于空白），拿到 qrContent 后再重画。
        ImageView qr = findViewById(R.id.gqQr);
        applyQrContent(qr, groupId);
        loadGroupInfo();
    }

    /**
     * 画二维码。
     * @param content 二维码内容；为空则退回 groupId（本地兜底，保证页面不空白）
     */
    private void applyQrContent(ImageView qr, String content) {
        String text = (content == null || content.isEmpty()) ? groupId : content;
        if (text == null || text.isEmpty()) {
            return;
        }
        qrBitmap = QRCodeGenerator.generate(text, 720);
        if (qrBitmap != null) {
            qr.setImageBitmap(qrBitmap);
        }
    }

    /** 本地会话缓存兜底：会话名/会话头像（局域网缓存，无网络也能显示） */
    private void applyLocalConversationInfo() {
        try {
            com.zegocloud.zimkit.services.model.ZIMKitConversation conv =
                ZIMKitCore.getInstance().getZIMKitConversation(groupId);
            if (conv == null || conv.getZimConversation() == null) {
                return;
            }
            String name = conv.getZimConversation().conversationName;
            if (name != null && !name.isEmpty()) {
                groupName = name;
                ((TextView) findViewById(R.id.gqName)).setText(name);
            }
            String avatar = conv.getZimConversation().conversationAvatarUrl;
            if (avatar != null && !avatar.isEmpty()) {
                Glide.with(this)
                    .load(avatar)
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(dp(12))))
                    .into((ImageView) findViewById(R.id.gqLogo));
            }
        } catch (Exception e) {
            android.util.Log.w("GroupQrcode", "local conversation info fail: " + e.getMessage());
        }
    }

    /** 群名/群头像：ZIM 群信息 */
    private void loadGroupInfo() {
        // ① 先问后端要权威群资料（含二维码字符串 qrContent）
        //    格式约定 TEMP_GROUP:GROUP_xxx，扫码解析靠它识别类型；本地拼裸 groupId 会被判成"无法识别"。
        loadTempGroupDetail();
        // ② 同时用 ZIM 兜名称/头像（后端没有这个群的本地记录时，ZIM 是唯一来源）
        try {
            ZIMKitCore.getInstance().zim().queryGroupInfo(groupId, (info, error) -> {
                if (error != null || info == null || info.baseInfo == null) {
                    android.util.Log.w("GroupQrcode", "queryGroupInfo fail code="
                        + (error == null ? "null" : error.code) + " msg="
                        + (error == null ? "" : error.message));
                    return;
                }
                runOnUiThread(() -> {
                    groupName = info.baseInfo.groupName == null ? "" : info.baseInfo.groupName;
                    if (!groupName.isEmpty()) {
                        ((TextView) findViewById(R.id.gqName)).setText(groupName);
                    }
                    String avatar = info.baseInfo.groupAvatarUrl;
                    if (avatar != null && !avatar.isEmpty()) {
                        Glide.with(GroupQrcodeActivity.this)
                            .load(avatar)
                            .apply(RequestOptions.bitmapTransform(new RoundedCorners(dp(12))))
                            .into((ImageView) findViewById(R.id.gqLogo));
                    }
                });
            });
        } catch (Exception e) {
            android.util.Log.w("GroupQrcode", "queryGroupInfo exception: " + e.getMessage());
        }
    }

    /**
     * 向后端要群详情：拿权威 `qrContent`（画二维码）与 `groupName`/`groupLogo`。
     *
     * <p>顺序：**先用建群登记时缓存的 qrContent**（register 接口返回里就带了，省一次请求），
     * 没有缓存（比如登记时没拿到、或这个群是旧数据）再走 `GET /buyer/social/temp-group/{groupId}`。
     * 群未登记时 GET 会失败 → 保持兜底版本，日志 tag `GroupQrcode` 能看到原因。
     */
    private void loadTempGroupDetail() {
        if (groupId == null || groupId.isEmpty()) {
            return;
        }
        // ① 建群登记时缓存的二维码字符串（最省事也最权威）
        String cached = io.dcloud.uniplugin.TestModule.getCachedTempGroupQr(groupId);
        if (cached != null && !cached.isEmpty()) {
            android.util.Log.i("GroupQrcode", "use cached qrContent=" + cached);
            ImageView qr = findViewById(R.id.gqQr);
            if (qr != null) {
                applyQrContent(qr, cached);
            }
            return;
        }
        // ② 没缓存 → 问后端要详情
        try {
            io.dcloud.uniplugin.others.RedPacketApi.get(
                io.dcloud.uniplugin.TestModule.getBusinessBaseUrl() + "/social/temp-group/" + groupId,
                new com.alibaba.fastjson.JSONObject(),
                new io.dcloud.uniplugin.others.RedPacketApi.Callback() {
                    @Override
                    public void onSuccess(com.alibaba.fastjson.JSONObject result) {
                        if (result == null) {
                            return;
                        }
                        final String qrContent = result.getString("qrContent");
                        final String name = result.getString("groupName");
                        final String logo = result.getString("groupLogo");
                        android.util.Log.i("GroupQrcode", "backend qrContent=" + qrContent
                            + " name=" + name + " logo=" + logo);
                        runOnUiThread(() -> {
                            // 用后端 qrContent 重画（TEMP_GROUP:GROUP_xxx）—— 扫码解析靠这个前缀
                            ImageView qr = findViewById(R.id.gqQr);
                            if (qr != null && qrContent != null && !qrContent.isEmpty()) {
                                applyQrContent(qr, qrContent);
                            }
                            if (name != null && !name.isEmpty()) {
                                groupName = name;
                                TextView tv = findViewById(R.id.gqName);
                                if (tv != null) {
                                    tv.setText(name);
                                }
                            }
                            if (logo != null && !logo.isEmpty()) {
                                Glide.with(GroupQrcodeActivity.this)
                                    .load(logo)
                                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(dp(12))))
                                    .into((ImageView) findViewById(R.id.gqLogo));
                            }
                        });
                    }

                    @Override
                    public void onError(int code, String message) {
                        // 后端本地无这个群（SDK 建的群没登记成功）→ 保持兜底版本，日志能定位
                        android.util.Log.w("GroupQrcode", "temp-group detail fail gid=" + groupId
                            + " code=" + code + " msg=" + message);
                    }
                });
        } catch (Exception e) {
            android.util.Log.w("GroupQrcode", "loadTempGroupDetail exception: " + e.getMessage());
        }
    }

    private void copyId() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("groupId", groupId));
                Toast.makeText(this, "已复制群ID", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "复制失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveQr() {
        if (qrBitmap == null) {
            Toast.makeText(this, "二维码未生成", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String fileName = "group_qr_" + groupId + "_" + System.currentTimeMillis() + ".png";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GroupQr");
            }
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
                return;
            }
            OutputStream out = getContentResolver().openOutputStream(uri);
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            if (out != null) {
                out.flush();
                out.close();
            }
            Toast.makeText(this, "已保存到相册", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "保存失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
