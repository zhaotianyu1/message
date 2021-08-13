package com.android.messaging.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.VideoView;

public class CustomVideoView extends VideoView {
    //声明屏幕的大小
    int width = 240;
    int height = 240;
    public CustomVideoView(Context context) {
        super(context);
    }

    public CustomVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.i("cccc","onMeasure----:");
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        if (widthMeasureSpec > 0 && heightMeasureSpec > 0) {
//            width = getDefaultSize(widthMeasureSpec, widthMeasureSpec);
//            height = getDefaultSize(heightMeasureSpec, heightMeasureSpec);
//            if (widthMeasureSpec * height  > width * heightMeasureSpec) {
//                height = width * heightMeasureSpec / widthMeasureSpec;
//            } else if (widthMeasureSpec * height  < width * heightMeasureSpec) {
//                width = height * widthMeasureSpec / heightMeasureSpec;
//            }
//        }

        //设置宽高
        int defaultWidth = getDefaultSize(getSuggestedMinimumWidth(),widthMeasureSpec);
        int defaultHeight = getDefaultSize(getSuggestedMinimumHeight(),heightMeasureSpec);
        setMeasuredDimension(defaultWidth,defaultHeight);
    }


}