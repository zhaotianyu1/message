package com.juphoon.chatbotmaap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.LruCache;

import java.io.File;

public class RcsChatbotImageUtil {

    private static LruCache<String, Bitmap> sLruCache;

    public static void init() {
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int lruMemory = maxMemory / 8;
        sLruCache = new LruCache<String, Bitmap>(lruMemory) {};
    }

    public static Bitmap createWaterMaskImage(Bitmap src, Bitmap logo) {
        if (src == null) {
            return null;
        }
        Bitmap retBmp;
        int width = src.getWidth();
        int h2 = logo.getHeight() * width / logo.getWidth();
        retBmp = Bitmap.createBitmap(width, src.getHeight() , Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(retBmp);
        Bitmap newSizeBmp2 = resizeBitmap(logo, width / 6, h2 / 6);
        canvas.drawBitmap(src, 0, 0, null);
        canvas.drawBitmap(newSizeBmp2, 5 * width / 12, src.getHeight() /2-width/12, null);
        return retBmp;
    }

    public static Bitmap resizeBitmap(Bitmap bitmap, int newWidth, int newHeight) {
        float scaleWidth = ((float) newWidth) / bitmap.getWidth();
        float scaleHeight = ((float) newHeight) / bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap bmpScale = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return bmpScale;
    }

    //获取压缩后的卡片css背景图片
    public static Bitmap getCssBackgroudBitmapFromPath(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        Bitmap bitmap = sLruCache.get(path + new File(path).lastModified());
        if (bitmap == null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
            options.inSampleSize = caculateInSampleSize(options, 300, 300);
            options.inJustDecodeBounds = false;
            Bitmap thumbBitmap = BitmapFactory.decodeFile(path, options);
            if (thumbBitmap != null) {
                bitmap = thumbBitmap;
                sLruCache.put(path + new File(path).lastModified(), bitmap);
            }
        }
        return bitmap;
    }

    /**
     * 计算采样率
     * @param options
     * @param reqWidth  压缩后的宽度
     * @param reqHeight  压缩后的高度
     * @return 缩放截取正中部分后的位图。
     */
    private static int caculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth; //原始图片宽
        int height = options.outHeight; //原始图片高
        int inSampleSize = 1; //采样率
        if (width > reqWidth || height > reqHeight) {
            int widthRadio = Math.round(width * 1.0f / reqWidth);
            int heightRadio = Math.round(height * 1.0f / reqHeight);
            inSampleSize = Math.max(widthRadio, heightRadio);
        }
        return inSampleSize;
    }
}
