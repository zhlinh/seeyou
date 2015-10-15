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
        // 只获取wifi地址
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);//获取WifiManager
        //检查wifi是否开启
        if (wifiManager.isWifiEnabled()) { // 没开启wifi时,ip地址为0.0.0.0
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            // wifiInfo.getSSID得到的SSID会带双引号
            return (wifiInfo == null) ? "NULL" : wifiInfo.getSSID().substring(1, wifiInfo.getSSID().length()-1);
        }
        return "NULL";
    }

     public static int getRSSI(Context context) {
        // 只获取wifi地址
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);//获取WifiManager
        //检查wifi是否开启
        if (wifiManager.isWifiEnabled()) { // 没开启wifi时,ip地址为0.0.0.0
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            return  (wifiInfo == null) ? 0 : wifiInfo.getRssi();
        }
         return  0;
    }

     public static String getServerAddress(Context context) {
        // 只获取wifi地址
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);//获取WifiManager
        //检查wifi是否开启
        if (wifiManager.isWifiEnabled()) { // 没开启wifi时,ip地址为0.0.0.0
            DhcpInfo dhcpinfo = wifiManager.getDhcpInfo();
            return (dhcpinfo == null) ? "NULL" : Util.formatIpAddress(dhcpinfo.serverAddress);
        }
        return "NULL";
    }

    // 将int格式的IP地址转换为字符串格式
    public static String formatIpAddress(int ip) {
        return (ip & 0xFF)+ "." + ((ip >> 8 ) & 0xFF) + "." + ((ip >> 16 ) & 0xFF)
                    +"."+((ip >> 24 ) & 0xFF);
    }
}
