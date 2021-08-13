package com.juphoon.chatbotmaap.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.juphoon.chatbotmaap.R;

public class RoundLayoutView extends RelativeLayout {

    private int radius;

    public RoundLayoutView(Context context) {
        super(context);
    }

    public RoundLayoutView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RoundLayoutView);
        radius = (int) ta.getDimension(0,0);
    }

    public RoundLayoutView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        Path path = new Path();
        int w = this.getWidth();
        int h = this.getHeight();
        float[] rids = {radius, radius, radius, radius, radius, radius, radius, radius};
        path.addRoundRect(new RectF(0, 0, w, h), rids, Path.Direction.CW);
        canvas.clipPath(path);
        super.dispatchDraw(canvas);
    }
}
