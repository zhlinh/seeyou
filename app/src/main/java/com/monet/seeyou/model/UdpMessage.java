package com.monet.seeyou.model;

import android.util.Base64;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by Monet on 2015/6/16.
 * 消息的实体类，接收者根据接收到的消息的type来对消息进行相应的操作
 *
 */
public class UdpMessage {
    private String senderName; //发送者姓名
    private String destIp; //目的地ip地址
    private String msg; //信息内容
    private String sendTime; //发送时间
    private String deviceCode; //手机唯一标识码
    private String apDesc; //发送者所连接的AP描述符，目前采用的是BSSID
    private int apRssi; //发送者接收到所连接AP的信号强度RSSI
    private int type; //消息的类型
    private boolean own; //判断这条消息是否是自己发送的
    private boolean isRefreshIcon; // 判断是否为更新的头像

    public UdpMessage(){
        setSendTime(System.currentTimeMillis()+"");
    }

    public UdpMessage(String msg, boolean own){
        this();
        this.setMsg(msg);
        this.setOwn(own);
    }

    /**
     * 在接收消息的packet中获取到byte类消息，生成对应的String类消息，再生成jsonobject类
     * 然后传入jsonObject对象作为参数，生成对应的UdpMessage
     * @throws JSONException
     */
    public UdpMessage(JSONObject object) throws JSONException {
        //有可能出现中文字符的变量都要进行base64转码，避免不规则字符导致的错误unterminated string character
        senderName = new String(Base64.decode(object.getString("senderName").getBytes(),Base64.DEFAULT));
        destIp = object.getString("destIp");
        msg = new String(Base64.decode(object.getString("msg").getBytes(),Base64.DEFAULT));
        setSendTime(object.getString("sendTime"));
        setDeviceCode(object.getString("deviceCode"));
        setApDesc(object.getString("apDesc"));
        setApRssi(object.getInt("apRssi"));
        setIsRefreshIcon(object.getBoolean("isRefreshIcon"));
        type = object.getInt("type");
        object = null;//销毁
    }

    /**
     * 把UdpMessgae转换成JSONObject对象，然后转为字符串的形式传递
     * @return
     */
    public String toJOString(){
        JSONObject object = new JSONObject();
        try {
            //有可能出现中文字符的变量都要进行base64转码，避免不规则字符导致的错误unterminated string character
            object.put("senderName", Base64.encodeToString(senderName.getBytes(), Base64.DEFAULT));
            object.put("destIp", destIp);
            object.put("msg", Base64.encodeToString(msg.getBytes(),Base64.DEFAULT));
            object.put("deviceCode", getDeviceCode());
            object.put("apDesc", getApDesc());
            object.put("apRssi", getApRssi());
            object.put("isRefreshIcon", getIsRefreshIcon());
            object.put("type", type);
            object.put("sendTime", sendTime);
            return object.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";

    }

    public String getSenderName() {
        return senderName;
    }
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    public int getType() {
        return type;
    }
    public void setType(int type) {
        this.type = type;
    }
    public String getDestIp() {
        return destIp;
    }
    public void setDestIp(String destIp) {
        this.destIp = destIp;
    }
    public String getSendTime() {
        return sendTime;
    }
    public void setSendTime(String sendTime) {
        this.sendTime = sendTime;
    }
    public boolean isOwn() {
        return own;
    }
    public void setOwn(boolean own) {
        this.own = own;
    }
    public String getMsg() {
        return msg;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }
    public String getDeviceCode() {
        return deviceCode;
    }
    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }
    public String getApDesc() {
        return apDesc;
    }
    public void setApDesc(String apDesc) {
        this.apDesc= apDesc;
    }
    public int getApRssi() {
        return apRssi;
    }
    public void setApRssi(int apRssi) {
        this.apRssi = apRssi;
    }
    public boolean getIsRefreshIcon() {
        return isRefreshIcon;
    }
    public void setIsRefreshIcon(boolean isRefreshIcon) {
        this.isRefreshIcon = isRefreshIcon;
    }
}
