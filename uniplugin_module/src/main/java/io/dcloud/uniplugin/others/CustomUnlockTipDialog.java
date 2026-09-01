//package io.dcloud.uniplugin.others;
//
//import android.app.Dialog;
//import android.graphics.Color;
//import android.graphics.drawable.ColorDrawable;
//import android.os.Bundle;
//import android.view.Gravity;
//import android.view.KeyEvent;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.Window;
//import android.view.WindowManager;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.DialogFragment;
//
//import com.bumptech.glide.Glide;
//import uni.dcloud.io.uniplugin_module.R;
//import com.zj.zjsdk.api.v2.tube.ZJTubeAdItem;
//
//import java.util.Locale;
//import java.util.Objects;
//
///**
// * 自定义的短剧解锁提示对话框
// */
//public class CustomUnlockTipDialog extends DialogFragment {
//
//    private final ZJTubeAdItem adItem;
//
//    private DialogListener dialogListener;
//    private View contentView;
//
//    public CustomUnlockTipDialog(ZJTubeAdItem adItem) {
//        this.adItem = adItem;
//    }
//
//    @Override
//    public void onStart() {
//        super.onStart();
//        Dialog dialog = getDialog();
//        if (dialog == null) {
//            return;
//        }
//        Window window = dialog.getWindow();
//        if (window == null) {
//            return;
//        }
//        WindowManager.LayoutParams windowParams = window.getAttributes();
//        windowParams.dimAmount = 0.0f;
//        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
//        windowParams.gravity = Gravity.CENTER;
//        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//        window.setAttributes(windowParams);
//    }
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        contentView = inflater.inflate(R.layout.fragment_dialog_unlock_tip, container);
//        return contentView;
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//        setCancelable(false);
//        ((TextView) contentView.findViewById(R.id.tube_title)).setText(String.format(Locale.getDefault(), "%s · 第%d集", adItem.tubeName, adItem.episodeNumber));
//        ((TextView) contentView.findViewById(R.id.tube_desc)).setText(String.format(Locale.getDefault(), "观看激励视频免费解锁[%d]集", adItem.unlockEpisodeCount));
//        ImageView image = contentView.findViewById(R.id.tube_cover);
//        Glide.with(image).load(adItem.coverUrl).into(image);
//        contentView.findViewById(R.id.tube_close)
//                .setOnClickListener(v -> {
//                    dismiss();
//                    if (dialogListener != null) {
//                        dialogListener.cancel();
//                        dialogListener = null;
//                    }
//                });
//        contentView.findViewById(R.id.tube_confirm).setOnClickListener(v -> {
//            dismiss();
//            if (dialogListener != null) {
//                dialogListener.confirm();
//                dialogListener = null;
//            }
//        });
//        Objects.requireNonNull(getDialog()).setOnKeyListener((dialog, keyCode, event) -> keyCode == KeyEvent.KEYCODE_BACK);
//    }
//
//    public void setListener(DialogListener dialogListener) {
//        this.dialogListener = dialogListener;
//    }
//
//    public interface DialogListener {
//
//        void confirm();
//
//        void cancel();
//
//    }
//
//}
