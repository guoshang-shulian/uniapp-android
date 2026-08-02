package com.zegocloud.uikit.prebuilt.liveaudioroom.core;

import com.zegocloud.uikit.prebuilt.liveaudioroom.ZegoUIKitPrebuiltLiveAudioRoomConfig;

/**
 * @deprecated use {@link ZegoUIKitPrebuiltLiveAudioRoomConfig#translationText} instead
 */
@Deprecated
public class ZegoInnerText {

    public String removeSpeakerMenuDialogButton = "%s 下麦";
    public String muteSpeakerMenuDialogButton = "禁言 %s";
    public String takeSeatMenuDialogButton = "上麦";
    public String leaveSeatMenuDialogButton = "下麦";
    public String cancelMenuDialogButton = "取消";
    public String memberListTitle = "公众";
    public String removeSpeakerFailedToast = "%s 下麦失败";
    public String applyToTakeSeatButton = "申请上麦";
    public String cancelTheTakeSeatApplicationButton = "取消";
    public String memberListAgreeButton = "同意";
    public String memberListDisagreeButton = "取消";
    public String inviteToTakeSeatMenuDialogButton = "邀请 %s 上麦";
    public String sendRequestTakeSeatToast = "您已申请上麦，请稍等.";

    public ZegoDialogInfo leaveSeatDialogInfo = new ZegoDialogInfo("下麦", "确认下麦?",
        "取消", "同意");
    public ZegoDialogInfo removeSpeakerFromSeatDialogInfo = new ZegoDialogInfo("去掉下麦",
        "让 %s 下麦?", "取消", "同意");
    public ZegoDialogInfo receivedCoHostInvitationDialogInfo = new ZegoDialogInfo("邀请",
        "房主邀请上麦", "取消", "同意");
}
