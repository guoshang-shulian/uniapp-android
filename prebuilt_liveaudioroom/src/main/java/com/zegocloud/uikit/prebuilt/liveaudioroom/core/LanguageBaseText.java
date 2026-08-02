package com.zegocloud.uikit.prebuilt.liveaudioroom.core;

public abstract class LanguageBaseText {
    public String removeSpeakerMenuDialogButton = "将 %s 移出座位";
    public String muteSpeakerMenuDialogButton = "禁言 %s";
    public String takeSeatMenuDialogButton = "上麦";
    public String leaveSeatMenuDialogButton = "下麦";
    public String cancelMenuDialogButton = "取消";
    public String memberListTitle = "观众";
    public String removeSpeakerFailedToast = "移出 %s 失败";

    public String applyToTakeSeatButton = "申请上麦";
    public String cancelTheTakeSeatApplicationButton = "取消";
    public String memberListAgreeButton = "同意";
    public String memberListDisagreeButton = "拒绝";
    public String inviteToTakeSeatMenuDialogButton = "邀请 %s 上麦";
    public String sendRequestTakeSeatToast = "你正在申请上麦，请等待确认。";

    public String you = "你";
    public String speaker = "主播";
    public String host = "房主";

    public String explainMic = "开始直播需要麦克风权限";
    public String settingsMic = "请前往系统设置开启麦克风权限。";
    public String ok = "确定";
    public String cancel = "取消";
    public String settings = "设置";
    public String confirm = "确认";

    public ZegoDialogInfo leaveSeatDialogInfo = new ZegoDialogInfo("下麦", "Are you sure to leave the seat?",
        "取消", "确认");
    public ZegoDialogInfo removeSpeakerFromSeatDialogInfo = new ZegoDialogInfo("去掉说话人",
        "确认想把 %s 下麦?", "取消", "确认");
    public ZegoDialogInfo receivedCoHostInvitationDialogInfo = new ZegoDialogInfo("邀请",
        "主人邀请你上麦", "取消", "确认");
    public ZegoDialogInfo leaveRoomConfirmDialogInfo = new ZegoDialogInfo("离开房间", "您确认?", "取消", "确认");
}
