package com.monet.seeyou.tool;

/**
 * Created by Monet on 2015/6/16.
 */

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

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
}
