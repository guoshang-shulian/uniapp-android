package com.cool.dianshang.data;

import java.util.ArrayList;
import java.util.List;

public class PrivacyData {

    // 允许使用定位信息
    public boolean canReadLocation = true;
    // 经度
    public double longitude = 0.00;
    // 纬度
    public double latitude = 0.00;
    // 是否允许SDK主动使用手机硬件参数
    public boolean canUsePhoneState = true;
    // imei
    public String imei = "";
    // 是否允许主动获取AndroidID
    public boolean canUseAndroidId = true;
    // AndroidID
    public String androidId = "";
    // 是否允许主动获取MAC_ADDRESS
    public boolean canUseMacAddress = true;
    // MAC_ADDRESS
    public String mac = "";
    // 是否允许主动获取OAID
    public boolean canUseOaid = true;
    // OAID
    public String oaid = "";
    // GAID
    public boolean canUseGaid = true;
    // GAID
    public String gaid = "";
    // 是否允许SDK使用'ACCESS_NETWORK_STATE'权限
    public boolean canUseNetworkState = true;
    // 是否允许SDK使用存储权限
    public boolean canUseStoragePermission = true;
    // 是否允许SDK主动读取应用安装列表
    public boolean canReadInstalledPackages = true;
    // 已安装的应用列表
    public List<String> installedPackages = new ArrayList<>();
    // 是否允许SDK在申明和授权了的情况下使用录音权限
    public boolean canRecordAudio = true;
    // 是否允许SDK主动获取BootID
    public boolean canReadBootId = true;
    // 是否允许SDK主动附近的Wifi列表
    public boolean canReadNearbyWifiList = true;
    // 是否允许获取传感器信息
    public boolean canUseSensor = true;
    // 是否允许 SDK 主动获取运营商信息
    public boolean canUseSimOperator = true;
    // 当 canUseSimOperator==false 时，可传入运营商编码
    public String simOperatorCode = "";
    // 当 canUseSimOperator==false 时，可传入运营商名称
    public String simOperatorName = "";

}
