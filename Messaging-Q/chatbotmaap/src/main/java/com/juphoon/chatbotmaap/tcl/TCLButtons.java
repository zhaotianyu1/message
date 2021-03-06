package com.juphoon.chatbotmaap.tcl;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.tcl.ff.component.animer.glow.view.AllCellsGlowLayout;
import com.tcl.uicompat.ElementSoundUtils;
import com.tcl.uicompat.TCLButton;
import com.tcl.uicompat.TCLTextView;
import com.tcl.uicompat.mask.MaskUtils;

public class  TCLButtons extends TCLButton {


    public TCLButtons(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas = getTopCanvas(canvas);
        super.onDraw(canvas);
    }
    private Canvas getTopCanvas(Canvas canvas) {
        Drawable[] drawables = getCompoundDrawables();
        if (drawables != null) {
            Log.i("one","-----------1");
            Drawable drawableLeft = drawables[0];
            if (drawableLeft != null) {
                Log.i("one","-----------2");
//                float textWidth = getPaint().measureText(getText().toString());
//                int drawablePadding = getCompoundDrawablePadding();
//                int drawableWidth = 0;
//                drawableWidth = drawableLeft.getIntrinsicWidth();
//                float bodyWidth = textWidth + drawableWidth + drawablePadding;
//                setPadding(0, 0, (int)(getWidth() - bodyWidth), 0);
//                canvas.translate((getWidth() - bodyWidth) / 2, 0);
                //获取文字的宽度
                float textWidth = getPaint().measureText(getText().toString());
                int drawablePadding = getCompoundDrawablePadding();
                int drawableWidth = 0;
                drawableWidth = drawableLeft.getIntrinsicWidth();
                float bodyWidth = textWidth + drawableWidth + drawablePadding;
                int y = getWidth();
                canvas.translate((getWidth() - bodyWidth) / 2, 0);
            }
        }
//        Drawable[] drawables = getCompoundDrawables();
//
//        if (drawables == null) {
//            return canvas;
//
//        }
//
//        Drawable drawable = drawables[0];// 左面的drawable
//
//        if (drawable == null) {
//            drawable = drawables[2];// 右面的drawable
//
//        }
//// float textSize = getPaint().getTextSize(); // 使用这个会导致文字竖向排下来
//
//        float textSize = getPaint().measureText(getText().toString());
//
//        int drawWidth = drawable.getIntrinsicWidth();
//
//        int drawPadding = getCompoundDrawablePadding();
//
//        float contentWidth = textSize + drawWidth + drawPadding;
//
//        int leftPadding = (int) (getWidth() - contentWidth);
//
//        setPadding(0, 0, leftPadding, 0); // 直接贴到左边
//
//        float dx = (getWidth() - contentWidth) / 2;
//
//        canvas.translate(dx, 0);// 往右移动
//
        return canvas;

    }
}
