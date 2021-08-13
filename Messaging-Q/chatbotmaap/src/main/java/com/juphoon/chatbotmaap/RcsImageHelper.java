package com.juphoon.chatbotmaap;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;

public class RcsImageHelper {
    public static void setImage(Context context, String filePath, ImageView imageView) {
        if (filePath.endsWith(".gif")) {
            Glide.with(context).load(filePath)
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .into(imageView);
        } else {
            Glide.with(context).load(filePath)
                    .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(imageView);
        }
    }

    public static void setIcon(Context context, String filePath, ImageView imageView) {
        if (filePath == null || imageView == null) return;
        if (filePath.endsWith(".gif")) {
            Glide.with(context).load(filePath)
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .into(imageView);
        } else {
            Glide.with(context).load(filePath)
                    .skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)
                    .apply(RequestOptions.bitmapTransform(new CircleCrop())).into(imageView);//标准圆形图片。
        }
    }
}
