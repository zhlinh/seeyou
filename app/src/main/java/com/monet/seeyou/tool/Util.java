package com.monet.seeyou.tool;

/**
 * Created by Monet on 2015/6/16.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.text.DecimalFormat;
import java.util.Calendar;

/**
 * 存放一些工具函数
 *
 */
public class Util {
    // RSSI的粗略评定标准
    private static final int RARE = -80;
    private static final int MEDIUM_RARE = -60;
    private static final int MEDIUM_WELL = -40;
    private static final int WELL_DONE = -20;
    // 与RSSI相对应的距离
    private static final String DIST_1 = " 0- 5m";
    private static final String DIST_2 = " 5-10m";
    private static final String DIST_3 = "10-15m";
    private static final String DIST_4 = "15-20m";
    private static final String DIST_5 = "20-25m";
    //生成圆角图片
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int left = 0, top = 0, right = width, bottom = height;
            float roundPx = height/2;
            if (width > height) {
                left = (width - height)/2;
                top = 0;
                right = left + height;
                bottom = height;
            } else if (height > width) {
                left = 0;
                top = (height - width)/2;
                right = width;
                bottom = top + width;
                roundPx = width/2;
            }
            //ZLog.i(TAG, "ps:"+ left +", "+ top +", "+ right +", "+ bottom);

            Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            int color = 0xff424242;
            Paint paint = new Paint();
            Rect rect = new Rect(left, top, right, bottom);
            RectF rectF = new RectF(rect);

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(color);
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bitmap, rect, rect, paint);
            return output;
        } catch (Exception e) {
            return bitmap;
        }
    }

    public static String getDate() {
        Calendar c = Calendar.getInstance();
        DecimalFormat df = new DecimalFormat("00");

        String year = String.valueOf(c.get(Calendar.YEAR));
        String month = String.valueOf(c.get(Calendar.MONTH));
        String day = String.valueOf(c.get(Calendar.DAY_OF_MONTH) + 1);
        String hour = String.valueOf(c.get(Calendar.HOUR_OF_DAY));
        String mins = df.format(c.get(Calendar.MINUTE));
        StringBuffer sbBuffer = new StringBuffer();
        sbBuffer.append(year + "-" + month + "-" + day + " " + hour + ":" + mins);
        return sbBuffer.toString();
    }

    /**
     * 获取Wifi环境下的一些参数
     */
    public static String getSSID(Context context) {
        //获取WifiManager
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        //检查wifi是否开启
        if (wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            // wifiInfo.getSSID得到的SSID会带双引号
            return (wifiInfo == null) ? null : wifiInfo.getSSID().substring(1, wifiInfo.getSSID().length()-1);
        }
        return null;
    }

    public static String getBSSID(Context context) {
        //获取WifiManager
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        //检查wifi是否开启
        if (wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            // wifiInfo.getSSID得到的SSID会带双引号
            return (wifiInfo == null) ? null : wifiInfo.getBSSID();
        }
        return null;
    }

     // 获取所连接的AP的接收信号强度，即RSSI
     public static int getRSSI(Context context) {
        //获取WifiManager
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        //检查wifi是否开启
        if (wifiManager.isWifiEnabled()) {
            wifiManager.startScan();
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            return  (wifiInfo == null) ? -255 : wifiInfo.getRssi();
        }
        // 获取不了接收信号强度，则返回-255，因RSSI为负值
        return  -255;
    }

     public static String getServerAddress(Context context) {
        //获取WifiManager
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        //检查wifi是否开启
        if (wifiManager.isWifiEnabled()) { // 没开启wifi时,ip地址为0.0.0.0
            // 获取的是DHCP服务器的IP地址
            DhcpInfo dhcpinfo = wifiManager.getDhcpInfo();
            return (dhcpinfo == null) ? null : Util.formatIpAddress(dhcpinfo.serverAddress);
        }
        return null;
    }

    // 将int格式的IP地址转换为字符串格式
    public static String formatIpAddress(int ip) {
        return (ip & 0xFF)+ "." + ((ip >> 8 ) & 0xFF) + "." + ((ip >> 16 ) & 0xFF)
                    +"."+((ip >> 24 ) & 0xFF);
    }

    // 将RSSI转换为粗略估计的距离
    public static String rssi2Distance(int rssi) {
        if (rssi < RARE) {
            return DIST_5;
        } else if (RARE <= rssi && rssi< MEDIUM_RARE) {
            return DIST_4;
        } else if (MEDIUM_RARE <= rssi && rssi< MEDIUM_WELL) {
            return DIST_3;
        } else if (MEDIUM_WELL <= rssi && rssi< WELL_DONE) {
            return DIST_2;
        } else {
            return DIST_1;
        }
    }
}
