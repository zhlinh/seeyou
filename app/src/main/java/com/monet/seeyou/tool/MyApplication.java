package com.monet.seeyou.tool;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.monet.seeyou.model.UdpMessage;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by Monet on 2015/6/16.
 * 用来存储设备识别信息
 */

public class MyApplication extends Application {
    public static MyApplication appInstance;//单实例模式--static
    private String localIp;//本地的ip地址
    private String deviceCode;//手机的唯一标识码

    public static String iconPath;

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化, 获取IP地址，获取实例本身，获取头像地址路径，获取设备识别码
        appInstance = this;
        localIp = getLocalIpAddress();
        //getFilesDir是Android内部函数
        iconPath = getFilesDir() + "/";
        initDeviceCode();
    }

    // 需要import .seeyou.model.UdpMessage
    public UdpMessage generateMyMessage(String msg,int type) {
        UdpMessage message = new UdpMessage();
        message.setType(type);
        message.setSenderName(getMyName());
        message.setDestIp("");
        message.setMsg(msg);
        message.setDeviceCode(getDeviceCode());
        message.setOwn(true);
        message.setSendTime(Util.getDate());
        return message;
    }

    /**
     * 得到本机IP地址
     * @return
     */
    private String getLocalIpAddress(){
        // 只获取wifi地址
        WifiManager wifiManage = (WifiManager) appInstance.getSystemService(Context.WIFI_SERVICE);//获取WifiManager
        //检查wifi是否开启
        if(wifiManage.isWifiEnabled()) { // 没开启wifi时,ip地址为0.0.0.0
            WifiInfo wifiInfo= wifiManage.getConnectionInfo();
            return Util.formatIpAddress(wifiInfo.getIpAddress());
        } else {
            try {
                for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                    NetworkInterface intf = en.nextElement();
                    for (Enumeration<InetAddress> enumIpAddr = intf
                            .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()) {
                            return inetAddress.getHostAddress().toString();
                        }
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
                Toast.makeText(this, "获取本机IP地址失败", Toast.LENGTH_SHORT).show();
            }
        }
        return null;
    }

    /**
     * 获取设备唯一标识
     */
    private void initDeviceCode(){
        TelephonyManager telephonyManager=(TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        setDeviceCode(telephonyManager.getDeviceId());
        Log.d("=============", "DeviceId  :" + deviceCode);
        // 如果获取不了设备唯一识别码就用Mac地址代替
        if(getDeviceCode() == null){
            WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifi.getConnectionInfo();
            setDeviceCode(info.getMacAddress());
        }
        // 获取不了Mac地址就用当前时间来代替
        if(getDeviceCode() == null){
            SharedPreferences.Editor editor = getSharedPreferences("me", MODE_PRIVATE).edit();
            editor.putString("deviceCode", System.currentTimeMillis()+"").apply();
            setDeviceCode(getSharedPreferences("me", MODE_PRIVATE).getString(deviceCode, ""));
        }
    }

    public String getMyName() {
        return getSharedPreferences("me", MODE_PRIVATE).getString("name", "无名");
    }

    public String getLocalIp(){
        if(localIp == null)
            localIp = getLocalIpAddress();
        return localIp;
    }
    public String getDeviceCode() {
        return deviceCode;
    }
    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }
}
