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

        // 二维码（内容=群ID）
        ImageView qr = findViewById(R.id.gqQr);
        qrBitmap = QRCodeGenerator.generate(groupId, 720);
        if (qrBitmap != null) {
            qr.setImageBitmap(qrBitmap);
        }

        loadGroupInfo();
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
